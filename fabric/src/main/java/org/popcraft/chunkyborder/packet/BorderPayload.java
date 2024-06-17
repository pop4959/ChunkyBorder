package org.popcraft.chunkyborder.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.util.ClientBorder;
import org.popcraft.chunkyborder.util.PluginMessage;

public class BorderPayload implements CustomPayload {
    public static final CustomPayload.Id<BorderPayload> ID = new CustomPayload.Id<>(Identifier.of("chunky:border"));
    private World world;
    private Shape shape;
    private ClientBorder border;

    public BorderPayload(final World world, final Shape shape) {
        this.world = world;
        this.shape = shape;
    }

    public BorderPayload(final PacketByteBuf buf) {
        final ByteBuf unwrapped = buf.unwrap();
        final byte[] bytes = new byte[unwrapped.readableBytes()];
        unwrapped.readBytes(bytes);
        this.border = PluginMessage.readBorder(bytes);
    }

    public void write(final PacketByteBuf buf) {
        buf.writeBytes(PluginMessage.writeBorder(world, shape));
    }

    public ClientBorder getBorder() {
        return border;
    }

    @Override
    public CustomPayload.Id<BorderPayload> getId() {
        return ID;
    }
}
