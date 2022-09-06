package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.Input;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoveCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public RemoveCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        if (arguments.size() > 1) {
            final Chunky chunky = chunkyBorder.getChunky();
            final Optional<World> world = arguments.next().flatMap(arg -> Input.tryWorld(chunky, arg));
            if (world.isPresent()) {
                chunky.getSelection().world(world.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        final Map<String, BorderData> borders = chunkyBorder.getBorders();
        final Selection selection = chunkyBorder.getChunky().getSelection().build();
        final World world = selection.world();
        final BorderData currentBorder = borders.get(world.getName());
        if (currentBorder != null) {
            borders.remove(world.getName());
            chunkyBorder.getMapIntegrations().forEach(mapIntegration -> mapIntegration.removeShapeMarker(world));
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_REMOVE, world.getName());
            chunkyBorder.saveBorders();
        } else {
            sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_NO_BORDER, world.getName());
        }
        chunkyBorder.getChunky().getEventBus().call(new BorderChangeEvent(world, null));
    }

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        if (commandArguments.size() == 2) {
            final List<String> suggestions = new ArrayList<>();
            chunkyBorder.getChunky().getServer().getWorlds().forEach(world -> suggestions.add(world.getName()));
            return suggestions;
        }
        return List.of();
    }
}
