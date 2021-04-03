package othello;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static othello.Localization.L10N;

/**
 * JavaFX GUI for the project.
 */
public class GUI extends Application {

    private static final Path HIGHSCORE_PATH = Paths.get("highscores.csv");

    // layout-related constants
    private static final int PADDING = 10;
    private static final int MIN_WIDTH = 610;
    private static final int MIN_HEIGHT = 660;
    /**
     * Unusually big font size for the heading and the start game button.
     */
    private static final int BIG_FONT_SIZE = 30;

    // string constants for css classes
    private static final String HELP_CLASS = "helpButton";
    private static final String SKIP_CLASS = "skipButton";
    private static final String DISK_CLASS = "diskButton";
    private static final String FIELD_BACKGROUND_CLASS = "fieldBackground";
    private static final String BLACK_BUTTON_CLASS = "blackButton";
    private static final String WHITE_BUTTON_CLASS = "whiteButton";
    private static final String HOVER_ERROR_CLASS = "hoverError";
    private static final String POSSIBLE_MOVE_BLACK_CLASS = "possibleMoveBlack";
    private static final String POSSIBLE_MOVE_WHITE_CLASS = "possibleMoveWhite";

    private final Button[][] guiField = new Button[8][8];
    /**
     * Skip button that changes color depending on the current player.
     */
    final private Button skipButton = new Button(L10N.get("skip_button_text"));
    final private Button restartButton = new Button(L10N.get("restart_button_text"));
    final private Label playerLabel = new Label(L10N.get("player_label_text"));
    final private Label scoreLabel = new Label(L10N.get("black_player_text") + ": 0\n" + L10N.get("white_player_text") + ": 0");
    /**
     * stores the game state
     */
    private FieldState state;
    private FieldState.FieldType currentPlayer;
    private Player[] players;
    private FieldState.FieldType aiPlayer;
    private GameMode mode;
    private OthelloAI ai;
    private Highscores highscores;
    /**
     * Needed to execute different code on first run.
     * Is set to false on first run.
     */
    private boolean firstRun = true;

    // set fields to default values
    {
        this.reset();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(L10N.get("window_title"));

        primaryStage.setMinHeight(GUI.MIN_HEIGHT);
        primaryStage.setMinWidth(GUI.MIN_WIDTH);

        this.makeStartScene(primaryStage);
        primaryStage.show();
    }

    /**
     * Creates the start scene.
     *
     * @param stage The (primary) stage. Must not be {@code null}.
     */
    private void makeStartScene(Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        final GridPane grid = new GridPane();
        grid.setHgap(GUI.PADDING);
        grid.setVgap(GUI.PADDING);
        grid.setPadding(new Insets(GUI.PADDING));

        // reset default values
        this.reset();

        // add skip tooltip
        this.playerLabel.setTooltip(new Tooltip(L10N.get("skip_tooltip")));
        this.skipButton.setTooltip(new Tooltip(L10N.get("skip_tooltip")));

        // make the grid scale correctly
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(50);
        columnConstraints.setHgrow(Priority.ALWAYS);
        // two columns, so two times
        grid.getColumnConstraints().add(columnConstraints);
        grid.getColumnConstraints().add(columnConstraints);

        final Label newGame = new Label(L10N.get("new_game_text"));
        newGame.setFont(new Font(BIG_FONT_SIZE));
        grid.add(newGame, 0, 0, 2, 1);

        // text fields for name entry
        final TextField blackField = new TextField(L10N.get("black_player_text").replaceAll("\\s+", ""));
        blackField.textProperty().addListener(action ->
                GUI.this.players[FieldState.FieldType.BLACK.ordinal()] =
                        new Player(FieldState.FieldType.BLACK, blackField.getText()));
        final TextField whiteField = new TextField(L10N.get("white_player_text").replaceAll("\\s+", ""));
        whiteField.textProperty().addListener(action ->
                GUI.this.players[FieldState.FieldType.WHITE.ordinal()] =
                        new Player(FieldState.FieldType.WHITE, whiteField.getText()));
        this.players[FieldState.FieldType.WHITE.ordinal()] = new Player(FieldState.FieldType.WHITE, L10N.get("white_player_text"));
        this.players[FieldState.FieldType.BLACK.ordinal()] = new Player(FieldState.FieldType.BLACK, L10N.get("black_player_text"));

        int row = 1;
        int col = 0;

        // game mode selection
        final Label gameMode = new Label(L10N.get("game_mode_label_text"));
        final ToggleGroup modeGroup = new ToggleGroup();
        final RadioButton pvpMode = new RadioButton(L10N.get("pvp_mode_text"));
        pvpMode.setToggleGroup(modeGroup);
        final RadioButton aiMode = new RadioButton(L10N.get("ai_mode_text"));
        aiMode.setToggleGroup(modeGroup);
        // the UI should not allow entering a name for the AI
        aiMode.setOnAction(action -> {
            GUI.this.mode = GameMode.PLAYER_VERSUS_AI;
            switch (GUI.this.currentPlayer) {
                case BLACK -> whiteField.setDisable(true);
                case WHITE -> blackField.setDisable(true);
            }
        });
        modeGroup.selectToggle(pvpMode);
        pvpMode.setOnAction(action -> {
            GUI.this.mode = GameMode.PLAYER_VERSUS_PLAYER;
            blackField.setDisable(false);
            whiteField.setDisable(false);
        });
        grid.add(gameMode, col, row++);
        grid.add(pvpMode, col, row++);
        grid.add(aiMode, col, row);

        row = 1;

        // starting player selection
        final Label startingPlayer = new Label(L10N.get("select_starting_player_text"));
        final ToggleGroup startGroup = new ToggleGroup();
        final RadioButton black = new RadioButton(L10N.get("black_player_text"));
        black.setToggleGroup(startGroup);
        black.setOnAction(action -> {
            GUI.this.currentPlayer = FieldState.FieldType.BLACK;
            if (GUI.this.mode == GameMode.PLAYER_VERSUS_PLAYER) {
                blackField.setDisable(false);
                whiteField.setDisable(false);
            } else {
                // don't allow entering a name for the AI player
                whiteField.setDisable(true);
                blackField.setDisable(false);
            }
        });
        final RadioButton white = new RadioButton(L10N.get("white_player_text"));
        white.setToggleGroup(startGroup);
        white.setOnAction(action -> {
            GUI.this.currentPlayer = FieldState.FieldType.WHITE;
            // don't allow entering a name for the AI player
            blackField.setDisable(GUI.this.mode == GameMode.PLAYER_VERSUS_AI);
            whiteField.setDisable(false);
        });
        startGroup.selectToggle(black);
        grid.add(startingPlayer, ++col, row++);
        grid.add(black, col, row++);
        grid.add(white, col, row++);

        // name entry
        col = 0;
        final Label enterNames = new Label(L10N.get("enter_name_text"));
        final Label blackName = new Label(L10N.get("black_player_text"));
        final Label whiteName = new Label(L10N.get("white_player_text"));
        grid.add(enterNames, col, row++, 2, 1);
        grid.add(blackName, col, row);
        grid.add(blackField, col + 1, row++);
        grid.add(whiteName, col, row);
        grid.add(whiteField, col + 1, row++);

        // start button
        final Button start = new Button(L10N.get("start_game_text"));
        start.setFont(new Font(BIG_FONT_SIZE));
        start.setMaxHeight(Double.MAX_VALUE);
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnMouseClicked(event -> GUI.this.makeMainScene(stage));
        grid.add(start, 0, row, 2, 1);

        // on the first run, it's not possible to simple use the width of the previous scene
        if (this.firstRun) {
            this.firstRun = false;
            // use the minimal values as initial values for width and height
            stage.setWidth(MIN_WIDTH);
            stage.setHeight(MIN_HEIGHT);
            stage.setScene(new Scene(grid, stage.getWidth(), stage.getHeight()));
        } else {
            // if there already was a previous scene, use its dimensions
            stage.setScene(new Scene(grid, stage.getScene().getWidth(), stage.getScene().getHeight()));
        }
    }

    /**
     * Creates the main scene.
     *
     * @param stage The (primary) stage. Must not be {@code null}.
     */
    private void makeMainScene(Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        this.state = new FieldState();
        final BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(GUI.PADDING));

        // try loading the highscores, if it fails, display an error alert
        try {
            this.highscores = new Highscores(HIGHSCORE_PATH);
        } catch (ParseException | IOException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Highscore loading failed");
            alert.setHeaderText(e.getMessage());
            // store the exception's stack trace in a string
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            e.printStackTrace();
            alert.setContentText(stackTrace.toString());
            alert.show(); // show() is non-blocking
        }

        // UI controls
        final GridPane controls = new GridPane();
        int controlsCol = 0;
        controls.setHgap((3 * GUI.PADDING) / 2.0);
        controls.setVgap(GUI.PADDING);
        controls.setPadding(new Insets(0, 0, GUI.PADDING, 0));
        borderPane.setTop(controls);

        // current player and skip button
        this.skipButton.getStyleClass().setAll(SKIP_CLASS, BLACK_BUTTON_CLASS);
        this.skipButton.setOnMouseClicked(this::skipButtonClicked);
        controls.add(this.playerLabel, controlsCol++, 0);
        controls.add(this.skipButton, controlsCol++, 0);

        // score text
        controls.add(this.scoreLabel, controlsCol++, 0);

        // restart button
        this.restartButton.setOnMouseClicked(event -> this.restartButtonClicked(stage));
        this.restartButton.setMaxHeight(Double.MAX_VALUE);
        controls.add(this.restartButton, controlsCol++, 0);

        // highscore button
        final Button highscoreButton = new Button(L10N.get("highscore_text"));
        highscoreButton.setOnMouseClicked(this::highscoreButtonClicked);
        highscoreButton.setMaxHeight(Double.MAX_VALUE);
        controls.add(highscoreButton, controlsCol++, 0);

        // Help button
        final Button help = new Button("?");
        help.getStyleClass().add(HELP_CLASS);
        help.setOnMouseClicked(this::helpButtonClicked);
        controls.add(help, controlsCol++, 0);

        // Field
        final GridPane field = new GridPane();
        field.setHgap(GUI.PADDING);
        field.setVgap(GUI.PADDING);
        field.setPadding(new Insets(GUI.PADDING));

        // make the field scale correctly
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100 / 8.0);
        columnConstraints.setHgrow(Priority.ALWAYS);
        for (int i = 0; i < 8; ++i) {
            field.getColumnConstraints().add(columnConstraints);
        }
        final RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(100 / 8.0);
        rowConstraints.setVgrow(Priority.ALWAYS);
        for (int i = 0; i < 8; ++i) {
            field.getRowConstraints().add(rowConstraints);
        }

        // create buttons
        for (int row = 0; row < 8; ++row) {
            for (int col = 0; col < 8; ++col) {
                final Button tmp = new Button();
                this.guiField[col][row] = tmp;
                // final copies needed in lambda expression below
                final int xPos = col;
                final int yPos = row;
                tmp.getStyleClass().add(DISK_CLASS);

                // make sure the buttons grow when the window is resized
                tmp.setMaxHeight(Double.MAX_VALUE);
                tmp.setMaxWidth(Double.MAX_VALUE);

                // try to keep the buttons width and height aligned
                // the add(2) is there to allow resizing
                // (the max height/width is always two pixels more than the current value)
                // (currently unused, because it's buggy when you restart the game in a resized window)
                //tmp.maxWidthProperty().bind(tmp.heightProperty().add(2));
                //tmp.maxHeightProperty().bind(tmp.widthProperty().add(2));

                // add event listener
                tmp.setOnMouseClicked(event -> GUI.this.buttonClicked(xPos, yPos));
                field.add(tmp, col, row);
            }
        }
        field.getStyleClass().add(FIELD_BACKGROUND_CLASS);
        borderPane.setCenter(field);
        // use the same width and height as the start screen for the main scene
        final Scene scene = new Scene(borderPane, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getClassLoader().getResource("style.css")).toExternalForm());
        stage.setScene(scene);
        this.disableButtons(false);
        this.update();
    }

    /**
     * Apply changes to the field state to the GUI.
     */
    private void update() {
        this.state.forEach((idx, type) -> {
            final Button tmp = GUI.this.guiField[idx.xPos][idx.yPos];
            if (type == FieldState.FieldType.EMPTY) {
                tmp.getStyleClass().setAll(DISK_CLASS, HOVER_ERROR_CLASS);
            } else {
                tmp.getStyleClass().setAll(DISK_CLASS, HOVER_ERROR_CLASS,
                        type == FieldState.FieldType.BLACK ? GUI.BLACK_BUTTON_CLASS : GUI.WHITE_BUTTON_CLASS
                );
            }
        });
        final Set<Index> possibleMoves = this.state.findPossibleMoves(this.currentPlayer, true);
        possibleMoves.forEach(idx -> GUI.this.guiField[idx.xPos][idx.yPos].getStyleClass().setAll(
                this.currentPlayer == FieldState.FieldType.BLACK ? POSSIBLE_MOVE_BLACK_CLASS : POSSIBLE_MOVE_WHITE_CLASS,
                DISK_CLASS));
        // if the player can't make a move, "encourage" him to click the skip button
        if (possibleMoves.isEmpty() && !this.state.gameOver()) {
            this.skipButton.setScaleX(1.2);
            this.skipButton.setScaleY(1.2);
        } else {
            this.skipButton.setScaleX(1);
            this.skipButton.setScaleY(1);
        }
        // update skip button color
        this.skipButton.getStyleClass().setAll(SKIP_CLASS,
                this.currentPlayer == FieldState.FieldType.WHITE ? WHITE_BUTTON_CLASS : BLACK_BUTTON_CLASS);
        // update current player and score labels
        // update current player and score labels
        switch (this.mode) {
            case PLAYER_VERSUS_PLAYER -> {
                this.playerLabel.setText(
                        L10N.get("player_label_text").replaceAll("%p",
                                this.currentPlayer == FieldState.FieldType.WHITE ? L10N.get("white_player_text") : L10N.get("black_player_text")));
                this.scoreLabel.setText(
                        L10N.get("black_player_text") + ": " + this.state.countFields(FieldState.FieldType.BLACK) + "\n" +
                                L10N.get("white_player_text") + ": " + this.state.countFields(FieldState.FieldType.WHITE));
            }
            case PLAYER_VERSUS_AI -> {
                this.playerLabel.setText(
                        L10N.get("player_label_text").replaceAll("%p",
                                this.currentPlayer == FieldState.FieldType.WHITE ? L10N.get("white_player_text") : L10N.get("black_player_text"))
                                .replaceAll("%a", this.aiPlayer == FieldState.FieldType.WHITE ? L10N.get("white_player_text") : L10N.get("black_player_text")));
                final String player = this.currentPlayer == FieldState.FieldType.BLACK ? L10N.get("black_player_text") : L10N.get("white_player_text");
                this.scoreLabel.setText(
                        player + ": " + this.state.countFields(this.currentPlayer) + "\n" +
                                L10N.get("ai_text") + ": " + this.state.countFields(this.aiPlayer));
            }
        }
    }

    /**
     * Handle a button click.
     *
     * @param xPos The x position of the button. 0 <= xPos <= 7
     * @param yPos The y position of the button. 0 <= yPos <= 7
     */
    private void buttonClicked(int xPos, int yPos) {
        if (xPos < 0 || xPos > 7 || yPos < 0 || yPos > 7) {
            throw new IllegalArgumentException("invalid xPos or yPos argument");
        }
        // if the move fails, makeMove() throws an exception and the execution stops here
        this.state = this.state.makeMove(this.currentPlayer, Index.of(xPos, yPos));
        this.finishPlayerMove();
    }

    /**
     * This method is called after a player finishes a move.
     * It toggles the player or allows the AI to make a move, depending on the game mode.
     */
    private void finishPlayerMove() {
        switch (this.mode) {
            case PLAYER_VERSUS_PLAYER -> this.toggleCurrentPlayer();
            // the buttons should be disabled while the AI is thinking
            case PLAYER_VERSUS_AI -> {
                this.disableButtons(true);
                this.aiMove();
                this.disableButtons(false);
            }
        }
        this.update();
        if (this.state.gameOver()) {
            this.gameOver();
        }
    }

    /**
     * Disable or enable all the buttons on the field.
     *
     * @param disabled {@code true} when the buttons should be disabled, {@code false} when they should be enabled.
     */
    private void disableButtons(boolean disabled) {
        for (Button[] row : this.guiField) {
            for (Button btn : row) {
                btn.setDisable(disabled);
            }
        }
        this.skipButton.setDisable(disabled);
    }

    /**
     * Let the AI make a move. (Does not update the field).
     */
    private void aiMove() {
        // initialize the AI if it wasn't initialized before
        if (this.ai == null || this.ai.getType() != this.aiPlayer || this.ai.getType() == this.currentPlayer) {
            this.aiPlayer = this.currentPlayer == FieldState.FieldType.WHITE ?
                    FieldState.FieldType.BLACK : FieldState.FieldType.WHITE;
            this.ai = new OthelloAI(this.aiPlayer);
            this.players[this.ai.getType().ordinal()] = this.ai;
        }
        // the AI can't always make moves, hence the value is optional
        Optional<Index> move = this.ai.makeMove(state);
        move.ifPresent(index -> this.state = this.state.makeMove(this.aiPlayer, index));
    }

    /**
     * Reset fields to default values.
     */
    private void reset() {
        // this method is called when the game is restarted
        this.mode = GameMode.PLAYER_VERSUS_PLAYER;
        this.currentPlayer = FieldState.FieldType.BLACK;
        this.players = new Player[FieldState.FieldType.values().length];
        this.aiPlayer = FieldState.FieldType.WHITE;
        this.ai = null;
    }

    /**
     * This method is called when the game is over.
     */
    private void gameOver() {
        this.disableButtons(true);
        this.restartButton.setEffect(null); // there doesn't seem to be a method to focus a button, but this does the job...
        Optional<FieldState.FieldType> winnerType = this.state.calculateWinner();
        Alert.AlertType alertType = Alert.AlertType.INFORMATION;

        // this shouldn't happen, but it doesn't hurt to check
        if (winnerType.isEmpty()) return;

        String alertHeading;
        String alertBody;
        final FieldState.FieldType type = winnerType.get();
        if (type != FieldState.FieldType.EMPTY) {
            final Player winner = GUI.this.players[type.ordinal()];
            final Date now = Calendar.getInstance().getTime();
            final int score = GUI.this.state.countFields(type);
            try {
                // if high score loading failed at the start of the game, try again
                if (this.highscores == null) this.highscores = new Highscores(HIGHSCORE_PATH);
                if (this.mode != GameMode.PLAYER_VERSUS_AI || winner.getType() != this.aiPlayer) {
                    // store the new high score
                    this.highscores.addScore(new Highscores.Score(now, winner.getName(), score));
                    // remove the padding whitespace
                    final String color = winner.getType() == FieldState.FieldType.WHITE ?
                            L10N.get("white_player_text").replaceAll("\\s+", "") :
                            L10N.get("black_player_text").replaceAll("\\s+", "");
                    // if the name is the default name (i.e. the color) there's no need to display it twice
                    final String replacement = color.equals(winner.getName()) ?
                            winner.getName() : winner.getName() + " (" + color + ")";
                    alertHeading = L10N.get("winner_text").replace("%n", replacement);
                } else {
                    alertHeading = L10N.get("ai_won_text");
                }
                alertBody = L10N.get("score_text").replace("%n", Integer.toString(score));
            } catch (IOException | ParseException e) {
                // change the alert type and display an error instead of the actual message
                alertType = Alert.AlertType.WARNING;
                alertHeading = "Saving high scores failed";
                alertBody = e.getMessage();
                e.printStackTrace();
            }
        } else { // if the game was a draw
            alertHeading = L10N.get("draw_text");
            final int score = GUI.this.state.countFields(FieldState.FieldType.BLACK);
            alertBody = L10N.get("score_text").replace("%n", Integer.toString(score));
        }

        // display an alert box announcing the winner
        final Alert alert = new Alert(alertType);
        alert.setTitle(alertHeading);
        alert.setHeaderText(alertHeading);
        alert.setContentText(alertBody);
        alert.show();
    }

    /**
     * Toggles the current player.
     */
    private void toggleCurrentPlayer() {
        this.currentPlayer = this.currentPlayer == FieldState.FieldType.BLACK ?
                FieldState.FieldType.WHITE : FieldState.FieldType.BLACK;
    }

    /**
     * Handle a click on the help button. (Display game rules)
     *
     * @param ignored This parameter is ignored.
     */
    private void helpButtonClicked(MouseEvent ignored) {
        final Alert.AlertType type = Alert.AlertType.INFORMATION;
        final Alert alert = new Alert(type);
        alert.setTitle(L10N.get("game_rules_text"));
        alert.setHeaderText(L10N.get("game_rules_text"));
        final String rules = L10N.get("game_rules_html");
        WebView webView = new WebView();
        webView.getEngine().loadContent(rules);
        webView.setPrefSize(MIN_WIDTH, MIN_HEIGHT);
        alert.getDialogPane().setContent(webView);
        alert.getDialogPane().setPrefHeight(MIN_HEIGHT);
        alert.getDialogPane().setPrefWidth(MIN_WIDTH);
        alert.show(); // show() is non-blocking
    }

    /**
     * Handles a click on the skip button.
     *
     * @param ignored This parameter is ignored.
     */
    private void skipButtonClicked(MouseEvent ignored) {
        this.finishPlayerMove();
    }

    /**
     * Handles a click on the restart button.
     *
     * @param stage The stage. Must not be {@code null}.
     */
    private void restartButtonClicked(Stage stage) {
        this.makeStartScene(Objects.requireNonNull(stage, "stage must not be null"));
    }

    /**
     * Handles a click on the highscore button.
     *
     * @param ignored This parameter is ignored.
     */
    private void highscoreButtonClicked(MouseEvent ignored) {
        final TableView<ScoreDataModel> table = new TableView<>();
        table.setEditable(false);

        // add high scores to table
        ObservableList<ScoreDataModel> scores = FXCollections.observableArrayList();
        if (this.highscores != null) {
            for (Highscores.Score score : this.highscores.getScores()) {
                scores.add(new ScoreDataModel(score.date, score.player, score.score));
            }
        }
        table.setItems(scores);

        // set up the properties corresponding to the table columns

        final TableColumn<ScoreDataModel, Integer> scoreCol = new TableColumn<>(L10N.get("score_col_text"));
        scoreCol.setCellValueFactory(
                new PropertyValueFactory<>("score")
        );
        scoreCol.setMinWidth(100);
        final TableColumn<ScoreDataModel, String> nameCol = new TableColumn<>(L10N.get("name_col_text"));
        nameCol.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );
        final TableColumn<ScoreDataModel, Date> dateCol = new TableColumn<>(L10N.get("date_col_text"));
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("date")
        );
        table.getColumns().addAll(scoreCol, nameCol, dateCol);

        // sort the table by high score and date
        table.getSortOrder().add(scoreCol);
        table.getSortOrder().add(dateCol);
        scoreCol.setSortType(TableColumn.SortType.DESCENDING);
        dateCol.setSortType(TableColumn.SortType.DESCENDING);
        table.sort();
        table.setPrefWidth(MIN_WIDTH);

        // display high score table in an alert box
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getDialogPane().setContent(table);
        alert.setTitle(L10N.get("highscore_list_text"));
        alert.setHeaderText(L10N.get("highscore_list_text"));
        alert.show();
    }

    /**
     * Data model for the highscore table.
     */
    public static class ScoreDataModel {
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        private final SimpleStringProperty date;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty score;

        /**
         * @param date  The score's date. Must not be {@code null}.
         * @param name  The player's name. Must not be {@code null}.
         * @param score The score. Must be positive.
         */
        public ScoreDataModel(Date date, String name, int score) {
            this.date = new SimpleStringProperty(
                    DATE_FORMAT.format(Objects.requireNonNull(date, "date must not be null")));
            this.name = new SimpleStringProperty(Objects.requireNonNull(name, "player must not be null").isEmpty() ?
                    "<no_name>" : name);
            this.score = new SimpleIntegerProperty(score);
            if (score < 0) {
                throw new IllegalArgumentException("score must be positive");
            }
        }

        // javafx needs those getters (via reflection)

        /**
         * @return the date.
         */
        @SuppressWarnings("unused")
        public String getDate() {
            return date.get();
        }

        /**
         * @return the date property.
         */
        @SuppressWarnings("unused")
        public SimpleStringProperty dateProperty() {
            return date;
        }

        /**
         * @return the name.
         */
        @SuppressWarnings("unused")
        public String getName() {
            return name.get();
        }

        /**
         * @return the name property.
         */
        @SuppressWarnings("unused")
        public SimpleStringProperty nameProperty() {
            return name;
        }

        /**
         * @return the score.
         */
        @SuppressWarnings("unused")
        public int getScore() {
            return score.get();
        }

        /**
         * @return the score property.
         */
        @SuppressWarnings("unused")
        public SimpleIntegerProperty scoreProperty() {
            return score;
        }
    }

}
