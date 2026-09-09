package guessmarket.engine;

public final class OrderResult {
    private final int filledQuantity;
    private final int remainingQuantity;
    private final double shareCost;
    private final double commission;
    private final String summary;

    public OrderResult(int filledQuantity, int remainingQuantity, double shareCost, double commission, String summary) {
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.shareCost = Money.round(shareCost);
        this.commission = Money.round(commission);
        this.summary = summary;
    }

    public int getFilledQuantity() {
        return filledQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public double getShareCost() {
        return shareCost;
    }

    public double getCommission() {
        return commission;
    }

    public String getSummary() {
        return summary;
    }
}
