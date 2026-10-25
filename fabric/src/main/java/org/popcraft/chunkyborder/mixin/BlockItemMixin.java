package org.popcraft.chunkyborder.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.platform.FabricPlayer;
import org.popcraft.chunky.platform.FabricWorld;
import org.popcraft.chunky.platform.util.Location;
import org.popcraft.chunkyborder.event.server.BlockPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void place(final BlockPlaceContext context, final CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
            return;
        }
        final BlockPos pos = context.getClickedPos();
        final BlockPlaceEvent placeEvent = new BlockPlaceEvent(new FabricPlayer(player), new Location(new FabricWorld((ServerLevel) context.getLevel()), pos.getX(), pos.getY(), pos.getZ()));
        ChunkyProvider.get().getEventBus().call(placeEvent);
        if (placeEvent.isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
