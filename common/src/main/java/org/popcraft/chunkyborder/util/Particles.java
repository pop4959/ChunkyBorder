package org.popcraft.chunkyborder.util;

import org.popcraft.chunky.platform.Player;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.platform.util.Vector3;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunky.shape.ShapeUtil;

import java.util.ArrayList;
import java.util.List;

public class Particles {
    private static int maxDistance = 8;
    private static int maxDistanceSquared = maxDistance * maxDistance;

    private Particles() {
    }

    public static void setMaxDistance(final int maxDistance) {
        Particles.maxDistance = maxDistance;
        maxDistanceSquared = maxDistance * maxDistance;
    }

    public static List<Vector3> at(final Player player, final Shape border, final double offsetPercent) {
        final Vector3 pos = player.getLocation().toVector();
        final List<Vector3> particles = new ArrayList<>();
        if (border instanceof final AbstractPolygon polygon) {
            final List<Vector2> points = polygon.points();
            final int numPoints = points.size();
            for (int i = 0; i < numPoints; ++i) {
                final Vector2 p1 = points.get(i);
                final Vector2 p2 = points.get(i == numPoints - 1 ? 0 : i + 1);
                final Vector2 closestPoint = ShapeUtil.closestPointOnLine(pos.getX(), pos.getZ(), p1.getX(), p1.getZ(), p2.getX(), p2.getZ());
                if (ShapeUtil.distanceBetweenPoints(pos.getX(), pos.getZ(), closestPoint.getX(), closestPoint.getZ()) > maxDistance) {
                    continue;
                }
                final Vector2 unit = Vector2.of(p1.getX() - p2.getX(), p1.getZ() - p2.getZ()).normalize();
                final double unitX = unit.getX();
                final double unitZ = unit.getZ();
                final double startX = unitX == 0 ? closestPoint.getX() : Math.floor(closestPoint.getX() / unitX) * unitX;
                final double startY = Math.floor(pos.getY());
                final double startZ = unitZ == 0 ? closestPoint.getZ() : Math.floor(closestPoint.getZ() / unitZ) * unitZ;
                final double minX = Math.min(p1.getX(), p2.getX());
                final double minZ = Math.min(p1.getZ(), p2.getZ());
                final double maxX = Math.max(p1.getX(), p2.getX());
                final double maxZ = Math.max(p1.getZ(), p2.getZ());
                for (double dx = -unitX, dz = -unitZ; ; dx -= unitX, dz -= unitZ) {
                    final double x = startX + dx;
                    final double z = startZ + dz;
                    final Vector3 startPos = Vector3.of(x, startY, z);
                    if (pos.distanceSquared(startPos) > maxDistanceSquared) {
                        break;
                    }
                    final List<Vector3> pointsBack = verticalPoints(pos, startPos, offsetPercent, unitX, unitZ, minX, minZ, maxX, maxZ);
                    particles.addAll(pointsBack);
                }
                for (double dx = 0, dz = 0; ; dx += unitX, dz += unitZ) {
                    final double x = startX + dx;
                    final double z = startZ + dz;
                    final Vector3 startPos = Vector3.of(x, startY, z);
                    if (pos.distanceSquared(startPos) > maxDistanceSquared) {
                        break;
                    }
                    final List<Vector3> pointsForward = verticalPoints(pos, startPos, offsetPercent, unitX, unitZ, minX, minZ, maxX, maxZ);
                    particles.addAll(pointsForward);
                }
            }
        } else if (border instanceof final AbstractEllipse ellipse) {
            final Vector2 center = ellipse.center();
            final Vector2 radii = ellipse.radii();
            final double radius = Math.min(radii.getX(), radii.getZ());
            final double angle = Math.acos((2 * radius * radius - 1) / (2 * radius * radius));
            final double cameraAngle = Math.atan2((radii.getX() * pos.getZ()) - center.getZ(), (radii.getZ() * pos.getX()) - center.getX());
            final double startY = Math.floor(pos.getY());
            final double forwardStartAngle = Math.floor(cameraAngle / angle) * angle;
            final double backwardStartAngle = forwardStartAngle - angle;
            final double backwardStopAngle = backwardStartAngle - Math.PI;
            for (double da = backwardStartAngle; da > backwardStopAngle; da = da - angle) {
                final Vector2 start = ShapeUtil.pointOnEllipse(center.getX(), center.getZ(), radii.getX(), radii.getZ(), da);
                final Vector2 end = ShapeUtil.pointOnEllipse(center.getX(), center.getZ(), radii.getX(), radii.getZ(), da + angle);
                final double minX = Math.min(start.getX(), end.getX());
                final double minZ = Math.min(start.getZ(), end.getZ());
                final double maxX = Math.max(start.getX(), end.getX());
                final double maxZ = Math.max(start.getZ(), end.getZ());
                final Vector3 startPos = Vector3.of(start.getX(), startY, start.getZ());
                final List<Vector3> pointsBack = verticalPoints(pos, startPos, offsetPercent, end.getX() - start.getX(), end.getZ() - start.getZ(), minX, minZ, maxX, maxZ);
                if (pointsBack.isEmpty()) {
                    break;
                }
                particles.addAll(pointsBack);
            }
            final double forwardStopAngle = forwardStartAngle + Math.PI;
            for (double da = forwardStartAngle; da < forwardStopAngle; da = da + angle) {
                final Vector2 start = ShapeUtil.pointOnEllipse(center.getX(), center.getZ(), radii.getX(), radii.getZ(), da);
                final Vector2 end = ShapeUtil.pointOnEllipse(center.getX(), center.getZ(), radii.getX(), radii.getZ(), da + angle);
                final double minX = Math.min(start.getX(), end.getX());
                final double minZ = Math.min(start.getZ(), end.getZ());
                final double maxX = Math.max(start.getX(), end.getX());
                final double maxZ = Math.max(start.getZ(), end.getZ());
                final Vector3 startPos = Vector3.of(start.getX(), startY, start.getZ());
                final List<Vector3> pointsBack = verticalPoints(pos, startPos, offsetPercent, end.getX() - start.getX(), end.getZ() - start.getZ(), minX, minZ, maxX, maxZ);
                if (pointsBack.isEmpty()) {
                    break;
                }
                particles.addAll(pointsBack);
            }
        }
        return particles;
    }

    private static List<Vector3> verticalPoints(final Vector3 playerPos, final Vector3 startPos, final double offsetPercent, final double unitX, final double unitZ, final double minX, final double minZ, final double maxX, final double maxZ) {
        final List<Vector3> points = new ArrayList<>();
        final double x = startPos.getX();
        final double y = startPos.getY();
        final double z = startPos.getZ();
        final double unitOffsetX = unitX * offsetPercent;
        final double unitOffsetZ = unitZ * offsetPercent;
        final double offsetX = x + unitOffsetX;
        final double offsetZ = z + unitOffsetZ;
        final Vector3 start = Vector3.of(offsetX, y - offsetPercent, offsetZ);
        if (!(offsetX >= minX && offsetX <= maxX && offsetZ >= minZ && offsetZ <= maxZ)) {
            return points;
        }
        if (playerPos.distanceSquared(start) > maxDistanceSquared) {
            return points;
        }
        points.add(start);
        for (double dy = 0; ; ++dy) {
            final double up = y + dy;
            final double down = y - dy;
            final Vector3 upPos = Vector3.of(offsetX, up - offsetPercent, offsetZ);
            final Vector3 downPos = Vector3.of(offsetX, down - offsetPercent, offsetZ);
            final boolean upInsideDistance = playerPos.distanceSquared(upPos) <= maxDistanceSquared;
            final boolean downInsideDistance = playerPos.distanceSquared(downPos) <= maxDistanceSquared;
            if (!upInsideDistance && !downInsideDistance) {
                break;
            }
            if (upInsideDistance) {
                points.add(upPos);
            }
            if (downInsideDistance) {
                points.add(downPos);
            }
        }
        return points;
    }
}
