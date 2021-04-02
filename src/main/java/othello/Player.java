package othello;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an Othello player.
 */
public class Player {

    final protected FieldState.FieldType type;
    final protected String name;

    /**
     * Construct a new player.
     *
     * @param type The type of tiles that the player uses. Must either be
     *             {@link othello.FieldState.FieldType#WHITE} or
     *             {@link othello.FieldState.FieldType#BLACK}
     * @param name The player's name. Must not be {@code null}.
     */
    public Player(FieldState.FieldType type, String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (Objects.requireNonNull(type, "parameter type must not be null") == FieldState.FieldType.EMPTY) {
            throw new IllegalArgumentException("parameter type must not be empty");
        }
        this.type = type;
        this.name = name;
    }

    /**
     * Construct a new player.
     *
     * @param type The type of tiles that the player uses. Must either be
     *             {@link othello.FieldState.FieldType#WHITE} or
     *             {@link othello.FieldState.FieldType#BLACK}
     */
    public Player(FieldState.FieldType type) {
        this(type, "");
    }

    /**
     * @return the type of the player
     */
    public FieldState.FieldType getType() {
        return this.type;
    }

    /**
     * Make a move.
     *
     * @param state the current state of the game (should not be {@code null})
     * @return the index of the tile to be placed. (should not be {@code null},
     * but instead {@link Optional#empty()})
     */
    public Optional<Index> makeMove(FieldState state) {
        return Optional.empty(); // this method is only valid for AI players
    }

    /**
     * @return The player's name.
     */
    public String getName() {
        return this.name;
    }
}
