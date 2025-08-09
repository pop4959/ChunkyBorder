package org.popcraft.chunkyborder;

import org.popcraft.chunky.platform.Folia;
import org.popcraft.chunky.platform.World;

import java.util.concurrent.CompletableFuture;

public class BukkitBorderCheckTask extends BorderCheckTask {
    private final ChunkyBorderBukkit plugin;

    public BukkitBorderCheckTask(ChunkyBorderBukkit plugin, ChunkyBorder chunkyBorder) {
        super(chunkyBorder);
        this.plugin = plugin;
    }

    @Override
    protected CompletableFuture<Integer> getElevation(World world, int blockX, int blockZ) {
        if (!Folia.isFolia()) {
            return super.getElevation(world, blockX, blockZ);
        }

        final org.bukkit.World bukkitWorld = plugin.getServer().getWorld(world.getUUID()); // TODO: direct way to get bukkit world
        if (bukkitWorld == null) {
            return CompletableFuture.completedFuture(64);
        }

        if (plugin.getServer().isOwnedByCurrentRegion(bukkitWorld, blockX >> 4, blockZ >> 4)) {
            return super.getElevation(world, blockX, blockZ);
        }

        final CompletableFuture<Integer> future = new CompletableFuture<>();
        plugin.getServer().getRegionScheduler().run(plugin, bukkitWorld, blockX >> 4, blockZ >> 4, task -> {
            future.complete(world.getElevation(blockX, blockZ));
        });

        return future;
    }
}
