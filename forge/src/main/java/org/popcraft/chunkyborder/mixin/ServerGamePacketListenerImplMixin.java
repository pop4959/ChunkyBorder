package org.popcraft.chunkyborder.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.ForgePlayer;
import org.popcraft.chunky.platform.ForgeWorld;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunkyborder.event.server.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Unique
    private Location redirect;

    @Inject(
            method = "teleport(DDDFF)V",
            at = @At("HEAD")
    )
    private void requestTeleport(final double x, final double y, final double z, final float yaw, final float pitch, final CallbackInfo ci) {
        final ForgePlayer forgePlayer = new ForgePlayer(this.player);
        final ForgeWorld world = new ForgeWorld(this.player.serverLevel());
        final Location location = new Location(world, x, y, z, yaw, pitch);
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(forgePlayer, location);
        ChunkyProvider.get().getEventBus().call(playerTeleportEvent);
        final Optional<Location> optionalLocation = playerTeleportEvent.redirect();
        optionalLocation.ifPresentOrElse(redirectLocation -> redirect = redirectLocation, () -> redirect = null);
    }

    @ModifyVariable(
            method = "teleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private double requestTeleportX(final double x) {
        return redirect == null ? x : redirect.getX();
    }

    @ModifyVariable(
            method = "teleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3
    )
    private double requestTeleportY(final double y) {
        return redirect == null ? y : redirect.getY();
    }

    @ModifyVariable(
            method = "teleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 5
    )
    private double requestTeleportZ(final double z) {
        return redirect == null ? z : redirect.getZ();
    }

    @ModifyVariable(
            method = "teleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 7
    )
    private float requestTeleportYaw(final float yaw) {
        return redirect == null ? yaw : redirect.getYaw();
    }

    @ModifyVariable(
            method = "teleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 8
    )
    private float requestTeleportPitch(final float pitch) {
        return redirect == null ? pitch : redirect.getPitch();
    }
}
