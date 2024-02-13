package org.popcraft.chunkyborder;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.joml.Vector3f;
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
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.Particles;
import org.popcraft.chunkyborder.util.PluginMessage;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ChunkyBorderForge.MOD_ID)
public class ChunkyBorderForge {
    public static final String MOD_ID = "chunkyborder";
    private static final ResourceLocation PLAY_BORDER_PACKET_ID = new ResourceLocation("chunky", "border");
    private static final Map<ResourceLocation, BorderShape> borderShapes = new ConcurrentHashMap<>();
    private static Config config;
    private ChunkyBorder chunkyBorder;
    private boolean registered;
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
    public void onCustomPayload(final CustomPayloadEvent event) {
        final ResourceLocation channel = event.getChannel();
        final ServerPlayer player = event.getSource().getSender();
        if (player == null) {
            return;
        }
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!registered) {
            chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, e -> sendBorderPacket(server.getPlayerList().getPlayers(), e.world(), e.shape()));
            registered = true;
        }
        if (PLAY_BORDER_PACKET_ID.equals(channel)) {
            chunkyBorder.getPlayerData(player.getUUID()).setUsingMod(true);
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
                final Vector3f visualizerColor = new Vector3f(BorderColor.getRGB());
                for (final Vector3 location : particleLocations) {
                    final BlockPos pos = BlockPos.containing(location.getX(), location.getY(), location.getZ());
                    final boolean fullyOccluded = serverLevel.getBlockState(pos).isSolidRender(serverLevel, pos)
                            && serverLevel.getBlockState(pos.north()).isSolidRender(serverLevel, pos.north())
                            && serverLevel.getBlockState(pos.east()).isSolidRender(serverLevel, pos.east())
                            && serverLevel.getBlockState(pos.south()).isSolidRender(serverLevel, pos.south())
                            && serverLevel.getBlockState(pos.west()).isSolidRender(serverLevel, pos.west());
                    if (!fullyOccluded) {
                        serverLevel.sendParticles(forgePlayer, new DustParticleOptions(visualizerColor, 1f), false, location.getX(), location.getY(), location.getZ(), 1, 0d, 0d, 0d, 0d);
                    }
                }
            }
        });
    }

    private void sendBorderPacket(final Collection<ServerPlayer> players, final World world, final Shape shape) {
        final FriendlyByteBuf data;
        try {
            data = new FriendlyByteBuf(Unpooled.buffer())
                    .writeResourceLocation(PLAY_BORDER_PACKET_ID)
                    .writeBytes(PluginMessage.writeBorderData(world, shape));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for (final ServerPlayer player : players) {
            player.connection.send(new ClientboundCustomPayloadPacket(data));
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

        @SubscribeEvent
        public void onCustomPayload(final CustomPayloadEvent event) {
            final ResourceLocation channel = event.getChannel();
            if (!PLAY_BORDER_PACKET_ID.equals(channel)) {
                return;
            }
            try (final ByteBufInputStream in = new ByteBufInputStream(event.getPayload()); final DataInputStream data = new DataInputStream(in)) {
                final int version = data.readInt();
                final String worldKey = data.readUTF();
                if (version == 0) {
                    final byte type = data.readByte();
                    switch (type) {
                        case 1 -> {
                            final int numPoints = data.readInt();
                            final double[] pointsX = new double[numPoints];
                            final double[] pointsZ = new double[numPoints];
                            for (int i = 0; i < numPoints; ++i) {
                                pointsX[i] = data.readDouble();
                                pointsZ[i] = data.readDouble();
                            }
                            ChunkyBorderForge.setBorderShape(worldKey, new PolygonBorderShape(pointsX, pointsZ));
                        }
                        case 2 -> {
                            final double centerX = data.readDouble();
                            final double centerZ = data.readDouble();
                            final double radiusX = data.readDouble();
                            final double radiusZ = data.readDouble();
                            ChunkyBorderForge.setBorderShape(worldKey, new EllipseBorderShape(centerX, centerZ, radiusX, radiusZ));
                        }
                        default -> ChunkyBorderForge.setBorderShape(worldKey, null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setBorderShape(final String id, final BorderShape borderShape) {
        final ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier != null) {
            if (borderShape == null) {
                ChunkyBorderForge.borderShapes.remove(identifier);
            } else {
                ChunkyBorderForge.borderShapes.put(identifier, borderShape);
            }
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
