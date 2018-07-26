package in.samvidinfotech.directinfo.events;

/**
 * Created by samvidmistry on 3/4/18.
 */

public final class MessageCreatedEvent {
    private final String mMessage;

    public MessageCreatedEvent(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}
