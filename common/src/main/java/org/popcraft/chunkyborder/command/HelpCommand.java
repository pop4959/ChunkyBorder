package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.ChunkyBorder;

public class HelpCommand extends ChunkyCommand {
    public HelpCommand(ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
    }

    @Override
    public void execute(Sender sender, String[] args) {
        sender.sendMessage(TranslationKey.HELP_BORDER);
    }
}
