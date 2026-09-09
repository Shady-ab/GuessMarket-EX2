package guessmarket.ui;

final class UiFormat {
    private UiFormat() {
    }

    static String money(double value) {
        return String.format("%.2f", value);
    }

    static String moneyOrDash(Double value) {
        return value == null ? "-" : money(value);
    }

    static String commission(guessmarket.engine.MarketEvent event) {
        return event.getCommissionPercentage() + "% " + event.getCommissionType().getXmlValue();
    }
}
