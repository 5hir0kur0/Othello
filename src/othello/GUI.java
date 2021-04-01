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

    // UI strings
    private static final String PLAYER_LABEL_TEXT = "Spieler: %p";
    private static final String SKIP_TOOLTIP = "Klicke „Pass“, um einen Zug auszusetzen.";
    private static final String BLACK_PLAYER_TEXT = "Schwarz";
    private static final String WHITE_PLAYER_TEXT = "Wei\u00DF     ";
    private static final String SKIP_BUTTON_TEXT = "Pass";
    private static final String RESTART_BUTTON_TEXT = "Neustart";
    private static final String HIGHSCORE_TEXT = "Highscores";
    private static final String WINDOW_TITLE = "Othello";
    private static final String GAME_MODE_LABEL_TEXT = "Spielmodus";
    private static final String PVP_MODE_TEXT = "Spieler gegen Spieler";
    private static final String AI_MODE_TEXT = "Spieler gegen KI";
    private static final String SELECT_STARTING_PLAYER_TEXT = "Auswahl des beginnenden Spielers";
    private static final String START_GAME_TEXT = "Spiel starten";
    private static final String NEW_GAME_TEXT = "Neues Spiel starten";
    private static final String AI_TEXT = "KI          ";
    private static final String ENTER_NAME_TEXT = "Spielernamen eingeben (f\u00FCr Highscore-Speicherung)";
    private static final String WINNER_TEXT = "Der Spieler %n hat gewonnen!";
    private static final String AI_WON_TEXT = "Die KI hat gewonnen!";
    private static final String DRAW_TEXT = "Das Spiel endet unentschieden";
    private static final String SCORE_TEXT = "Score: %n";
    private static final String SCORE_COL_TEXT = "Score";
    private static final String NAME_COL_TEXT = "Name";
    private static final String DATe_COL_TEXT = "Datum";
    private static final String HIGHSCORE_LIST_TEXT = "Highscores";

    private final Button[][] guiField = new Button[8][8];
    /**
     * Skip button that changes color depending on the current player.
     */
    final private Button skipButton = new Button(SKIP_BUTTON_TEXT);
    final private Button restartButton = new Button(RESTART_BUTTON_TEXT);
    final private Label playerLabel = new Label(PLAYER_LABEL_TEXT);
    final private Label scoreLabel = new Label(BLACK_PLAYER_TEXT + ": 0\n" + WHITE_PLAYER_TEXT + ": 0");
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
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle(GUI.WINDOW_TITLE);

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
        this.playerLabel.setTooltip(new Tooltip(SKIP_TOOLTIP));
        this.skipButton.setTooltip(new Tooltip(SKIP_TOOLTIP));

        // make the grid scale correctly
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100 / 2);
        columnConstraints.setHgrow(Priority.ALWAYS);
        // two columns, so two times
        grid.getColumnConstraints().add(columnConstraints);
        grid.getColumnConstraints().add(columnConstraints);

        final Label newGame = new Label(NEW_GAME_TEXT);
        newGame.setFont(new Font(BIG_FONT_SIZE));
        grid.add(newGame, 0, 0, 2, 1);

        // text fields for name entry
        final TextField blackField = new TextField(BLACK_PLAYER_TEXT.replaceAll("\\s+", ""));
        blackField.textProperty().addListener(action ->
                GUI.this.players[FieldState.FieldType.BLACK.ordinal()] =
                        new Player(FieldState.FieldType.BLACK, blackField.getText()));
        final TextField whiteField = new TextField(WHITE_PLAYER_TEXT.replaceAll("\\s+", ""));
        whiteField.textProperty().addListener(action ->
                GUI.this.players[FieldState.FieldType.WHITE.ordinal()] =
                        new Player(FieldState.FieldType.WHITE, whiteField.getText()));
        this.players[FieldState.FieldType.WHITE.ordinal()] = new Player(FieldState.FieldType.WHITE, WHITE_PLAYER_TEXT);
        this.players[FieldState.FieldType.BLACK.ordinal()] = new Player(FieldState.FieldType.BLACK, BLACK_PLAYER_TEXT);

        int row = 1;
        int col = 0;

        // game mode selection
        final Label gameMode = new Label(GAME_MODE_LABEL_TEXT);
        final ToggleGroup modeGroup = new ToggleGroup();
        final RadioButton pvpMode = new RadioButton(PVP_MODE_TEXT);
        pvpMode.setToggleGroup(modeGroup);
        final RadioButton aiMode = new RadioButton(AI_MODE_TEXT);
        aiMode.setToggleGroup(modeGroup);
        // the UI should not allow entering a name for the AI
        aiMode.setOnAction(action -> {
            GUI.this.mode = GameMode.PLAYER_VERSUS_AI;
            switch (GUI.this.currentPlayer) {
                case BLACK:
                    whiteField.setDisable(true);
                    break;
                case WHITE:
                    blackField.setDisable(true);
                    break;
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
        final Label startingPlayer = new Label(SELECT_STARTING_PLAYER_TEXT);
        final ToggleGroup startGroup = new ToggleGroup();
        final RadioButton black = new RadioButton(BLACK_PLAYER_TEXT);
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
        final RadioButton white = new RadioButton(WHITE_PLAYER_TEXT);
        white.setToggleGroup(startGroup);
        white.setOnAction(action -> {
            GUI.this.currentPlayer = FieldState.FieldType.WHITE;
            if (GUI.this.mode == GameMode.PLAYER_VERSUS_PLAYER) {
                blackField.setDisable(false);
                whiteField.setDisable(false);
                return;
            } else {
                // don't allow entering a name for the AI player
                blackField.setDisable(true);
                whiteField.setDisable(false);
            }
        });
        startGroup.selectToggle(black);
        grid.add(startingPlayer, ++col, row++);
        grid.add(black, col, row++);
        grid.add(white, col, row++);

        // name entry
        col = 0;
        final Label enterNames = new Label(ENTER_NAME_TEXT);
        final Label blackName = new Label(BLACK_PLAYER_TEXT);
        final Label whiteName = new Label(WHITE_PLAYER_TEXT);
        grid.add(enterNames, col, row++, 2, 1);
        grid.add(blackName, col, row);
        grid.add(blackField, col + 1, row++);
        grid.add(whiteName, col, row);
        grid.add(whiteField, col + 1, row++);

        // start button
        final Button start = new Button(START_GAME_TEXT);
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
        controls.setHgap(3 * GUI.PADDING / 2);
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
        final Button highscoreButton = new Button(HIGHSCORE_TEXT);
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
        scene.getStylesheets().add("style.css");
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
        possibleMoves.forEach(idx -> {
            GUI.this.guiField[idx.xPos][idx.yPos].getStyleClass().setAll(
                    this.currentPlayer == FieldState.FieldType.BLACK ? POSSIBLE_MOVE_BLACK_CLASS : POSSIBLE_MOVE_WHITE_CLASS,
                    DISK_CLASS);
        });
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
        switch (this.mode) {
            case PLAYER_VERSUS_PLAYER:
                // update current player and score labels
                this.playerLabel.setText(
                        PLAYER_LABEL_TEXT.replaceAll("%p",
                                this.currentPlayer == FieldState.FieldType.WHITE ? WHITE_PLAYER_TEXT : BLACK_PLAYER_TEXT));
                this.scoreLabel.setText(
                        BLACK_PLAYER_TEXT + ": " + this.state.countFields(FieldState.FieldType.BLACK) + "\n" +
                                WHITE_PLAYER_TEXT + ": " + this.state.countFields(FieldState.FieldType.WHITE));
                break;
            case PLAYER_VERSUS_AI:
                // update current player and score labels
                this.playerLabel.setText(
                        PLAYER_LABEL_TEXT.replaceAll("%p",
                                this.currentPlayer == FieldState.FieldType.WHITE ? WHITE_PLAYER_TEXT : BLACK_PLAYER_TEXT)
                                .replaceAll("%a", this.aiPlayer == FieldState.FieldType.WHITE ? WHITE_PLAYER_TEXT : BLACK_PLAYER_TEXT));
                final String player = this.currentPlayer == FieldState.FieldType.BLACK ? BLACK_PLAYER_TEXT : WHITE_PLAYER_TEXT;
                this.scoreLabel.setText(
                        player + ": " + this.state.countFields(this.currentPlayer) + "\n" +
                                AI_TEXT + ": " + this.state.countFields(this.aiPlayer));
                break;
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
        switch (this.mode) {
            case PLAYER_VERSUS_PLAYER:
                this.toggleCurrentPlayer();
                break;
            case PLAYER_VERSUS_AI:
                // the buttons should be disabled while the AI is thinking
                this.disableButtons(true);
                this.aiMove();
                this.disableButtons(false);
                break;
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
        if (!winnerType.isPresent()) return;

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
                            WHITE_PLAYER_TEXT.replaceAll("\\s+", "") :
                            BLACK_PLAYER_TEXT.replaceAll("\\s+", "");
                    // if the name is the default name (i.e. the color) there's no need to display it twice
                    final String replacement = color.equals(winner.getName()) ?
                            winner.getName() : winner.getName() + " (" + color + ")";
                    alertHeading = WINNER_TEXT.replace("%n", replacement);
                } else {
                    alertHeading = AI_WON_TEXT;
                }
                alertBody = SCORE_TEXT.replace("%n", Integer.toString(score));
            } catch (IOException | ParseException e) {
                // change the alert type and display an error instead of the actual message
                alertType = Alert.AlertType.WARNING;
                alertHeading = "Saving high scores failed";
                alertBody = e.getMessage();
                e.printStackTrace();
            }
        } else { // if the game was a draw
            alertHeading = DRAW_TEXT;
            final int score = GUI.this.state.countFields(FieldState.FieldType.BLACK);
            alertBody = SCORE_TEXT.replace("%n", Integer.toString(score));
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
        alert.setTitle("Spielregeln");
        alert.setHeaderText("Spielregeln");
        // sorry, but platform-independent file handling is difficult, so I hard-coded the text...
        final String rules = "<html><body>" +
                "<p>Es wird auf einem Brett mit 8×8 Feldern gespielt.</p>" +
                "<p>Als Startaufstellung werden vor dem Spielbeginn zwei wei\u00DFe und zwei schwarze Steine auf die mittleren Felder des Bretts gelegt, je zwei diagonal gegen\u00FCberliegende mit der gleichen Farbe \u0028Bild\u0029.</p>" +
                "<p>Die Zahl der Steine jedes Spielers ist unbegrenzt.</p>" +
                "<p>Die Spieler ziehen abwechselnd, Schwarz beginnt. Man setzt entweder einen Stein mit der eigenen Farbe auf ein leeres Feld, oder man passt.</p>" +
                "<p>Man darf nur so setzen, dass ausgehend von dem gesetzten Stein in beliebiger Richtung \u0028senkrecht, waagerecht oder diagonal\u0029 ein oder mehrere gegnerische Steine anschlie\u00DFen und danach wieder ein eigener Stein liegt. Es muss also mindestens ein gegnerischer Stein von dem gesetzten Stein und einem anderen eigenen Stein in gerader Linie eingeschlossen werden. Dabei m\u00FCssen alle Felder zwischen den beiden eigenen Steinen von gegnerischen Steinen besetzt sein.</p>" +
                "<p>Alle gegnerischen Steine, die so eingeschlossen werden, wechseln die Farbe, indem sie umgedreht werden. Dies geschieht als Teil desselben Zuges, bevor der Gegner zum Zug kommt. Ein Zug kann mehrere Reihen gegnerischer Steine gleichzeitig einschlie\u00DFen, die dann alle umgedreht werden. Wenn aber ein gerade umgedrehter Stein weitere gegnerische Steine einschlie\u00DFt, werden diese nicht umgedreht.</p>" +
                "<p>Wenn keiner mehr einen Stein setzen kann, ist das Spiel beendet.</p>" +
                "<p>Der Spieler, der am Ende die meisten Steine seiner Farbe auf dem Brett hat, gewinnt. Haben beide die gleiche Zahl, ist das Spiel unentschieden.</p>" +
                "<p>Die H\u00F6he des Gewinns zu ermitteln, werden die Steine des Gewinners gez\u00E4hlt.</p>" +
                "<p><br></p>" +
                "Angepasst von: <a href='https://de.wikipedia.org/wiki/Othello_(Spiel)'>https://de.wikipedia.org/wiki/Othello_(Spiel)</a>" +
                "</body></html>";
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
        switch (this.mode) {
            case PLAYER_VERSUS_PLAYER:
                this.toggleCurrentPlayer();
                break;
            case PLAYER_VERSUS_AI:
                // if the game is in the AI mode, skipping causes an AI move
                this.disableButtons(true);
                this.aiMove();
                this.disableButtons(false);
                break;
        }
        this.update();
        if (this.state.gameOver()) {
            this.gameOver();
        }
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
        final TableView table = new TableView();
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

        final TableColumn scoreCol = new TableColumn(SCORE_COL_TEXT);
        scoreCol.setCellValueFactory(
                new PropertyValueFactory<>("score")
        );
        scoreCol.setMinWidth(100);
        final TableColumn nameCol = new TableColumn(NAME_COL_TEXT);
        nameCol.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );
        final TableColumn dateCol = new TableColumn(DATe_COL_TEXT);
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
        alert.setTitle(HIGHSCORE_LIST_TEXT);
        alert.setHeaderText(HIGHSCORE_LIST_TEXT);
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
        public String getDate() {
            return date.get();
        }

        /**
         * @return the date property.
         */
        public SimpleStringProperty dateProperty() {
            return date;
        }

        /**
         * @return the name.
         */
        public String getName() {
            return name.get();
        }

        /**
         * @return the name property.
         */
        public SimpleStringProperty nameProperty() {
            return name;
        }

        /**
         * @return the score.
         */
        public int getScore() {
            return score.get();
        }

        /**
         * @return the score property.
         */
        public SimpleIntegerProperty scoreProperty() {
            return score;
        }
    }

}
