import guessmarket.engine.*;

public class ScenarioTest {
    static GuessMarketEngine e;

    public static void main(String[] args) throws Exception {
        mintScenario();
        blockScenario();
        walkBookScenario();
        closeOnCloseCommissionScenario();
    }

    static void mintScenario() throws Exception {
        System.out.println("=== MINT (event 2, allow-mint=true, d=1) ===");
        e = new GuessMarketEngineImpl();
        e.loadFromXml("samples/valid-two-events.xml");
        e.openEvent("Avrum", 2);
        say("Avrum");
        OrderResult a = e.placeOrder("Tikva", 2, 1, OrderSide.BUY, 10, 0.60);
        System.out.println("Tikva BUY Argentina 10 @0.60 -> filled=" + a.getFilledQuantity()
                + " rest=" + a.getRemainingQuantity() + " | " + a.getSummary());
        OrderResult b = e.placeOrder("Menash", 2, 2, OrderSide.BUY, 10, 0.50);
        System.out.println("Menash BUY Spain 10 @0.50 -> filled=" + b.getFilledQuantity()
                + " rest=" + b.getRemainingQuantity() + " | " + b.getSummary());
        System.out.println("  Menash order cost=" + b.getShareCost() + " commission=" + b.getCommission()
                + "   (expected 4.0 / 0.0)");
        say("Tikva");
        say("Menash");
        MarketEvent ev = e.getEventById(2);
        System.out.println("Tikva Argentina shares=" + e.getUserByName("Tikva").getHolding(2, 0)
                + " | Menash Spain shares=" + e.getUserByName("Menash").getHolding(2, 1));
        System.out.println("Event account=" + ev.getAccountBalance());
        book(ev, 0);
        book(ev, 1);
    }

    static void blockScenario() throws Exception {
        System.out.println("\n=== NEGATIVE BALANCE BLOCK (Menash has 100) ===");
        e = new GuessMarketEngineImpl();
        e.loadFromXml("samples/valid-two-events.xml");
        e.openEvent("Tikva", 1);
        BuyResult r = e.buyLmsrShares("Menash", 1, 1, 300);
        System.out.println("Menash buys 300 LMSR shares, total=" + r.getTotalPaid());
        User m = e.getUserByName("Menash");
        System.out.println("Menash cash=" + m.getCash() + " blocked=" + m.isBlocked());
        System.out.println("notice: " + m.getLastNotice());
        try {
            e.buyLmsrShares("Menash", 1, 1, 1);
        } catch (GuessMarketException ex) {
            System.out.println("second attempt rejected: " + ex.getMessage());
        }
    }

    static void walkBookScenario() throws Exception {
        System.out.println("\n=== ONE ORDER WALKING MULTIPLE LEVELS ===");
        e = new GuessMarketEngineImpl();
        e.loadFromXml("samples/valid-four-events.xml");
        e.openEvent("Tikva", 3);
        System.out.println("Tikva opened event 3, Yes shares=" + e.getUserByName("Tikva").getHolding(3, 0)
                + " cash=" + e.getUserByName("Tikva").getCash());
        e.placeOrder("Tikva", 3, 1, OrderSide.SELL, 20, 0.40);
        e.placeOrder("Tikva", 3, 1, OrderSide.SELL, 40, 0.45);
        MarketEvent ev = e.getEventById(3);
        book(ev, 0);
        OrderResult buy = e.placeOrder("Avrum", 3, 1, OrderSide.BUY, 50, 0.45);
        System.out.println("Avrum BUY 50 @0.45 -> filled=" + buy.getFilledQuantity()
                + " rest=" + buy.getRemainingQuantity() + " | " + buy.getSummary());
        System.out.println("  Avrum order cost=" + buy.getShareCost() + " commission=" + buy.getCommission()
                + "   (expected 21.5 / 10.75)");
        book(ev, 0);
        System.out.println("Avrum shares=" + e.getUserByName("Avrum").getHolding(3, 0)
                + " cash=" + e.getUserByName("Avrum").getCash());
        CloseResult c = e.closeEvent("Tikva", 3, 1);
        System.out.println("closed: winner=" + c.getWinningOptionName() + " gross=" + c.getGrossPayout()
                + " commission=" + c.getCommission() + " leftoverToMM=" + c.getReturnedToMarketMaker());
        System.out.println("Avrum final cash=" + e.getUserByName("Avrum").getCash());
    }

    static void closeOnCloseCommissionScenario() throws Exception {
        System.out.println("\n=== CLOSE WITH on-close COMMISSION (event 2, 15%) ===");
        e = new GuessMarketEngineImpl();
        e.loadFromXml("samples/valid-two-events.xml");
        e.openEvent("Avrum", 2);
        e.placeOrder("Tikva", 2, 1, OrderSide.BUY, 10, 0.60);
        e.placeOrder("Menash", 2, 2, OrderSide.BUY, 10, 0.50);
        double before = e.getUserByName("Avrum").getCash() + e.getUserByName("Tikva").getCash()
                + e.getUserByName("Menash").getCash() + e.getEventById(2).getAccountBalance();
        CloseResult c = e.closeEvent("Avrum", 2, 1);
        System.out.println("winner=" + c.getWinningOptionName() + " winningShares=" + c.getWinningShares()
                + " gross=" + c.getGrossPayout() + " commission=" + c.getCommission()
                + " leftoverToMM=" + c.getReturnedToMarketMaker());
        say("Avrum");
        say("Tikva");
        say("Menash");
        double after = e.getUserByName("Avrum").getCash() + e.getUserByName("Tikva").getCash()
                + e.getUserByName("Menash").getCash() + e.getEventById(2).getAccountBalance();
        System.out.println("cash conservation: before=" + before + " after=" + after
                + " (must be equal, and 11100.0 overall)");
    }

    static void book(MarketEvent ev, int idx) {
        OptionBookSnapshot s = ev.getBookSnapshot(idx);
        System.out.println("  book[" + s.getOptionName() + "] LAST=" + s.getLast() + " BID=" + s.getBestBid()
                + " ASK=" + s.getBestAsk() + " MID=" + s.getMid() + " SPREAD=" + s.getSpread()
                + " bids=" + s.getBids().size() + " asks=" + s.getAsks().size());
    }

    static void say(String name) throws Exception {
        User u = e.getUserByName(name);
        System.out.println("  " + name + " cash=" + u.getCash() + " blocked=" + u.isBlocked());
    }
}
