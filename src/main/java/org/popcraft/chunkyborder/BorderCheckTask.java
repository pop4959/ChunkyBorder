package org.popcraft.chunkyborder;

import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunky.util.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BorderCheckTask implements Runnable {
    private ChunkyBorder chunkyBorder;

    public BorderCheckTask(ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public void run() {
        for (final Player player : chunkyBorder.getServer().getOnlinePlayers()) {
            final Map<String, BorderData> borders = chunkyBorder.getBorders();
            final World world = player.getWorld();
            if (borders == null || !borders.containsKey(world.getName())) {
                continue;
            }
            final BorderData borderData = borders.get(world.getName());
            if (borderData == null) {
                continue;
            }
            final Shape border = borderData.getBorder();
            if (border == null) {
                continue;
            }
            final Location loc = player.getLocation();
            boolean currentLocationValid = border.isBounding(loc.getX(), loc.getZ());
            boolean lastLocationValid = chunkyBorder.getPlayerData(player).isLastLocationValid();
            chunkyBorder.getPlayerData(player).setLastLocationValid(currentLocationValid);
            if (currentLocationValid) {
                chunkyBorder.getPlayerData(player).setLastLocation(loc);
            } else {
                if (player.hasPermission("chunkyborder.bypass.move") || chunkyBorder.getPlayerData(player).isBypassing()) {
                    if (lastLocationValid) {
                        chunkyBorder.sendBorderMessage(player);
                    }
                    continue;
                }
                final Location newLoc;
                if (borderData.isWrap()) {
                    newLoc = wrap(borderData, player);
                    chunkyBorder.getPlayerData(player).setLastLocation(newLoc);
                } else {
                    newLoc = chunkyBorder.getPlayerData(player).getLastLocation().orElse(world.getSpawnLocation());
                    newLoc.setYaw(loc.getYaw());
                    newLoc.setPitch(loc.getPitch());
                }
                chunkyBorder.sendBorderMessage(player);
                chunkyBorder.getBorderEffect().ifPresent(effect -> player.getWorld().playEffect(loc, effect, 0));
                chunkyBorder.getBorderSound().ifPresent(sound -> player.getWorld().playSound(loc, sound, 2f, 1f));
                final Entity vehicle = player.getVehicle();
                player.setMetadata("NPC", new FixedMetadataValue(chunkyBorder, false));
                PaperLib.teleportAsync(player, newLoc).thenRun(() -> player.removeMetadata("NPC", chunkyBorder));
                if (vehicle != null) {
                    PaperLib.teleportAsync(vehicle, newLoc);
                }
            }
        }
    }

    private Location wrap(BorderData borderData, Player player) {
        Location loc = player.getLocation();
        Location newLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
        switch (borderData.getShape()) {
            case "square":
            case "rectangle":
                double minX = borderData.getCenterX() - borderData.getRadiusX();
                double maxX = borderData.getCenterX() + borderData.getRadiusX();
                double minZ = borderData.getCenterZ() - borderData.getRadiusZ();
                double maxZ = borderData.getCenterZ() + borderData.getRadiusZ();
                if (loc.getX() <= minX) {
                    newLoc.setX(maxX - 3);
                } else if (loc.getX() >= maxX) {
                    newLoc.setX(minX + 3);
                } else if (loc.getZ() <= minZ) {
                    newLoc.setZ(maxZ - 3);
                } else if (loc.getZ() >= maxZ) {
                    newLoc.setZ(minZ + 3);
                }
                if (!borderData.getBorder().isBounding(newLoc.getX(), newLoc.getZ())) {
                    return chunkyBorder.getPlayerData(player).getLastLocation().orElse(loc.getWorld().getSpawnLocation());
                }
                newLoc.setYaw(loc.getYaw());
                newLoc.setPitch(loc.getPitch());
                break;
            default:
                double centerX = borderData.getCenterX();
                double centerZ = borderData.getCenterZ();
                double fromX = loc.getX();
                double fromY = loc.getY();
                double fromZ = loc.getZ();
                Shape border = borderData.getBorder();
                final List<double[]> intersections = new ArrayList<>();
                if (border instanceof AbstractPolygon) {
                    AbstractPolygon polygonBorder = (AbstractPolygon) border;
                    double[] pointsX = polygonBorder.pointsX();
                    double[] pointsZ = polygonBorder.pointsZ();
                    for (int i = 0; i < pointsX.length; ++i) {
                        ShapeUtil.intersection(centerX, centerZ, fromX, fromZ, pointsX[i], pointsZ[i], pointsX[i == pointsX.length - 1 ? 0 : i + 1], pointsZ[i == pointsZ.length - 1 ? 0 : i + 1]).ifPresent(intersections::add);
                    }
                } else if (border instanceof AbstractEllipse) {
                    AbstractEllipse ellipticalBorder = (AbstractEllipse) border;
                    double[] radii = ellipticalBorder.getRadii();
                    double angle = Math.PI + Math.atan2(fromZ - centerX, fromX - centerZ);
                    intersections.add(ShapeUtil.pointOnEllipse(centerX, centerZ, radii[0], radii[1], angle));
                }
                if (intersections.isEmpty()) {
                    return chunkyBorder.getPlayerData(player).getLastLocation().orElse(loc.getWorld().getSpawnLocation());
                }
                Vector centerDirection = new Vector(fromX - centerX, 0, fromZ - centerZ).normalize().multiply(3);
                double closestX = intersections.get(0)[0];
                double closestZ = intersections.get(0)[1];
                double longestDistance = Double.MIN_VALUE;
                for (double[] intersection : intersections) {
                    double intersectionX = intersection[0];
                    double intersectionZ = intersection[1];
                    Vector position = new Vector(intersectionX, fromY, intersectionZ).add(centerDirection);
                    double distance = loc.toVector().distanceSquared(position);
                    if (distance > longestDistance && border.isBounding(position.getX(), position.getZ())) {
                        longestDistance = distance;
                        closestX = intersectionX;
                        closestZ = intersectionZ;
                    }
                }
                if (longestDistance == Double.MIN_VALUE) {
                    return chunkyBorder.getPlayerData(player).getLastLocation().orElse(loc.getWorld().getSpawnLocation());
                }
                newLoc = new Location(loc.getWorld(), closestX, fromY, closestZ);
                newLoc.add(centerDirection);
                newLoc.setDirection(centerDirection);
                break;
        }
        final int yOffset = Version.getCurrentMinecraftVersion().isHigherThanOrEqualTo(Version.v1_15_0) ? 1 : 0;
        newLoc.setY(newLoc.getWorld().getHighestBlockYAt(newLoc) + yOffset);
        return newLoc;
    }
}
