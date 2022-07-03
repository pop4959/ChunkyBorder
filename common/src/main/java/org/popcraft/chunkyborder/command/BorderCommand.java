package org.popcraft.chunkyborder.command;

import org.popcraft.chunky.command.ChunkyCommand;
import org.popcraft.chunky.command.CommandLiteral;
import org.popcraft.chunky.platform.Sender;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderCommand extends ChunkyCommand {
    private final Map<String, ChunkyCommand> subCommands = new HashMap<>();

    public BorderCommand(final ChunkyBorder chunkyBorder) {
        super(chunkyBorder.getChunky());
        subCommands.put("add", new AddCommand(chunkyBorder));
        subCommands.put("bypass", new BypassCommand(chunkyBorder));
        subCommands.put("help", new HelpCommand(chunkyBorder));
        subCommands.put("list", new ListCommand(chunkyBorder));
        subCommands.put("load", new LoadCommand(chunkyBorder));
        subCommands.put("remove", new RemoveCommand(chunkyBorder));
        subCommands.put("wrap", new WrapCommand(chunkyBorder));
    }

    @Override
    public void execute(final Sender sender, final String[] args) {
        if (args.length > 1) {
            final String subCommand = args[1].toLowerCase();
            if (subCommands.containsKey(subCommand)) {
                subCommands.get(subCommand).execute(sender, args);
                return;
            }
        }
        final String label = String.join(" ", args);
        subCommands.get(CommandLiteral.HELP).execute(sender, new String[]{label});
    }

    @Override
    public List<String> tabSuggestions(final String[] args) {
        final List<String> suggestions = new ArrayList<>();
        if (args.length == 2) {
            suggestions.addAll(subCommands.keySet());
        } else if (args.length > 2) {
            final String subCommand = args[1].toLowerCase();
            if (subCommands.containsKey(subCommand)) {
                suggestions.addAll(subCommands.get(subCommand).tabSuggestions(args));
            }
        }
        return suggestions;
    }
}
