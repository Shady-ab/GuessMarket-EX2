package guessmarket.ui;

import guessmarket.engine.BookOrder;
import guessmarket.engine.BuyResult;
import guessmarket.engine.CloseResult;
import guessmarket.engine.CommissionType;
import guessmarket.engine.EventStatus;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineImpl;
import guessmarket.engine.GuessMarketException;
import guessmarket.engine.MarketEvent;
import guessmarket.engine.MarketOption;
import guessmarket.engine.MarketType;
import guessmarket.engine.OptionBookSnapshot;
import guessmarket.engine.OrderResult;
import guessmarket.engine.OrderSide;
import guessmarket.engine.TradeRecord;
import guessmarket.engine.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuessMarketApp extends Application {
    private final GuessMarketEngine engine = new GuessMarketEngineImpl();

    private Label statusLabel;
    private ProgressBar progressBar;
    private ListView<String> usersList;
    private TableView<EventRow> eventsTable;
    private TextArea userDetails;
    private TextArea eventDetails;
    private ToggleButton lmsrToggle;
    private ToggleButton orderBookToggle;
    private ToggleButton notStartedToggle;
    private ToggleButton activeToggle;
    private ToggleButton closedToggle;
    private ToggleButton onPurchaseToggle;
    private ToggleButton onCloseToggle;
    private Button openButton;
    private Button closeButton;
    private Button tradeButton;

    private String selectedUserName;
    private Integer selectedEventId;

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Guess Market - Exercise 2");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(buildToolbar(stage));
        root.setLeft(buildUsersPane());
        root.setCenter(buildEventsPane());
        root.setBottom(buildActionsPane());

        Scene scene = new Scene(root, 1280, 800);
        var css = getClass().getResource("/guessmarket/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
        refresh();
    }

    private VBox buildToolbar(Stage stage) {
        Button loadButton = new Button("Load XML");
        loadButton.setOnAction(e -> loadXml(stage));
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(220);
        statusLabel = new Label("Load an Exercise-2 XML file to begin.");
        HBox row = new HBox(12, loadButton, progressBar, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, row);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    private VBox buildUsersPane() {
        Label title = new Label("Users");
        title.getStyleClass().add("section-title");
        usersList = new ListView<>();
        usersList.setPrefWidth(260);
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedUserName = extractUserName(newV);
            refreshDetails();
        });
        userDetails = new TextArea();
        userDetails.setEditable(false);
        userDetails.setWrapText(true);
        userDetails.setPrefHeight(280);
        VBox.setVgrow(userDetails, Priority.ALWAYS);
        VBox box = new VBox(8, title, usersList, new Label("Selected user"), userDetails);
        box.setPadding(new Insets(0, 12, 0, 0));
        box.setPrefWidth(300);
        VBox.setVgrow(usersList, Priority.ALWAYS);
        return box;
    }

    private VBox buildEventsPane() {
        Label title = new Label("Events");
        title.getStyleClass().add("section-title");

        lmsrToggle = filterToggle("LMSR", true);
        orderBookToggle = filterToggle("Order Book", true);
        notStartedToggle = filterToggle("Not started", true);
        activeToggle = filterToggle("Active", true);
        closedToggle = filterToggle("Closed", true);
        onPurchaseToggle = filterToggle("on-purchase", true);
        onCloseToggle = filterToggle("on-close", true);

        FlowPane filters = new FlowPane(8, 8,
                new Label("Type:"), lmsrToggle, orderBookToggle,
                new Label("Status:"), notStartedToggle, activeToggle, closedToggle,
                new Label("Commission:"), onPurchaseToggle, onCloseToggle);
        filters.getStyleClass().add("filters");

        eventsTable = new TableView<>();
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventsTable.getColumns().add(col("ID", "id", 50));
        eventsTable.getColumns().add(col("Name", "name", 180));
        eventsTable.getColumns().add(col("Status", "status", 110));
        eventsTable.getColumns().add(col("Type", "type", 110));
        eventsTable.getColumns().add(col("Commission", "commission", 140));
        eventsTable.getColumns().add(col("Account", "account", 90));
        eventsTable.getColumns().add(col("Market maker", "marketMaker", 110));
        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedEventId = newV == null ? null : newV.getEventId();
            refreshDetails();
        });

        eventDetails = new TextArea();
        eventDetails.setEditable(false);
        eventDetails.setWrapText(true);
        eventDetails.setPrefHeight(260);
        ScrollPane tableScroll = new ScrollPane(eventsTable);
        tableScroll.setFitToWidth(true);
        tableScroll.setFitToHeight(true);
        VBox.setVgrow(tableScroll, Priority.ALWAYS);
        VBox.setVgrow(eventDetails, Priority.ALWAYS);

        VBox box = new VBox(8, title, filters, tableScroll, new Label("Selected event"), eventDetails);
        return box;
    }

    private HBox buildActionsPane() {
        openButton = new Button("Open event");
        closeButton = new Button("Close event");
        tradeButton = new Button("Participate / Trade");
        openButton.setOnAction(e -> openSelected());
        closeButton.setOnAction(e -> closeSelected());
        tradeButton.setOnAction(e -> tradeSelected());
        HBox box = new HBox(10, openButton, closeButton, tradeButton);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private ToggleButton filterToggle(String text, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.setSelected(selected);
        button.setOnAction(e -> refreshEvents());
        return button;
    }

    private TableColumn<EventRow, String> col(String title, String property, int width) {
        TableColumn<EventRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private void loadXml(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Guess Market XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Loading " + file.getName() + " ...");
                updateProgress(0.2, 1);
                Thread.sleep(1200);
                updateProgress(0.7, 1);
                engine.loadFromXml(file.getAbsolutePath());
                Thread.sleep(400);
                updateProgress(1, 1);
                updateMessage("XML loaded successfully: " + file.getName());
                return null;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            selectedUserName = null;
            selectedEventId = null;
            refresh();
            statusLabel.setText("XML is valid and was loaded successfully.");
        });
        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setProgress(0);
            Throwable error = task.getException();
            String message = error instanceof GuessMarketException ? error.getMessage() : error.getMessage();
            statusLabel.setText("Load failed. Previous valid market was not changed.");
            showError("The XML file is not valid.\n" + message);
        });
        Thread thread = new Thread(task, "xml-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void refresh() {
        refreshUsers();
        refreshEvents();
        refreshDetails();
    }

    private void refreshUsers() {
        String keep = selectedUserName;
        usersList.getItems().clear();
        if (!engine.hasLoadedMarket()) {
            return;
        }
        for (User user : engine.getUsers()) {
            String blocked = user.isBlocked() ? " [BLOCKED]" : "";
            usersList.getItems().add(user.getName() + " | cash " + UiFormat.money(user.getCash()) + blocked);
        }
        if (keep != null) {
            usersList.getItems().stream()
                    .filter(item -> extractUserName(item).equals(keep))
                    .findFirst()
                    .ifPresent(item -> usersList.getSelectionModel().select(item));
        } else if (!usersList.getItems().isEmpty()) {
            usersList.getSelectionModel().selectFirst();
        }
    }

    private void refreshEvents() {
        Integer keep = selectedEventId;
        eventsTable.getItems().clear();
        if (!engine.hasLoadedMarket()) {
            return;
        }
        for (MarketEvent event : engine.getEvents()) {
            if (!passesFilters(event)) {
                continue;
            }
            eventsTable.getItems().add(new EventRow(event));
        }
        if (keep != null) {
            eventsTable.getItems().stream()
                    .filter(row -> row.getEventId() == keep)
                    .findFirst()
                    .ifPresent(row -> eventsTable.getSelectionModel().select(row));
        } else if (!eventsTable.getItems().isEmpty()) {
            eventsTable.getSelectionModel().selectFirst();
        }
    }

    private boolean passesFilters(MarketEvent event) {
        boolean typeOk = (event.getMarketType() == MarketType.LMSR && lmsrToggle.isSelected())
                || (event.getMarketType() == MarketType.ORDER_BOOK && orderBookToggle.isSelected());
        boolean statusOk = (event.getStatus() == EventStatus.NOT_STARTED && notStartedToggle.isSelected())
                || (event.getStatus() == EventStatus.ACTIVE && activeToggle.isSelected())
                || (event.getStatus() == EventStatus.CLOSED && closedToggle.isSelected());
        boolean commissionOk = (event.getCommissionType() == CommissionType.ON_PURCHASE && onPurchaseToggle.isSelected())
                || (event.getCommissionType() == CommissionType.ON_CLOSE && onCloseToggle.isSelected());
        return typeOk && statusOk && commissionOk;
    }

    private void refreshDetails() {
        userDetails.setText(buildUserText());
        eventDetails.setText(buildEventText());
        boolean loaded = engine.hasLoadedMarket();
        boolean userOk = selectedUserName != null;
        boolean eventOk = selectedEventId != null;
        MarketEvent event = null;
        User user = null;
        try {
            if (loaded && eventOk) {
                event = engine.getEventById(selectedEventId);
            }
            if (loaded && userOk) {
                user = engine.getUserByName(selectedUserName);
            }
        } catch (GuessMarketException ignored) {
            // Details already show the missing selection.
        }
        boolean isMm = user != null && event != null && user.isMarketMakerOf(event.getId());
        openButton.setDisable(!(isMm && event.getStatus() == EventStatus.NOT_STARTED));
        closeButton.setDisable(!(isMm && event.getStatus() == EventStatus.ACTIVE));
        tradeButton.setDisable(!(user != null && event != null && event.getStatus() == EventStatus.ACTIVE
                && !user.isBlocked()));
    }

    private String buildUserText() {
        if (!engine.hasLoadedMarket()) {
            return "No market is loaded.";
        }
        if (selectedUserName == null) {
            return "Select a user.";
        }
        try {
            User user = engine.getUserByName(selectedUserName);
            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(user.getName()).append('\n');
            sb.append("Cash: ").append(UiFormat.money(user.getCash())).append('\n');
            sb.append("Blocked: ").append(user.isBlocked() ? "YES" : "NO").append('\n');
            if (user.getLastNotice() != null) {
                sb.append(user.getLastNotice()).append('\n');
            }
            sb.append("Market maker of events: ").append(user.getMarketMakerEventIds()).append('\n');
            sb.append("Participating in: ").append(user.getParticipatingEventIds()).append("\n\n");
            if (selectedEventId != null && user.getParticipatingEventIds().contains(selectedEventId)) {
                MarketEvent event = engine.getEventById(selectedEventId);
                sb.append("Involvement in event ").append(event.getName()).append(":\n");
                if (event.getMarketType() == MarketType.LMSR) {
                    List<TradeRecord> trades = user.getPersonalTradesNewestFirst(event.getId());
                    if (trades.isEmpty()) {
                        sb.append("  No personal trades yet.\n");
                    } else {
                        for (TradeRecord trade : trades) {
                            sb.append("  ").append(trade.getOptionName())
                                    .append(" | shares: ").append(trade.getQuantity())
                                    .append(" | paid: ").append(UiFormat.money(trade.getTotalPaid()))
                                    .append(" | commission: ").append(UiFormat.money(trade.getCommission()))
                                    .append('\n');
                        }
                    }
                } else {
                    for (int i = 0; i < 2; i++) {
                        MarketOption option = event.getOptions().get(i);
                        sb.append("  ").append(option.getName())
                                .append(" | shares: ").append(user.getHolding(event.getId(), i))
                                .append(" | paid: ").append(UiFormat.money(user.getAmountPaid(event.getId(), i)))
                                .append('\n');
                    }
                    sb.append("  Commission paid: ").append(UiFormat.money(user.getCommissionPaid(event.getId()))).append('\n');
                    if (event.getStatus() == EventStatus.CLOSED) {
                        int winner = event.getWinningOptionIndex();
                        double unit = event.getD();
                        double profit = user.getHolding(event.getId(), winner) * unit
                                - user.getAmountPaid(event.getId(), 0) - user.getAmountPaid(event.getId(), 1);
                        sb.append("  Profit/loss after close: ").append(UiFormat.money(profit)).append('\n');
                    }
                }
                if (event.getStatus() == EventStatus.CLOSED) {
                    sb.append("  Winner: ").append(event.getWinningOptionName()).append('\n');
                    for (int i = 0; i < 2; i++) {
                        sb.append("  Total shares ").append(event.getOptions().get(i).getName())
                                .append(": ").append(event.getOptions().get(i).getOutstandingShares()).append('\n');
                    }
                }
            }
            return sb.toString();
        } catch (GuessMarketException ex) {
            return ex.getMessage();
        }
    }

    private String buildEventText() {
        if (!engine.hasLoadedMarket()) {
            return "No market is loaded.";
        }
        if (selectedEventId == null) {
            return "Select an event.";
        }
        try {
            MarketEvent event = engine.getEventById(selectedEventId);
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(event.getId()).append('\n');
            sb.append("Name: ").append(event.getName()).append('\n');
            sb.append("Description: ").append(event.getDescription()).append('\n');
            sb.append("Status: ").append(event.getStatus().displayName()).append('\n');
            sb.append("Type: ").append(event.getMarketType()).append('\n');
            sb.append("Commission: ").append(UiFormat.commission(event)).append('\n');
            sb.append("Market maker: ").append(event.getMarketMakerName()).append('\n');
            sb.append("Event account: ").append(UiFormat.money(event.getAccountBalance())).append('\n');
            sb.append("Collected commission: ").append(UiFormat.money(event.getCollectedCommission())).append('\n');
            if (event.getMarketType() == MarketType.LMSR) {
                sb.append("LMSR b: ").append(UiFormat.money(event.getB())).append('\n');
                sb.append("Required subsidy: ").append(UiFormat.money(event.getRequiredSubsidy())).append('\n');
                sb.append("Current market:\n");
                for (int i = 0; i < 2; i++) {
                    MarketOption option = event.getOptions().get(i);
                    sb.append("  ").append(i + 1).append(") ").append(option.getName())
                            .append(" | value: ").append(UiFormat.money(event.getCurrentPrice(i)))
                            .append(" | shares: ").append(option.getOutstandingShares()).append('\n');
                }
                sb.append("Trade history (newest first):\n");
                List<TradeRecord> history = event.getTradeHistoryNewestFirst();
                if (history.isEmpty()) {
                    sb.append("  No trades yet.\n");
                } else {
                    for (TradeRecord trade : history) {
                        sb.append("  ").append(trade.getUserName()).append(" | ").append(trade.getOptionName())
                                .append(" | shares: ").append(trade.getQuantity())
                                .append(" | paid: ").append(UiFormat.money(trade.getTotalPaid())).append('\n');
                    }
                }
            } else {
                sb.append("d: ").append(event.getD()).append(" | initial: ").append(event.getInitialInvestment())
                        .append(" | allow-mint: ").append(event.isAllowMint()).append('\n');
                for (int i = 0; i < 2; i++) {
                    OptionBookSnapshot book = event.getBookSnapshot(i);
                    sb.append('\n').append(book.getOptionName()).append(" book:\n");
                    sb.append("  LAST=").append(UiFormat.moneyOrDash(book.getLast()))
                            .append(" BID=").append(UiFormat.moneyOrDash(book.getBestBid()))
                            .append(" ASK=").append(UiFormat.moneyOrDash(book.getBestAsk()))
                            .append(" MID=").append(UiFormat.moneyOrDash(book.getMid()))
                            .append(" SPREAD=").append(UiFormat.moneyOrDash(book.getSpread())).append('\n');
                    sb.append("  Bids:\n");
                    appendOrders(sb, book.getBids());
                    sb.append("  Asks:\n");
                    appendOrders(sb, book.getAsks());
                }
                sb.append("\nParticipants:\n");
                event.holdingsByUser(toUserMap()).forEach((name, shares) -> {
                    try {
                        User user = engine.getUserByName(name);
                        sb.append("  ").append(name)
                                .append(" | ").append(event.getOptions().get(0).getName()).append("=").append(shares[0])
                                .append(" (").append(UiFormat.money(user.getAmountPaid(event.getId(), 0))).append(")")
                                .append(" | ").append(event.getOptions().get(1).getName()).append("=").append(shares[1])
                                .append(" (").append(UiFormat.money(user.getAmountPaid(event.getId(), 1))).append(")\n");
                    } catch (GuessMarketException ex) {
                        sb.append("  ").append(name).append('\n');
                    }
                });
            }
            if (event.getStatus() == EventStatus.CLOSED) {
                sb.append("\nWinner: ").append(event.getWinningOptionName()).append('\n');
            }
            return sb.toString();
        } catch (GuessMarketException ex) {
            return ex.getMessage();
        }
    }

    private void appendOrders(StringBuilder sb, List<BookOrder> orders) {
        if (orders.isEmpty()) {
            sb.append("    none\n");
            return;
        }
        for (BookOrder order : orders) {
            sb.append("    ").append(order.getUserName())
                    .append(" | ").append(order.getSide())
                    .append(" | qty ").append(order.getRemainingQuantity())
                    .append(" | price ").append(UiFormat.money(order.getPrice())).append('\n');
        }
    }

    private java.util.Map<String, User> toUserMap() {
        java.util.Map<String, User> map = new java.util.LinkedHashMap<>();
        for (User user : engine.getUsers()) {
            map.put(user.getName(), user);
        }
        return map;
    }

    private void openSelected() {
        try {
            engine.openEvent(selectedUserName, selectedEventId);
            statusLabel.setText("Event opened.");
            refresh();
        } catch (GuessMarketException ex) {
            showError(ex.getMessage());
        }
    }

    private void closeSelected() {
        try {
            MarketEvent event = engine.getEventById(selectedEventId);
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Close event");
            dialog.setHeaderText("Choose the winning option");
            ComboBox<String> options = new ComboBox<>();
            options.getItems().add(event.getOptions().get(0).getName());
            options.getItems().add(event.getOptions().get(1).getName());
            options.getSelectionModel().selectFirst();
            dialog.getDialogPane().setContent(options);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return options.getSelectionModel().getSelectedIndex() + 1;
                }
                return null;
            });
            dialog.showAndWait().ifPresent(winner -> {
                try {
                    CloseResult result = engine.closeEvent(selectedUserName, selectedEventId, winner);
                    showInfo("Event closed.\nWinner: " + result.getWinningOptionName()
                            + "\nGross payout: " + UiFormat.money(result.getGrossPayout())
                            + "\nCommission: " + UiFormat.money(result.getCommission())
                            + "\nReturned to MM: " + UiFormat.money(result.getReturnedToMarketMaker()));
                    refresh();
                } catch (GuessMarketException ex) {
                    showError(ex.getMessage());
                }
            });
        } catch (GuessMarketException ex) {
            showError(ex.getMessage());
        }
    }

    private void tradeSelected() {
        try {
            MarketEvent event = engine.getEventById(selectedEventId);
            if (event.getMarketType() == MarketType.LMSR) {
                lmsrTradeDialog(event);
            } else {
                orderBookTradeDialog(event);
            }
        } catch (GuessMarketException ex) {
            showError(ex.getMessage());
        }
    }

    private void lmsrTradeDialog(MarketEvent event) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("LMSR purchase");
        ComboBox<String> optionBox = new ComboBox<>();
        optionBox.getItems().addAll(event.getOptions().get(0).getName(), event.getOptions().get(1).getName());
        optionBox.getSelectionModel().selectFirst();
        Spinner<Integer> qty = new Spinner<>(1, 1_000_000, 1);
        qty.setEditable(true);
        GridPane grid = labeledGrid(
                "Option", optionBox,
                "Quantity", qty
        );
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK);
        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) {
                return;
            }
            try {
                BuyResult result = engine.buyLmsrShares(selectedUserName, event.getId(),
                        optionBox.getSelectionModel().getSelectedIndex() + 1, qty.getValue());
                showInfo("Purchase completed.\nOption: " + result.getOptionName()
                        + "\nShares: " + result.getQuantity()
                        + "\nShare cost: " + UiFormat.money(result.getShareCost())
                        + "\nCommission: " + UiFormat.money(result.getCommission())
                        + "\nTotal paid: " + UiFormat.money(result.getTotalPaid()));
                refresh();
            } catch (GuessMarketException ex) {
                showError(ex.getMessage());
            }
        });
    }

    private void orderBookTradeDialog(MarketEvent event) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Order book command");
        ComboBox<String> optionBox = new ComboBox<>();
        optionBox.getItems().addAll(event.getOptions().get(0).getName(), event.getOptions().get(1).getName());
        optionBox.getSelectionModel().selectFirst();
        ComboBox<String> sideBox = new ComboBox<>();
        sideBox.getItems().addAll("BUY", "SELL");
        sideBox.getSelectionModel().selectFirst();
        Spinner<Integer> qty = new Spinner<>(1, 1_000_000, 1);
        qty.setEditable(true);
        TextField price = new TextField("0.50");
        GridPane grid = labeledGrid(
                "Option", optionBox,
                "Side", sideBox,
                "Quantity", qty,
                "Price per share", price
        );
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK);
        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) {
                return;
            }
            try {
                double parsedPrice = Double.parseDouble(price.getText().trim());
                OrderResult result = engine.placeOrder(selectedUserName, event.getId(),
                        optionBox.getSelectionModel().getSelectedIndex() + 1,
                        OrderSide.valueOf(sideBox.getValue()), qty.getValue(), parsedPrice);
                showInfo("Order processed.\nFilled: " + result.getFilledQuantity()
                        + "\nResting: " + result.getRemainingQuantity()
                        + "\n" + result.getSummary());
                refresh();
            } catch (NumberFormatException ex) {
                showError("Price must be a number.");
            } catch (GuessMarketException ex) {
                showError(ex.getMessage());
            }
        });
    }

    private GridPane labeledGrid(Object... labelAndControl) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        for (int i = 0; i < labelAndControl.length; i += 2) {
            grid.add(new Label(String.valueOf(labelAndControl[i])), 0, i / 2);
            grid.add((javafx.scene.Node) labelAndControl[i + 1], 1, i / 2);
        }
        return grid;
    }

    private String extractUserName(String listValue) {
        if (listValue == null) {
            return null;
        }
        int cut = listValue.indexOf(" | ");
        return cut < 0 ? listValue : listValue.substring(0, cut);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Error");
        alert.showAndWait();
        if (selectedUserName != null) {
            try {
                User user = engine.getUserByName(selectedUserName);
                if (user.getLastNotice() != null) {
                    statusLabel.setText(user.getLastNotice());
                }
            } catch (GuessMarketException ignored) {
                // Keep the original error dialog.
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText("Success");
        alert.showAndWait();
    }

    public static final class EventRow {
        private final int eventId;
        private final String id;
        private final String name;
        private final String status;
        private final String type;
        private final String commission;
        private final String account;
        private final String marketMaker;

        public EventRow(MarketEvent event) {
            this.eventId = event.getId();
            this.id = String.valueOf(event.getId());
            this.name = event.getName();
            this.status = event.getStatus().displayName();
            this.type = event.getMarketType() == MarketType.LMSR ? "LMSR" : "Order Book";
            this.commission = UiFormat.commission(event);
            this.account = UiFormat.money(event.getAccountBalance());
            this.marketMaker = event.getMarketMakerName();
        }

        public int getEventId() {
            return eventId;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getType() {
            return type;
        }

        public String getCommission() {
            return commission;
        }

        public String getAccount() {
            return account;
        }

        public String getMarketMaker() {
            return marketMaker;
        }
    }
}
