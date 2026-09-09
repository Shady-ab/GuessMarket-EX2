package guessmarket.engine;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

final class OptionOrderBook {
    private final NavigableMap<Double, ArrayDeque<BookOrder>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Double, ArrayDeque<BookOrder>> asks = new TreeMap<>();
    private Double lastPrice;

    Double getLastPrice() {
        return lastPrice;
    }

    void setLastPrice(double price) {
        lastPrice = Money.round(price);
    }

    Double bestBid() {
        return firstLivePrice(bids);
    }

    Double bestAsk() {
        return firstLivePrice(asks);
    }

    BookOrder peekBestBid() {
        return peekBest(bids);
    }

    BookOrder peekBestAsk() {
        return peekBest(asks);
    }

    void add(BookOrder order) {
        if (order.getRemainingQuantity() <= 0) {
            return;
        }
        NavigableMap<Double, ArrayDeque<BookOrder>> book = order.getSide() == OrderSide.BUY ? bids : asks;
        book.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>()).addLast(order);
    }

    void removeEmptyLevels() {
        bids.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        asks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    List<BookOrder> snapshotBids() {
        return flatten(bids);
    }

    List<BookOrder> snapshotAsks() {
        return flatten(asks);
    }

    List<BookOrder> allOrders() {
        List<BookOrder> all = new ArrayList<>();
        all.addAll(flatten(bids));
        all.addAll(flatten(asks));
        return all;
    }

    void cancelAll() {
        bids.clear();
        asks.clear();
    }

    private static Double firstLivePrice(NavigableMap<Double, ArrayDeque<BookOrder>> book) {
        cleanup(book);
        return book.isEmpty() ? null : book.firstKey();
    }

    private static BookOrder peekBest(NavigableMap<Double, ArrayDeque<BookOrder>> book) {
        cleanup(book);
        if (book.isEmpty()) {
            return null;
        }
        ArrayDeque<BookOrder> level = book.firstEntry().getValue();
        return level.isEmpty() ? null : level.peekFirst();
    }

    static void consumeBest(NavigableMap<Double, ArrayDeque<BookOrder>> book, int quantity) {
        BookOrder order = peekBest(book);
        if (order == null) {
            throw new IllegalStateException("No order to consume.");
        }
        order.fill(quantity);
        if (order.isFilled()) {
            book.firstEntry().getValue().removeFirst();
        }
        cleanup(book);
    }

    NavigableMap<Double, ArrayDeque<BookOrder>> bidLevels() {
        return bids;
    }

    NavigableMap<Double, ArrayDeque<BookOrder>> askLevels() {
        return asks;
    }

    private static void cleanup(NavigableMap<Double, ArrayDeque<BookOrder>> book) {
        while (!book.isEmpty()) {
            ArrayDeque<BookOrder> level = book.firstEntry().getValue();
            while (!level.isEmpty() && level.peekFirst().getRemainingQuantity() <= 0) {
                level.removeFirst();
            }
            if (level.isEmpty()) {
                book.pollFirstEntry();
            } else {
                break;
            }
        }
    }

    private static List<BookOrder> flatten(NavigableMap<Double, ArrayDeque<BookOrder>> book) {
        cleanup(book);
        List<BookOrder> result = new ArrayList<>();
        for (ArrayDeque<BookOrder> level : book.values()) {
            for (BookOrder order : level) {
                if (order.getRemainingQuantity() > 0) {
                    result.add(order);
                }
            }
        }
        return result;
    }
}
