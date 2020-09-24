package org.popcraft.chunkyborder;

import com.google.gson.Gson;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.integration.BlueMapIntegration;
import org.popcraft.chunky.integration.DynmapIntegration;
import org.popcraft.chunky.integration.MapIntegration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ChunkyBorder extends JavaPlugin implements Listener {
    private Chunky chunky;
    private Map<World, Shape> borders;
    private Selection selection;
    private Map<UUID, Location> lastKnownLocation;
    private List<MapIntegration> mapIntegrations;

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.getConfig().options().copyHeader(true);
        this.saveConfig();
        this.chunky = (Chunky) getServer().getPluginManager().getPlugin("Chunky");
        if (chunky == null) {
            // TL
            this.setEnabled(false);
            return;
        } else if (!getDescription().getVersion().equals(chunky.getDescription().getVersion())) {
            // TL
            this.setEnabled(false);
            return;
        }
        this.borders = new HashMap<>();
        this.lastKnownLocation = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        // Load integrations
        this.mapIntegrations = new ArrayList<>();
        Optional.ofNullable(getServer().getPluginManager().getPlugin("dynmap"))
                .ifPresent(dynmap -> mapIntegrations.add(new DynmapIntegration((DynmapAPI) dynmap)));
        Optional.ofNullable(getServer().getPluginManager().getPlugin("BlueMap"))
                .ifPresent(blueMap -> {
                    BlueMapIntegration blueMapIntegration = new BlueMapIntegration();
                    BlueMapAPI.registerListener(blueMapIntegration);
                    mapIntegrations.add(blueMapIntegration);
                });
        // Load from config
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
                    if (player.hasPermission("chunkyborder.bypass.movement")) {
                        return;
                    }
                    Location newLoc = this.lastKnownLocation.getOrDefault(player.getUniqueId(), world.getSpawnLocation());
                    newLoc.setYaw(loc.getYaw());
                    newLoc.setPitch(loc.getPitch());
                    // TODO: remove debug
                    this.getServer().getConsoleSender().sendMessage("Outside of border!");
                    // TODO: TL actionbar
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + "You have reached the edge of this world."));
                    Entity vehicle = player.getVehicle();
                    player.teleport(newLoc);
                    if (vehicle != null) {
                        vehicle.teleport(newLoc);
                    }
                }
            }
        }, 0L, 20L);
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
        // Save to config
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.selection = chunky.getSelection();
        final Shape shape = ShapeFactory.getShape(selection);
        borders.put(selection.world, shape);
        mapIntegrations.forEach(mapIntegration -> mapIntegration.addShapeMarker(selection.world, shape));
        // TL
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.lastKnownLocation.remove(e.getPlayer().getUniqueId());
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
        Shape border = borders.get(toWorld);
        if (border == null) {
            return;
        }
        Vector to = toLocation.toVector();
        if (!border.isBounding(to.getX(), to.getZ())) {
            if (player.hasPermission("chunkyborder.bypass.movement")) {
                return;
            }
            if (PlayerTeleportEvent.TeleportCause.ENDER_PEARL.equals(e.getCause())) {
                e.setCancelled(true);
                return;
            }
            // TL
            // TODO: don't use selection, use saved border
            double centerX = selection.x;
            double centerZ = selection.z;
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
                double[] center = ellipticalBorder.getCenter();
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
            // TODO: getHighestBlockYAt version offset
            insideBorder.setY(toWorld.getHighestBlockYAt(insideBorder));
            // TODO: remove debug
            this.getServer().getConsoleSender().sendMessage("Teleporting into border!");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED + "You have reached the edge of this world."));
            lastKnownLocation.put(player.getUniqueId(), insideBorder);
            e.setTo(insideBorder);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
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
}
