package guessmarket.engine;

public final class CloseResult {
    private final String winningOptionName;
    private final int winningShares;
    private final double grossPayout;
    private final double commission;
    private final double netPayout;
    private final double returnedToMarketMaker;

    public CloseResult(String winningOptionName, int winningShares, double grossPayout,
                       double commission, double netPayout, double returnedToMarketMaker) {
        this.winningOptionName = winningOptionName;
        this.winningShares = winningShares;
        this.grossPayout = Money.round(grossPayout);
        this.commission = Money.round(commission);
        this.netPayout = Money.round(netPayout);
        this.returnedToMarketMaker = Money.round(returnedToMarketMaker);
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public int getWinningShares() {
        return winningShares;
    }

    public double getGrossPayout() {
        return grossPayout;
    }

    public double getCommission() {
        return commission;
    }

    public double getNetPayout() {
        return netPayout;
    }

    public double getReturnedToMarketMaker() {
        return returnedToMarketMaker;
    }
}
