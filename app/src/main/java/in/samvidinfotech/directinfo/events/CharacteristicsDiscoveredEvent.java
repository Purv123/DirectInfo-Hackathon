package in.samvidinfotech.directinfo.events;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.Collection;

/**
 * Created by samvidmistry on 27/2/18.
 */

public final class CharacteristicsDiscoveredEvent {
    private final Collection<BluetoothGattCharacteristic> mBluetoothGattCharacteristics;

    public CharacteristicsDiscoveredEvent(Collection<BluetoothGattCharacteristic>
                                                  bluetoothGattCharacteristics) {
        mBluetoothGattCharacteristics = bluetoothGattCharacteristics;
    }

    public Collection<BluetoothGattCharacteristic> getBluetoothGattCharacteristics() {
        return mBluetoothGattCharacteristics;
    }
}
