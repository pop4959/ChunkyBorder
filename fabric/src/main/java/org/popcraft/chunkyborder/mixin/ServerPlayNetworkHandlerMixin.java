package org.popcraft.chunkyborder.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.FabricPlayer;
import org.popcraft.chunky.platform.FabricWorld;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunkyborder.bridge.RespawningPlayerBridge;
import org.popcraft.chunkyborder.event.server.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements RespawningPlayerBridge {
    @Shadow
    public ServerPlayerEntity player;

    @Unique
    private Location redirect;

    @Unique
    private ServerPlayerEntity respawningPlayer;

    @Inject(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD")
    )
    private void requestTeleport(final double x, final double y, final double z, final float yaw, final float pitch, final CallbackInfo ci) {
        ServerPlayerEntity player = Objects.requireNonNullElse(this.respawningPlayer, this.player);
        this.respawningPlayer = null; // Reset respawning player, that way it keeps using 'this.player' until it's respawning again
        final FabricPlayer fabricPlayer = new FabricPlayer(player);
        final FabricWorld world = new FabricWorld(player.getWorld());
        final Location location = new Location(world, x, y, z, yaw, pitch);
        final PlayerTeleportEvent playerTeleportEvent = new PlayerTeleportEvent(fabricPlayer, location);
        ChunkyProvider.get().getEventBus().call(playerTeleportEvent);
        final Optional<Location> optionalLocation = playerTeleportEvent.redirect();
        optionalLocation.ifPresentOrElse(redirectLocation -> redirect = redirectLocation, () -> redirect = null);
    }

    @ModifyVariable(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private double requestTeleportX(final double x) {
        return redirect == null ? x : redirect.getX();
    }

    @ModifyVariable(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3
    )
    private double requestTeleportY(final double y) {
        return redirect == null ? y : redirect.getY();
    }

    @ModifyVariable(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 5
    )
    private double requestTeleportZ(final double z) {
        return redirect == null ? z : redirect.getZ();
    }

    @ModifyVariable(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 7
    )
    private float requestTeleportYaw(final float yaw) {
        return redirect == null ? yaw : redirect.getYaw();
    }

    @ModifyVariable(
            method = "requestTeleport(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 8
    )
    private float requestTeleportPitch(final float pitch) {
        return redirect == null ? pitch : redirect.getPitch();
    }

    @Override
    public void chunkyborder$setRespawningPlayer(ServerPlayerEntity player) {
        this.respawningPlayer = player;
    }
}
