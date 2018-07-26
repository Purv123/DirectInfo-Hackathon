package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleConnection;

/**
 * Created by samvidmistry on 23/2/18.
 */

public final class BleDeviceConnectedEvent {
    private final RxBleConnection mRxBleConnection;

    public BleDeviceConnectedEvent(RxBleConnection rxBleConnection) {
        mRxBleConnection = rxBleConnection;
    }

    public RxBleConnection getRxBleConnection() {
        return mRxBleConnection;
    }
}
