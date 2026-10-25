package org.popcraft.chunkyborder.mixin;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
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
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void place(final ItemPlacementContext context, final CallbackInfoReturnable<ActionResult> cir) {
        if (!(context.getPlayer() instanceof final ServerPlayerEntity player)) {
            return;
        }
        final BlockPos pos = context.getBlockPos();
        final BlockPlaceEvent placeEvent = new BlockPlaceEvent(new FabricPlayer(player), new Location(new FabricWorld((ServerWorld) context.getWorld()), pos.getX(), pos.getY(), pos.getZ()));
        ChunkyProvider.get().getEventBus().call(placeEvent);
        if (placeEvent.isCancelled()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
