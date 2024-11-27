/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Scott Ware
 *******************************************************************************/
package org.eclipse.kura.internal.ble.beacon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothTransportType;
import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconDecoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconManager;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.ble.beacon.advertisement.AdvertiserBackend;
import org.eclipse.kura.internal.ble.beacon.advertisement.DBusAdvertiserBackend;
import org.eclipse.kura.internal.ble.beacon.advertisement.HcitoolAdvertiserBackend;
import org.eclipse.kura.internal.ble.util.BTSnoopListener;
import org.eclipse.kura.internal.ble.util.BluetoothLeUtil;
import org.eclipse.kura.internal.ble.util.BluetoothProcess;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;

public class BluetoothLeBeaconManagerImpl implements BluetoothLeBeaconManager<BluetoothLeBeacon>, BTSnoopListener {

    private static final Logger logger = LogManager.getLogger(BluetoothLeBeaconManagerImpl.class);

    private static Map<String, BluetoothLeBeaconAdvertiser<BluetoothLeBeacon>> advertisers = new HashMap<>();
    private static Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>> scanners = new HashMap<>();

    private BluetoothProcess dumpProc;
    private BluetoothProcess hcitoolProc;
    private Map<BluetoothLeBeaconListener<BluetoothLeBeacon>, Class<?>> listeners;
    private CommandExecutorService executorService;
    private SystemService systemService;

    private AdvertiserBackend advertiserBackend;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        if (this.executorService == executorService) {
            this.executorService = null;
        }
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        if (this.systemService == systemService) {
            this.systemService = null;
        }
    }

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Beacon Manager...");
        this.listeners = new HashMap<>();
        try {
            this.advertiserBackend = new DBusAdvertiserBackend();
        } catch (final Exception e) {
            logger.warn("failed to start DBus advertiser backend", e);
            this.advertiserBackend = new HcitoolAdvertiserBackend(executorService);
        }

    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le Beacon Manager...");
    }

    protected BluetoothProcess execBtDump(String interfaceName) throws IOException {
        return BluetoothLeUtil.btdumpCmd(interfaceName, this.executorService, this);
    }

    protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
        return BluetoothLeUtil.hcitoolCmd(interfaceName, cmd, this.executorService, HcitoolProcessListener.instance());
    }

    @Override
    public BluetoothLeBeaconScanner<BluetoothLeBeacon> newBeaconScanner(BluetoothLeAdapter adapter,
            BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder) {
        BluetoothLeBeaconScannerImpl<BluetoothLeBeacon> scanner = new BluetoothLeBeaconScannerImpl<>(adapter, decoder,
                this);
        if (scanners.containsKey(adapter.getInterfaceName())) {
            scanners.get(adapter.getInterfaceName()).add(scanner);
        } else {
            List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>> scannerList = new ArrayList<>();
            scannerList.add(scanner);
            scanners.put(adapter.getInterfaceName(), scannerList);
        }
        return scanner;
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable {

        if (advertisers.containsKey(adapter.getInterfaceName())) {
            throw new KuraBluetoothBeaconAdvertiserNotAvailable(
                    "The Beacon Advertiser for " + adapter.getInterfaceName() + " has been already instanciated");
        }

        final BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> result = this.advertiserBackend
                .newBeaconAdvertiser(adapter, encoder);
        advertisers.put(adapter.getInterfaceName(), result);

        return result;
    }

    @Override
    public void deleteBeaconScanner(BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner) {
        String interfaceName = scanner.getAdapter().getInterfaceName();
        if (scanners.containsKey(interfaceName)) {
            scanners.get(interfaceName).remove(scanner);
        }
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser) {
        this.advertiserBackend.deleteBeaconAdvertiser(advertiser);
        advertisers.remove(advertiser.getAdapter().getInterfaceName());
    }

    public static String[] toHexStringArray(byte[] in) {
        String[] out = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = String.format("%02X", in[i]);
        }
        return out;
    }

    public void startBeaconScan(BluetoothLeAdapter adapter) throws KuraBluetoothCommandException {
        if (checkStartScanCondition(adapter.getInterfaceName())) {
            logger.info("Starting bluetooth beacon scan on {}", adapter.getInterfaceName());
            try {
                // Start scanning
                if (!adapter.isDiscovering()) {
                    if (systemService.isLegacyBluetoothBeaconScan()) {
                        logger.info("Starting legacy bluetooth beacon scan on {}", adapter.getInterfaceName());
                        this.hcitoolProc = execHcitool(adapter.getInterfaceName(), "lescan-passive", "--duplicates");
                    } else {
                        logger.info("Starting bluetooth beacon scan on {}", adapter.getInterfaceName());
                        adapter.setDiscoveryFilter(null, 0, 0, BluetoothTransportType.LE, false);
                        adapter.startDiscovery();
                    }
                }
                this.dumpProc = execBtDump(adapter.getInterfaceName());
            } catch (IOException | KuraBluetoothDiscoveryException e) {
                throw new KuraBluetoothCommandException(e, "Start bluetooth beacon scan failed");
            }
        }
    }

    public void stopBeaconScan(BluetoothLeAdapter adapter) {
        // Stop scan on interface only if there is only one scanner that is scanning...
        if (checkStopScanCondition(adapter.getInterfaceName())) {
            // Stop scanning
            boolean isHcitoolStopped = true;
            if (systemService.isLegacyBluetoothBeaconScan()) {
                logger.info("Stopping legacy bluetooth beacon scan on {}", adapter.getInterfaceName());
                if (this.hcitoolProc != null) {
                    this.hcitoolProc.destroy();
                }
                isHcitoolStopped = BluetoothLeUtil.stopHcitool(adapter.getInterfaceName(), this.executorService,
                        "lescan-passive", "--duplicates");
            } else {
                logger.info("Stopping bluetooth beacon scan on {}", adapter.getInterfaceName());
                try {
                    adapter.stopDiscovery();
                    adapter.setDiscoveryFilter(null, 0, 0, BluetoothTransportType.AUTO, false);
                } catch (KuraBluetoothDiscoveryException e) {
                    logger.warn("Stop bluetooth beacon scan failed");
                }
            }
            if (this.dumpProc != null) {
                this.dumpProc.destroyBTSnoop();
            }
            boolean isBtdumpStopped = BluetoothLeUtil.stopBtdump(adapter.getInterfaceName(), this.executorService);
            if (!isHcitoolStopped || !isBtdumpStopped) {
                logger.warn("Failed to stop bluetooth beacon scan");
            }
        }
    }

    public boolean checkStopScanCondition(String interfaceName) {
        // Stop scanning on given interface if only one scanner is scanning
        boolean stopScan = false;
        if (scanners.containsKey(interfaceName) && getScannersCount(interfaceName) == 1) {
            stopScan = true;
        }
        return stopScan;
    }

    public boolean checkStartScanCondition(String interfaceName) {
        // Start scanning on given interface if no one is already scanning
        boolean startScan = false;
        if (scanners.containsKey(interfaceName) && getScannersCount(interfaceName) == 0) {
            startScan = true;
        }
        return startScan;
    }

    public void addBeaconListener(BluetoothLeBeaconListener<BluetoothLeBeacon> listener, Class<?> clazz) {
        if (!this.listeners.containsKey(listener)) {
            this.listeners.put(listener, clazz);
        } else {
            logger.warn("The listener has been already registered");
        }
    }

    public void removeBeaconListener(BluetoothLeBeaconListener<BluetoothLeBeacon> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void processBTSnoopRecord(byte[] record) {
        // Extract raw advertisement data
        List<AdvertisingReportRecord> reportRecords = BluetoothLeUtil.parseLEAdvertisement(record);
        if (!reportRecords.isEmpty()) {

            // Get the active decoders
            List<BluetoothLeBeaconDecoder<BluetoothLeBeacon>> decoders = scanners.values().stream()
                    .flatMap(List::stream).filter(BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>::isScanning)
                    .map(BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>::getDecoder).distinct()
                    .collect(Collectors.toList());

            List<BluetoothLeBeacon> beacons = new ArrayList<>();
            for (AdvertisingReportRecord report : reportRecords) {
                for (BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder : decoders) {
                    BluetoothLeBeacon beacon = decoder.decode(report.getReportData());
                    if (beacon != null) {
                        beacon.setAddress(report.getAddress());
                        beacon.setRssi(report.getRssi());
                        beacons.add(beacon);
                    }
                }
            }

            // Notify listeners
            notifyListeners(beacons);
        }
    }

    private void notifyListeners(List<BluetoothLeBeacon> beacons) {
        if (!beacons.isEmpty() && !this.listeners.isEmpty()) {
            for (Entry<BluetoothLeBeaconListener<BluetoothLeBeacon>, Class<?>> entry : this.listeners.entrySet()) {
                beacons.stream().filter(beacon -> entry.getValue() == beacon.getClass()).collect(Collectors.toList())
                        .forEach(entry.getKey()::onBeaconsReceived);
            }
        }
    }

    @Override
    public void processBTSnoopErrorStream(String string) {
        throw new UnsupportedOperationException("Process of BTSnoop error stream is not supported");
    }

    private int getScannersCount(String interfaceName) {
        return scanners.get(interfaceName).stream().mapToInt(e -> e.isScanning() ? 1 : 0).sum();
    }
}
