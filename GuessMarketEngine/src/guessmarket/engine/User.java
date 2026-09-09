package guessmarket.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class User {
    private final String name;
    private final Set<Integer> marketMakerEventIds;
    private double cash;
    private boolean blocked;
    private String lastNotice;
    private final Set<Integer> participatingEventIds = new LinkedHashSet<>();
    private final Map<Integer, int[]> holdings = new HashMap<>();
    private final Map<Integer, double[]> amountPaid = new HashMap<>();
    private final Map<Integer, Double> commissionPaid = new HashMap<>();
    private final Map<Integer, List<TradeRecord>> personalTrades = new HashMap<>();

    public User(String name, double initialCash, Set<Integer> marketMakerEventIds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be empty.");
        }
        this.name = name.trim();
        this.cash = Money.round(initialCash);
        this.marketMakerEventIds = Set.copyOf(marketMakerEventIds);
    }

    public String getName() {
        return name;
    }

    public double getCash() {
        return cash;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getLastNotice() {
        return lastNotice;
    }

    public Set<Integer> getMarketMakerEventIds() {
        return marketMakerEventIds;
    }

    public boolean isMarketMakerOf(int eventId) {
        return marketMakerEventIds.contains(eventId);
    }

    public Set<Integer> getParticipatingEventIds() {
        return Collections.unmodifiableSet(participatingEventIds);
    }

    public int getHolding(int eventId, int optionIndex) {
        int[] shares = holdings.get(eventId);
        if (shares == null) {
            return 0;
        }
        return shares[optionIndex];
    }

    public double getAmountPaid(int eventId, int optionIndex) {
        double[] paid = amountPaid.get(eventId);
        if (paid == null) {
            return 0;
        }
        return Money.round(paid[optionIndex]);
    }

    public double getCommissionPaid(int eventId) {
        return Money.round(commissionPaid.getOrDefault(eventId, 0.0));
    }

    public List<TradeRecord> getPersonalTradesNewestFirst(int eventId) {
        List<TradeRecord> list = new ArrayList<>(personalTrades.getOrDefault(eventId, List.of()));
        Collections.reverse(list);
        return Collections.unmodifiableList(list);
    }

    void ensureCanAct() throws GuessMarketException {
        if (blocked) {
            throw new GuessMarketException("User " + name + " is blocked because the account balance is negative.");
        }
    }

    void markParticipant(int eventId) {
        participatingEventIds.add(eventId);
    }

    void addHolding(int eventId, int optionIndex, int quantity, double paidForShares) {
        holdings.computeIfAbsent(eventId, id -> new int[2])[optionIndex] += quantity;
        amountPaid.computeIfAbsent(eventId, id -> new double[2])[optionIndex] =
                Money.round(getAmountPaid(eventId, optionIndex) + paidForShares);
    }

    void removeHolding(int eventId, int optionIndex, int quantity, double soldValue) {
        int current = getHolding(eventId, optionIndex);
        if (quantity > current) {
            throw new IllegalArgumentException("User does not hold enough shares.");
        }
        holdings.computeIfAbsent(eventId, id -> new int[2])[optionIndex] = current - quantity;
        double remainingPaid = getAmountPaid(eventId, optionIndex);
        if (current > 0) {
            double portion = remainingPaid * quantity / (double) current;
            amountPaid.computeIfAbsent(eventId, id -> new double[2])[optionIndex] =
                    Money.round(Math.max(0, remainingPaid - portion));
        }
        // soldValue is used by caller for cash; cost basis is reduced proportionally above.
        if (soldValue < 0) {
            throw new IllegalArgumentException("Sold value cannot be negative.");
        }
    }

    void addCommission(int eventId, double amount) {
        commissionPaid.put(eventId, Money.round(getCommissionPaid(eventId) + amount));
    }

    void addPersonalTrade(int eventId, TradeRecord record) {
        personalTrades.computeIfAbsent(eventId, id -> new ArrayList<>()).add(record);
    }

    void credit(double amount) {
        cash = Money.round(cash + amount);
    }

    void debit(double amount) throws GuessMarketException {
        cash = Money.round(cash - amount);
        if (cash < 0) {
            blocked = true;
            lastNotice = "Account " + name + " went negative (cash=" + String.format("%.2f", cash)
                    + ") and is now blocked from further actions.";
        }
    }
}
