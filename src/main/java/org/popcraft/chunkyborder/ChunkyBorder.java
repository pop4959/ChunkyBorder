package org.popcraft.chunkyborder;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.Selection;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.AbstractShape;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeFactory;
import org.popcraft.chunky.shape.ShapeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChunkyBorder extends JavaPlugin implements Listener {
    private Chunky chunky;
    private Map<World, Shape> borders;
    private Selection selection;

    @Override
    public void onEnable() {
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
        getServer().getPluginManager().registerEvents(this, this);
        // Load from config
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : this.getServer().getOnlinePlayers()) {
                World world = player.getWorld();
                if (!borders.containsKey(world)) {
                    return;
                }
                Shape border = borders.get(world);
                Location loc = player.getLocation();
                if (!border.isBounding(loc.getX(), loc.getZ())) {
                    // TODO: Handle zero vector (might not need to since using getDirection()?)
                    // TODO: Account for Y direction
                    // TODO: Safe teleportation
                    Vector forward = loc.getDirection().setY(0).multiply(3);
                    Vector backward = forward.clone().rotateAroundY(Math.PI);
                    Vector posForward = loc.toVector().add(forward);
                    Vector posBackward = loc.toVector().add(backward);
                    boolean boundedForward = border.isBounding(posForward.getX(), posForward.getZ());
                    Vector newPos = boundedForward ? posForward : posBackward;
                    Location newLoc = newPos.toLocation(world, loc.getYaw(), loc.getPitch());
                    // TODO: remove debug
                    this.getServer().getConsoleSender().sendMessage("Outside of border!");
                    player.teleport(newLoc);
                    // TL
                }
            }
        }, 0L, 1L);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        // Save to config
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.selection = chunky.getSelection();
        Shape shape = ShapeFactory.getShape(selection);
        if (!(shape instanceof AbstractPolygon)) {
            return true;
        }
        AbstractPolygon poly = (AbstractPolygon) shape;
        borders.put(selection.world, shape);
        // TL
        Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap == null) {
            return true;
        }
        DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
        MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
        MarkerSet oldMarkerSet = markerAPI.getMarkerSet("chunkyborder.markerset");
        if (oldMarkerSet != null) {
            oldMarkerSet.deleteMarkerSet();
        }
        MarkerSet markerSet = markerAPI.createMarkerSet("chunkyborder.markerset", "World Border", null, false);
        AreaMarker areaMarker = markerSet.createAreaMarker("chunkyborder.marker." + selection.world.getName(), "World Border", false, selection.world.getName(), poly.pointsX(), poly.pointsZ(), false);
        areaMarker.setLineStyle(3, 1f, 0xFF0000);
        areaMarker.setFillStyle(0f, 0x000000);
        //CircleMarker circleMarker = markerSet.createCircleMarker("chunkyborder.marker." + selection.world.getName(), "World Border", false, selection.world.getName(), selection.x + 8, Math.min(selection.world.getSeaLevel(), selection.world.getMaxHeight()), selection.z + 8, (selection.getDiameterChunks() * 16) / 2f, (selection.getDiameterChunksZ() * 16) / 2f, false);
        //circleMarker.setLineStyle(3, 1f, 0xFF0000);
        //circleMarker.setFillStyle(0f, 0x000000);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent e) {
//        Player player = e.getPlayer();
//        World world = player.getWorld();
//        Location location = player.getEyeLocation();
//        if (!borders.containsKey(world)) {
//            return;
//        }
//        Shape border = borders.get(world);
//        double centerX = 0, centerZ = 0;
//        if (!border.isBounding(location.getX(), location.getZ())) {
//            // TL
//            double deltaX = location.getX() - e.getFrom().getX();
//            double deltaZ = location.getZ() - e.getFrom().getZ();
//            if (deltaX == 0 && deltaZ == 0) {
//                return;
//            }
//            Vector toEdge = new Vector(deltaX, 0, deltaZ).normalize().multiply(2);
//            if (border.isBounding(location.getX() + toEdge.getX(), location.getZ() + toEdge.getZ())) {
//                player.teleport(new Location(location.getWorld(), location.getX() + toEdge.getX(), location.getY(), location.getZ() + toEdge.getZ(), location.getYaw(), location.getPitch()));
//            } else {
//                player.teleport(new Location(location.getWorld(), location.getX() - toEdge.getX(), location.getY(), location.getZ() - toEdge.getZ(), location.getYaw(), location.getPitch()));
//            }
//        }
//    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Location toLocation = e.getTo();
        if (toLocation == null) {
            return;
        }
        World toWorld = toLocation.getWorld();
        if (toWorld == null || !borders.containsKey(toWorld)) {
            return;
        }
        AbstractPolygon border = (AbstractPolygon) borders.get(toWorld); // TODO: This could be something else
        Vector to = toLocation.toVector();
        if (!border.isBounding(to.getX(), to.getZ())) {
            // TL
            double[] pointsX = border.pointsX();
            double[] pointsZ = border.pointsZ();
            double centerX = selection.x;
            double centerZ = selection.z;
            double toX = to.getX();
            double toY = to.getY();
            double toZ = to.getZ();
            final List<double[]> intersections = new ArrayList<>();
            for (int i = 0; i < pointsX.length; ++i) {
                ShapeUtil.intersection(centerX, centerZ, toX, toZ, pointsX[i], pointsZ[i], pointsX[i == pointsX.length - 1 ? 0 : i + 1], pointsZ[i == pointsZ.length - 1 ? 0 : i + 1]).ifPresent(intersections::add);
            }
            if (intersections.isEmpty()) {
                e.setTo(toWorld.getSpawnLocation());
                return;
            }
            double closestX = intersections.get(0)[0];
            double closestZ = intersections.get(0)[1];
            double shortestDistance = Double.MAX_VALUE;
            for (double[] intersection : intersections) {
                double intersectionX = intersection[0];
                double intersectionZ = intersection[1];
                double distance = to.distanceSquared(new Vector(intersectionX, toY, intersectionZ));
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    closestX = intersectionX;
                    closestZ = intersectionZ;
                }
            }
            Vector towardsCenter = new Vector(centerX - toX, 0, centerZ - toZ).normalize().multiply(3);
            // TODO: set the yaw and pitch to look towards the center
            Location insideBorder = new Location(toWorld, closestX, toY, closestZ);
            insideBorder.add(towardsCenter);
            insideBorder.setY(toWorld.getHighestBlockYAt(insideBorder));
            // TODO: remove debug
            this.getServer().getConsoleSender().sendMessage("Teleporting into border!");
            e.setTo(insideBorder);
        }
    }
}
