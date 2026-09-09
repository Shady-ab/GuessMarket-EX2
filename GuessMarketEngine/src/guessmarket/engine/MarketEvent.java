package guessmarket.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketEvent {
    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercentage;
    private final CommissionType commissionType;
    private final MarketType marketType;
    private final List<MarketOption> options;
    private final Double b;
    private final Integer initialInvestment;
    private final Integer d;
    private final boolean allowMint;
    private final String marketMakerName;
    private final List<TradeRecord> tradeHistory = new ArrayList<>();
    private final OptionOrderBook[] books = {new OptionOrderBook(), new OptionOrderBook()};

    private EventStatus status = EventStatus.NOT_STARTED;
    private Integer winningOptionIndex;
    private double accountBalance;
    private double collectedCommission;
    private long nextOrderId = 1;

    public MarketEvent(int id, String name, String description, int commissionPercentage,
                       CommissionType commissionType, String firstOption, String secondOption,
                       MarketType marketType, Double b, Integer initialInvestment, Integer d,
                       boolean allowMint, String marketMakerName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name must not be empty.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Event description must not be empty.");
        }
        if (commissionPercentage < 0 || commissionPercentage > 90) {
            throw new IllegalArgumentException("Commission must be between 0 and 90 inclusive.");
        }
        if (marketMakerName == null || marketMakerName.isBlank()) {
            throw new IllegalArgumentException("Every event must have a market maker.");
        }
        this.id = id;
        this.name = name.trim();
        this.description = description.trim();
        this.commissionPercentage = commissionPercentage;
        this.commissionType = commissionType;
        this.options = List.of(new MarketOption(firstOption), new MarketOption(secondOption));
        this.marketType = marketType;
        this.b = b;
        this.initialInvestment = initialInvestment;
        this.d = d;
        this.allowMint = allowMint;
        this.marketMakerName = marketMakerName.trim();
        if (marketType == MarketType.LMSR) {
            if (b == null || b <= 0 || !Double.isFinite(b)) {
                throw new IllegalArgumentException("LMSR b must be a positive number.");
            }
        } else {
            if (d == null || d <= 0) {
                throw new IllegalArgumentException("Order-book base value d must be a positive integer.");
            }
            if (initialInvestment == null || initialInvestment < 0) {
                throw new IllegalArgumentException("Order-book initial investment must be >= 0.");
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercentage() {
        return commissionPercentage;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public MarketType getMarketType() {
        return marketType;
    }

    public List<MarketOption> getOptions() {
        return options;
    }

    public Double getB() {
        return b;
    }

    public Integer getInitialInvestment() {
        return initialInvestment;
    }

    public Integer getD() {
        return d;
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public EventStatus getStatus() {
        return status;
    }

    public double getAccountBalance() {
        return Money.round(accountBalance);
    }

    public double getCollectedCommission() {
        return Money.round(collectedCommission);
    }

    public Integer getWinningOptionIndex() {
        return winningOptionIndex;
    }

    public String getWinningOptionName() {
        return winningOptionIndex == null ? null : options.get(winningOptionIndex).getName();
    }

    public double getRequiredSubsidy() {
        if (marketType != MarketType.LMSR) {
            return 0;
        }
        return Money.round(b * Math.log(2.0));
    }

    public double getOpeningCost() {
        if (marketType == MarketType.LMSR) {
            return getRequiredSubsidy();
        }
        return initialInvestment == null ? 0 : initialInvestment;
    }

    public double getCurrentPrice(int optionIndex) {
        validateOptionIndex(optionIndex);
        if (marketType != MarketType.LMSR) {
            Double last = books[optionIndex].getLastPrice();
            return last == null ? Money.round(d / 2.0) : last;
        }
        return softmaxPrice(optionIndex, options.get(0).getOutstandingShares(), options.get(1).getOutstandingShares());
    }

    public List<TradeRecord> getTradeHistoryNewestFirst() {
        List<TradeRecord> result = new ArrayList<>(tradeHistory);
        Collections.reverse(result);
        return Collections.unmodifiableList(result);
    }

    public OptionBookSnapshot getBookSnapshot(int optionIndex) {
        validateOptionIndex(optionIndex);
        OptionOrderBook book = books[optionIndex];
        return new OptionBookSnapshot(options.get(optionIndex).getName(), book.getLastPrice(),
                book.bestBid(), book.bestAsk(), book.snapshotBids(), book.snapshotAsks());
    }

    public Map<String, int[]> holdingsByUser(Map<String, User> users) {
        Map<String, int[]> result = new LinkedHashMap<>();
        for (User user : users.values()) {
            int a = user.getHolding(id, 0);
            int bShares = user.getHolding(id, 1);
            boolean hasOrder = false;
            for (int i = 0; i < 2; i++) {
                for (BookOrder order : books[i].allOrders()) {
                    if (order.getUserName().equals(user.getName())) {
                        hasOrder = true;
                        break;
                    }
                }
            }
            if (a > 0 || bShares > 0 || hasOrder || user.getParticipatingEventIds().contains(id)) {
                result.put(user.getName(), new int[]{a, bShares});
            }
        }
        return result;
    }

    void open(User marketMaker) throws GuessMarketException {
        if (status != EventStatus.NOT_STARTED) {
            throw new GuessMarketException("Event " + id + " cannot be opened because it is " + status.displayName() + ".");
        }
        if (!marketMaker.getName().equals(marketMakerName)) {
            throw new GuessMarketException("Only market maker " + marketMakerName + " can open event " + id + ".");
        }
        marketMaker.ensureCanAct();
        double cost = getOpeningCost();
        if (marketMaker.getCash() < cost) {
            throw new GuessMarketException("Market maker " + marketMaker.getName()
                    + " does not have enough cash to open event " + id + ". Required: "
                    + String.format("%.2f", cost) + ", available: " + String.format("%.2f", marketMaker.getCash()) + ".");
        }
        if (cost > 0) {
            marketMaker.debit(cost);
            accountBalance = Money.round(accountBalance + cost);
        }
        if (marketType == MarketType.ORDER_BOOK && initialInvestment > 0) {
            int pairs = initialInvestment / d;
            if (pairs > 0) {
                options.get(0).addShares(pairs);
                options.get(1).addShares(pairs);
                marketMaker.addHolding(id, 0, pairs, Money.round(pairs * d / 2.0));
                marketMaker.addHolding(id, 1, pairs, Money.round(pairs * d / 2.0));
            }
        }
        marketMaker.markParticipant(id);
        status = EventStatus.ACTIVE;
    }

    BuyResult buyLmsr(User buyer, int optionIndex, int quantity, User marketMaker) throws GuessMarketException {
        ensureActive();
        if (marketType != MarketType.LMSR) {
            throw new GuessMarketException("Event " + id + " is not an LMSR event.");
        }
        buyer.ensureCanAct();
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new GuessMarketException("Share quantity must be a positive whole number.");
        }

        int q0Before = options.get(0).getOutstandingShares();
        int q1Before = options.get(1).getOutstandingShares();
        int q0After = q0Before;
        int q1After = q1Before;
        if (optionIndex == 0) {
            q0After = Math.addExact(q0Before, quantity);
        } else {
            q1After = Math.addExact(q1Before, quantity);
        }

        double shareCost = Money.round(costFunction(q0After, q1After) - costFunction(q0Before, q1Before));
        if (!Double.isFinite(shareCost) || shareCost < 0) {
            throw new GuessMarketException("The LMSR calculation produced an invalid trade cost.");
        }
        double commission = purchaseCommission(shareCost);
        double total = Money.round(shareCost + commission);
        // A debit that exceeds the balance is allowed on purpose: the account goes negative and is then blocked.
        buyer.debit(total);
        accountBalance = Money.round(accountBalance + shareCost);
        payCommissionToMm(marketMaker, commission);
        options.get(optionIndex).addShares(quantity);
        buyer.addHolding(id, optionIndex, quantity, shareCost);
        buyer.addCommission(id, commission);
        buyer.markParticipant(id);

        TradeRecord record = new TradeRecord(TradeRecord.Kind.LMSR_BUY, buyer.getName(), marketMakerName,
                options.get(optionIndex).getName(), OrderSide.BUY, quantity,
                quantity == 0 ? 0 : shareCost / quantity, shareCost, commission);
        tradeHistory.add(record);
        buyer.addPersonalTrade(id, record);
        return new BuyResult(buyer.getName(), options.get(optionIndex).getName(), quantity, shareCost, commission,
                "LMSR purchase completed.");
    }

    OrderResult placeOrder(User actor, int optionIndex, OrderSide side, int quantity, double price,
                           Map<String, User> users, User marketMaker) throws GuessMarketException {
        ensureActive();
        if (marketType != MarketType.ORDER_BOOK) {
            throw new GuessMarketException("Event " + id + " is not an order-book event.");
        }
        actor.ensureCanAct();
        validateOptionIndex(optionIndex);
        if (quantity <= 0) {
            throw new GuessMarketException("Share quantity must be a positive whole number.");
        }
        double roundedPrice = Money.round(price);
        double maxPrice = Money.round(d - 0.01);
        if (roundedPrice < 0.01 || roundedPrice > maxPrice) {
            throw new GuessMarketException("Price must be between 0.01 and " + String.format("%.2f", maxPrice)
                    + " (d - 0.01).");
        }
        if (side == OrderSide.SELL && actor.getHolding(id, optionIndex) < quantity) {
            throw new GuessMarketException("User " + actor.getName() + " does not hold enough "
                    + options.get(optionIndex).getName() + " shares to sell.");
        }

        BookOrder incoming = new BookOrder(nextOrderId++, actor.getName(), optionIndex, side, roundedPrice, quantity);
        actor.markParticipant(id);
        StringBuilder log = new StringBuilder();
        FillTally tally = new FillTally();

        if (side == OrderSide.BUY) {
            matchAgainstAsks(incoming, users, marketMaker, log, tally);
            if (incoming.getRemainingQuantity() > 0 && allowMint) {
                mintAgainstOtherBids(incoming, users, marketMaker, log, tally);
            }
        } else {
            matchAgainstBids(incoming, users, marketMaker, log, tally);
        }

        if (incoming.getRemainingQuantity() > 0) {
            books[optionIndex].add(incoming);
            log.append("Resting ").append(incoming.getRemainingQuantity())
                    .append(" at ").append(String.format("%.2f", incoming.getPrice())).append(". ");
        }

        return new OrderResult(tally.filled, incoming.getRemainingQuantity(), tally.shareCost, tally.commission,
                log.toString().trim());
    }

    /** Accumulates what the acting user actually filled, spent and paid in commission for a single order. */
    private static final class FillTally {
        private int filled;
        private double shareCost;
        private double commission;

        void add(int quantity, double cost, double commissionPaid) {
            filled += quantity;
            shareCost = Money.round(shareCost + cost);
            commission = Money.round(commission + commissionPaid);
        }
    }

    private void matchAgainstAsks(BookOrder buy, Map<String, User> users, User marketMaker, StringBuilder log,
                                  FillTally tally) throws GuessMarketException {
        OptionOrderBook book = books[buy.getOptionIndex()];
        while (buy.getRemainingQuantity() > 0) {
            BookOrder ask = book.peekBestAsk();
            if (ask == null || ask.getPrice() > buy.getPrice()) {
                break;
            }
            User seller = users.get(ask.getUserName());
            User buyer = users.get(buy.getUserName());
            if (seller == null || buyer == null) {
                break;
            }
            if (seller.getHolding(id, buy.getOptionIndex()) <= 0) {
                OptionOrderBook.consumeBest(book.askLevels(), ask.getRemainingQuantity());
                continue;
            }
            int qty = Math.min(buy.getRemainingQuantity(),
                    Math.min(ask.getRemainingQuantity(), seller.getHolding(id, buy.getOptionIndex())));
            if (qty <= 0) {
                OptionOrderBook.consumeBest(book.askLevels(), ask.getRemainingQuantity());
                continue;
            }
            double tradeValue = Money.round(qty * ask.getPrice());
            double commission = purchaseCommission(tradeValue);
            buyer.debit(Money.round(tradeValue + commission));
            seller.credit(tradeValue);
            payCommissionToMm(marketMaker, commission);
            transferShares(seller, buyer, buy.getOptionIndex(), qty, tradeValue);
            buyer.addCommission(id, commission);
            book.setLastPrice(ask.getPrice());
            buy.fill(qty);
            OptionOrderBook.consumeBest(book.askLevels(), qty);
            tally.add(qty, tradeValue, commission);
            TradeRecord buyRec = new TradeRecord(TradeRecord.Kind.ORDER_MATCH, buyer.getName(), seller.getName(),
                    options.get(buy.getOptionIndex()).getName(), OrderSide.BUY, qty, ask.getPrice(), tradeValue, commission);
            TradeRecord sellRec = new TradeRecord(TradeRecord.Kind.ORDER_MATCH, seller.getName(), buyer.getName(),
                    options.get(buy.getOptionIndex()).getName(), OrderSide.SELL, qty, ask.getPrice(), tradeValue, 0);
            tradeHistory.add(buyRec);
            buyer.addPersonalTrade(id, buyRec);
            seller.addPersonalTrade(id, sellRec);
            seller.markParticipant(id);
            log.append("Matched ").append(qty).append(" with ").append(seller.getName())
                    .append(" at ").append(String.format("%.2f", ask.getPrice())).append(". ");
        }
    }

    private void matchAgainstBids(BookOrder sell, Map<String, User> users, User marketMaker, StringBuilder log,
                                  FillTally tally) throws GuessMarketException {
        OptionOrderBook book = books[sell.getOptionIndex()];
        User seller = users.get(sell.getUserName());
        while (sell.getRemainingQuantity() > 0) {
            BookOrder bid = book.peekBestBid();
            if (bid == null || bid.getPrice() < sell.getPrice()) {
                break;
            }
            User buyer = users.get(bid.getUserName());
            if (seller == null || buyer == null) {
                break;
            }
            int available = seller.getHolding(id, sell.getOptionIndex());
            if (available <= 0) {
                break;
            }
            int qty = Math.min(sell.getRemainingQuantity(), Math.min(bid.getRemainingQuantity(), available));
            double tradeValue = Money.round(qty * bid.getPrice());
            double commission = purchaseCommission(tradeValue);
            buyer.debit(Money.round(tradeValue + commission));
            seller.credit(tradeValue);
            payCommissionToMm(marketMaker, commission);
            transferShares(seller, buyer, sell.getOptionIndex(), qty, tradeValue);
            buyer.addCommission(id, commission);
            book.setLastPrice(bid.getPrice());
            sell.fill(qty);
            OptionOrderBook.consumeBest(book.bidLevels(), qty);
            // For a SELL the acting user receives the trade value; the commission is charged to the buyer.
            tally.add(qty, tradeValue, 0);
            TradeRecord buyRec = new TradeRecord(TradeRecord.Kind.ORDER_MATCH, buyer.getName(), seller.getName(),
                    options.get(sell.getOptionIndex()).getName(), OrderSide.BUY, qty, bid.getPrice(), tradeValue, commission);
            TradeRecord sellRec = new TradeRecord(TradeRecord.Kind.ORDER_MATCH, seller.getName(), buyer.getName(),
                    options.get(sell.getOptionIndex()).getName(), OrderSide.SELL, qty, bid.getPrice(), tradeValue, 0);
            tradeHistory.add(buyRec);
            buyer.addPersonalTrade(id, buyRec);
            seller.addPersonalTrade(id, sellRec);
            buyer.markParticipant(id);
            log.append("Matched ").append(qty).append(" with ").append(buyer.getName())
                    .append(" at ").append(String.format("%.2f", bid.getPrice())).append(". ");
        }
    }

    private void mintAgainstOtherBids(BookOrder incomingBuy, Map<String, User> users, User marketMaker,
                                      StringBuilder log, FillTally tally) throws GuessMarketException {
        int otherIndex = 1 - incomingBuy.getOptionIndex();
        OptionOrderBook otherBook = books[otherIndex];
        User incomingUser = users.get(incomingBuy.getUserName());
        while (incomingBuy.getRemainingQuantity() > 0) {
            BookOrder otherBid = otherBook.peekBestBid();
            if (otherBid == null) {
                break;
            }
            if (Money.round(incomingBuy.getPrice() + otherBid.getPrice()) < d) {
                break;
            }
            User otherUser = users.get(otherBid.getUserName());
            if (otherUser == null || incomingUser == null) {
                break;
            }
            int qty = Math.min(incomingBuy.getRemainingQuantity(), otherBid.getRemainingQuantity());
            double otherPay = Money.round(qty * otherBid.getPrice());
            double incomingPay = Money.round(qty * (d - otherBid.getPrice()));
            double incomingCommission = purchaseCommission(incomingPay);
            double otherCommission = purchaseCommission(otherPay);
            incomingUser.debit(Money.round(incomingPay + incomingCommission));
            otherUser.debit(Money.round(otherPay + otherCommission));
            accountBalance = Money.round(accountBalance + incomingPay + otherPay);
            payCommissionToMm(marketMaker, Money.round(incomingCommission + otherCommission));
            options.get(incomingBuy.getOptionIndex()).addShares(qty);
            options.get(otherIndex).addShares(qty);
            incomingUser.addHolding(id, incomingBuy.getOptionIndex(), qty, incomingPay);
            otherUser.addHolding(id, otherIndex, qty, otherPay);
            incomingUser.addCommission(id, incomingCommission);
            otherUser.addCommission(id, otherCommission);
            incomingUser.markParticipant(id);
            otherUser.markParticipant(id);
            books[incomingBuy.getOptionIndex()].setLastPrice(d - otherBid.getPrice());
            otherBook.setLastPrice(otherBid.getPrice());
            incomingBuy.fill(qty);
            OptionOrderBook.consumeBest(otherBook.bidLevels(), qty);
            tally.add(qty, incomingPay, incomingCommission);
            TradeRecord incomingRec = new TradeRecord(TradeRecord.Kind.MINT, incomingUser.getName(), otherUser.getName(),
                    options.get(incomingBuy.getOptionIndex()).getName(), OrderSide.BUY, qty,
                    d - otherBid.getPrice(), incomingPay, incomingCommission);
            TradeRecord otherRec = new TradeRecord(TradeRecord.Kind.MINT, otherUser.getName(), incomingUser.getName(),
                    options.get(otherIndex).getName(), OrderSide.BUY, qty, otherBid.getPrice(), otherPay, otherCommission);
            tradeHistory.add(incomingRec);
            incomingUser.addPersonalTrade(id, incomingRec);
            otherUser.addPersonalTrade(id, otherRec);
            log.append("Minted ").append(qty).append(" pair(s) with ").append(otherUser.getName()).append(". ");
        }
    }

    CloseResult close(User marketMaker, int winningOptionIndex, Map<String, User> users) throws GuessMarketException {
        ensureActive();
        if (!marketMaker.getName().equals(marketMakerName)) {
            throw new GuessMarketException("Only market maker " + marketMakerName + " can close event " + id + ".");
        }
        marketMaker.ensureCanAct();
        validateOptionIndex(winningOptionIndex);

        books[0].cancelAll();
        books[1].cancelAll();

        int winningShares = 0;
        for (User user : users.values()) {
            winningShares += user.getHolding(id, winningOptionIndex);
        }

        double unitPayout = marketType == MarketType.ORDER_BOOK ? d : 1.0;
        double grossPayout = Money.round(winningShares * unitPayout);
        double closeCommissionRate = commissionType == CommissionType.ON_CLOSE ? commissionPercentage / 100.0 : 0.0;

        double totalCommission = 0;
        for (User user : users.values()) {
            int shares = user.getHolding(id, winningOptionIndex);
            if (shares <= 0) {
                continue;
            }
            double gross = Money.round(shares * unitPayout);
            double commission = Money.round(gross * closeCommissionRate);
            double net = Money.round(gross - commission);
            accountBalance = Money.round(accountBalance - gross);
            user.credit(net);
            if (commission > 0) {
                marketMaker.credit(commission);
                collectedCommission = Money.round(collectedCommission + commission);
                user.addCommission(id, commission);
                totalCommission = Money.round(totalCommission + commission);
            }
        }

        double leftover = accountBalance;
        if (leftover != 0) {
            marketMaker.credit(leftover);
            accountBalance = 0;
        }

        status = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex;
        return new CloseResult(options.get(winningOptionIndex).getName(), winningShares,
                grossPayout, totalCommission, Money.round(grossPayout - totalCommission), leftover);
    }

    private void transferShares(User seller, User buyer, int optionIndex, int qty, double tradeValue) {
        seller.removeHolding(id, optionIndex, qty, tradeValue);
        buyer.addHolding(id, optionIndex, qty, tradeValue);
    }

    private double purchaseCommission(double shareCost) {
        if (commissionType != CommissionType.ON_PURCHASE) {
            return 0;
        }
        return Money.round(shareCost * commissionPercentage / 100.0);
    }

    private void payCommissionToMm(User marketMaker, double commission) {
        if (commission <= 0) {
            return;
        }
        marketMaker.credit(commission);
        collectedCommission = Money.round(collectedCommission + commission);
    }

    private void ensureActive() throws GuessMarketException {
        if (status != EventStatus.ACTIVE) {
            throw new GuessMarketException("Event " + id + " is not active (status: " + status.displayName() + ").");
        }
    }

    private double costFunction(int q0, int q1) {
        double a = q0 / b;
        double c = q1 / b;
        double max = Math.max(a, c);
        return b * (max + Math.log(Math.exp(a - max) + Math.exp(c - max)));
    }

    private double softmaxPrice(int optionIndex, int q0, int q1) {
        double a = q0 / b;
        double c = q1 / b;
        double max = Math.max(a, c);
        double e0 = Math.exp(a - max);
        double e1 = Math.exp(c - max);
        double denominator = e0 + e1;
        return optionIndex == 0 ? e0 / denominator : e1 / denominator;
    }

    private void validateOptionIndex(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Option index must be 0 or 1.");
        }
    }
}
