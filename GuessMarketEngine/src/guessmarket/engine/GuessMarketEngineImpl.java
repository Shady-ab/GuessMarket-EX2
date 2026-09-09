package guessmarket.engine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GuessMarketEngineImpl implements GuessMarketEngine {
    private final Map<Integer, MarketEvent> events = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private boolean loadedMarket;

    @Override
    public void loadFromXml(String fullPath) throws GuessMarketException {
        if (fullPath == null || fullPath.isBlank()) {
            throw new GuessMarketException("XML path must not be empty.");
        }
        Path path;
        try {
            path = Path.of(fullPath.trim());
        } catch (RuntimeException ex) {
            throw new GuessMarketException("The supplied XML path is invalid.", ex);
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new GuessMarketException("The XML file does not exist: " + path);
        }
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new GuessMarketException("The selected file must have an .xml extension.");
        }

        ParsedMarket parsed = parseAndValidateXml(path);
        events.clear();
        users.clear();
        for (MarketEvent event : parsed.events()) {
            events.put(event.getId(), event);
        }
        users.putAll(parsed.users());
        loadedMarket = true;
    }

    @Override
    public boolean hasLoadedMarket() {
        return loadedMarket;
    }

    @Override
    public List<MarketEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events.values()));
    }

    @Override
    public List<User> getUsers() {
        return Collections.unmodifiableList(new ArrayList<>(users.values()));
    }

    @Override
    public MarketEvent getEventById(int eventId) throws GuessMarketException {
        ensureLoaded();
        MarketEvent event = events.get(eventId);
        if (event == null) {
            throw new GuessMarketException("Event id " + eventId + " was not found.");
        }
        return event;
    }

    @Override
    public User getUserByName(String name) throws GuessMarketException {
        ensureLoaded();
        if (name == null || name.isBlank()) {
            throw new GuessMarketException("User name must not be empty.");
        }
        User user = users.get(name.trim());
        if (user == null) {
            throw new GuessMarketException("User " + name + " was not found.");
        }
        return user;
    }

    @Override
    public void openEvent(String marketMakerName, int eventId) throws GuessMarketException {
        ensureLoaded();
        MarketEvent event = getEventById(eventId);
        User mm = getUserByName(marketMakerName);
        event.open(mm);
    }

    @Override
    public BuyResult buyLmsrShares(String userName, int eventId, int optionNumber, int quantity)
            throws GuessMarketException {
        ensureLoaded();
        if (optionNumber < 1 || optionNumber > 2) {
            throw new GuessMarketException("Option number must be 1 or 2.");
        }
        MarketEvent event = getEventById(eventId);
        User buyer = getUserByName(userName);
        User mm = getUserByName(event.getMarketMakerName());
        try {
            return event.buyLmsr(buyer, optionNumber - 1, quantity, mm);
        } catch (ArithmeticException ex) {
            throw new GuessMarketException("The share quantity is too large.", ex);
        }
    }

    @Override
    public OrderResult placeOrder(String userName, int eventId, int optionNumber, OrderSide side,
                                  int quantity, double price) throws GuessMarketException {
        ensureLoaded();
        if (optionNumber < 1 || optionNumber > 2) {
            throw new GuessMarketException("Option number must be 1 or 2.");
        }
        MarketEvent event = getEventById(eventId);
        User actor = getUserByName(userName);
        User mm = getUserByName(event.getMarketMakerName());
        return event.placeOrder(actor, optionNumber - 1, side, quantity, price, users, mm);
    }

    @Override
    public CloseResult closeEvent(String marketMakerName, int eventId, int winningOptionNumber)
            throws GuessMarketException {
        ensureLoaded();
        if (winningOptionNumber < 1 || winningOptionNumber > 2) {
            throw new GuessMarketException("Winning option number must be 1 or 2.");
        }
        MarketEvent event = getEventById(eventId);
        User mm = getUserByName(marketMakerName);
        return event.close(mm, winningOptionNumber - 1, users);
    }

    private ParsedMarket parseAndValidateXml(Path path) throws GuessMarketException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(path.toFile());
            document.getDocumentElement().normalize();

            if (!"Guess-Market".equals(document.getDocumentElement().getNodeName())) {
                throw new GuessMarketException("The XML root element must be Guess-Market.");
            }

            Map<Integer, RawEvent> rawEvents = parseEvents(document);
            Map<String, User> parsedUsers = parseUsers(document, rawEvents.keySet());

            Map<Integer, String> mmByEvent = new HashMap<>();
            for (User user : parsedUsers.values()) {
                for (Integer eventId : user.getMarketMakerEventIds()) {
                    if (mmByEvent.containsKey(eventId)) {
                        throw new GuessMarketException("Event " + eventId
                                + " is assigned to more than one market maker.");
                    }
                    mmByEvent.put(eventId, user.getName());
                }
            }
            for (Integer eventId : rawEvents.keySet()) {
                if (!mmByEvent.containsKey(eventId)) {
                    throw new GuessMarketException("Event " + eventId + " does not have exactly one market maker.");
                }
            }

            List<MarketEvent> resultEvents = new ArrayList<>();
            for (RawEvent raw : rawEvents.values()) {
                resultEvents.add(new MarketEvent(raw.id, raw.name, raw.description, raw.commission, raw.commissionType,
                        raw.option1, raw.option2, raw.marketType, raw.b, raw.initial, raw.d, raw.allowMint,
                        mmByEvent.get(raw.id)));
            }
            return new ParsedMarket(resultEvents, parsedUsers);
        } catch (ParserConfigurationException ex) {
            throw new GuessMarketException("The XML parser could not be configured.", ex);
        } catch (SAXException ex) {
            throw new GuessMarketException("The XML file is malformed or invalid XML: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new GuessMarketException("The XML file could not be read: " + ex.getMessage(), ex);
        }
    }

    private Map<Integer, RawEvent> parseEvents(Document document) throws GuessMarketException {
        NodeList eventNodes = document.getElementsByTagName("GM-event");
        if (eventNodes.getLength() == 0) {
            throw new GuessMarketException("The XML file does not contain any events.");
        }
        Map<Integer, RawEvent> result = new LinkedHashMap<>();
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            String eventName = eventElement.getAttribute("name").trim();
            int id = parseInt(requiredText(eventElement, "id", "event id"), "event id");
            if (result.containsKey(id)) {
                throw new GuessMarketException("Duplicate event id detected: " + id + ". Every event id must be unique.");
            }
            String description = requiredText(eventElement, "description", "description");
            Element commissionElement = firstChildElementAny(eventElement, "commission", "comision");
            if (commissionElement == null) {
                throw new GuessMarketException("Event " + id + " is missing its commission element.");
            }
            int commission = parseInt(commissionElement.getTextContent().trim(), "commission of event " + id);
            if (commission < 0 || commission > 90) {
                throw new GuessMarketException("Invalid commission for event " + id + ": " + commission
                        + ". Commission must be between 0 and 90 inclusive.");
            }
            CommissionType commissionType;
            try {
                commissionType = CommissionType.fromXml(commissionElement.getAttribute("type"));
            } catch (IllegalArgumentException ex) {
                throw new GuessMarketException("Invalid commission type for event " + id + ".", ex);
            }

            Element optionsElement = firstChildElement(eventElement, "GM-options");
            if (optionsElement == null) {
                throw new GuessMarketException("Event " + id + " is missing GM-options.");
            }
            List<String> optionNames = directChildTexts(optionsElement, "GM-option");
            if (optionNames.size() != 2) {
                throw new GuessMarketException("Event " + id + " must contain exactly two GM-option elements.");
            }

            Element methodElement = firstChildElement(eventElement, "GM-method");
            if (methodElement == null) {
                throw new GuessMarketException("Event " + id + " is missing GM-method.");
            }
            Element lmsr = firstChildElement(methodElement, "GM-LMSR");
            Element orderBook = firstChildElement(methodElement, "GM-order-book");
            MarketType type;
            Double b = null;
            Integer initial = null;
            Integer d = null;
            boolean allowMint = false;
            if (lmsr != null && orderBook == null) {
                type = MarketType.LMSR;
                b = (double) parseInt(requiredText(lmsr, "b", "LMSR b"), "LMSR b of event " + id);
                if (b <= 0) {
                    throw new GuessMarketException("Invalid LMSR b for event " + id + ": b must be greater than zero.");
                }
            } else if (orderBook != null && lmsr == null) {
                type = MarketType.ORDER_BOOK;
                String initialAttr = firstAttribute(orderBook, "initial", "inital");
                String dAttr = orderBook.getAttribute("d");
                String mintAttr = orderBook.getAttribute("allow-mint");
                if (initialAttr == null || initialAttr.isBlank()) {
                    throw new GuessMarketException("Event " + id + " order-book is missing the initial attribute.");
                }
                initial = parseInt(initialAttr.trim(), "initial of event " + id);
                if (initial < 0) {
                    throw new GuessMarketException("Invalid initial investment for event " + id + ".");
                }
                d = parseInt(dAttr.trim(), "d of event " + id);
                if (d <= 0) {
                    throw new GuessMarketException("Invalid base value d for event " + id + ": d must be greater than zero.");
                }
                if (mintAttr == null || mintAttr.isBlank()) {
                    throw new GuessMarketException("Event " + id + " order-book is missing allow-mint.");
                }
                allowMint = Boolean.parseBoolean(mintAttr.trim());
            } else {
                throw new GuessMarketException("Event " + id + " must define exactly one of GM-LMSR or GM-order-book.");
            }

            result.put(id, new RawEvent(id, eventName, description, commission, commissionType,
                    optionNames.get(0), optionNames.get(1), type, b, initial, d, allowMint));
        }
        return result;
    }

    private Map<String, User> parseUsers(Document document, Set<Integer> eventIds) throws GuessMarketException {
        NodeList userNodes = document.getElementsByTagName("GM-user");
        if (userNodes.getLength() == 0) {
            throw new GuessMarketException("The XML file does not contain any users.");
        }
        Map<String, User> result = new LinkedHashMap<>();
        for (int i = 0; i < userNodes.getLength(); i++) {
            Element userElement = (Element) userNodes.item(i);
            String name = userElement.getAttribute("name").trim();
            if (name.isEmpty()) {
                throw new GuessMarketException("A user is missing a name.");
            }
            if (result.containsKey(name)) {
                throw new GuessMarketException("Duplicate user name detected: " + name + ". Every user name must be unique.");
            }
            int cash = parseInt(requiredText(userElement, "initial-cash", "initial-cash of user " + name),
                    "initial-cash of user " + name);
            if (cash <= 0) {
                throw new GuessMarketException("User " + name + " has invalid initial-cash " + cash
                        + ". Initial cash must be greater than 0.");
            }
            Set<Integer> mmEvents = new HashSet<>();
            Element mmElement = firstChildElementAny(userElement, "GM-market-maker", "GM-mareket-maker");
            if (mmElement != null) {
                NodeList children = mmElement.getChildNodes();
                for (int c = 0; c < children.getLength(); c++) {
                    Node node = children.item(c);
                    if (node.getNodeType() != Node.ELEMENT_NODE || !"event".equals(node.getNodeName())) {
                        continue;
                    }
                    Element eventRef = (Element) node;
                    int eventId = parseInt(eventRef.getAttribute("id").trim(), "market-maker event id of user " + name);
                    if (!eventIds.contains(eventId)) {
                        throw new GuessMarketException("User " + name + " is a market maker of event " + eventId
                                + ", but that event does not exist in the file.");
                    }
                    if (!mmEvents.add(eventId)) {
                        throw new GuessMarketException("User " + name + " is assigned twice as market maker of event "
                                + eventId + ".");
                    }
                }
            }
            result.put(name, new User(name, cash, mmEvents));
        }
        return result;
    }

    private void ensureLoaded() throws GuessMarketException {
        if (!loadedMarket) {
            throw new GuessMarketException("No valid Guess Market XML is currently loaded.");
        }
    }

    private static String requiredText(Element parent, String tagName, String label) throws GuessMarketException {
        Element child = firstChildElement(parent, tagName);
        if (child == null || child.getTextContent() == null || child.getTextContent().trim().isEmpty()) {
            throw new GuessMarketException("Missing or empty " + label + ".");
        }
        return child.getTextContent().trim();
    }

    private static Element firstChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Element firstChildElementAny(Element parent, String... tagNames) {
        for (String tagName : tagNames) {
            Element found = firstChildElement(parent, tagName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String firstAttribute(Element element, String... names) {
        for (String name : names) {
            if (element.hasAttribute(name)) {
                return element.getAttribute(name);
            }
        }
        return null;
    }

    private static List<String> directChildTexts(Element parent, String tagName) {
        List<String> values = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                values.add(node.getTextContent().trim());
            }
        }
        return values;
    }

    private static int parseInt(String value, String label) throws GuessMarketException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new GuessMarketException("Invalid integer value for " + label + ": " + value, ex);
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // Optional parser feature.
        }
    }

    private record ParsedMarket(List<MarketEvent> events, Map<String, User> users) {
    }

    private record RawEvent(int id, String name, String description, int commission, CommissionType commissionType,
                            String option1, String option2, MarketType marketType, Double b, Integer initial,
                            Integer d, boolean allowMint) {
    }
}
