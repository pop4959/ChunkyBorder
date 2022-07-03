package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.Formatting;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.Map;

public class AddCommand extends ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public AddCommand(final ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final String[] args) {
        final Selection selection = chunky.getSelection().build();
        final World world = selection.world();
        final BorderData borderData = new BorderData(selection);
        final Map<String, BorderData> borders = chunkyBorder.getBorders();
        final BorderData currentBorder = borders.get(world.getName());
        if (currentBorder != null) {
            borderData.setWrap(currentBorder.isWrap());
        }
        borders.put(world.getName(), borderData);
        chunkyBorder.getMapIntegrations().forEach(mapIntegration -> mapIntegration.addShapeMarker(world, borderData.getBorder()));
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
