package org.popcraft.chunkyborder;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.joml.Vector3f;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.NeoForgePlayer;
import org.popcraft.chunky.platform.NeoForgeWorld;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.integration.DynmapCommonAPIProvider;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.Particles;
import org.popcraft.chunkyborder.util.PluginMessage;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ChunkyBorderNeoForge.MOD_ID)
public class ChunkyBorderNeoForge {
    public static final String MOD_ID = "chunkyborder";
    private static final ResourceLocation PLAY_BORDER_PACKET_ID = new ResourceLocation("chunky", "border");
    private static final Map<ResourceLocation, BorderShape> borderShapes = new ConcurrentHashMap<>();
    private static Config config;
    private ChunkyBorder chunkyBorder;
    private BorderCheckTask borderCheckTask;
    private boolean initialized;

    public ChunkyBorderNeoForge() {
        try {
            Class.forName("org.dynmap.DynmapCommonAPI");
            new DynmapCommonAPIProvider();
        } catch (ClassNotFoundException ignored) {
            // Dynmap is not installed
        }
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        final Chunky chunky = ChunkyProvider.get();
        final Path configPath = FMLPaths.CONFIGDIR.get().resolve("chunkyborder/config.json");
        final Config config = new NeoForgeConfig(configPath);
        final MapIntegrationLoader mapIntegrationLoader = new NeoForgeMapIntegrationLoader();
        this.chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        Translator.addCustomTranslation("custom_border_message", config.message());
        BorderColor.parseColor(config.visualizerColor());
        new BorderInitializationTask(chunkyBorder).run();
        this.borderCheckTask = new BorderCheckTask(chunkyBorder);
        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
        this.initialized = true;
    }

    @SubscribeEvent
    public void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof final ServerPlayer player)) {
            return;
        }
        for (final World world : chunkyBorder.getChunky().getServer().getWorlds()) {
            final Shape shape = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
            sendBorderPacket(List.of(player), world, shape);
        }
    }

    @SubscribeEvent
    public void onServerStopping(final ServerStoppingEvent event) {
        chunkyBorder.disable();
    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (!initialized) {
            return;
        }
        final MinecraftServer server = event.getServer();
        final long tick = server.getTickCount();
        final long checkInterval = Math.max(1, chunkyBorder.getConfig().checkInterval());
        if (tick % checkInterval == 0) {
            borderCheckTask.run();
        }
        final boolean visualizerEnabled = chunkyBorder.getConfig().visualizerEnabled();
        if (!visualizerEnabled) {
            return;
        }
        final int maxRange = chunkyBorder.getConfig().visualizerRange();
        Particles.setMaxDistance(maxRange);
        server.getPlayerList().getPlayers().forEach(neoForgePlayer -> {
            final ServerLevel serverLevel = neoForgePlayer.serverLevel();
            final World world = new NeoForgeWorld(serverLevel);
            final Player player = new NeoForgePlayer(neoForgePlayer);
            final Shape border = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
            final boolean isUsingMod = chunkyBorder.getPlayerData(player.getUUID()).isUsingMod();
            if (border != null && !isUsingMod) {
                final List<Vector3> particleLocations = Particles.at(player, border, (tick % 20) / 20d);
                final Vector3f visualizerColor = new Vector3f(BorderColor.getRGB());
                for (final Vector3 location : particleLocations) {
                    final BlockPos pos = BlockPos.containing(location.getX(), location.getY(), location.getZ());
                    final boolean fullyOccluded = serverLevel.getBlockState(pos).isSolidRender(serverLevel, pos)
                            && serverLevel.getBlockState(pos.north()).isSolidRender(serverLevel, pos.north())
                            && serverLevel.getBlockState(pos.east()).isSolidRender(serverLevel, pos.east())
                            && serverLevel.getBlockState(pos.south()).isSolidRender(serverLevel, pos.south())
                            && serverLevel.getBlockState(pos.west()).isSolidRender(serverLevel, pos.west());
                    if (!fullyOccluded) {
                        serverLevel.sendParticles(neoForgePlayer, new DustParticleOptions(visualizerColor, 1f), false, location.getX(), location.getY(), location.getZ(), 1, 0d, 0d, 0d, 0d);
                    }
                }
            }
        });
    }

    private void sendBorderPacket(final Collection<ServerPlayer> players, final World world, final Shape shape) {
        final FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer())
                .writeResourceLocation(PLAY_BORDER_PACKET_ID)
                .writeBytes(PluginMessage.writeBorder(world, shape));
        for (final ServerPlayer player : players) {
            player.connection.send(new ClientboundCustomPayloadPacket(data));
        }
    }

    public static void setBorderShape(final String id, final BorderShape borderShape) {
        final ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier != null) {
            if (borderShape == null) {
                ChunkyBorderNeoForge.borderShapes.remove(identifier);
            } else {
                ChunkyBorderNeoForge.borderShapes.put(identifier, borderShape);
            }
        }
    }

    public static BorderShape getBorderShape(final ResourceLocation identifier) {
        return ChunkyBorderNeoForge.borderShapes.get(identifier);
    }

    public static Config getConfig() {
        return ChunkyBorderNeoForge.config;
    }

    public static void setConfig(final Config config) {
        ChunkyBorderNeoForge.config = config;
    }
}
