package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.List;
import java.util.Map;

import static org.popcraft.chunky.util.Translator.translate;

public class WrapCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public WrapCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        final Map<String, BorderData> borders = chunkyBorder.getBorders();
        final Selection selection = chunkyBorder.getChunky().getSelection().build();
        final World world = selection.world();
        final BorderData currentBorder = borders.get(world.getName());
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

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        return List.of();
    }
}
