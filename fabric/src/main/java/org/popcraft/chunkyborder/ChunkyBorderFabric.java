package org.popcraft.chunkyborder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.nio.file.Path;

public class ChunkyBorderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        final Chunky chunky = ChunkyProvider.get();
        final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkyborder/chunkyborder.json");
        final Config config = new FabricConfig(configPath);
        final MapIntegrationLoader mapIntegrationLoader = new FabricMapIntegrationLoader();
        final ChunkyBorder chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        Translator.addCustomTranslation("custom_border_message", config.message());
        new BorderInitializationTask(chunkyBorder).run();
        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> chunkyBorder.disable());
        final long checkInterval = Math.max(1, chunkyBorder.getConfig().checkInterval());
        final BorderCheckTask borderCheckTask = new BorderCheckTask(chunkyBorder);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % checkInterval == 0) {
                borderCheckTask.run();
            }
        });
        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
    }
}
