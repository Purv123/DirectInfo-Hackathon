package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleConnection;

/**
 * Created by samvidmistry on 28/2/18.
 */

public final class ReadCharacteristicEvent {
    private final RxBleConnection mRxBleConnection;
    private final String mUuid;

    public ReadCharacteristicEvent(RxBleConnection rxBleConnection, String uuid) {
        mRxBleConnection = rxBleConnection;
        mUuid = uuid;
    }

    public RxBleConnection getRxBleConnection() {
        return mRxBleConnection;
    }

    public String getUuid() {
        return mUuid;
    }
}
