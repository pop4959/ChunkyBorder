package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandArguments;
import org.popcraft.chunky.command.CommandLiteral;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderCommand implements ChunkyCommand {
    private final Map<String, ChunkyCommand> subCommands = new HashMap<>();

    public BorderCommand(final ChunkyBorder chunkyBorder) {
        subCommands.put("add", new AddCommand(chunkyBorder));
        subCommands.put("bypass", new BypassCommand(chunkyBorder));
        subCommands.put("help", new HelpCommand(chunkyBorder));
        subCommands.put("list", new ListCommand(chunkyBorder));
        subCommands.put("load", new LoadCommand(chunkyBorder));
        subCommands.put("remove", new RemoveCommand(chunkyBorder));
        subCommands.put("wrap", new WrapCommand(chunkyBorder));
    }

    @Override
    public void execute(final Sender sender, final CommandArguments arguments) {
        final ChunkyCommand subCommand = arguments.next()
                .map(String::toLowerCase)
                .map(subCommands::get)
                .orElse(null);
        if (subCommand == null) {
            subCommands.get(CommandLiteral.HELP).execute(sender, CommandArguments.empty());
            return;
        }
        subCommand.execute(sender, arguments);
    }

    @Override
    public List<String> suggestions(final CommandArguments arguments) {
        final List<String> suggestions = new ArrayList<>();
        if (arguments.size() == 1) {
            suggestions.addAll(subCommands.keySet());
        } else if (arguments.size() > 1) {
            arguments.next()
                    .map(String::toLowerCase)
                    .map(subCommands::get)
                    .ifPresent(subCommand -> suggestions.addAll(subCommand.suggestions(arguments)));
        }
        return suggestions;
    }
}
