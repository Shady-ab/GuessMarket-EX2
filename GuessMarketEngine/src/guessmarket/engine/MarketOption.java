package guessmarket.engine;

public final class MarketOption {
    private final String name;
    private int outstandingShares;

    public MarketOption(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Option name must not be empty.");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public int getOutstandingShares() {
        return outstandingShares;
    }

    void addShares(int quantity) {
        outstandingShares = Math.addExact(outstandingShares, quantity);
    }

    void removeShares(int quantity) {
        if (quantity > outstandingShares) {
            throw new IllegalArgumentException("Not enough outstanding shares.");
        }
        outstandingShares -= quantity;
    }
}
