package guessmarket.engine;

public enum EventStatus {
    NOT_STARTED,
    ACTIVE,
    CLOSED;

    public String displayName() {
        return switch (this) {
            case NOT_STARTED -> "Not started";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }
}
