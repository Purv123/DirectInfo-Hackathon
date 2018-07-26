package in.samvidinfotech.directinfo.events;

/**
 * Created by samvidmistry on 11/4/18.
 */

public final class StopProgressIndicatorEvent {
    private final String TAG;

    public StopProgressIndicatorEvent(String tag) {
        TAG = tag;
    }

    public String getTAG() {
        return TAG;
    }
}
