package org.popcraft.chunkyborder.util;

import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

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
                    default -> new ClientBorder(worldKey, null);
                };
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ClientBorder(null, null);
    }

    public static byte[] writeBorder(final World world, final Shape shape) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(VERSION);
            data.writeUTF(world.getKey());
            if (shape instanceof final AbstractPolygon polygon) {
                data.writeByte(1);
                final List<Vector2> points = polygon.points();
                data.writeInt(points.size());
                for (final Vector2 point : points) {
                    data.writeDouble(point.getX());
                    data.writeDouble(point.getZ());
                }
            } else if (shape instanceof final AbstractEllipse ellipse) {
                data.writeByte(2);
                final Vector2 center = ellipse.center();
                final Vector2 radii = ellipse.radii();
                data.writeDouble(center.getX());
                data.writeDouble(center.getZ());
                data.writeDouble(radii.getX());
                data.writeDouble(radii.getZ());
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
