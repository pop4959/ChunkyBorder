package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.Map;

import static org.popcraft.chunky.util.Translator.translate;

public class WrapCommand extends ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public WrapCommand(ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(Sender sender, String[] args) {
        Map<String, BorderData> borders = chunkyBorder.getBorders();
        Selection selection = chunky.getSelection().build();
        final World world = selection.world();
        BorderData currentBorder = borders.get(world.getName());
        if (currentBorder != null) {
            currentBorder.setWrap(!currentBorder.isWrap());
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_WRAP,
                    translate(currentBorder.isWrap() ? TranslationKey.ENABLED : TranslationKey.DISABLED),
                    world.getName()
            );
            chunkyBorder.saveBorders();
        } else {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_NO_BORDER, world.getName());
        }
    }
}
