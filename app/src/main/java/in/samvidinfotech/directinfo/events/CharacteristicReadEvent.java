package in.samvidinfotech.directinfo.events;

/**
 * Created by samvidmistry on 28/2/18.
 */

public final class CharacteristicReadEvent {
    private final byte[] mCharacteristicValue;

    public CharacteristicReadEvent(byte[] characteristicValue) {
        mCharacteristicValue = characteristicValue;
    }

    public byte[] getCharacteristicValue() {
        return mCharacteristicValue;
    }
}
