package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.Formatting;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.Map;

public class ListCommand extends ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public ListCommand(ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(Sender sender, String[] args) {
        Map<String, BorderData> borders = chunkyBorder.getBorders();
        if (!borders.isEmpty()) {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_LIST);
            borders.values().forEach(border -> {
                Selection borderSelection = border.asSelection().build();
                sender.sendMessage(TranslationKey.FORMAT_BORDER_LIST_BORDER,
                        border.getWorld(),
                        border.getShape(),
                        Formatting.number(border.getCenterX()),
                        Formatting.number(border.getCenterZ()),
                        Formatting.radius(borderSelection)
                );
            });
        } else {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_LIST_NONE);
        }
    }
}
