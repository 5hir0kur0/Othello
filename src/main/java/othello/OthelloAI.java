package othello;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A super smart artificial intelligence player of othello :D
 */
public class OthelloAI extends Player {
    // see wikipedia article about Othello if you want to know why some fields are more valuable than others
    /**
     * This array stores how "valuable" certain fields are to the player.
     * If you want to know more about why some fields are considered more valuable than others,
     * see: <a href="https://en.wikipedia.org/wiki/Computer_Othello#Disk-square_tables">Wikipedia Article on Computer Othello</a>
     */
    private static final int[][] FIELD_WEIGHTS = {
            {50, -5, 4, 4, 4, 4, -5, 50},
            {-5, -10, 1, 1, 1, 1, -10, -5},
            {4, 1, 1, 1, 1, 1, 1, 4},
            {4, 1, 1, 1, 1, 1, 1, 4},
            {4, 1, 1, 1, 1, 1, 1, 4},
            {4, 1, 1, 1, 1, 1, 1, 4},
            {4, 1, 1, 1, 1, 1, 1, 4},
            {-5, -10, 1, 1, 1, 1, -10, -5},
            {50, -5, 4, 4, 4, 4, -5, 50}};
    /**
     * This is the number of moves the AI looks into the future.
     * A higher value makes the AI play better but slower.
     */
    private static final int SEARCH_DEPTH = 6;
    /**
     * This is the branching factor for the exploration of the game tree.
     * Note that in the topmost calls an offset is added to this.
     */
    private static final int OPTIONS_TO_EXPLORE_LIMIT = 3; // set to 64 if you want to explore all options
    /**
     * Stores the last successful move.
     */
    private Index savedMove = null;

    /**
     * Create a new super smart AI.
     *
     * @param type @see {@link Player#Player(FieldState.FieldType)}
     */
    public OthelloAI(FieldState.FieldType type) {
        super(type);
    }

    /**
     * Calculate a set of moves to explore.
     *
     * @param possibleMoves The possible moves for the player (AI). Must not be {@code null}.
     * @param state         The current game state. Must not be {@code null}.
     * @param player        The player. Must not be {@code null}.
     * @param add           Added to {@link OthelloAI#OPTIONS_TO_EXPLORE_LIMIT} or ignored if it's smaller than 0.
     * @return A set of moves to explore (limited by {@link OthelloAI#OPTIONS_TO_EXPLORE_LIMIT}).
     */
    private static Set<MoveValue> exploreMoves(Set<Index> possibleMoves, FieldState state, FieldState.FieldType player, int add) {
        // sort moves by rating and return the top n (n=OPTIONS_TO_EXPLORE_LIMIT)
        return possibleMoves.stream().map(move -> {
            FieldState tmp = state.makeMove(player, move);
            return new MoveValue(tmp.rateState(player, OthelloAI.FIELD_WEIGHTS), move, tmp);
        })
                .sorted(Comparator.comparingInt(mv -> -mv.value))
                //                                    ^-- note the minus sign here (it reverses the sort order)
                // add offset if it is bigger than 0
                .limit(add < 0 ? OPTIONS_TO_EXPLORE_LIMIT : OPTIONS_TO_EXPLORE_LIMIT + add)
                .collect(Collectors.toSet());
    }

    /**
     * (no javadoc)
     *
     * @see Player#makeMove(FieldState)
     */
    @Override
    public Optional<Index> makeMove(FieldState state) {
        Objects.requireNonNull(state, "parameter state must not be null");

        if (!state.findPossibleMoves(this.type, true).isEmpty()) {
            max(state, this.type, OthelloAI.SEARCH_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE);
            // sometimes the AI can't make a move...
            return this.savedMove == null ? Optional.empty() : Optional.of(this.savedMove);
        } else {
            return Optional.empty();
        }
    }

    /**
     * The parameters are a subset of the parameters of
     * {@link OthelloAI#max(FieldState, FieldState.FieldType, int, int, int)}
     * or {@link OthelloAI#min(FieldState, FieldState.FieldType, int, int, int)}.
     * @return the rating if the recursion should end or {@link OptionalInt#empty()} otherwise
     */
    private OptionalInt checkIfRecursionEnd(int depth, Set<Index> possibleMoves, FieldState state,
                                            FieldState.FieldType player) {
        if (depth == 0 || possibleMoves.size() <= 1 || state.gameOver()) {
            if (possibleMoves.size() == 1) {
                // save move if in top-level call
                if (depth == OthelloAI.SEARCH_DEPTH) {
                    // there is only one, so the forEach will only run once
                    possibleMoves.forEach(move -> this.savedMove = move);
                }
                // if there is only one move left, execute it
                return OptionalInt.of(state.makeMove(player, possibleMoves.toArray(new Index[1])[0])
                        .rateState(player, OthelloAI.FIELD_WEIGHTS));
            }
            return OptionalInt.of(state.rateState(player, OthelloAI.FIELD_WEIGHTS));
        }
        return OptionalInt.empty();
    }

    /**
     * Max part of the Alpha Beta Minimax algorithm.
     * See <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea">https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea</a>
     *
     * @param state        The current game state. Must not be {@code null}.
     * @param player       The player. Must not be {@code null}.
     * @param depth        The current recursion level.
     * @param bestMaxSoFar The best maximum value that was found so far.
     * @param bestMinSoFar The best minimum value that was found so far.
     * @return The actual move is stored in {@link OthelloAI#savedMove}. This return value is only needed for the recursion.
     * (The return value is the rating of the current subtree.)
     */
    private int max(FieldState state, FieldState.FieldType player,
                    int depth, int bestMaxSoFar, int bestMinSoFar) {
        final Set<Index> possibleMoves = state.findPossibleMoves(player, true);
        final OptionalInt over = this.checkIfRecursionEnd(depth, possibleMoves, state, player);
        if (over.isPresent()) {
            return over.getAsInt();
        }
        final FieldState.FieldType opponent = player.opponent();
        int maxVal = bestMaxSoFar;
        // see wiki page https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea for explanation
        // about the algorithm
                                                                         // add a small offset to the number of explored moves
        																 // (at top levels)
    for (MoveValue move : exploreMoves(possibleMoves, state, player, depth/3)) {
            int val = min(move.result, opponent, depth - 1, maxVal, bestMinSoFar);
            if (val > maxVal) {
                maxVal = val;
                // save move if in top-level call
                if (depth == OthelloAI.SEARCH_DEPTH) this.savedMove = move.move;
                if (maxVal >= bestMinSoFar) break;
            }
        }
        return maxVal;
    }

    /**
     * Min part of the Alpha Beta Minimax algorithm.
     * See <a href="https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea">https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea</a>
     *
     * @param state        The current game state. Must not be {@code null}.
     * @param player       The player. Must not be {@code null}.
     * @param depth        The current recursion level.
     * @param bestMaxSoFar The best maximum value that was found so far.
     * @param bestMinSoFar The best minimum value that was found so far.
     * @return The actual move is stored in {@link OthelloAI#savedMove}. This return value is only needed for the recursion.
     * (The return value is the rating of the current subtree.)
     */
    private int min(FieldState state, FieldState.FieldType player,
                    int depth, int bestMaxSoFar, int bestMinSoFar) {
        final Set<Index> possibleMoves = state.findPossibleMoves(player, true);
        final OptionalInt over = this.checkIfRecursionEnd(depth, possibleMoves, state, player);
        if (over.isPresent()) {
            return over.getAsInt();
        }
        final FieldState.FieldType opponent = player.opponent();
        int minVal = bestMinSoFar;
        // see wiki page https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Core_idea for explanation
        // about the algorithm
                                                                         // add a small offset to the number of explored moves
        																 // (at top levels)
        for (MoveValue move : exploreMoves(possibleMoves, state, player, depth/2)) {
            int val = max(move.result, opponent, depth - 1, bestMaxSoFar, minVal);
            if (val < minVal) {
                minVal = val;
                // save move if in top-level call
                if (depth == OthelloAI.SEARCH_DEPTH) this.savedMove = move.move;
                if (minVal <= bestMaxSoFar) break;
            }
        }
        return minVal;
    }

    /**
     * Represents an AI move and it's associated value (rating).
     */
    private static final class MoveValue {
        /**
         * The move's value (rating).
         */
        final int value;
        /**
         * The position at which the disk is placed.
         */
        final Index move;
        /**
         * The game state resulting from the move.
         */
        final FieldState result;

        /**
         * Create a new MoveValue.
         *
         * @param value  The value. May be any integer.
         * @param move   The move. Must not be {@code null}.
         * @param result The result of the move. May be {@code null}.
         */
        MoveValue(int value, Index move, FieldState result) {
            this.value = value;
            this.move = Objects.requireNonNull(move, "move may not be null");
            this.result = result;
        }
    }

}
