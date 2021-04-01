package othello;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Represents a Othello (8x8) field's current state.
 * The state is immutable.
 */
public class FieldState {
    /**
     * Stores the othello field.
     * The class makes sure that the entries are never {@code null}.
     */
    private final FieldType[][] field;
    // needed for performance (because the findPossibleMoves() method is called twice with the same args in some
    // circumstances)
    private Set<Index> findPossibleMovesCache = null;
    private FieldType findPossibleMovesPlayerCache = null;
    private boolean findPossibleMovesDeepCheckCache = false;

    /**
     * Construct a new immutable Othello field with the starting state for Othello.
     */
    public FieldState() {
        this.field = new FieldType[8][8]; // Reversi is always played on a 8x8 board
        // Fill the new field with empty tiles
        this.forEach((index, type) -> this.setField(index, FieldType.EMPTY, false));
        // set the initial field state
        this.setField(3, 3, FieldType.WHITE, false);
        this.setField(4, 4, FieldType.WHITE, false);
        this.setField(4, 3, FieldType.BLACK, false);
        this.setField(3, 4, FieldType.BLACK, false);
    }

    /**
     * Construct a new field state from an old one.
     *
     * @param oldState The old state. Must not be {@code null}.
     */
    private FieldState(FieldState oldState) {
        Objects.requireNonNull(oldState, "parameter oldState must not be null");
        this.field = new FieldType[8][8];
        // copy the old state to this one
        this.forEach((index, type) -> this.setField(index, oldState.getField(index), false));
    }

    /**
     * Check whether a field position is valid, i.e. 0 <= xPos <= 7 and 0 <= yPos <= 7.
     *
     * @param xPos The x position.
     * @param yPos The y position.
     * @throws IllegalArgumentException If the position is invalid.
     */
    private static void validateFieldPosition(int xPos, int yPos) {
        // check if the position is actually within the limits of the 8x8 Othello board
        if (xPos >= 8 || yPos >= 8 || xPos < 0 || yPos < 0) {
            throw new IllegalArgumentException("invalid field position: [" + xPos + "][" + yPos + "]");
        }
    }

    /**
     * Check if a {@link FieldType} object is a valid player, i.e. if it is not {@code null} and either
     * {@link FieldType#WHITE} or {@link FieldType#BLACK}.
     *
     * @param player The player to be checked. May be {@code null}.
     * @throws IllegalArgumentException If the player is not valid.
     * @throws NullPointerException     If the parameter is {@code null}.
     */
    private static void validatePlayer(FieldType player) {
        if (Objects.requireNonNull(player, "player parameter must not be null") == FieldType.EMPTY) {
            throw new IllegalArgumentException("player parameter must not be FieldType.EMPTY");
        }
    }

    /**
     * Get the field type at the given position.
     *
     * @param xPos The x position. Must be non-negative and smaller than 8.
     * @param yPos The y position. Must be non-negative and smaller than 8.
     * @return The field type at the given position.
     * @throws IllegalArgumentException If the position is invalid.
     */
    public FieldType getField(int xPos, int yPos) {
        validateFieldPosition(xPos, yPos);
        return this.field[xPos][yPos];
    }

    /**
     * Get the field type at the given index.
     *
     * @param index The index. Must not be {@code null}.
     * @return The field type at the given index.
     * @throws NullPointerException If the parameter is {@code null}.
     */
    public FieldType getField(Index index) {
        Objects.requireNonNull(index, "parameter index must not be null");
        return this.getField(index.xPos, index.yPos);
    }

    /**
     * Set the field type at the specified position.
     * This method is private and should only be used for initializing new FieldStates,
     * because FieldStates are immutable.
     *
     * @param xPos        The x position. Must be non-negative and smaller than 8.
     * @param yPos        The y position. Must be non-negative and smaller than 8.
     * @param type        The new type. Must not be {@code null}.
     * @param checkIfFlip If {@code true}, throw an exception if the disk is't being flipped from
     *                    {@link FieldType#WHITE} to {@link FieldType#BLACK} or the other way around.
     * @throws IllegalArgumentException If the position or type is invalid or
     *                                  if the parameter checkIfFlip is {@code true} and the action did not cause a flip.
     * @throws NullPointerException     If the parameter type is {@code null}.
     */
    private void setField(int xPos, int yPos, FieldType type, boolean checkIfFlip) {
        FieldState.validateFieldPosition(xPos, yPos);
        if (checkIfFlip) {
            FieldState.validatePlayer(type);
            if (this.field[xPos][yPos] == type) {
                throw new IllegalArgumentException("argument didn't cause a flip at position:" + Index.of(xPos, yPos));
            }
        }
        this.field[xPos][yPos] = Objects.requireNonNull(type, "parameter type must not be null");
    }

    /**
     * Set the field type at the specified index.
     * This method is private and should only be used for initializing new FieldStates,
     * because FieldStates are immutable.
     *
     * @param index       The index. Must not be {@code null}.
     * @param type        The new type. Must not be {@code null}.
     * @param checkIfFlip If {@code true}, throw an exception if the disk is't being flipped from
     *                    {@link FieldType#WHITE} to {@link FieldType#BLACK} or the other way around.
     * @throws NullPointerException If the parameter index is {@code null}.
     */
    private void setField(Index index, FieldType type, boolean checkIfFlip) {
        Objects.requireNonNull(index, "parameter index must not be null");
        this.setField(index.xPos, index.yPos, type, checkIfFlip);
    }

    /**
     * Make a move.
     *
     * @param player The player. Must not be {@code null}. Must be either {@link FieldType#BLACK} or {@link FieldType#WHITE}.
     * @param index  The index where the disk is placed. Must not be {@code null}.
     * @return A new FieldState object representing the state after the move.
     * @throws NullPointerException If the either of the parameters is {@code null}.
     * @throws InvalidMoveException If the index is not a valid move for the specified player.
     */
    public FieldState makeMove(FieldType player, Index index) {
        FieldState.validatePlayer(player);
        final Set<Index> possibleMoves = this.findPossibleMoves(player, false);
        if (!possibleMoves.contains(index)) {
            throw new InvalidMoveException("index " + index + " is not a valid move for player " + player);
        }
        // create a new FieldState instead of mutating this, because FieldState is immutable
        FieldState newState = new FieldState(this);
        List<List<Index>> linesToFlip = findLinesToFlip(index, player);
        // a move that doesn't cause any disks to flip is not valid
        if (linesToFlip.isEmpty()) {
            throw new InvalidMoveException("index " + index + " is not a valid move for player " + player);
        }
        // flip all relevant disks
        // (the setField method's parameter checkIfFlip is set to true as a safeguard to check if the action actually
        // causes the disk to flip from one color to the other)
        linesToFlip.forEach(list -> list.forEach(
                index1 -> newState.setField(index1, player, true)
        ));
        // finally, place the disk for this move on the field and return the new FieldState
        newState.setField(index, player, true);
        return newState;
    }

    /**
     * @return {@code true} if the game is over. This is the case when both players don't have any possible moves left.
     */
    public boolean gameOver() {
        // the game is over when no player can make a move anymore
        return this.findPossibleMoves(FieldType.WHITE, true).isEmpty()
                && this.findPossibleMoves(FieldType.BLACK, true).isEmpty();
    }

    /**
     * Calculate the winner.
     *
     * @return The {@link FieldType} of the winner if there is a winner or {@link java.util.Optional#EMPTY} if the game
     * is not yet over or {@link FieldType#EMPTY} if the game was a draw.
     */
    public Optional<FieldType> calculateWinner() {
        // if the game isn't over yet, there's no winner
        if (!this.gameOver()) {
            return Optional.empty();
        }

        final int whiteCount = this.countFields(FieldType.WHITE);
        final int blackCount = this.countFields(FieldType.BLACK);

        // the player with a higher count wins the game
        if (whiteCount > blackCount) {
            return Optional.of(FieldType.WHITE);
        } else if (blackCount > whiteCount) {
            return Optional.of(FieldType.BLACK);
        } else { // the game is a draw
            return Optional.of(FieldType.EMPTY);
        }

    }

    /**
     * Calculate the number of points for a player.
     * This method is not used for now, because the Othello games I tested don't seem to count empty fields,
     * even though the Wikipedia article says empty fields are supposed to be attributed to the winner of the game.
     *
     * @param player The player. Must not be {@code null}. Must be either {@link FieldType#BLACK} or {@link FieldType#WHITE}.
     * @return The number of points the player has. If the game has been won, this includes the number of empty fields
     * for the winner.
     * @throws IllegalArgumentException If the player is not valid.
     * @throws NullPointerException     If the player is {@code null}.
     */
    public int getPoints(FieldType player) {
        FieldState.validatePlayer(player);
        final int num = this.countFields(player);
        final Optional<FieldType> winner = this.calculateWinner();
        if (winner.isPresent() && winner.get() == player) {
            return num + this.countFields(FieldType.EMPTY);
        } else {
            return num;
        }
    }

    /**
     * Count the number of fields that have the specified type.
     *
     * @param type The field type. Must not be {@code null}.
     * @return The number of fields that have the specified type.
     * @throws NullPointerException If the type is {@code null}.
     */
    public int countFields(FieldType type) {
        Objects.requireNonNull(type, "parameter type must not be null");
        return this.findMatchingIndexes(type).size();
    }

    /**
     * Calculate the set of "possibly possible" moves for a player.
     * This means all empty fields adjacent to opponent fields.
     * If you want to see whether a move is really possible, you need to check with {@link this#findLinesToFlip(Index, FieldType)}
     * or set {@code deepCheck} to {@code true}.
     *
     * @param player    The player. Must not be {@code null}. Must be either {@link FieldType#BLACK} or {@link FieldType#WHITE}.
     * @param deepCheck Check whether a move is actually valid (i.e. if a chain ends in a stone of the player)
     * @return The set of possible moves of the player or an empty set if there are non. Never returns {@code null}.
     * @throws IllegalArgumentException If the player is not valid.
     * @throws NullPointerException     If the player is {@code null}.
     */
    public Set<Index> findPossibleMoves(FieldType player, boolean deepCheck) {
        // if the result has already been calculated, return the cached result
        if (this.findPossibleMovesDeepCheckCache == deepCheck && this.findPossibleMovesPlayerCache == player
                && this.findPossibleMovesCache != null) {
            return this.findPossibleMovesCache;
        }
        // else, reset the cache
        this.findPossibleMovesCache = null;
        this.findPossibleMovesPlayerCache = player;
        this.findPossibleMovesDeepCheckCache = deepCheck;

        FieldState.validatePlayer(player);
        final FieldType opponent = player == FieldType.BLACK ? FieldType.WHITE : FieldType.BLACK;
        final Set<Index> res = new HashSet<>(); // this set will contain the resulting moves
        // find all fields that a disk of the opponent is placed on
        final Set<Index> opponentFields = this.findMatchingIndexes(opponent);
        // iterate through them and find all directly adjacent fields
        opponentFields.forEach(
                index -> res.addAll(
                        this.findAdjacentFields(index, (field, index1) -> field.getField(index1) == FieldType.EMPTY)));
        if (deepCheck) {
            // actually check if an index would be a valid position to place a disk
            // this is the case when the set of lines to flip is not empty at that index
            this.findPossibleMovesCache = res.stream()
                    .filter(idx -> !FieldState.this.findLinesToFlip(idx, player).isEmpty())
                    .collect(Collectors.toSet());
        } else {
            // if deepCheck is false there's nothing left to do
            this.findPossibleMovesCache = res;
        }
        return this.findPossibleMovesCache;
    }

    /**
     * Find all indexes that match a given field type.
     *
     * @param type The field type. Must not be {@code null}.
     * @return A set of indexes matching the field type.
     * @throws NullPointerException If the type is {@code null}.
     */
    public Set<Index> findMatchingIndexes(FieldType type) {
        Objects.requireNonNull(type, "parameter type must not be null");
        final Set<Index> res = new HashSet<>(); // create a set to store the result
        // iterate over the field to find matching indexes
        this.forEach((index, type1) -> {
            if (type1 == type) {
                res.add(index);
            }
        });
        return res;
    }

    /**
     * Returns a list of adjacent fields to an index.
     * A predicate may be specified to further restrict the results.
     *
     * @param index     The index. Must not be {@code null}.
     * @param predicate An optional predicate. (May be {@code null}).
     * @return All fields that are adjacent to the index.
     * @throws NullPointerException If the index is {@code null}.
     */
    private Set<Index> findAdjacentFields(Index index, BiPredicate<FieldState, Index> predicate) {
        Objects.requireNonNull(index, "parameter index must not be null");
        // if there is no predicate, create a "dummy" that always returns true
        final BiPredicate<FieldState, Index> pred = predicate == null ? (f, i) -> true : predicate;
        final Set<Index> res = new HashSet<>(8); // create a set to store the result
        // find the adjacent fields by iterating over the field array
        this.forEach((idx, type) -> {
            // calculate the difference in x and y position of the current index (idx) compared to the parameter (index)
            final int dx = Math.abs(idx.xPos - index.xPos);
            final int dy = Math.abs(idx.yPos - index.yPos);
            // check if the current index (idx) is adjacent to the parameter (index)
            if (((dx == 1 && dy == 1) || (idx.xPos == index.xPos && dy == 1) || (idx.yPos == index.yPos && dx == 1))
                    && pred.test(this, idx)) {
                res.add(idx);
            }
        });
        return res;
    }

    /**
     * Find diagonal and vertical lines starting from an index where there is at least one disk of the opponent
     * in either direction and a disk of the player itself following it.
     * Those are the lines that the player is allowed to flip if he places a disk at that index.
     *
     * @param index  The start index. Must not be {@code null}.
     * @param player The opponent. Must be either {@link FieldType#BLACK} or {@link FieldType#WHITE}.
     * @return A list of possible vertical, horizontal and diagonal lines.
     * @throws NullPointerException     If the player or the index are {@code null}.
     * @throws IllegalArgumentException If the player is not valid.
     */
    public List<List<Index>> findLinesToFlip(Index index, FieldType player) {
        Objects.requireNonNull(index, "parameter index must not be null");
        FieldState.validatePlayer(player);
        final FieldType opponent = player == FieldType.WHITE ? FieldType.BLACK : FieldType.WHITE;
        final List<List<Index>> res = new ArrayList<>(4); // create a set to store the result

        // calculate the lines to flip in vertical direction

        final List<Index> verticalTmp1 = new ArrayList<>(4);
        boolean playerReached = false;
        for (int y = index.yPos + 1; y < this.field[index.xPos].length; ++y) {
            if (this.getField(index.xPos, y) == opponent) {
                verticalTmp1.add(Index.of(index.xPos, y));
            } else if (this.getField(index.xPos, y) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            verticalTmp1.clear();
        }
        playerReached = false;
        final List<Index> verticalTmp2 = new ArrayList<>(4);
        for (int y = index.yPos - 1; y >= 0; --y) {
            if (this.getField(index.xPos, y) == opponent) {
                verticalTmp2.add(Index.of(index.xPos, y));
            } else if (this.getField(index.xPos, y) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            verticalTmp2.clear();
        }
        verticalTmp1.addAll(verticalTmp2);
        if (!verticalTmp1.isEmpty()) {
            res.add(verticalTmp1);
        }

        // calculate the lines to flip in horizontal direction

        final List<Index> horizontalTmp1 = new ArrayList<>(4);
        playerReached = false;
        for (int x = index.xPos + 1; x < this.field.length; ++x) {
            if (this.getField(x, index.yPos) == opponent) {
                horizontalTmp1.add(Index.of(x, index.yPos));
            } else if (this.getField(x, index.yPos) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            horizontalTmp1.clear();
        }
        final List<Index> horizontalTmp2 = new ArrayList<>(4);
        playerReached = false;
        for (int x = index.xPos - 1; x >= 0; --x) {
            if (this.getField(x, index.yPos) == opponent) {
                horizontalTmp2.add(Index.of(x, index.yPos));
            } else if (this.getField(x, index.yPos) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            horizontalTmp2.clear();
        }
        horizontalTmp1.addAll(horizontalTmp2);
        if (!horizontalTmp1.isEmpty()) {
            res.add(horizontalTmp1);
        }

        // calculate the lines to flip in diagonal direction

        final List<Index> diagonal1Tmp1 = new ArrayList<>(4);
        playerReached = false;
        for (int offset = 1; (offset + index.xPos) < 8 && (offset + index.xPos) >= 0
                && (offset + index.yPos) < 8 && (offset + index.yPos) >= 0; ++offset) {
            Index tmp = Index.of(index.xPos + offset, index.yPos + offset);
            if (this.getField(tmp) == opponent) {
                diagonal1Tmp1.add(tmp);
            } else if (this.getField(tmp) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            diagonal1Tmp1.clear();
        }
        final List<Index> diagonal1Tmp2 = new ArrayList<>(4);
        playerReached = false;
        for (int offset = -1; (offset + index.xPos) < 8 && (offset + index.xPos) >= 0
                && (offset + index.yPos) < 8 && (offset + index.yPos) >= 0; --offset) {
            Index tmp = Index.of(index.xPos + offset, index.yPos + offset);
            if (this.getField(tmp) == opponent) {
                diagonal1Tmp2.add(tmp);
            } else if (this.getField(tmp) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            diagonal1Tmp2.clear();
        }
        diagonal1Tmp1.addAll(diagonal1Tmp2);
        if (!diagonal1Tmp1.isEmpty()) {
            res.add(diagonal1Tmp1);
        }

        final List<Index> diagonal2Tmp1 = new ArrayList<>(4);
        playerReached = false;
        for (int offset = 1; (index.xPos + offset) < 8
                && (index.xPos + offset) >= 0
                && (index.yPos - offset) < 8
                && (index.yPos - offset) >= 0; ++offset) {
            Index tmp = Index.of(index.xPos + offset, index.yPos - offset);
            if (this.getField(tmp) == opponent) {
                diagonal2Tmp1.add(tmp);
            } else if (this.getField(tmp) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            diagonal2Tmp1.clear();
        }
        final List<Index> diagonal2Tmp2 = new ArrayList<>(4);
        playerReached = false;
        for (int offset = 1; (index.xPos - offset) < 8
                && (index.xPos - offset) >= 0
                && (index.yPos + offset) < 8
                && (index.yPos + offset) >= 0; ++offset) {
            Index tmp = Index.of(index.xPos - offset, index.yPos + offset);
            if (this.getField(tmp) == opponent) {
                diagonal2Tmp2.add(tmp);
            } else if (this.getField(tmp) == player) {
                playerReached = true;
                break; // the disks to flip end when a disk of the player is reached
            } else break; // if we reach an empty index, the line is invalid
        }
        if (!playerReached) { // if the line doesn't end in a disk of the player, the result cannot be used
            diagonal2Tmp2.clear();
        }
        diagonal2Tmp1.addAll(diagonal2Tmp2);
        if (!diagonal2Tmp1.isEmpty()) {
            res.add(diagonal2Tmp1);
        }

        return res;
    }

    /**
     * Execute an action for each field.
     *
     * @param consumer The action to be executed as a consumer.
     * @throws NullPointerException If the consumer is {@code null}.
     */
    public void forEach(BiConsumer<Index, FieldType> consumer) {
        Objects.requireNonNull(consumer, "parameter consumer must not be null");
        for (int x = 0; x < this.field.length; ++x) {
            for (int y = 0; y < this.field[x].length; ++y) {
                consumer.accept(Index.of(x, y), this.field[x][y]);
            }
        }
    }

    /**
     * Rate the state from the perspective of a player.
     * This includes counting the number of disks a player owns and subtracting the amount of possible moves the
     * opponent can make in this state.
     * This method is used by the artificial intelligence class.
     *
     * @param player       The player. Must not be {@code null}. Must be either {@link FieldState.FieldType#BLACK} or {@link FieldState.FieldType#WHITE}.
     * @param fieldWeights A 8 x 8 array that specifies how much each field is worth. Must not be {@code null}.
     *                     Note that y and x position are reversed to make it easier to specify the array in the source code.
     *                     Must not be {@code null}.
     * @return a number that represents how "good" a state is for the given player. If the player wins this number is
     * {@link Integer#MAX_VALUE}/2, if he loses it's {@link Integer#MIN_VALUE}/2
     * (needs to be smaller than MAX_VALUE / bigger than MIN_VALUE, because of how the return value is used)
     * @throws IllegalArgumentException If the player is not valid.
     * @throws NullPointerException     If the player or the fieldWeights array is {@code null}.
     */
    public int rateState(FieldType player, int[][] fieldWeights) {
        Objects.requireNonNull(fieldWeights, "parameter fieldWeights must not be null");
        FieldState.validatePlayer(player);
        final FieldType opponent = player == FieldType.WHITE ? FieldType.BLACK : FieldType.WHITE;

        final int myCount = this.weightedCount(player, fieldWeights);
        final Set<Index> opponentMoves = this.findPossibleMoves(opponent, true);

        final Optional<FieldState.FieldType> winner = this.calculateWinner();
        if (winner.isPresent() && winner.get() == player) {
            // if the player wins, return a very high value (not MAX_VALUE, because other ratings of branches that the
            // AI explores are calculated cumulatively and using MAX_VALUE might cause an overflow)
            return Integer.MAX_VALUE / 2;
        }
        if (winner.isPresent() && winner.get() == opponent) {
            // if the player loses, return a very low value (not MIN_VALUE, because other ratings of branches that the
            // AI explores are calculated cumulatively and using MIN_VALUE might cause an underflow)
            return Integer.MIN_VALUE / 2;
        }
        return myCount - opponentMoves.size();
    }

    /**
     * Count the fields according to the weights described in {@code FIELD_WEIGHTS}.
     * This method is supposed to be used only internally.
     * It doesn't check whether the parameters are valid.
     *
     * @param player       The player. Must not be {@code null}. Must be either {@link FieldState.FieldType#BLACK} or {@link FieldState.FieldType#WHITE}.
     * @param fieldWeights A 8 x 8 array that specifies how much each field is worth. Must not be {@code null}.
     *                     Note that y and x position are reversed to make it easier to specify the array in the source code.
     */
    private int weightedCount(FieldType player, int[][] fieldWeights) {
        // this whole method works like this with lambdas, but had to rewrite it manually for performance
        //return this.findMatchingIndexes(player).stream().mapToInt(index -> fieldWeights[index.yPos][index.xPos]).sum();
        int sum = 0;
        for (int x = 0; x < this.field.length; ++x) {
            for (int y = 0; y < this.field[x].length; ++y) {
                if (this.field[x][y] == player) sum += fieldWeights[y][x];
            }
        }
        return sum;
    }

    /**
     * Represents the type of a single item on the field.
     * An item can either be EMTPY (no disk present) or BLACK or WHITE.
     */
    public enum FieldType {
        EMPTY, BLACK, WHITE
    }

    /**
     * Exception that signifies an invalid move.
     * This class is needed so the default uncaught exception handler can ignore those "trivial" exceptions.
     */
    public static final class InvalidMoveException extends RuntimeException {
        /**
         * @see Exception#Exception(String)
         */
        public InvalidMoveException(String message) {
            super(message);
        }
    }
}
