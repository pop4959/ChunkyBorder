package org.popcraft.chunkyborder.util;

import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PluginMessage {
    public static final int VERSION = 0;
    public static final byte[] INVALID_MESSAGE = ByteBuffer.allocate(4).putInt(-1).array();

    public static ClientBorder readBorder(final byte[] bytes) {
        try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes); final DataInputStream data = new DataInputStream(in)) {
            final int version = data.readInt();
            if (version == 0) {
                final String worldKey = data.readUTF();
                final byte type = data.readByte();
                return switch (type) {
                    case 1 -> {
                        final int numPoints = data.readInt();
                        final double[] pointsX = new double[numPoints];
                        final double[] pointsZ = new double[numPoints];
                        for (int i = 0; i < numPoints; ++i) {
                            pointsX[i] = data.readDouble();
                            pointsZ[i] = data.readDouble();
                        }
                        yield new ClientBorder(worldKey, new PolygonBorderShape(pointsX, pointsZ));
                    }
                    case 2 -> {
                        final double centerX = data.readDouble();
                        final double centerZ = data.readDouble();
                        final double radiusX = data.readDouble();
                        final double radiusZ = data.readDouble();
                        yield new ClientBorder(worldKey, new EllipseBorderShape(centerX, centerZ, radiusX, radiusZ));
                    }
                    default -> new ClientBorder(worldKey, (BorderShape) null);
                };
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ClientBorder(null, (BorderShape) null);
    }

    public static byte[] writeBorder(final ClientBorder border) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(VERSION);
            data.writeUTF(border.worldKey());
            if (border.borderShape() instanceof final PolygonBorderShape polygon) {
                data.writeByte(1);
                final double[] pointsX = polygon.getPointsX();
                final double[] pointsZ = polygon.getPointsZ();
                data.writeInt(pointsX.length);
                for (int i = 0; i < pointsX.length; i++) {
                    data.writeDouble(pointsX[i]);
                    data.writeDouble(pointsZ[i]);
                }
            } else if (border.borderShape() instanceof final EllipseBorderShape ellipse) {
                data.writeByte(2);
                data.writeDouble(ellipse.getCenterX());
                data.writeDouble(ellipse.getCenterZ());
                data.writeDouble(ellipse.getRadiusX());
                data.writeDouble(ellipse.getRadiusZ());
            } else {
                data.writeByte(0);
            }
            return out.toByteArray();
        } catch (final IOException e) {
            e.printStackTrace();
            return INVALID_MESSAGE;
        }
    }
}
