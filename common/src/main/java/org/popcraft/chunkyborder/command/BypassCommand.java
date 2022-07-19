package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.PlayerData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.popcraft.chunky.util.Translator.translate;

public class BypassCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public BypassCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        final Optional<String> argument = arguments.next();
        final Sender target;
        if (argument.isEmpty()) {
            target = sender;
        } else {
            final String playerName = argument.get();
            target = chunkyBorder.getChunky().getServer().getPlayer(playerName).orElse(null);
            if (target == null) {
                sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_BYPASS_NO_TARGET, playerName);
                return;
            }
        }
        if (target instanceof final Player player) {
            final PlayerData playerData = chunkyBorder.getPlayerData(player.getUUID());
            playerData.setBypassing(!playerData.isBypassing());
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_BYPASS,
                    translate(playerData.isBypassing() ? TranslationKey.ENABLED : TranslationKey.DISABLED),
                    target.getName()
            );
        }
    }

    @Override
    public List<String> suggestions(final CommandArguments arguments) {
        if (arguments.size() == 2) {
            return chunkyBorder.getChunky().getServer().getPlayers().stream().map(Sender::getName).toList();
        } else {
            return Collections.emptyList();
        }
    }
}
