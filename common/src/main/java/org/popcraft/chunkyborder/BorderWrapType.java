package org.popcraft.chunkyborder;

public enum BorderWrapType {
    NONE,
    DEFAULT,
    BOTH,
    RADIAL,
    X,
    Z,
    EARTH;

    public static BorderWrapType fromString(final String type) {
        if (type == null) {
            return NONE;
        }
        final String typeUpper = type.toUpperCase();
        return switch (typeUpper) {
            case "TRUE" -> DEFAULT;
            case "FALSE" -> NONE;
            default -> {
                try {
                    yield BorderWrapType.valueOf(typeUpper);
                } catch (final IllegalArgumentException e) {
                    yield NONE;
                }
            }
        };
    }
}
