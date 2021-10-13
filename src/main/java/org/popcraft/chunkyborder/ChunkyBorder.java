package org.popcraft.chunkyborder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.Validate;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyBukkit;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.MapIntegration;
import org.popcraft.chunky.integration.Pl3xMapIntegration;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.util.TranslationKey;
import org.popcraft.chunky.util.Version;
import org.popcraft.chunkyborder.command.BorderCommand;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.popcraft.chunky.util.Translator.translate;

public final class ChunkyBorder extends JavaPlugin implements Listener {
    private Map<String, BorderData> borders;
    private Map<UUID, PlayerData> players;
    private List<MapIntegration> mapIntegrations;

    @Override
    public void onEnable() {
        this.borders = loadBorders();
        this.players = new HashMap<>();
        this.mapIntegrations = new ArrayList<>();
        if (!isCompatibleChunkyVersion()) {
            getLogger().severe(() -> translate(TranslationKey.BORDER_DEPENDENCY_UPDATE));
            this.setEnabled(false);
            return;
        }
        getConfig().options().copyDefaults(true);
        getConfig().options().copyHeader(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncDelayedTask(this, new BorderInitializationTask(this));
        final long checkInterval = getConfig().getLong("border-options.check-interval", 20);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BorderCheckTask(this), checkInterval, checkInterval);
        getChunky().getCommands().put("border", new BorderCommand(this));
        Metrics metrics = new Metrics(this, 8953);
        metrics.addCustomChart(new AdvancedPie("mapIntegration", () -> {
            Map<String, Integer> map = new HashMap<>();
            mapIntegrations.forEach(mapIntegration -> {
                if (mapIntegration instanceof BlueMapIntegration) {
                    map.put("BlueMap", 1);
                } else if (mapIntegration instanceof DynmapIntegration) {
                    map.put("Dynmap", 1);
                } else if (mapIntegration instanceof Pl3xMapIntegration) {
                    map.put("Pl3xMap", 1);
                }
            });
            if (map.isEmpty()) {
                map.put("None", 1);
            }
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("borderSize", () -> {
            Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> {
                    String size = String.valueOf((int) Math.max(border.getRadiusX(), border.getRadiusZ()));
                    map.put(size, map.getOrDefault(size, 0) + 1);
                });
            }
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("borderShape", () -> {
            Map<String, Integer> map = new HashMap<>();
            if (borders != null) {
                borders.values().forEach(border -> {
                    String shape = border.getShape().toLowerCase();
                    map.put(shape, map.getOrDefault(shape, 0) + 1);
                });
            }
            return map;
        }));
    }

    @Override
    public void onDisable() {
        saveBorders();
        HandlerList.unregisterAll((Plugin) this);
        getServer().getScheduler().cancelTasks(this);
        mapIntegrations.forEach(MapIntegration::removeAllShapeMarkers);
        mapIntegrations.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent e) {
        String worldName = e.getWorld().getName();
        Optional<org.popcraft.chunky.platform.World> world = getChunky().getServer().getWorld(worldName);
        if (!world.isPresent()) {
            return;
        }
        BorderData borderData = borders.get(worldName);
        if (borderData == null) {
            return;
        }
        Shape border = borderData.getBorder();
        mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(world.get(), border));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent e) {
        String worldName = e.getWorld().getName();
        Optional<org.popcraft.chunky.platform.World> world = getChunky().getServer().getWorld(worldName);
        if (!world.isPresent()) {
            return;
        }
        mapIntegrations.forEach(mapIntegration -> mapIntegration.removeShapeMarker(world.get()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        Location toLocation = e.getTo();
        World toWorld = toLocation.getWorld();
        if (toWorld == null || borders == null || !borders.containsKey(toWorld.getName())) {
            return;
        }
        BorderData borderData = borders.get(toWorld.getName());
        if (borderData == null) {
            return;
        }
        Shape border = borderData.getBorder();
        if (border == null) {
            return;
        }
        Vector to = toLocation.toVector();
        if (!border.isBounding(to.getX(), to.getZ())) {
            if (player.hasPermission("chunkyborder.bypass.move")) {
                return;
            }
            if (PlayerTeleportEvent.TeleportCause.ENDER_PEARL.equals(e.getCause()) && getConfig().getBoolean("border-options.prevent-enderpearl", false)) {
                e.setCancelled(true);
                return;
            }
            double centerX = borderData.getCenterX();
            double centerZ = borderData.getCenterZ();
            double toX = to.getX();
            double toY = to.getY();
            double toZ = to.getZ();
            final List<double[]> intersections = new ArrayList<>();
            if (border instanceof AbstractPolygon) {
                AbstractPolygon polygonBorder = (AbstractPolygon) border;
                double[] pointsX = polygonBorder.pointsX();
                double[] pointsZ = polygonBorder.pointsZ();
                for (int i = 0; i < pointsX.length; ++i) {
                    ShapeUtil.intersection(centerX, centerZ, toX, toZ, pointsX[i], pointsZ[i], pointsX[i == pointsX.length - 1 ? 0 : i + 1], pointsZ[i == pointsZ.length - 1 ? 0 : i + 1]).ifPresent(intersections::add);
                }
            } else if (border instanceof AbstractEllipse) {
                AbstractEllipse ellipticalBorder = (AbstractEllipse) border;
                double[] radii = ellipticalBorder.getRadii();
                double angle = Math.atan2(toZ - centerX, toX - centerZ);
                intersections.add(ShapeUtil.pointOnEllipse(centerX, centerZ, radii[0], radii[1], angle));
            }
            if (intersections.isEmpty()) {
                e.setTo(toWorld.getSpawnLocation());
                return;
            }
            Vector centerDirection = new Vector(centerX - toX, 0, centerZ - toZ).normalize().multiply(3);
            double closestX = intersections.get(0)[0];
            double closestZ = intersections.get(0)[1];
            double shortestDistance = Double.MAX_VALUE;
            for (double[] intersection : intersections) {
                double intersectionX = intersection[0];
                double intersectionZ = intersection[1];
                Vector position = new Vector(intersectionX, toY, intersectionZ).add(centerDirection);
                double distance = to.distanceSquared(position);
                if (distance < shortestDistance && border.isBounding(position.getX(), position.getZ())) {
                    shortestDistance = distance;
                    closestX = intersectionX;
                    closestZ = intersectionZ;
                }
            }
            if (shortestDistance == Double.MAX_VALUE) {
                e.setTo(toWorld.getSpawnLocation());
                return;
            }
            Location insideBorder = new Location(toWorld, closestX, toY, closestZ);
            insideBorder.add(centerDirection);
            insideBorder.setDirection(centerDirection);
            final int yOffset = Version.getCurrentMinecraftVersion().isHigherThanOrEqualTo(Version.v1_15_0) ? 1 : 0;
            insideBorder.setY(toWorld.getHighestBlockYAt(insideBorder) + yOffset);
            sendBorderMessage(player);
            getPlayerData(player).setLastLocation(insideBorder);
            e.setTo(insideBorder);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!getConfig().getBoolean("border-options.prevent-mob-spawns", true)) {
            return;
        }
        Location location = e.getLocation();
        World world = location.getWorld();
        if (world == null || borders == null) {
            return;
        }
        BorderData borderData = borders.get(world.getName());
        if (borderData == null) {
            return;
        }
        Shape border = borderData.getBorder();
        if (border != null && !border.isBounding(location.getX(), location.getZ())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Location location = e.getBlockPlaced().getLocation();
        World world = location.getWorld();
        if (world == null || borders == null) {
            return;
        }
        BorderData borderData = borders.get(world.getName());
        if (borderData == null) {
            return;
        }
        Shape border = borderData.getBorder();
        if (border == null) {
            return;
        }
        double blockX = location.getBlockX() + 0.5;
        double blockZ = location.getBlockZ() + 0.5;
        if (!border.isBounding(blockX, blockZ) && !e.getPlayer().hasPermission("chunkyborder.bypass.place")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.players.remove(e.getPlayer().getUniqueId());
    }

    public void sendBorderMessage(Player player) {
        Optional<String> message = getBorderMessage();
        if (!message.isPresent()) {
            return;
        }
        if (getConfig().getBoolean("border-options.use-action-bar", true)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message.get()));
        } else {
            player.sendMessage(message.get());
        }
    }

    public Map<String, BorderData> loadBorders() {
        try (FileReader fileReader = new FileReader(new File(this.getDataFolder(), "borders.json"))) {
            Map<String, BorderData> loadedBorders = new Gson().fromJson(fileReader, new TypeToken<Map<String, BorderData>>() {
            }.getType());
            if (loadedBorders != null) {
                return loadedBorders;
            }
        } catch (IOException e) {
            getLogger().warning(() -> translate(TranslationKey.BORDER_LOAD_FAILED));
        }
        return new HashMap<>();
    }

    public void saveBorders() {
        if (borders == null) {
            return;
        }
        try (FileWriter fileWriter = new FileWriter(new File(this.getDataFolder(), "borders.json"))) {
            fileWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(borders));
        } catch (IOException e) {
            getLogger().warning(() -> translate(TranslationKey.BORDER_SAVE_FAILED));
        }
    }

    public Map<String, BorderData> getBorders() {
        return borders;
    }

    public PlayerData getPlayerData(final Player player) {
        final UUID uuid = player.getUniqueId();
        this.players.computeIfAbsent(uuid, x -> new PlayerData(player));
        return players.get(uuid);
    }

    public List<MapIntegration> getMapIntegrations() {
        return mapIntegrations;
    }

    public Chunky getChunky() {
        ChunkyBukkit chunkyBukkit = ((ChunkyBukkit) getServer().getPluginManager().getPlugin("Chunky"));
        Validate.notNull(chunkyBukkit);
        Chunky chunky = chunkyBukkit.getChunky();
        Validate.notNull(chunky);
        return chunky;
    }

    public boolean isCompatibleChunkyVersion() {
        try {
            Class.forName("org.popcraft.chunky.util.Version");
            Version minimumRequiredVersion = new Version(1, 2, 128);
            Plugin chunkyPlugin = getServer().getPluginManager().getPlugin("Chunky");
            if (chunkyPlugin == null) {
                return false;
            }
            Version currentVersion = new Version(chunkyPlugin.getDescription().getVersion());
            return currentVersion.isHigherThanOrEqualTo(minimumRequiredVersion);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public Optional<String> getBorderMessage() {
        String message = getConfig().getString("border-options.message", "&cYou have reached the edge of this world.");
        if (message == null) {
            return Optional.empty();
        }
        return Optional.of(getColoredMessage(message));
    }

    private static final Pattern RGB_PATTERN = Pattern.compile("&#[0-9a-fA-F]{6}");
    private static final boolean RGB_COLORS_SUPPORTED;
    static {
        boolean rgbSupported;
        try {
            ChatColor.class.getMethod("of", String.class);
            rgbSupported = true;
        } catch (NoSuchMethodException e) {
            rgbSupported = false;
        }
        RGB_COLORS_SUPPORTED = rgbSupported;
    }

    private String getColoredMessage(String message) {
        if (RGB_COLORS_SUPPORTED) {
            Matcher rgbMatcher = RGB_PATTERN.matcher(message);
            while (rgbMatcher.find()) {
                final ChatColor rgbColor = ChatColor.of(rgbMatcher.group().substring(1));
                final String messageStart = message.substring(0, rgbMatcher.start());
                final String messageEnd = message.substring(rgbMatcher.end());
                message = messageStart + rgbColor + messageEnd;
                rgbMatcher = RGB_PATTERN.matcher(message);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public Optional<Effect> getBorderEffect() {
        String effectName = getConfig().getString("border-options.effect", "ender_signal");
        if (effectName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Effect.valueOf(effectName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<Sound> getBorderSound() {
        String soundName = getConfig().getString("border-options.sound", "entity_enderman_teleport");
        if (soundName == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Sound.valueOf(soundName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
