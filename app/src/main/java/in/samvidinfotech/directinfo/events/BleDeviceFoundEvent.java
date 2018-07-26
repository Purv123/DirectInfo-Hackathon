package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleDevice;

/**
 * Created by samvidmistry on 20/2/18.
 */

public final class BleDeviceFoundEvent {
    private final RxBleDevice mRxBleDevice;

    public BleDeviceFoundEvent(RxBleDevice rxBleDevice) {
        mRxBleDevice = rxBleDevice;
    }

    public RxBleDevice getRxBleDevice() {
        return mRxBleDevice;
    }
}
