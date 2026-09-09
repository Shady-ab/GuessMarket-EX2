package guessmarket.engine;

public class GuessMarketException extends Exception {
    public GuessMarketException(String message) {
        super(message);
    }

    public GuessMarketException(String message, Throwable cause) {
        super(message, cause);
    }
}
