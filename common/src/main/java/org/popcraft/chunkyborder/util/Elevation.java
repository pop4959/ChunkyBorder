package org.popcraft.chunkyborder.util;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

public class Elevation {
    private static final int SEARCH_DISTANCE = 16;

    private Elevation() {
    }

    public static OptionalInt search(final int y, final int minY, final int maxY, final IntPredicate isSafe) {
        for (int offset = 0; offset <= SEARCH_DISTANCE; ++offset) {
            final int up = y + offset;
            if (up >= minY && up <= maxY && isSafe.test(up)) {
                return OptionalInt.of(up);
            }
            final int down = y - offset;
            if (offset > 0 && down >= minY && down <= maxY && isSafe.test(down)) {
                return OptionalInt.of(down);
            }
        }
        return OptionalInt.empty();
    }
}
