package org.eclipse.kura.internal.ble.beacon.advertisement;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.ble.beacon.HcitoolProcessListener;
import org.eclipse.kura.internal.ble.util.BluetoothLeUtil;
import org.eclipse.kura.internal.ble.util.BluetoothProcess;

public class HcitoolAdvertiserBackend implements AdvertiserBackend {

    private static final String SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE = "Set Advertising Parameters : hcitool -i {} {}";

    private static final Logger logger = LogManager.getLogger(HcitoolAdvertiserBackend.class);

    // See Bluetooth 4.0 Core specifications (https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)
    private static final String OGF_CONTROLLER_CMD = "0x08";
    private static final String OCF_ADVERTISING_PARAM_CMD = "0x0006";
    private static final String OCF_ADVERTISING_DATA_CMD = "0x0008";
    private static final String OCF_ADVERTISING_ENABLE_CMD = "0x000a";
    private static final String CMD = "cmd";
    private static final String TWO_CHAR_REGEX = "(?<=\\G..)";

    private final CommandExecutorService executorService;

    public HcitoolAdvertiserBackend(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable {
        return new BluetoothLeBeaconAdvertiserImpl<>(adapter, encoder);
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser) {
        // nothing to do
    }

    private BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
        return BluetoothLeUtil.hcitoolCmd(interfaceName, cmd, this.executorService, HcitoolProcessListener.instance());
    }

    private void startBeaconAdvertising(String interfaceName) throws KuraBluetoothCommandException {
        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "01" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Start Advertising on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Start bluetooth beacon advertising failed");
        }
    }

    private void stopBeaconAdvertising(String interfaceName) throws KuraBluetoothCommandException {
        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "00" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Stop Advertising on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Stop bluetooth beacon advertising failed");
        }
    }

    public void updateBeaconAdvertisingInterval(Integer min, Integer max, String interfaceName)
            throws KuraBluetoothCommandException {
        checkInterval(min, max);
        // See
        // http://stackoverflow.com/questions/21124993/is-there-a-way-to-increase-ble-advertisement-frequency-in-bluez
        String[] minHex = String.format("%04X", min).split(TWO_CHAR_REGEX);
        String[] maxHex = String.format("%04X", max).split(TWO_CHAR_REGEX);

        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1], maxHex[0],
                "03", "00", "00", "00", "00", "00", "00", "00", "00", "07", "00" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Set Advertising Parameters on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Update bluetooth beacon advertising interval failed");
        }
    }

    private void checkInterval(Integer min, Integer max) {
        if (min > max) {
            throw new IllegalArgumentException("The minimum interval cannot be greater than the maximum.");
        }
        if (min < 14 || min > 65534) {
            throw new IllegalArgumentException("The minimum interval value must be between 14 and 65534.");
        }
        if (max < 14 || max > 65534) {
            throw new IllegalArgumentException("The maximum interval value must be between 14 and 65534.");
        }
    }

    public void updateBeaconAdvertisingData(BluetoothLeBeacon beacon,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder, String interfaceName)
            throws KuraBluetoothCommandException {
        String[] data = toHexStringArray(encoder.encode(beacon));
        String[] cmd = new String[3 + data.length];
        cmd[0] = CMD;
        cmd[1] = OGF_CONTROLLER_CMD;
        cmd[2] = OCF_ADVERTISING_DATA_CMD;
        for (int i = 0; i < data.length; i++) {
            cmd[i + 3] = data[i];
        }

        logger.debug("Set Advertising Data : hcitool -i {} {}", () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Set Advertising Data on interface {}", interfaceName);
        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Update bluetooth beacon advertising data failed");
        }
    }

    private class BluetoothLeBeaconAdvertiserImpl<T extends BluetoothLeBeacon>
            implements BluetoothLeBeaconAdvertiser<T> {

        private final BluetoothLeAdapter adapter;
        private final BluetoothLeBeaconEncoder<T> encoder;

        public BluetoothLeBeaconAdvertiserImpl(BluetoothLeAdapter adapter, BluetoothLeBeaconEncoder<T> encoder) {
            this.adapter = adapter;
            this.encoder = encoder;
        }

        @Override
        public BluetoothLeAdapter getAdapter() {
            return this.adapter;
        }

        @Override
        public void startBeaconAdvertising() throws KuraBluetoothCommandException {
            HcitoolAdvertiserBackend.this.startBeaconAdvertising(this.adapter.getInterfaceName());
        }

        @Override
        public void stopBeaconAdvertising() throws KuraBluetoothCommandException {
            HcitoolAdvertiserBackend.this.stopBeaconAdvertising(this.adapter.getInterfaceName());
        }

        @Override
        public void updateBeaconAdvertisingInterval(Integer min, Integer max) throws KuraBluetoothCommandException {
            HcitoolAdvertiserBackend.this.updateBeaconAdvertisingInterval(min, max, this.adapter.getInterfaceName());
        }

        @Override
        public void updateBeaconAdvertisingData(T beacon) throws KuraBluetoothCommandException {
            HcitoolAdvertiserBackend.this.updateBeaconAdvertisingData((BluetoothLeBeacon) beacon,
                    (BluetoothLeBeaconEncoder<BluetoothLeBeacon>) this.encoder, this.adapter.getInterfaceName());
        }
    }

    public static String[] toHexStringArray(byte[] in) {
        String[] out = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = String.format("%02X", in[i]);
        }
        return out;
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

}
