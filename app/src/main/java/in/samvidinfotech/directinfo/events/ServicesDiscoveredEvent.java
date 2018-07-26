package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleDeviceServices;

/**
 * Created by samvidmistry on 24/2/18.
 */

public final class ServicesDiscoveredEvent {
    private final RxBleDeviceServices mRxBleDeviceServices;

    public ServicesDiscoveredEvent(RxBleDeviceServices rxBleDeviceServices) {
        mRxBleDeviceServices = rxBleDeviceServices;
    }

    public RxBleDeviceServices getRxBleDeviceServices() {
        return mRxBleDeviceServices;
    }
}
