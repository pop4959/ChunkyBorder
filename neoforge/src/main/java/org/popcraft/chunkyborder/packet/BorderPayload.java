package org.popcraft.chunkyborder.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.util.ClientBorder;
import org.popcraft.chunkyborder.util.PluginMessage;

public class BorderPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BorderPayload> ID = CustomPacketPayload.createType("chunky:border");
    private World world;
    private Shape shape;
    private ClientBorder border;

    public BorderPayload(final World world, final Shape shape) {
        this.world = world;
        this.shape = shape;
    }

    public BorderPayload(final FriendlyByteBuf buf) {
        final ByteBuf unwrapped = buf.unwrap();
        final byte[] bytes = new byte[unwrapped.readableBytes()];
        unwrapped.readBytes(bytes);
        this.border = PluginMessage.readBorder(bytes);
    }

    public void write(final FriendlyByteBuf buf) {
        buf.writeBytes(PluginMessage.writeBorder(world, shape));
    }

    public ClientBorder getBorder() {
        return border;
    }

    @Override
    public CustomPacketPayload.@NotNull Type<BorderPayload> type() {
        return ID;
    }
}
