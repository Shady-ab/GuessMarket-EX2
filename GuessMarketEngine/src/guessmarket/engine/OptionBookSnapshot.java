package guessmarket.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OptionBookSnapshot {
    private final String optionName;
    private final Double last;
    private final Double bestBid;
    private final Double bestAsk;
    private final Double mid;
    private final Double spread;
    private final List<BookOrder> bids;
    private final List<BookOrder> asks;

    public OptionBookSnapshot(String optionName, Double last, Double bestBid, Double bestAsk,
                              List<BookOrder> bids, List<BookOrder> asks) {
        this.optionName = optionName;
        this.last = last == null ? null : Money.round(last);
        this.bestBid = bestBid == null ? null : Money.round(bestBid);
        this.bestAsk = bestAsk == null ? null : Money.round(bestAsk);
        if (this.bestBid != null && this.bestAsk != null) {
            this.mid = Money.round((this.bestBid + this.bestAsk) / 2.0);
            this.spread = Money.round(this.bestAsk - this.bestBid);
        } else {
            this.mid = null;
            this.spread = null;
        }
        this.bids = Collections.unmodifiableList(new ArrayList<>(bids));
        this.asks = Collections.unmodifiableList(new ArrayList<>(asks));
    }

    public String getOptionName() {
        return optionName;
    }

    public Double getLast() {
        return last;
    }

    public Double getBestBid() {
        return bestBid;
    }

    public Double getBestAsk() {
        return bestAsk;
    }

    public Double getMid() {
        return mid;
    }

    public Double getSpread() {
        return spread;
    }

    public List<BookOrder> getBids() {
        return bids;
    }

    public List<BookOrder> getAsks() {
        return asks;
    }
}
