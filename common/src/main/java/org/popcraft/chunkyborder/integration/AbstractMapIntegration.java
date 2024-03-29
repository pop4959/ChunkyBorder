package org.popcraft.chunkyborder.integration;

public abstract class AbstractMapIntegration implements MapIntegration {
    protected static final int DEFAULT_COLOR = 0xFF0000;
    protected String label = "World Border";
    protected int color = DEFAULT_COLOR;
    protected int weight = 3;

    @Override
    public void setOptions(final String label, final String color, final boolean hideByDefault, final int priority, final int weight) {
        if (label != null && !label.isEmpty()) {
            this.label = label;
        }
        this.weight = Math.max(1, weight);
        if (color.length() != 6) {
            return;
        }
        try {
            this.color = Integer.parseInt(color, 16);
        } catch (NumberFormatException ignored) {
            this.color = DEFAULT_COLOR;
        }
    }
}
