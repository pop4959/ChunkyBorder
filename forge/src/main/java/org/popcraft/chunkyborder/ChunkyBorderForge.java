package org.popcraft.chunkyborder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.ForgePlayer;
import org.popcraft.chunky.platform.ForgeWorld;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.integration.DynmapCommonAPIProvider;
import org.popcraft.chunkyborder.packet.BorderPayload;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.ClientBorder;
import org.popcraft.chunkyborder.util.Particles;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ChunkyBorderForge.MOD_ID)
public class ChunkyBorderForge {
    public static final String MOD_ID = "chunkyborder";
    private static final ResourceLocation PLAY_BORDER_PACKET_ID = ResourceLocation.fromNamespaceAndPath("chunky", "border");
    public static final Channel<CustomPacketPayload> PLAY_BORDER_CHANNEL = ChannelBuilder.named(PLAY_BORDER_PACKET_ID)
            .optional()
            .payloadChannel()
            .play()
            .clientbound()
            .add(BorderPayload.ID, StreamCodec.ofMember(BorderPayload::write, BorderPayload::new), (borderPayload, context) -> {
                context.setPacketHandled(true);
                final ClientBorder clientBorder = borderPayload.getBorder();
                if (clientBorder.worldKey() == null) {
                    return;
                }
                final ResourceLocation identifier = ResourceLocation.tryParse(clientBorder.worldKey());
                if (identifier == null) {
                    return;
                }
                final BorderShape borderShape = clientBorder.borderShape();
                if (borderShape == null) {
                    ChunkyBorderForge.borderShapes.remove(identifier);
                } else {
                    ChunkyBorderForge.borderShapes.put(identifier, borderShape);
                }
            })
            .build();
    private static final Map<ResourceLocation, BorderShape> borderShapes = new ConcurrentHashMap<>();
    private static Config config;
    private ChunkyBorder chunkyBorder;
    private BorderCheckTask borderCheckTask;
    private boolean initialized;

    public ChunkyBorderForge() {
        try {
            Class.forName("org.dynmap.DynmapCommonAPI");
            new DynmapCommonAPIProvider();
        } catch (ClassNotFoundException ignored) {
            // Dynmap is not installed
        }
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        final Chunky chunky = ChunkyProvider.get();
        final Path configPath = FMLPaths.CONFIGDIR.get().resolve("chunkyborder/config.json");
        final Config config = new ForgeConfig(configPath);
        final MapIntegrationLoader mapIntegrationLoader = new ForgeMapIntegrationLoader();
        this.chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        Translator.addCustomTranslation("custom_border_message", config.message());
        BorderColor.parseColor(config.visualizerColor());
        new BorderInitializationTask(chunkyBorder).run();
        this.borderCheckTask = new BorderCheckTask(chunkyBorder);
        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
        chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, e -> sendBorderPacket(event.getServer().getPlayerList().getPlayers(), e.world(), e.shape()));
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
        server.getPlayerList().getPlayers().forEach(forgePlayer -> {
            final ServerLevel serverLevel = forgePlayer.serverLevel();
            final World world = new ForgeWorld(serverLevel);
            final Player player = new ForgePlayer(forgePlayer);
            final Shape border = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
            final boolean isUsingMod = chunkyBorder.getPlayerData(player.getUUID()).isUsingMod();
            if (border != null && !isUsingMod) {
                final List<Vector3> particleLocations = Particles.at(player, border, (tick % 20) / 20d);
                for (final Vector3 location : particleLocations) {
                    final BlockPos pos = BlockPos.containing(location.getX(), location.getY(), location.getZ());
                    final boolean fullyOccluded = serverLevel.getBlockState(pos).isSolidRender()
                            && serverLevel.getBlockState(pos.north()).isSolidRender()
                            && serverLevel.getBlockState(pos.east()).isSolidRender()
                            && serverLevel.getBlockState(pos.south()).isSolidRender()
                            && serverLevel.getBlockState(pos.west()).isSolidRender();
                    if (!fullyOccluded) {
                        serverLevel.sendParticles(forgePlayer, new DustParticleOptions(BorderColor.getColor(), 1f), false, location.getX(), location.getY(), location.getZ(), 1, 0d, 0d, 0d, 0d);
                    }
                }
            }
        });
    }

    private void sendBorderPacket(final Collection<ServerPlayer> players, final World world, final Shape shape) {
        for (final ServerPlayer player : players) {
            PLAY_BORDER_CHANNEL.send(new BorderPayload(world, shape), player.connection.getConnection());
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            final Path configPath = FMLPaths.CONFIGDIR.get().resolve("chunkyborder/config.json");
            ChunkyBorderForge.setConfig(new ForgeConfig(configPath));
            BorderColor.parseColor(config.visualizerColor());
        }
    }

    public static BorderShape getBorderShape(final ResourceLocation identifier) {
        return ChunkyBorderForge.borderShapes.get(identifier);
    }

    public static Config getConfig() {
        return ChunkyBorderForge.config;
    }

    public static void setConfig(final Config config) {
        ChunkyBorderForge.config = config;
    }
}
