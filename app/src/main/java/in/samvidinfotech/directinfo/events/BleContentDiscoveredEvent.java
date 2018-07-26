package in.samvidinfotech.directinfo.events;

public final class BleContentDiscoveredEvent {
    private final String mTitle, mContent;

    public BleContentDiscoveredEvent(String mTitle, String mContent) {
        this.mTitle = mTitle;
        this.mContent = mContent;
    }

    public String getmTitle() {
        return mTitle;
    }

    public String getmContent() {
        return mContent;
    }
}
