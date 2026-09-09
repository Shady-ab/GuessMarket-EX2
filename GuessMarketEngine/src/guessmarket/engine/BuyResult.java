package guessmarket.engine;

public final class BuyResult {
    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double shareCost;
    private final double commission;
    private final double totalPaid;
    private final String details;

    public BuyResult(String userName, String optionName, int quantity, double shareCost, double commission, String details) {
        this.userName = userName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.shareCost = Money.round(shareCost);
        this.commission = Money.round(commission);
        this.totalPaid = Money.round(this.shareCost + this.commission);
        this.details = details;
    }

    public String getUserName() {
        return userName;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getShareCost() {
        return shareCost;
    }

    public double getCommission() {
        return commission;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public String getDetails() {
        return details;
    }
}
