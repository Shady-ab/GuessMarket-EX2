package guessmarket.engine;

import java.time.LocalDateTime;

public final class TradeRecord {
    public enum Kind {
        LMSR_BUY,
        ORDER_MATCH,
        MINT,
        OPEN,
        CLOSE_PAYOUT
    }

    private final Kind kind;
    private final String userName;
    private final String counterparty;
    private final String optionName;
    private final OrderSide side;
    private final int quantity;
    private final double pricePerShare;
    private final double shareCost;
    private final double commission;
    private final double totalPaid;
    private final LocalDateTime timestamp;

    public TradeRecord(Kind kind, String userName, String counterparty, String optionName, OrderSide side,
                       int quantity, double pricePerShare, double shareCost, double commission) {
        this.kind = kind;
        this.userName = userName;
        this.counterparty = counterparty;
        this.optionName = optionName;
        this.side = side;
        this.quantity = quantity;
        this.pricePerShare = Money.round(pricePerShare);
        this.shareCost = Money.round(shareCost);
        this.commission = Money.round(commission);
        this.totalPaid = Money.round(this.shareCost + this.commission);
        this.timestamp = LocalDateTime.now();
    }

    public Kind getKind() {
        return kind;
    }

    public String getUserName() {
        return userName;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public String getOptionName() {
        return optionName;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPricePerShare() {
        return pricePerShare;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
