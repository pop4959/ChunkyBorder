package org.popcraft.chunkyborder.util;

import java.awt.Color;

public final class BorderColor {
    private static int colorCode;

    private BorderColor() {
    }

    public static void parseColor(final String color) {
        try {
            colorCode = Integer.parseInt(color, 16);
        } catch (final NumberFormatException e) {
            colorCode = -1;
        }
    }

    public static int getColor() {
        if (colorCode >= 0) {
            return colorCode;
        }
        final float hue = ((System.currentTimeMillis() % 10000000L) / 10000000F) * 360F;
        return 0xFFFFFF & Color.HSBtoRGB(hue, 1F, 1F);
    }

    public static float[] getRGB() {
        final int color = getColor();
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;
        return new float[]{r, g, b};
    }
}
