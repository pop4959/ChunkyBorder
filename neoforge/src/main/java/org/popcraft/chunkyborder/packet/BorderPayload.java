package org.popcraft.chunkyborder.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.util.ClientBorder;
import org.popcraft.chunkyborder.util.PluginMessage;

public class BorderPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BorderPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.parse("chunky:border"));
    private final ClientBorder border;

    public BorderPayload(final World world, final Shape shape) {
        this.border = new ClientBorder(world.getKey(), shape);
    }

    public BorderPayload(final FriendlyByteBuf buf) {
        final ByteBuf unwrapped = buf.unwrap();
        final byte[] bytes = new byte[unwrapped.readableBytes()];
        unwrapped.readBytes(bytes);
        this.border = PluginMessage.readBorder(bytes);
    }

    public void write(final FriendlyByteBuf buf) {
        buf.writeBytes(PluginMessage.writeBorder(this.border));
    }

    public ClientBorder getBorder() {
        return border;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<BorderPayload> type() {
        return ID;
    }
}
