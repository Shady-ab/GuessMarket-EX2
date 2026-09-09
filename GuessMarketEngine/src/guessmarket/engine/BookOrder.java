package guessmarket.engine;

public final class BookOrder {
    private final long id;
    private final String userName;
    private final int optionIndex;
    private final OrderSide side;
    private final double price;
    private int remainingQuantity;

    public BookOrder(long id, String userName, int optionIndex, OrderSide side, double price, int quantity) {
        this.id = id;
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.side = side;
        this.price = Money.round(price);
        this.remainingQuantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public OrderSide getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    void fill(int quantity) {
        if (quantity <= 0 || quantity > remainingQuantity) {
            throw new IllegalArgumentException("Invalid fill quantity.");
        }
        remainingQuantity -= quantity;
    }

    public boolean isFilled() {
        return remainingQuantity == 0;
    }
}
