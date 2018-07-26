package in.samvidinfotech.directinfo.events;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.List;

/**
 * Created by samvidmistry on 21/3/18.
 */

public final class MultipleCharacteristicsReadEvent {
    private final RxBleConnection mRxBleConnection;
    private final List<String> mCharacteristicList;

    public MultipleCharacteristicsReadEvent(RxBleConnection rxBleConnection,
                                            List<String> characteristicList) {
        mRxBleConnection = rxBleConnection;
        mCharacteristicList = characteristicList;
    }

    public List<String> getCharacteristicList() {
        return mCharacteristicList;
    }

    public RxBleConnection getRxBleConnection() {
        return mRxBleConnection;
    }
}
