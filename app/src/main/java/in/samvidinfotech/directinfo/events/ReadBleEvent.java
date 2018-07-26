package in.samvidinfotech.directinfo.events;

public final class ReadBleEvent {
    private final String mBleMac;

    public ReadBleEvent(String mBleMac) {
        this.mBleMac = mBleMac;
    }

    public String getmBleMac() {
        return mBleMac;
    }
}
