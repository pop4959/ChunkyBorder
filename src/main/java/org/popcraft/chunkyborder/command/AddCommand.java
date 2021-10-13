package org.popcraft.chunkyborder.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.Square;
import org.popcraft.chunky.util.Formatting;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.Map;

public class AddCommand extends ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public AddCommand(ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(Sender sender, String[] args) {
        Selection selection = chunky.getSelection().build();
        final org.popcraft.chunky.platform.World world = selection.world();
        BorderData borderData = new BorderData(selection);
        Map<String, BorderData> borders = chunkyBorder.getBorders();
        BorderData currentBorder = borders.get(world.getName());
        if (currentBorder != null) {
            borderData.setWrap(currentBorder.isWrap());
        }
        borders.put(world.getName(), borderData);
        chunkyBorder.getMapIntegrations().forEach(mapIntegration -> mapIntegration.addShapeMarker(world, borderData.getBorder()));
        final boolean syncVanilla = chunkyBorder.getConfig().getBoolean("border-options.sync-vanilla", false);
        if (syncVanilla) {
            Shape border = borderData.getBorder();
            if (border instanceof Square) {
                Square square = (Square) border;
                World toSync = Bukkit.getWorld(world.getName());
                if (toSync != null) {
                    double[] center = square.getCenter();
                    double[] points = square.pointsX();
                    double size = Math.abs(points[1] - points[0]);
                    WorldBorder worldBorder = toSync.getWorldBorder();
                    worldBorder.setCenter(center[0], center[1]);
                    worldBorder.setSize(size);
                }
            }
        }
        sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_ADD,
                selection.shape(),
                world.getName(),
                Formatting.number(selection.centerX()),
                Formatting.number(selection.centerZ()),
                Formatting.radius(selection)
        );
        chunkyBorder.saveBorders();
    }
}
