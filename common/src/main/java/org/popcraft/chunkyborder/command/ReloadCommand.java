package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.List;

public class ReloadCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public ReloadCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        chunkyBorder.getConfig().reload();
        chunkyBorder.reloadBorders();
        Translator.addCustomTranslation("custom_border_message", chunkyBorder.getConfig().message());
        sender.sendMessagePrefixed(TranslationKey.FORMAT_RELOAD);
    }

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        return List.of();
    }
}
