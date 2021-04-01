package othello;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manages high scores.
 */
public class Highscores {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private final Path path;
    private final List<Score> scores;

    /**
     * Create a new Highscores object.
     *
     * @param path The path to the high scores file. Must not be {@code null}.
     * @throws IOException    {@link Files#readAllLines(Path)}
     * @throws ParseException {@link DateFormat#parse(String)}
     */
    public Highscores(Path path) throws IOException, ParseException {
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.scores = new LinkedList<>();
        this.readScores();
    }

    /**
     * @return The high scores.
     */
    public List<Score> getScores() {
        return Collections.unmodifiableList(this.scores);
    }

    /**
     * Add a high score. This automatically saves to disk.
     *
     * @param score The score to be added. Must not be {@code null}.
     * @throws IOException {@link BufferedWriter#write(String)}
     */
    public void addScore(Score score) throws IOException {
        this.scores.add(Objects.requireNonNull(score, "score must not be null"));
        this.writeScores();
    }

    /**
     * Reads the high scores from disk.
     *
     * @throws IOException    {@link Files#readAllLines(Path)}
     * @throws ParseException {@link DateFormat#parse(String)}
     */
    private void readScores() throws IOException, ParseException {
        if (!Files.isRegularFile(this.path)) return;
        final List<String> lines = Files.readAllLines(this.path, CHARSET);
        final StringBuilder wholeFile = new StringBuilder();
        lines.forEach(wholeFile::append); // add all lines to the string
        // split at semicolons that are not preceded by a backslash
        final String[] scores = wholeFile.toString().split("(?<!\\\\)\\s*;\\s*");
        for (String score : scores) {
            if (!score.isEmpty())
                this.scores.add(Score.fromString(score));
        }
    }

    /**
     * Writes the high scores to disk.
     *
     * @throws IOException {@link BufferedWriter#write(String)}
     */
    private void writeScores() throws IOException {
        // This is a try-with-resources statement. It automatically calls the close() method.
        try (BufferedWriter writer = Files.newBufferedWriter(this.path, CHARSET)) {
            for (Score score : this.scores) {
                writer.write(score.toString() + ";");
            }
        }
    }

    /**
     * Stores a score.
     */
    public static class Score {
        /**
         * The date format used to store the state on disk.
         * Note that this is not necessarily the same format that is used by the GUI.
         */
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /**
         * The date when the score was achieved.
         */
        public final Date date;
        /**
         * The name of the player who achieved the score.
         */
        public final String player;
        public final int score;

        /**
         * Stores an Othello score.
         *
         * @param date  The score's date. Must not be {@code null}.
         * @param name  The player's name. Must not be {@code null}.
         * @param score The score. Must be positive.
         */
        public Score(Date date, String name, int score) {
            this.date = Objects.requireNonNull(date, "date must not be null");
            this.player = Objects.requireNonNull(name, "player must not be null").isEmpty() ?
                    "<no_name>" : name;
            this.score = score;
            if (this.score < 0) {
                throw new IllegalArgumentException("score must be positive");
            }
        }

        /**
         * Escape ",", ";" and "\" with backslashes.
         *
         * @param input The input string. Must not be {@code null}.
         * @return The escaped string.
         */
        private static String escape(String input) {
            Objects.requireNonNull(input, "input must not be null");
            return input.replace("\\", "\\\\")
                    .replace(",", "\\,")
                    .replace(";", "\\;");
        }

        /**
         * Unescape ",", ";" and "\" with backslashes.
         *
         * @param input The input string. Must not be {@code null}.
         * @return The unescaped string.
         */
        private static String unescape(String input) {
            Objects.requireNonNull(input, "input must not be null");
            return input.replace("\\,", ",")
                    .replace("\\;", ";")
                    .replace("\\\\", "\\");
        }

        /**
         * Create a score from a string created by {@link this#toString()}.
         *
         * @param input The input string. Must not be {@code null}.
         * @return The created score.
         * @throws ParseException If the date parsing fails.
         */
        public static Score fromString(String input) throws ParseException {
            Objects.requireNonNull(input, "input must not be null");
            // split at commas that are not preceded by a backslash
            final String[] fields = input.split("(?<!\\\\)\\s*,\\s*");
            final int score = Integer.parseInt(fields[0]);
            final String name = unescape(fields[1]);
            final Date date = DATE_FORMAT.parse(unescape(fields[2]));
            return new Score(date, name, score);
        }

        /**
         * @see {@link Object#toString()}
         */
        @Override
        public String toString() {
            return this.score + "," + escape(this.player) + ","
                    + escape(DATE_FORMAT.format(this.date));
        }
    }
}
