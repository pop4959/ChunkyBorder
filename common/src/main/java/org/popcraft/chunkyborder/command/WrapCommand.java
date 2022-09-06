package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.BorderWrapType;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.ArrayList;
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
            final BorderWrapType wrap = arguments.next().map(BorderWrapType::fromString).orElse(BorderWrapType.NONE);
            currentBorder.setWrap(wrap.toString());
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_WRAP,
                    translate("wrap_%s".formatted(wrap.toString().toLowerCase())),
                    world.getName()
            );
            chunkyBorder.saveBorders();
        } else {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_NO_BORDER, world.getName());
        }
    }

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        if (commandArguments.size() == 2) {
            final List<String> suggestions = new ArrayList<>();
            for (final BorderWrapType wrapType : BorderWrapType.values()) {
                suggestions.add(wrapType.name().toLowerCase());
            }
            return suggestions;
        }
        return List.of();
    }
}
