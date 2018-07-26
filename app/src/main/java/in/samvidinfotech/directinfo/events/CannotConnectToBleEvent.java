package in.samvidinfotech.directinfo.events;

/**
 * An event indicating that service could not successfully connect to BLE device
 */

public final class CannotConnectToBleEvent {
    private final Throwable mThrowable;

    public CannotConnectToBleEvent(Throwable throwable) {
        mThrowable = throwable;
    }

    public Throwable getThrowable() {
        return mThrowable;
    }
}
