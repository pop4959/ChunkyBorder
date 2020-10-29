package org.popcraft.chunkyborder;

import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.chunky.shape.Shape;

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
                return;
            }
            final BorderData borderData = borders.get(world.getName());
            if (borderData == null) {
                return;
            }
            final Shape border = borderData.getBorder();
            final Location loc = player.getLocation();
            if (border.isBounding(loc.getX(), loc.getZ())) {
                chunkyBorder.getLastKnownLocation().put(player.getUniqueId(), loc);
            } else {
                if (player.hasPermission("chunkyborder.bypass.move")) {
                    return;
                }
                final Location newLoc = chunkyBorder.getLastKnownLocation().getOrDefault(player.getUniqueId(), world.getSpawnLocation());
                newLoc.setYaw(loc.getYaw());
                newLoc.setPitch(loc.getPitch());
                chunkyBorder.sendBorderMessage(player);
                chunkyBorder.getBorderEffect().ifPresent(effect -> player.getWorld().playEffect(loc, effect, 0));
                chunkyBorder.getBorderSound().ifPresent(sound -> player.getWorld().playSound(loc, sound, 2f, 1f));
                final Entity vehicle = player.getVehicle();
                PaperLib.teleportAsync(player, newLoc);
                if (vehicle != null) {
                    PaperLib.teleportAsync(vehicle, newLoc);
                }
            }
        }
    }
}
