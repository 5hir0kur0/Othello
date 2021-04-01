package othello;

/**
 * Represents an index on an Othello field.
 */
public final class Index {
    // it doesn't matter that the following fields are public since they're read-only anyway
    /**
     * Index cache that stores all possible indexes.
     * Generated at class-initialization time.
     */
    private static final Index[][] cache = Index.generate();
    /**
     * The x position on the Othello field.
     * 0 is the leftmost position, 7 is the rightmost position.
     */
    public final int xPos;

    // needed for performance reasons; creating new indexes each time turned out to be too slow
    // (it doesn't matter if all equal indexes are the same object since they are immutable)
    /**
     * The y position on the Othello field.
     * 0 is the topmost position, 7 is the bottommost position.
     */
    public final int yPos;

    /**
     * Create a new othello field index.
     *
     * @param xPos The x position. Must be positive and smaller than 8.
     * @param yPos The y position. Must be positive and smaller than 8.
     */
    private Index(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    /**
     * Return an othello board index.
     *
     * @param xPos The x position. Must be positive and smaller than 8.
     * @param yPos The y position. Must be positive and smaller than 8.
     * @throws ArrayIndexOutOfBoundsException If the xPos and yPos parameters do not constitute a valid index
     */
    public static Index of(int xPos, int yPos) {
        return Index.cache[xPos][yPos];
    }

    /**
     * Generate the Index cache. This method is only called once during the class-initialization.
     *
     * @return an array containing all possible index values
     */
    private static Index[][] generate() {
        final Index[][] indexes = new Index[8][8];
        // generate all possible index objects
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                indexes[x][y] = new Index(x, y);
            }
        }
        return indexes;
    }

    /**
     * (no javadoc)
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        return other != null && other instanceof Index && this.hashCode() == other.hashCode();
    }

    /**
     * (no javadoc)
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "[" + this.xPos + "][" + this.yPos + "]";
    }

    /**
     * (no javadoc)
     *
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        // shift by 4 is enough, because 0 <= xPos, yPos <= 7
        return (this.xPos << 4) | this.yPos;
    }
}
