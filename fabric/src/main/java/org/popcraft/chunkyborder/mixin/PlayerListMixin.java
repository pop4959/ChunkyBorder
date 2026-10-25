package org.popcraft.chunkyborder.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.popcraft.chunkyborder.bridge.RespawningPlayerBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(
            method = "respawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleRespawningPlayerBridge(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir, TeleportTransition target, ServerLevel serverLevel, ServerPlayer respawningPlayer, Vec3 vec3, byte b, ServerLevel serverLevel2, LevelData levelData) {
        ((RespawningPlayerBridge) respawningPlayer.connection).chunkyborder$setRespawningPlayer(respawningPlayer);
    }
}
