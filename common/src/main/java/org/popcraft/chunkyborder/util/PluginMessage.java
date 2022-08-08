package org.popcraft.chunkyborder.util;

import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PluginMessage {
    public static byte[] writeBorderData(final World world, final Shape shape) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(0);
            data.writeUTF(world.getKey());
            if (shape instanceof AbstractPolygon polygon) {
                data.writeByte(1);
                final List<Vector2> points = polygon.points();
                data.writeInt(points.size());
                for (final Vector2 point : points) {
                    data.writeDouble(point.getX());
                    data.writeDouble(point.getZ());
                }
            } else if (shape instanceof AbstractEllipse ellipse) {
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
        }
    }
}
