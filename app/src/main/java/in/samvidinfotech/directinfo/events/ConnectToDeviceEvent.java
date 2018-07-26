package in.samvidinfotech.directinfo.events;

/**
 * Created by samvidmistry on 23/2/18.
 */

public final class ConnectToDeviceEvent {
    private final String mRxBleDeviceMac;

    public ConnectToDeviceEvent(String rxBleDeviceMac) {
        mRxBleDeviceMac = rxBleDeviceMac;
    }

    public String getRxBleDeviceMac() {
        return mRxBleDeviceMac;
    }
}
