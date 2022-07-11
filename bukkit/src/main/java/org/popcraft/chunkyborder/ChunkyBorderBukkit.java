package org.popcraft.chunkyborder;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.BukkitPlayer;
import org.popcraft.chunky.platform.BukkitWorld;
import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Translator;
import org.popcraft.chunkyborder.command.BorderCommand;
import org.popcraft.chunkyborder.event.BlockPlaceEvent;
import org.popcraft.chunkyborder.event.CreatureSpawnEvent;
import org.popcraft.chunkyborder.event.PlayerQuitEvent;
import org.popcraft.chunkyborder.event.PlayerTeleportEvent;
import org.popcraft.chunkyborder.event.WorldLoadEvent;
import org.popcraft.chunkyborder.event.WorldUnloadEvent;
import org.popcraft.chunkyborder.integration.BlueMapIntegration;
import org.popcraft.chunkyborder.integration.DynmapIntegration;
import org.popcraft.chunkyborder.integration.SquaremapIntegration;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.platform.MapIntegrationLoader;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.popcraft.chunky.util.Translator.translate;

public final class ChunkyBorderBukkit extends JavaPlugin implements Listener {
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
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncDelayedTask(this, new BorderInitializationTask(chunkyBorder));
        final long checkInterval = chunkyBorder.getConfig().checkInterval();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BorderCheckTask(chunkyBorder), checkInterval, checkInterval);
        chunkyBorder.getChunky().getCommands().put("border", new BorderCommand(chunkyBorder));
        startMetrics();
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
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        getServer().getScheduler().cancelTasks(this);
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
        final boolean usingEnderpearl = org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL.equals(e.getCause());
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(player, location, usingEnderpearl);
        chunkyBorder.getChunky().getEventBus().call(playerTeleportEvent);
        playerTeleportEvent.redirect()
                .map(redirect -> new org.bukkit.Location(e.getTo().getWorld(), redirect.getX(), redirect.getY(), redirect.getZ(), redirect.getYaw(), redirect.getPitch()))
                .ifPresent(e::setTo);
        if (playerTeleportEvent.isCancelled()) {
            e.setCancelled(true);
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
}
