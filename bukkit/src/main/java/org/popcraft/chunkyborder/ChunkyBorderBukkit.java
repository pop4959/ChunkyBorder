package org.popcraft.chunkyborder;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.BukkitPlayer;
import org.popcraft.chunky.platform.BukkitWorld;
import org.popcraft.chunky.platform.Folia;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.event.server.BlockPlaceEvent;
import org.popcraft.chunkyborder.event.server.CreatureSpawnEvent;
import org.popcraft.chunkyborder.event.server.PlayerQuitEvent;
import org.popcraft.chunkyborder.event.server.PlayerTeleportEvent;
import org.popcraft.chunkyborder.event.server.WorldLoadEvent;
import org.popcraft.chunkyborder.event.server.WorldUnloadEvent;
import org.popcraft.chunkyborder.integration.BlueMapIntegration;
import org.popcraft.chunkyborder.integration.DynmapIntegration;
import org.popcraft.chunkyborder.integration.Pl3xMapIntegration;
import org.popcraft.chunkyborder.integration.SquaremapIntegration;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.Particles;
import org.popcraft.chunkyborder.util.PluginMessage;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.popcraft.chunky.util.Translator.translate;

public final class ChunkyBorderBukkit extends JavaPlugin implements Listener {
    private static final String PLAY_BORDER_PACKET_ID = "chunky:border";
    private static final List<String> HEADER = Arrays.asList("ChunkyBorder Configuration", "https://github.com/pop4959/ChunkyBorder/wiki/Configuration");
    private ChunkyBorder chunkyBorder;

    @Override
    public void onEnable() {
        final FileConfigurationOptions options = getConfig().options();
        options.copyDefaults(true);
        try {
            FileConfigurationOptions.class.getMethod("header", String.class).invoke(options, String.join("\n", HEADER));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            options.setHeader(HEADER);
        }
        saveConfig();
        final Chunky chunky = ChunkyProvider.get();
        final Config config = new BukkitConfig(this);
        final MapIntegrationLoader mapIntegrationLoader = new BukkitMapIntegrationLoader(this);
        this.chunkyBorder = new ChunkyBorder(chunky, config, mapIntegrationLoader);
        getServer().getServicesManager().register(ChunkyBorder.class, chunkyBorder, this, ServicePriority.Normal);
        if (!chunkyBorder.hasCompatibleChunkyVersion()) {
            getLogger().severe(() -> translate(TranslationKey.BORDER_DEPENDENCY_UPDATE));
            this.setEnabled(false);
            return;
        }
        Translator.addCustomTranslation("custom_border_message", config.message());
        BorderColor.parseColor(config.visualizerColor());
        getServer().getPluginManager().registerEvents(this, this);
        final Runnable borderInitTask = new BorderInitializationTask(chunkyBorder);
        if (Folia.isFolia()) {
            Folia.onServerInit(this, borderInitTask);
        } else {
            getServer().getScheduler().scheduleSyncDelayedTask(this, borderInitTask);
        }
        final long checkInterval = chunkyBorder.getConfig().checkInterval();
        final Runnable borderCheckTask = new BorderCheckTask(chunkyBorder);
        if (Folia.isFolia()) {
            Folia.scheduleFixedGlobal(this, borderCheckTask, checkInterval, checkInterval);
        } else {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, borderCheckTask, checkInterval, checkInterval);
        }
        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
        getServer().getMessenger().registerOutgoingPluginChannel(this, PLAY_BORDER_PACKET_ID);
        chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, e -> sendBorderPacket(getServer().getOnlinePlayers(), e.world(), e.shape()));
        startMetrics();
        startVisualizer();
    }

    private void startMetrics() {
        final Metrics metrics = new Metrics(this, 8953);
        metrics.addCustomChart(new AdvancedPie("mapIntegration", () -> {
            final Map<String, Integer> map = new HashMap<>();
            chunkyBorder.getMapIntegrations().forEach(mapIntegration -> {
                if (mapIntegration instanceof BlueMapIntegration) {
                    map.put("BlueMap", 1);
                } else if (mapIntegration instanceof DynmapIntegration) {
                    map.put("Dynmap", 1);
                } else if (mapIntegration instanceof Pl3xMapIntegration) {
                    map.put("Pl3xMap", 1);
                } else if (mapIntegration instanceof SquaremapIntegration) {
                    map.put("squaremap", 1);
                }
            });
            if (map.isEmpty()) {
                map.put("None", 1);
            }
            return map;
        }));
        final Map<String, BorderData> borders = chunkyBorder.getBorders();
        metrics.addCustomChart(new AdvancedPie("borderSize", () -> {
            final Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> {
                    final String size = String.valueOf((int) Math.max(border.getRadiusX(), border.getRadiusZ()));
                    map.put(size, map.getOrDefault(size, 0) + 1);
                });
            }
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("borderShape", () -> {
            final Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> {
                    final String shape = border.getShape().toLowerCase();
                    map.put(shape, map.getOrDefault(shape, 0) + 1);
                });
            }
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("borderWrap", () -> {
            final Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> {
                    final String wrap = border.getWrap().toLowerCase();
                    map.put(wrap, map.getOrDefault(wrap, 0) + 1);
                });
            }
            return map;
        }));
    }

    private void startVisualizer() {
        final boolean visualizerEnabled = chunkyBorder.getConfig().visualizerEnabled();
        if (!visualizerEnabled) {
            return;
        }
        final int maxRange = chunkyBorder.getConfig().visualizerRange();
        Particles.setMaxDistance(maxRange);
        final AtomicLong tick = new AtomicLong();
        final Runnable borderVisualizerTask = () -> {
            tick.incrementAndGet();
            getServer().getOnlinePlayers().forEach(bukkitPlayer -> {
                final org.bukkit.World bukkitWorld = bukkitPlayer.getWorld();
                final Player player = new BukkitPlayer(bukkitPlayer);
                final Shape border = chunkyBorder.getBorder(bukkitWorld.getName()).map(BorderData::getBorder).orElse(null);
                final boolean isUsingMod = chunkyBorder.getPlayerData(player.getUUID()).isUsingMod();
                if (border != null && !isUsingMod) {
                    final List<Vector3> particleLocations = Particles.at(player, border, (tick.longValue() % 20) / 20d);
                    final Color visualizerColor = Color.fromRGB(BorderColor.getColor());
                    final Particle.DustOptions visualizerOptions = new Particle.DustOptions(visualizerColor, 1);
                    for (final Vector3 location : particleLocations) {
                        final org.bukkit.Location bukkitLocation = new org.bukkit.Location(bukkitWorld, location.getX(), location.getY(), location.getZ());
                        if (Folia.isFolia()) {
                            bukkitPlayer.spawnParticle(Particle.DUST, bukkitLocation, 1, visualizerOptions);
                        } else {
                            final Block block = bukkitWorld.getBlockAt(bukkitLocation);
                            final boolean fullyOccluded = block.getType().isOccluding()
                                    && block.getRelative(BlockFace.NORTH).getType().isOccluding()
                                    && block.getRelative(BlockFace.EAST).getType().isOccluding()
                                    && block.getRelative(BlockFace.SOUTH).getType().isOccluding()
                                    && block.getRelative(BlockFace.WEST).getType().isOccluding();
                            if (!fullyOccluded) {
                                bukkitPlayer.spawnParticle(Particle.DUST, bukkitLocation, 1, visualizerOptions);
                            }
                        }
                    }
                }
            });
        };
        if (Folia.isFolia()) {
            Folia.scheduleFixedGlobal(this, borderVisualizerTask, 1L, 1L);
        } else {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, borderVisualizerTask, 1L, 1L);
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        if (Folia.isFolia()) {
            Folia.cancelTasks(this);
        } else {
            getServer().getScheduler().cancelTasks(this);
        }
        getServer().getServicesManager().unregisterAll(this);
        chunkyBorder.disable();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(final org.bukkit.event.world.WorldLoadEvent e) {
        chunkyBorder.getChunky().getEventBus().call(new WorldLoadEvent(new BukkitWorld(e.getWorld())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(final org.bukkit.event.world.WorldUnloadEvent e) {
        chunkyBorder.getChunky().getEventBus().call(new WorldUnloadEvent(new BukkitWorld(e.getWorld())));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(final org.bukkit.event.player.PlayerTeleportEvent e) {
        if (e.getTo() == null || e.getTo().getWorld() == null) {
            return;
        }
        final Player player = new BukkitPlayer(e.getPlayer());
        final World world = new BukkitWorld(e.getTo().getWorld());
        final Location location = new Location(world, e.getTo().getX(), e.getTo().getY(), e.getTo().getZ(), e.getTo().getYaw(), e.getTo().getPitch());
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(player, location);
        chunkyBorder.getChunky().getEventBus().call(playerTeleportEvent);
        final Optional<Location> redirect = playerTeleportEvent.redirect();
        if (redirect.isPresent()) {
            if ((chunkyBorder.getConfig().preventEnderpearl() && org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL.equals(e.getCause()))
                    || (chunkyBorder.getConfig().preventChorusFruit() && org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT.equals(e.getCause()))) {
                e.setCancelled(true);
            } else {
                redirect.map(r -> new org.bukkit.Location(e.getTo().getWorld(), r.getX(), r.getY(), r.getZ(), r.getYaw(), r.getPitch()))
                        .ifPresent(e::setTo);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(final org.bukkit.event.entity.CreatureSpawnEvent e) {
        if (!chunkyBorder.getConfig().preventMobSpawns()) {
            return;
        }
        final org.bukkit.World bukkitWorld = e.getEntity().getWorld();
        final org.bukkit.Location bukkitLocation = e.getLocation();
        final World world = new BukkitWorld(bukkitWorld);
        final Location location = new Location(world, bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
        final CreatureSpawnEvent creatureSpawnEvent = new CreatureSpawnEvent(location);
        chunkyBorder.getChunky().getEventBus().call(creatureSpawnEvent);
        if (creatureSpawnEvent.isCancelled()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(final org.bukkit.event.block.BlockPlaceEvent e) {
        final org.bukkit.World bukkitWorld = e.getPlayer().getWorld();
        final org.bukkit.Location bukkitLocation = e.getBlockPlaced().getLocation();
        final World world = new BukkitWorld(bukkitWorld);
        final Location location = new Location(world, bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ());
        final Player player = new BukkitPlayer(e.getPlayer());
        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(player, location);
        chunkyBorder.getChunky().getEventBus().call(blockPlaceEvent);
        if (blockPlaceEvent.isCancelled()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final org.bukkit.event.player.PlayerQuitEvent e) {
        chunkyBorder.getChunky().getEventBus().call(new PlayerQuitEvent(new BukkitPlayer(e.getPlayer())));
    }

    @EventHandler
    public void onPlayerRegisterChannel(final PlayerRegisterChannelEvent e) {
        final String channel = e.getChannel();
        if (!PLAY_BORDER_PACKET_ID.equals(channel)) {
            return;
        }
        final org.bukkit.entity.Player player = e.getPlayer();
        final List<org.bukkit.entity.Player> players = List.of(player);
        for (final World world : chunkyBorder.getChunky().getServer().getWorlds()) {
            final Shape shape = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
            sendBorderPacket(players, world, shape);
        }
        chunkyBorder.getPlayerData(player.getUniqueId()).setUsingMod(true);
    }

    private void sendBorderPacket(final Collection<? extends org.bukkit.entity.Player> players, final World world, final Shape shape) {
        final byte[] data = PluginMessage.writeBorder(world, shape);
        for (final org.bukkit.entity.Player player : players) {
            player.sendPluginMessage(this, PLAY_BORDER_PACKET_ID, data);
        }
    }
}
