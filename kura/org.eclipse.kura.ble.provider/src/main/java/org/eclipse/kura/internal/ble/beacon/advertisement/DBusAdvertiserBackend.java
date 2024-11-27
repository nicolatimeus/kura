package org.eclipse.kura.internal.ble.beacon.advertisement;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bluez.LEAdvertisement1;
import org.bluez.LEAdvertisingManager1;
import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

public class DBusAdvertiserBackend implements AdvertiserBackend {

    private final DBusConnection connection;

    private static final Logger logger = LogManager.getLogger(DBusAdvertiserBackend.class);

    public DBusAdvertiserBackend() throws DBusException {
        this.connection = DBusConnection.getConnection(DBusBusType.SYSTEM);
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable {

        return new BluetoothLeBeaconAdvertiserImpl<>(adapter, encoder);
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser) {
        try {
            advertiser.stopBeaconAdvertising();
        } catch (KuraBluetoothCommandException e) {
            logger.warn("failed to stop advertisemet", e);
        }
    }

    private static class LEAdvertisement1Impl implements LEAdvertisement1, Properties {

        private static final Collection<String> ALLOWED_INTERFACES = new HashSet<>(
                Arrays.asList("", LEAdvertisement1.class.getName()));

        private final String objectPath;

        private final Map<String, Variant<?>> properties = new HashMap<>();

        LEAdvertisement1Impl(final String objectPath) {
            this.objectPath = objectPath;
        }

        @Override
        public String getObjectPath() {
            return objectPath;
        }

        @Override
        public void Release() {
            // do nothing
        }

        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            if (!ALLOWED_INTERFACES.contains(interfaceName)) {
                return null;
            }

            return (A) properties.get(propertyName);

        }

        public void setBeaconAdvertisingInterval(final int min, final int max) {
            this.properties.put("MinInterval", new Variant<>(new UInt32(min)));
            this.properties.put("MaxInterval", new Variant<>(new UInt32(max)));
        }

        public void setBeaconAdvertisingData(final byte[] encoded) {

            final AdvertisementPacket packet = new AdvertisementPacket(encoded);

            updateProperty("ServiceUUIDs", packet.serviceUUIDs, "as");
            updateProperty("ServiceData", packet.serviceData, "a{sv}");
            updateProperty("ManufacturerData", packet.manufacturerData, "a{qv}");
            updateProperty("Data", packet.rawData, "a{yv}");

        }

        private void updateProperty(final String name, final Object value, final String signature) {
            final boolean isEmpty;

            if (value instanceof Map) {
                isEmpty = ((Map<?, ?>) value).isEmpty();
            } else if (value instanceof List) {
                isEmpty = ((Collection<?>) value).isEmpty();
            } else {
                throw new IllegalArgumentException();
            }

            if (isEmpty) {
                this.properties.remove(name);
            } else {
                this.properties.put(name, new Variant<>(value, signature));
            }

        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            return properties;
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // do nothing
        }

    }

    private enum Status {
        UNREGISTERED,
        REGISTERED,
        ADVERTISING
    }

    private class BluetoothLeBeaconAdvertiserImpl<T extends BluetoothLeBeacon>
            implements BluetoothLeBeaconAdvertiser<T> {

        private final BluetoothLeAdapter adapter;
        private final BluetoothLeBeaconEncoder<T> encoder;
        private final String path;
        private final LEAdvertisement1Impl leAdvertisement;

        private Status status = Status.UNREGISTERED;

        public BluetoothLeBeaconAdvertiserImpl(BluetoothLeAdapter adapter, BluetoothLeBeaconEncoder<T> encoder) {
            this.adapter = adapter;
            this.encoder = encoder;
            this.path = "/org/eclipse/kura/advertisement/" + System.nanoTime();
            this.leAdvertisement = new LEAdvertisement1Impl(path);
        }

        @Override
        public synchronized void startBeaconAdvertising() throws KuraBluetoothCommandException {

            if (this.status == Status.ADVERTISING) {
                return;
            }

            try {
                LEAdvertisingManager1 advertisingManager = connection.getRemoteObject("org.bluez",
                        "/org/bluez/" + adapter.getInterfaceName(), LEAdvertisingManager1.class);

                if (this.status != Status.REGISTERED) {
                    connection.exportObject(this.path, this.leAdvertisement);
                    status = Status.REGISTERED;
                }

                advertisingManager.RegisterAdvertisement(new DBusPath(this.path), Collections.emptyMap());
                status = Status.ADVERTISING;
            } catch (final Exception e) {
                try {
                    stopBeaconAdvertising();
                } catch (final Exception ex) {
                    logger.warn("Failed to cleanup beacon advertising after failure", e);
                }
                throw new KuraBluetoothCommandException(e, e.getMessage());
            }
        }

        @Override
        public synchronized void stopBeaconAdvertising() throws KuraBluetoothCommandException {

            if (this.status == Status.UNREGISTERED) {
                return;
            }

            try {
                LEAdvertisingManager1 advertisingManager = connection.getRemoteObject("org.bluez",
                        "/org/bluez/" + adapter.getInterfaceName(), LEAdvertisingManager1.class);

                if (status == Status.ADVERTISING) {
                    advertisingManager.UnregisterAdvertisement(new DBusPath(this.path));
                    status = Status.REGISTERED;
                }

                if (status == Status.REGISTERED) {
                    connection.unExportObject(path);
                    status = Status.UNREGISTERED;
                }
            } catch (final Exception e) {
                throw new KuraBluetoothCommandException(e, "Failed to stop beacon advertising: " + e.getMessage());
            }

        }

        @Override
        public void updateBeaconAdvertisingInterval(Integer min, Integer max) throws KuraBluetoothCommandException {
            this.leAdvertisement.setBeaconAdvertisingInterval(min, max);
        }

        @Override
        public void updateBeaconAdvertisingData(T beacon) throws KuraBluetoothCommandException {
            this.leAdvertisement.setBeaconAdvertisingData(this.encoder.encode(beacon));
        }

        @Override
        public BluetoothLeAdapter getAdapter() {
            return adapter;
        }

    }

    @Override
    public void shutdown() {
        try {
            this.connection.close();
        } catch (IOException e) {
            logger.warn("failed to close DBusConnection", e);
        }

    }

    private static class AdvertisementPacket {

        private final List<String> serviceUUIDs = new ArrayList<>();
        private final Map<String, Variant<byte[]>> serviceData = new LinkedHashMap<>();
        private final Map<UInt16, Variant<byte[]>> manufacturerData = new LinkedHashMap<>();
        private final Map<Byte, Variant<byte[]>> rawData = new LinkedHashMap<>();

        AdvertisementPacket(final byte[] encoded) {

            final ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);

            final int dataLen = buf.get() & 0xff;

            if (buf.remaining() < dataLen) {
                throw new IllegalArgumentException("got data len " + dataLen + " buffer size " + buf.remaining() + 1);
            }

            while (buf.hasRemaining()) {
                final int len = buf.get() & 0xff;

                if (len == 0) {
                    return;
                }

                if (len > buf.remaining()) {
                    throw new IllegalArgumentException("AD lenght out of range");
                }

                final byte ty = buf.get();

                final ByteBuffer adData = buf.slice().order(ByteOrder.LITTLE_ENDIAN);
                adData.limit(len - 1);
                buf.position(buf.position() + len - 1);

                handleAD(ty, adData);

            }
        }

        private void handleAD(final byte type, final ByteBuffer data) {
            switch (type) {
            case 0x01:
                // flags, ignore
                break;
            case 0x02:
            case 0x03:
                handleServiceUUIDList(16, data);
                break;
            case 0x04:
            case 0x05:
                handleServiceUUIDList(32, data);
                break;
            case 0x06:
            case 0x07:
                handleServiceUUIDList(128, data);
                break;
            case 0x16:
                handleServiceData(16, data);
                break;
            case 0x20:
                handleServiceData(32, data);
                break;
            case 0x21:
                handleServiceData(128, data);
                break;
            case (byte) 0xff:
                handleManufacturerData(data);
                break;
            default:
                handleOtherAD(type, data);
            }
        }

        private void handleManufacturerData(final ByteBuffer data) {

            final UInt16 companyId = new UInt16(data.getShort() & 0xffff);

            final byte[] manufacturerDataBytes = new byte[data.remaining()];

            data.get(manufacturerDataBytes);

            manufacturerData.put(companyId, new Variant<>(manufacturerDataBytes));
        }

        private void handleServiceData(final int sizeBits, final ByteBuffer data) {

            final int sizeBytes = sizeBits / 8;

            final byte[] uuid = new byte[sizeBytes];

            data.get(uuid);

            final byte[] serviceDataBytes = new byte[data.remaining()];

            data.get(serviceDataBytes);

            serviceData.put(uuidToHexString(uuid), new Variant<>(serviceDataBytes));

        }

        private void handleServiceUUIDList(final int sizeBits, final ByteBuffer data) {

            final int sizeBytes = sizeBits / 8;

            while (data.hasRemaining()) {
                final byte[] uuid = new byte[sizeBytes];

                data.get(uuid);

                final String asString = uuidToHexString(uuid);

                serviceUUIDs.add(asString);
            }
        }

        private String uuidToHexString(final byte[] uuid) {

            final StringBuilder result = new StringBuilder();

            for (int i = 0; i < uuid.length; i++) {
                final String asHex = Integer.toHexString(uuid[uuid.length - 1 - i] & 0xff);

                if (asHex.length() == 1) {
                    result.append('0');
                }

                result.append(asHex);

                if (uuid.length == 128 / 8 && (i == 7 || i == 11 || i == 15 || i == 19)) {
                    result.append('-');
                }

            }

            return result.toString();

        }

        private void handleOtherAD(final byte type, final ByteBuffer data) {
            final byte[] adData = new byte[data.remaining()];

            data.get(adData);

            rawData.put(type, new Variant<>(adData));
        }
    }

}
