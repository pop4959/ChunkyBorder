package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.ShapeType;
import org.popcraft.chunky.util.Formatting;
import org.popcraft.chunky.util.Input;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddCommand implements ChunkyCommand {
    private final ChunkyBorder chunkyBorder;

    public AddCommand(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        final Chunky chunky = chunkyBorder.getChunky();
        if (arguments.size() > 1) {
            final Optional<World> world = arguments.next().flatMap(arg -> Input.tryWorld(chunky, arg));
            if (world.isPresent()) {
                chunky.getSelection().world(world.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        if (arguments.size() > 2) {
            final Optional<String> shape = arguments.next().flatMap(Input::tryShape);
            if (shape.isPresent()) {
                chunky.getSelection().shape(shape.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        if (arguments.size() > 3) {
            final Optional<Double> centerX = arguments.next().flatMap(Input::tryDoubleSuffixed).filter(c -> !Input.isPastWorldLimit(c));
            final Optional<Double> centerZ = arguments.next().flatMap(Input::tryDoubleSuffixed).filter(c -> !Input.isPastWorldLimit(c));
            if (centerX.isPresent() && centerZ.isPresent()) {
                chunky.getSelection().center(centerX.get(), centerZ.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        if (arguments.size() > 5) {
            final Optional<Double> radiusX = arguments.next().flatMap(Input::tryDoubleSuffixed).filter(r -> r >= 0 && !Input.isPastWorldLimit(r));
            if (radiusX.isPresent()) {
                chunky.getSelection().radius(radiusX.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        if (arguments.size() > 6) {
            final Optional<Double> radiusZ = arguments.next().flatMap(Input::tryDoubleSuffixed).filter(r -> r >= 0 && !Input.isPastWorldLimit(r));
            if (radiusZ.isPresent()) {
                chunky.getSelection().radiusZ(radiusZ.get());
            } else {
                sender.sendMessage(TranslationKey.HELP_BORDER);
                return;
            }
        }
        final Selection selection = chunky.getSelection().build();
        final World world = selection.world();
        chunkyBorder.addBorder(world, new BorderData(selection));
        sender.sendMessagePrefixed(TranslationKey.FORMAT_BORDER_ADD,
                selection.shape(),
                world.getName(),
                Formatting.number(selection.centerX()),
                Formatting.number(selection.centerZ()),
                Formatting.radius(selection)
        );
    }

    @Override
    public List<String> suggestions(final CommandArguments commandArguments) {
        if (commandArguments.size() == 2) {
            final List<String> suggestions = new ArrayList<>();
            chunkyBorder.getChunky().getServer().getWorlds().forEach(world -> suggestions.add(world.getName()));
            return suggestions;
        } else if (commandArguments.size() == 3) {
            return ShapeType.all();
        }
        return List.of();
    }
}
