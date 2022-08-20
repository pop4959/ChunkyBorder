package org.popcraft.chunkyborder;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.util.PluginMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class ChunkyBorderFabric implements ModInitializer {
    private static final Identifier PLAY_BORDER_PACKET_ID = new Identifier("chunky", "border");
    private boolean registered;

    @Override
    public void onInitialize() {
        final Chunky chunky = ChunkyProvider.get();
        final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkyborder/config.json");
        final Config config = new FabricConfig(configPath);
        final MapIntegrationLoader mapIntegrationLoader = new FabricMapIntegrationLoader();
        final ChunkyBorder chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        Translator.addCustomTranslation("custom_border_message", config.message());
        new BorderInitializationTask(chunkyBorder).run();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!registered) {
                chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, e -> sendBorderPacket(server.getPlayerManager().getPlayerList(), e.world(), e.shape()));
                registered = true;
            }
            for (final World world : chunkyBorder.getChunky().getServer().getWorlds()) {
                final Shape shape = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
                sendBorderPacket(List.of(handler.player), world, shape);
            }
        });
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

    private void sendBorderPacket(final Collection<ServerPlayerEntity> players, final World world, final Shape shape) {
        final PacketByteBuf data;
        try {
            data = new PacketByteBuf(Unpooled.wrappedBuffer(PluginMessage.writeBorderData(world, shape)));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for (final ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(PLAY_BORDER_PACKET_ID, data));
        }
    }
}
