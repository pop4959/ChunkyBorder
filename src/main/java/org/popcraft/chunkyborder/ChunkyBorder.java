package org.popcraft.chunkyborder;

import com.google.gson.Gson;
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
import org.popcraft.chunky.shape.AbstractShape;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.util.Version;

import java.io.File;
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
    private Map<World, AbstractShape> borders;
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
            if (new Version(1, 1, 13).isHigherThan(new Version(chunky.getDescription().getVersion()))) {
                throw new Exception();
            }
        } catch (Throwable e) {
            getLogger().severe("Chunky needs to be updated in order to use ChunkyBorder!");
            this.setEnabled(false);
            return;
        }
        HIGHEST_BLOCK_Y_OFFSET = Version.getCurrentMinecraftVersion().isHigherThanOrEqualTo(new Version(1, 15, 0)) ? 1 : 0;
        this.borders = new HashMap<>();
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
        final long checkInterval = this.getConfig().getLong("border-options.check-interval", 20);
        this.borderMessage = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(this.getConfig().getString("border-options.message", "&cYou have reached the edge of this world.")));
        this.useActionBar = this.getConfig().getBoolean("border-options.use-action-bar", true);
        this.preventMobSpawns = this.getConfig().getBoolean("border-options.prevent-mob-spawns", true);
        final Effect effect = Effect.valueOf(Objects.requireNonNull(this.getConfig().getString("border-options.effect", "ender_signal")).toUpperCase());
        final Sound sound = Sound.valueOf(Objects.requireNonNull(this.getConfig().getString("border-options.sound", "entity_enderman_teleport")).toUpperCase());
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : this.getServer().getOnlinePlayers()) {
                World world = player.getWorld();
                if (!borders.containsKey(world)) {
                    return;
                }
                Shape border = borders.get(world);
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
        // TODO: improve this
        try (FileWriter fileWriter = new FileWriter(new File(this.getDataFolder(), "borders.json"))) {
            fileWriter.write(new Gson().toJson(borders));
        } catch (IOException e) {
            this.getLogger().warning("Unable to save borders");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Selection selection = chunky.getSelection();
        final Shape shape = ShapeFactory.getShape(selection);
        if (shape instanceof AbstractShape) {
            borders.put(selection.world, (AbstractShape) shape);
            mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(selection.world, shape));
        }
        // TL
        return true;
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
        if (toWorld == null || !borders.containsKey(toWorld)) {
            return;
        }
        AbstractShape border = borders.get(toWorld);
        if (border == null) {
            return;
        }
        Vector to = toLocation.toVector();
        if (!border.isBounding(to.getX(), to.getZ())) {
            if (player.hasPermission("chunkyborder.bypass.move")) {
                return;
            }
            double[] center = border.getCenter();
            double centerX = center[0];
            double centerZ = center[1];
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
                intersections.add(ShapeUtil.pointOnEllipse(center[0], center[1], radii[0], radii[1], angle));
                double intersectionX = center[0] + radii[0] * Math.cos(angle);
                double intersectionZ = center[1] + radii[1] * Math.sin(angle);
                intersections.add(new double[]{intersectionX, intersectionZ});
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
        Shape border = borders.get(location.getWorld());
        if (border != null && !border.isBounding(location.getX(), location.getZ())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Location location = e.getBlockPlaced().getLocation();
        Shape border = borders.get(location.getWorld());
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
}
