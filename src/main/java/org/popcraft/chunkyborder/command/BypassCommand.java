package org.popcraft.chunkyborder.command;

import org.bukkit.entity.Player;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.PlayerData;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.popcraft.chunky.util.Translator.translate;

public class BypassCommand extends ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public BypassCommand(ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(Sender sender, String[] args) {
        final Player target;
        if (sender.isPlayer() && args.length == 2) {
            target = chunkyBorder.getServer().getPlayer(sender.getName());
        } else {
            target = args.length > 2 ? chunkyBorder.getServer().getPlayer(args[2]) : null;
        }
        if (target == null) {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_BYPASS_NO_TARGET, args[2]);
        } else {
            final PlayerData playerData = chunkyBorder.getPlayerData(target);
            playerData.setBypassing(!playerData.isBypassing());
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_BYPASS,
                    translate(playerData.isBypassing() ? TranslationKey.ENABLED : TranslationKey.DISABLED),
                    target.getName()
            );
        }
    }

    @Override
    public List<String> tabSuggestions(String[] args) {
        if (args.length == 3) {
            return chunkyBorder.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
