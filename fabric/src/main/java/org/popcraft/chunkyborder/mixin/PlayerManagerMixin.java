package org.popcraft.chunkyborder.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.WorldProperties;
import org.popcraft.chunkyborder.bridge.RespawningPlayerBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(
            method = "respawnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleRespawningPlayerBridge(ServerPlayerEntity player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayerEntity> cir, TeleportTarget target, ServerWorld serverWorld, ServerPlayerEntity respawningPlayer, Vec3d vec3d, byte b, ServerWorld serverWorld2, WorldProperties worldProperties) {
        ((RespawningPlayerBridge) respawningPlayer.networkHandler).chunkyborder$setRespawningPlayer(respawningPlayer);
    }
}
