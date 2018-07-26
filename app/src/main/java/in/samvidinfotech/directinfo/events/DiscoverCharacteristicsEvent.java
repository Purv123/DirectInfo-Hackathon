package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleConnection;

/**
 * Created by samvidmistry on 27/2/18.
 */

public final class DiscoverCharacteristicsEvent {
    private final RxBleConnection mRxBleConnection;
    private final String mServiceUuid;

    public DiscoverCharacteristicsEvent(RxBleConnection rxBleConnection, String serviceUuid) {
        mRxBleConnection = rxBleConnection;
        mServiceUuid = serviceUuid;
    }

    public RxBleConnection getRxBleConnection() {
        return mRxBleConnection;
    }

    public String getServiceUuid() {
        return mServiceUuid;
    }
}
