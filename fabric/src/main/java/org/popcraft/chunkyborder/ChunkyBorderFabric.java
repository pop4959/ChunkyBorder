package org.popcraft.chunkyborder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.FabricPlayer;
import org.popcraft.chunky.platform.FabricWorld;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.event.server.BlockBreakEvent;
import org.popcraft.chunkyborder.packet.BorderPayload;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.Particles;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class ChunkyBorderFabric implements ModInitializer {
    private ChunkyBorder chunkyBorder;
    private boolean registered;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onInitialize() {
        final Chunky chunky = ChunkyProvider.get();
        final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkyborder/config.json");
        final Config config = new FabricConfig(configPath);
        final MapIntegrationLoader mapIntegrationLoader = new FabricMapIntegrationLoader();
        this.chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        Translator.addCustomTranslation("custom_border_message", config.message());
        BorderColor.parseColor(config.visualizerColor());
        new BorderInitializationTask(chunkyBorder).run();
        final PayloadTypeRegistry<RegistryByteBuf> payloadTypeRegistry = PayloadTypeRegistry.playS2C();
        if (payloadTypeRegistry instanceof final PayloadTypeRegistryImpl<RegistryByteBuf> payloadTypeRegistryImpl) {
            if (payloadTypeRegistryImpl.get(BorderPayload.ID) == null) {
                PayloadTypeRegistry.playS2C().register(BorderPayload.ID, CustomPayload.codecOf(BorderPayload::write, BorderPayload::new));
            }
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (final World world : chunkyBorder.getChunky().getServer().getWorlds()) {
                final Shape shape = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
                sendBorderPacket(List.of(handler.player), world, shape);
            }
        });
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (!registered) {
                chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, e -> sendBorderPacket(server.getPlayerManager().getPlayerList(), e.world(), e.shape()));
                registered = true;
            }
            if (channels.contains(BorderPayload.ID.id())) {
                chunkyBorder.getPlayerData(handler.player.getUuid()).setUsingMod(true);
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

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return true;
            }

            final BlockBreakEvent breakEvent = new BlockBreakEvent(new FabricPlayer(serverPlayer), new Location(new FabricWorld((ServerWorld) world), pos.getX(), pos.getY(), pos.getZ()));
            chunky.getEventBus().call(breakEvent);
            return !breakEvent.isCancelled(); // Return false when cancelled to indicate we want to cancel this block break
        });

        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
        startVisualizer();
    }

    private void startVisualizer() {
        final boolean visualizerEnabled = chunkyBorder.getConfig().visualizerEnabled();
        if (!visualizerEnabled) {
            return;
        }
        final int maxRange = chunkyBorder.getConfig().visualizerRange();
        Particles.setMaxDistance(maxRange);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            final long tick = server.getTicks();
            server.getPlayerManager().getPlayerList().forEach(fabricPlayer -> {
                final ServerWorld serverWorld = fabricPlayer.getServerWorld();
                final World world = new FabricWorld(serverWorld);
                final Player player = new FabricPlayer(fabricPlayer);
                final Shape border = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
                final boolean isUsingMod = chunkyBorder.getPlayerData(player.getUUID()).isUsingMod();
                if (border != null && !isUsingMod) {
                    final List<Vector3> particleLocations = Particles.at(player, border, (tick % 20) / 20d);
                    for (final Vector3 location : particleLocations) {
                        final BlockPos pos = BlockPos.ofFloored(location.getX(), location.getY(), location.getZ());
                        final boolean fullyOccluded = serverWorld.getBlockState(pos).isOpaqueFullCube()
                                && serverWorld.getBlockState(pos.north()).isOpaqueFullCube()
                                && serverWorld.getBlockState(pos.east()).isOpaqueFullCube()
                                && serverWorld.getBlockState(pos.south()).isOpaqueFullCube()
                                && serverWorld.getBlockState(pos.west()).isOpaqueFullCube();
                        if (!fullyOccluded) {
                            serverWorld.spawnParticles(fabricPlayer, new DustParticleEffect(BorderColor.getColor(), 1f), false, false, location.getX(), location.getY(), location.getZ(), 1, 0d, 0d, 0d, 0d);
                        }
                    }
                }
            });
        });
    }

    private void sendBorderPacket(final Collection<ServerPlayerEntity> players, final World world, final Shape shape) {
        for (final ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(new BorderPayload(world, shape)));
        }
    }
}
