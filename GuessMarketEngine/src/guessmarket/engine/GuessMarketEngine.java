package guessmarket.engine;

import java.util.List;

public interface GuessMarketEngine {
    void loadFromXml(String fullPath) throws GuessMarketException;

    boolean hasLoadedMarket();

    List<MarketEvent> getEvents();

    List<User> getUsers();

    MarketEvent getEventById(int eventId) throws GuessMarketException;

    User getUserByName(String name) throws GuessMarketException;

    void openEvent(String marketMakerName, int eventId) throws GuessMarketException;

    BuyResult buyLmsrShares(String userName, int eventId, int optionNumber, int quantity) throws GuessMarketException;

    OrderResult placeOrder(String userName, int eventId, int optionNumber, OrderSide side,
                           int quantity, double price) throws GuessMarketException;

    CloseResult closeEvent(String marketMakerName, int eventId, int winningOptionNumber) throws GuessMarketException;
}
