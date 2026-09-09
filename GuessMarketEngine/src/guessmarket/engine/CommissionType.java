package guessmarket.engine;

public enum CommissionType {
    ON_PURCHASE("on-purchase"),
    ON_CLOSE("on-close");

    private final String xmlValue;

    CommissionType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String getXmlValue() {
        return xmlValue;
    }

    public static CommissionType fromXml(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Commission type is missing.");
        }
        String normalized = value.trim().toLowerCase();
        for (CommissionType type : values()) {
            if (type.xmlValue.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported commission type: " + value);
    }
}
