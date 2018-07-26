package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleConnection;

/**
 * Created by samvidmistry on 24/2/18.
 */

public final class DiscoverServicesEvent {
    private final RxBleConnection mRxBleConnection;

    public DiscoverServicesEvent(RxBleConnection rxBleConnection) {
        mRxBleConnection = rxBleConnection;
    }

    public RxBleConnection getRxBleConnection() {
        return mRxBleConnection;
    }
}
