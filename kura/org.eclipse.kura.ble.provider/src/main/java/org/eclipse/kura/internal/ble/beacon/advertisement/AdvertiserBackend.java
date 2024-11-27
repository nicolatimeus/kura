package org.eclipse.kura.internal.ble.beacon.advertisement;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;

public interface AdvertiserBackend {

    public BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable;

    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser);

    public void shutdown();
}
