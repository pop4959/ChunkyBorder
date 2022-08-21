package org.popcraft.chunkyborder;

import io.netty.buffer.ByteBufInputStream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ChunkyBorderFabricClient implements ClientModInitializer {
    private static final Identifier PLAY_BORDER_PACKET_ID = new Identifier("chunky", "border");
    private static final Map<Identifier, BorderShape> borderShapes = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PLAY_BORDER_PACKET_ID, (client, handler, buf, responseSender) -> {
            try (final ByteBufInputStream in = new ByteBufInputStream(buf); final DataInputStream data = new DataInputStream(in)) {
                final int version = data.readInt();
                final String worldKey = data.readUTF();
                if (version == 0) {
                    final byte type = data.readByte();
                    switch (type) {
                        case 1 -> {
                            final int numPoints = data.readInt();
                            final double[] pointsX = new double[numPoints];
                            final double[] pointsZ = new double[numPoints];
                            for (int i = 0; i < numPoints; ++i) {
                                pointsX[i] = data.readDouble();
                                pointsZ[i] = data.readDouble();
                            }
                            ChunkyBorderFabricClient.setBorderShape(worldKey, new PolygonBorderShape(pointsX, pointsZ));
                        }
                        case 2 -> {
                            final double centerX = data.readDouble();
                            final double centerZ = data.readDouble();
                            final double radiusX = data.readDouble();
                            final double radiusZ = data.readDouble();
                            ChunkyBorderFabricClient.setBorderShape(worldKey, new EllipseBorderShape(centerX, centerZ, radiusX, radiusZ));
                        }
                        default -> ChunkyBorderFabricClient.setBorderShape(worldKey, null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void setBorderShape(final String id, final BorderShape borderShape) {
        final Identifier identifier = Identifier.tryParse(id);
        if (identifier != null) {
            if (borderShape == null) {
                ChunkyBorderFabricClient.borderShapes.remove(identifier);
            } else {
                ChunkyBorderFabricClient.borderShapes.put(identifier, borderShape);
            }
        }
    }

    public static BorderShape getBorderShape(final Identifier identifier) {
        return ChunkyBorderFabricClient.borderShapes.get(identifier);
    }
}
