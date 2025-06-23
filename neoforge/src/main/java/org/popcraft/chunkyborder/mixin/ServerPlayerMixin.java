package org.popcraft.chunkyborder.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.NeoForgePlayer;
import org.popcraft.chunky.platform.NeoForgeWorld;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunkyborder.event.server.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Unique
    private Location redirect;

    // Simple teleport within same dimension
    @Inject(
            method = "teleportTo(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void chunkyborder$onTeleport(double x, double y, double z, CallbackInfo ci) {
        handleTeleport(((ServerPlayer)(Object)this).level(), x, y, z, 0.0F, 0.0F, null, false, ci, null);
    }

    // Teleport with full args (dimension, relative, yaw, pitch, etc)
    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void chunkyborder$onTeleportFull(ServerLevel level, double x, double y, double z, Set<Relative> set, float yaw, float pitch, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        handleTeleport(level, x, y, z, yaw, pitch, set, bl, null, cir);
    }

    /**
     * Handles both kinds of teleport, using nulls for missing args.
     */
    @Unique
    private void handleTeleport(ServerLevel level, double x, double y, double z, float yaw, float pitch, Set<Relative> set, boolean bl, CallbackInfo ci, CallbackInfoReturnable<Boolean> cir) {
        final NeoForgePlayer neoForgePlayer = new NeoForgePlayer((ServerPlayer)(Object)this);
        final NeoForgeWorld world = new NeoForgeWorld(level);
        final Location location = new Location(world, x, y, z, yaw, pitch);
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(neoForgePlayer, location);
        ChunkyProvider.get().getEventBus().call(playerTeleportEvent);
        final Optional<Location> optionalLocation = playerTeleportEvent.redirect();
        redirect = optionalLocation.orElse(null);

        if (redirect != null) {
            connection.teleport(
                    new PositionMoveRotation(
                            new Vec3(redirect.getX(), redirect.getY(), redirect.getZ()),
                            Vec3.ZERO,
                            redirect.getYaw(),
                            redirect.getPitch()
                    ),
                    set != null ? set : Relative.union(Relative.DELTA, Relative.ROTATION)
            );

            if (ci != null) ci.cancel();
            if (cir != null) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}