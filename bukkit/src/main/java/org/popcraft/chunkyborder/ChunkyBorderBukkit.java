package org.popcraft.chunkyborder;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.BukkitPlayer;
import org.popcraft.chunky.platform.BukkitWorld;
import org.popcraft.chunky.platform.Folia;
import org.popcraft.chunky.platform.Paper;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.event.server.BlockBreakEvent;
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
import org.popcraft.chunkyborder.util.ClientBorder;
import org.popcraft.chunkyborder.util.Elevation;
import org.popcraft.chunkyborder.util.Particles;
import org.popcraft.chunkyborder.util.PluginMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.popcraft.chunky.util.Translator.translate;

public final class ChunkyBorderBukkit extends JavaPlugin implements Listener {
    private static final String PLAY_BORDER_PACKET_ID = "chunky:border";
    private static final List<String> HEADER = Arrays.asList("ChunkyBorder Configuration", "https://github.com/pop4959/ChunkyBorder/wiki/Configuration");
    private ChunkyBorder chunkyBorder;

    @Override
    public void onEnable() {
        final FileConfigurationOptions options = getConfig().options();
        options.copyDefaults(true);
        options.setHeader(HEADER);
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
        final Runnable borderCheckTask = new BorderCheckTask(chunkyBorder, this::findElevation);
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

    private CompletableFuture<OptionalInt> findElevation(final World world, final int x, final int y, final int z) {
        final org.bukkit.World bukkitWorld = getServer().getWorld(world.getUUID());
        if (bukkitWorld == null) {
            return CompletableFuture.completedFuture(OptionalInt.empty());
        }
        return CompletableFuture.supplyAsync(() -> Elevation.search(y, bukkitWorld.getMinHeight() + 1, bukkitWorld.getMaxHeight() - 2, searchY -> isSafe(bukkitWorld, x, searchY, z)), task -> {
            if (Folia.isFolia()) {
                Folia.schedule(this, bukkitWorld, x >> 4, z >> 4, task);
            } else if (getServer().isPrimaryThread()) {
                task.run();
            } else {
                getServer().getScheduler().runTask(this, task);
            }
        });
    }

    private boolean isSafe(final org.bukkit.World world, final int x, final int y, final int z) {
        final Block ground = world.getBlockAt(x, y - 1, z);
        final Block feet = ground.getRelative(BlockFace.UP);
        final Block head = feet.getRelative(BlockFace.UP);
        final Material groundType = ground.getType();
        return (groundType.isSolid() || groundType == Material.WATER) && isClear(feet) && isClear(head);
    }

    private boolean isClear(final Block block) {
        final Material type = block.getType();
        return block.isPassable() && type != Material.LAVA && !Tag.FIRE.isTagged(type);
    }

    private void startMetrics() {
        final Metrics metrics = new Metrics(this, 8953);
        metrics.addCustomChart(new DrilldownPie("platform", () -> Map.of("Bukkit", Map.of(getDescription().getVersion(), 1))));
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
        metrics.addCustomChart(new AdvancedPie("borderDimension", () -> {
            final Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> chunkyBorder.getChunky().getServer().getWorld(border.getWorld()).ifPresent(world -> {
                    final String dimension = ((BukkitWorld) world).getDimension();
                    map.put(dimension, map.getOrDefault(dimension, 0) + 1);
                }));
            }
            return map;
        }));
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
        metrics.addCustomChart(new SimplePie("visualizer", () -> chunkyBorder.getConfig().visualizerEnabled() ? "enabled" : "disabled"));
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
                    || (chunkyBorder.getConfig().preventChorusFruit() && org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT.equals(e.getCause()))
                    || (chunkyBorder.getConfig().preventEndGateway() && org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.END_GATEWAY.equals(e.getCause()))) {
                e.setCancelled(true);
            } else {
                redirect.map(r -> new org.bukkit.Location(e.getTo().getWorld(), r.getX(), r.getY(), r.getZ(), r.getYaw(), r.getPitch()))
                        .ifPresent(e::setTo);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(final PlayerRespawnEvent e) {
        final org.bukkit.Location bukkitLocation = e.getRespawnLocation();
        final Player player = new BukkitPlayer(e.getPlayer());
        final World world = new BukkitWorld(bukkitLocation.getWorld());
        final Location location = new Location(world, bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ(), bukkitLocation.getYaw(), bukkitLocation.getPitch());
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(player, location);
        chunkyBorder.getChunky().getEventBus().call(playerTeleportEvent);
        playerTeleportEvent.redirect()
                .map(r -> new org.bukkit.Location(bukkitLocation.getWorld(), r.getX(), r.getY(), r.getZ(), r.getYaw(), r.getPitch()))
                .ifPresent(e::setRespawnLocation);
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent e) {
        if (!chunkyBorder.getConfig().preventTrident() || !(e.getEntity() instanceof final Trident trident) || !(trident.getShooter() instanceof final org.bukkit.entity.Player shooter)) {
            return;
        }
        final Player player = new BukkitPlayer(shooter);
        if (chunkyBorder.getPlayerData(player.getUUID()).isBypassing() || player.hasPermission("chunkyborder.bypass.move")) {
            return;
        }
        final AtomicReference<org.bukkit.Location> lastInside = new AtomicReference<>(trident.getLocation());
        if (!isInsideBorder(lastInside.get())) {
            return;
        }
        final BooleanSupplier stopped = () -> !trident.isValid() || trident.isInBlock();
        final Runnable tridentCheck = () -> {
            final org.bukkit.Location location = trident.getLocation();
            if (!isInsideBorder(location)) {
                trident.setVelocity(new Vector());
                if (Folia.isFolia()) {
                    Paper.teleportAsync(trident, lastInside.get());
                } else {
                    trident.teleport(lastInside.get());
                }
                return;
            }
            lastInside.set(location);
            if (!isInsideBorder(location.clone().add(trident.getVelocity()))) {
                trident.setVelocity(new Vector());
            }
        };
        if (Folia.isFolia()) {
            Folia.scheduleFixed(this, trident, () -> {
                if (!stopped.getAsBoolean()) {
                    tridentCheck.run();
                }
            }, 1L, 1L);
        } else {
            getServer().getScheduler().runTaskTimer(this, task -> {
                if (stopped.getAsBoolean()) {
                    task.cancel();
                } else {
                    tridentCheck.run();
                }
            }, 1L, 1L);
        }
    }

    private boolean isInsideBorder(final org.bukkit.Location location) {
        return chunkyBorder.getBorder(location.getWorld().getName())
                .map(BorderData::getBorder)
                .map(border -> border.isBounding(location.getX(), location.getZ()))
                .orElse(true);
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(final org.bukkit.event.block.BlockBreakEvent e) {
        final org.bukkit.Location bukkitLocation = e.getBlock().getLocation();
        final World world = new BukkitWorld(e.getPlayer().getWorld());
        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(new BukkitPlayer(e.getPlayer()), new Location(world, bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ()));
        chunkyBorder.getChunky().getEventBus().call(blockBreakEvent);
        if (blockBreakEvent.isCancelled()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final org.bukkit.event.player.PlayerQuitEvent e) {
        chunkyBorder.getChunky().getEventBus().call(new PlayerQuitEvent(new BukkitPlayer(e.getPlayer())));
    }

    @EventHandler
    public void onPlayerRegisterChannel(final PlayerRegisterChannelEvent e) {
        if (PLAY_BORDER_PACKET_ID.equals(e.getChannel())) {
            sendBorders(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        if (e.getPlayer().getListeningPluginChannels().contains(PLAY_BORDER_PACKET_ID)) {
            sendBorders(e.getPlayer());
        }
    }

    private void sendBorders(final org.bukkit.entity.Player player) {
        final List<org.bukkit.entity.Player> players = List.of(player);
        for (final World world : chunkyBorder.getChunky().getServer().getWorlds()) {
            final Shape shape = chunkyBorder.getBorder(world.getName()).map(BorderData::getBorder).orElse(null);
            sendBorderPacket(players, world, shape);
        }
        chunkyBorder.getPlayerData(player.getUniqueId()).setUsingMod(true);
    }

    private void sendBorderPacket(final Collection<? extends org.bukkit.entity.Player> players, final World world, final Shape shape) {
        final byte[] data = PluginMessage.writeBorder(new ClientBorder(world.getKey(), shape));
        for (final org.bukkit.entity.Player player : players) {
            player.sendPluginMessage(this, PLAY_BORDER_PACKET_ID, data);
        }
    }
}
