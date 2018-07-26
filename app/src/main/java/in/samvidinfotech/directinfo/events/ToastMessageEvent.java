package in.samvidinfotech.directinfo.events;

/**
 * Created by samvidmistry on 11/4/18.
 */

public final class ToastMessageEvent {
    private final String mMessage;

    public ToastMessageEvent(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}
