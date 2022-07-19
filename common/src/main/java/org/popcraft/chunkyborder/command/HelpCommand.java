package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.List;

public class HelpCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public HelpCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        sender.sendMessage(TranslationKey.HELP_BORDER);
    }

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        return List.of();
    }
}
