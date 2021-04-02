package othello;

import javafx.application.Application;
import javafx.scene.control.Alert;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Main class that launches the GUI.
 */
public class Main {

    /**
     * The default uncaught exception handler to be used.
     * (Even though it's in the thread class the default uncaught exception handler actually applies to all threads,
     * and thus only needs to be set once.)
     */
    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (thread, throwable) -> {
        // invalid move exceptions can be ignored because they simply occur, when the player has pressed a wrong button
        if (throwable instanceof FieldState.InvalidMoveException) {
            return;
        }
        final Alert.AlertType type = throwable instanceof RuntimeException ?
                Alert.AlertType.WARNING : Alert.AlertType.ERROR;
        final Alert alert = new Alert(type);
        alert.setTitle("Warning");
        alert.setHeaderText(throwable.getMessage());
        // store the exception's stack trace in a string
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        throwable.printStackTrace();
        alert.setContentText(stackTrace.toString());
        alert.show(); // show() is non-blocking
    };

    /**
     * Main method which starts the Othello GUI.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
        Application.launch(GUI.class);
    }
}
