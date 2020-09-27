package org.popcraft.chunkyborder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.dynmap.DynmapAPI;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.MapIntegration;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.util.Version;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ChunkyBorder extends JavaPlugin implements Listener {
    private Chunky chunky;
    private Map<String, BorderData> borders;
    private Map<UUID, Location> lastKnownLocation;
    private List<MapIntegration> mapIntegrations;
    private String borderMessage;
    private boolean useActionBar, preventMobSpawns;
    private static int HIGHEST_BLOCK_Y_OFFSET;

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.getConfig().options().copyHeader(true);
        this.saveConfig();
        this.chunky = (Chunky) getServer().getPluginManager().getPlugin("Chunky");
        if (chunky == null) {
            getLogger().severe("Chunky is required to use this plugin!");
            this.setEnabled(false);
            return;
        }
        try {
            Class.forName("org.popcraft.chunky.util.Version");
            if (new Version(1, 1, 14).isHigherThan(new Version(chunky.getDescription().getVersion()))) {
                throw new Exception();
            }
        } catch (Throwable e) {
            getLogger().severe("Chunky needs to be updated in order to use ChunkyBorder!");
            this.setEnabled(false);
            return;
        }
        HIGHEST_BLOCK_Y_OFFSET = Version.getCurrentMinecraftVersion().isHigherThanOrEqualTo(Version.v1_15_0) ? 1 : 0;
        this.lastKnownLocation = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        this.mapIntegrations = new ArrayList<>();
        if (this.getConfig().getBoolean("map-options.enable.bluemap", true)) {
            try {
                Class.forName("de.bluecolored.bluemap.api.BlueMapAPIListener");
                BlueMapWorkaround.load(this, mapIntegrations);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (this.getConfig().getBoolean("map-options.enable.dynmap", true)) {
            Optional.ofNullable(getServer().getPluginManager().getPlugin("dynmap"))
                    .ifPresent(dynmap -> mapIntegrations.add(new DynmapIntegration((DynmapAPI) dynmap)));
        }
        final String label = this.getConfig().getString("map-options.label", "World Border");
        final String color = this.getConfig().getString("map-options.color", "FF0000");
        final boolean hideByDefault = this.getConfig().getBoolean("map-options.hide-by-default", false);
        final int priority = this.getConfig().getInt("map-options.priority", 0);
        final int weight = this.getConfig().getInt("map-options.weight", 3);
        this.mapIntegrations.forEach(mapIntegration -> mapIntegration.setOptions(label, color, hideByDefault, priority, weight));
        this.borders = new HashMap<>();
        loadBorders();
        borders.values().forEach(border -> {
            border.reinitializeBorder();
            mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(getServer().getWorld(border.getWorld()), border.getBorder()));
        });
        final long checkInterval = this.getConfig().getLong("border-options.check-interval", 20);
        this.borderMessage = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(this.getConfig().getString("border-options.message", "&cYou have reached the edge of this world.")));
        this.useActionBar = this.getConfig().getBoolean("border-options.use-action-bar", true);
        this.preventMobSpawns = this.getConfig().getBoolean("border-options.prevent-mob-spawns", true);
        final Effect effect = Effect.valueOf(Objects.requireNonNull(this.getConfig().getString("border-options.effect", "ender_signal")).toUpperCase());
        final Sound sound = Sound.valueOf(Objects.requireNonNull(this.getConfig().getString("border-options.sound", "entity_enderman_teleport")).toUpperCase());
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : this.getServer().getOnlinePlayers()) {
                World world = player.getWorld();
                if (!borders.containsKey(world.getName())) {
                    return;
                }
                BorderData borderData = borders.get(world.getName());
                if (borderData == null) {
                    return;
                }
                Shape border = borderData.getBorder();
                Location loc = player.getLocation();
                if (border.isBounding(loc.getX(), loc.getZ())) {
                    this.lastKnownLocation.put(player.getUniqueId(), loc);
                } else {
                    if (player.hasPermission("chunkyborder.bypass.move")) {
                        return;
                    }
                    Location newLoc = this.lastKnownLocation.getOrDefault(player.getUniqueId(), world.getSpawnLocation());
                    newLoc.setYaw(loc.getYaw());
                    newLoc.setPitch(loc.getPitch());
                    sendBorderMessage(player);
                    player.getWorld().playEffect(player.getLocation(), effect, 0);
                    player.getWorld().playSound(player.getLocation(), sound, 2f, 1f);
                    Entity vehicle = player.getVehicle();
                    PaperLib.teleportAsync(player, newLoc);
                    if (vehicle != null) {
                        PaperLib.teleportAsync(vehicle, newLoc);
                    }
                }
            }
        }, checkInterval, checkInterval);
        Metrics metrics = new Metrics(this, 8953);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        if (mapIntegrations != null) {
            mapIntegrations.forEach(MapIntegration::removeAllShapeMarkers);
            mapIntegrations.clear();
        }
        saveBorders();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && "add".equalsIgnoreCase(args[0])) {
            Selection selection = chunky.getSelection();
            BorderData borderData = new BorderData(selection);
            borders.put(selection.world.getName(), borderData);
            mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(selection.world, borderData.getBorder()));
            sender.sendMessage(String.format("[Chunky] Added %s world border to %s with center %d, %d, and radius %s.",
                    selection.shape,
                    selection.world.getName(),
                    selection.centerX,
                    selection.centerZ,
                    selection.radiusX == selection.radiusZ ? String.valueOf(selection.radiusX) : String.format("%d, %d", selection.radiusX, selection.radiusZ)
            ));
            saveBorders();
            return true;
        } else if (args.length == 1 && "remove".equalsIgnoreCase(args[0])) {
            final World world = chunky.getSelection().world;
            borders.remove(world.getName());
            mapIntegrations.forEach(mapIntegration -> mapIntegration.removeShapeMarker(world));
            sender.sendMessage(String.format("[Chunky] Removed world border from %s.", world.getName()));
            saveBorders();
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        Location toLocation = e.getTo();
        if (toLocation == null) {
            return;
        }
        World toWorld = toLocation.getWorld();
        if (toWorld == null || !borders.containsKey(toWorld.getName())) {
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
            insideBorder.setY(toWorld.getHighestBlockYAt(insideBorder) + HIGHEST_BLOCK_Y_OFFSET);
            sendBorderMessage(player);
            lastKnownLocation.put(player.getUniqueId(), insideBorder);
            e.setTo(insideBorder);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!preventMobSpawns) {
            return;
        }
        Location location = e.getLocation();
        World world = location.getWorld();
        if (world == null) {
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
        if (world == null) {
            return;
        }
        BorderData borderData = borders.get(world.getName());
        if (borderData == null) {
            return;
        }
        Shape border = borderData.getBorder();
        if (border != null && !border.isBounding(location.getX(), location.getZ()) && !e.getPlayer().hasPermission("chunkyborder.bypass.place")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.lastKnownLocation.remove(e.getPlayer().getUniqueId());
    }

    private void sendBorderMessage(Player player) {
        if (useActionBar) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(borderMessage));
        } else {
            player.sendMessage(borderMessage);
        }
    }

    private void loadBorders() {
        try (FileReader fileReader = new FileReader(new File(this.getDataFolder(), "borders.json"))) {
            this.borders = new Gson().fromJson(fileReader, new TypeToken<Map<String, BorderData>>() {
            }.getType());
        } catch (IOException e) {
            this.getLogger().warning("No saved borders found");
        }
    }

    private void saveBorders() {
        try (FileWriter fileWriter = new FileWriter(new File(this.getDataFolder(), "borders.json"))) {
            fileWriter.write(new Gson().toJson(borders));
        } catch (IOException e) {
            this.getLogger().warning("Unable to save borders");
        }
    }
}
