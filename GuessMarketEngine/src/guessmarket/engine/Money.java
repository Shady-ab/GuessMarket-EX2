package guessmarket.engine;

public final class Money {
    private Money() {
    }

    public static double round(double value) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
