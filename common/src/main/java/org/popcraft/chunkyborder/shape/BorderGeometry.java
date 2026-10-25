package org.popcraft.chunkyborder.shape;

import java.util.ArrayList;
import java.util.List;

public final class BorderGeometry {
    private static final long MAX_ELLIPSE_SEGMENTS = 65536;
    private static final int ARC_LENGTH_STEPS = 4096;

    private BorderGeometry() {
    }

    public static double distanceToBorder(final BorderShape shape, final double x, final double z) {
        if (shape instanceof final PolygonBorderShape polygon) {
            final double[] pointsX = polygon.getPointsX();
            final double[] pointsZ = polygon.getPointsZ();
            double distance = Double.MAX_VALUE;
            boolean inside = false;
            for (int i = 0, j = pointsX.length - 1; i < pointsX.length; j = i++) {
                distance = Math.min(distance, distanceToSegment(x, z, pointsX[j], pointsZ[j], pointsX[i], pointsZ[i]));
                if ((pointsZ[i] > z) != (pointsZ[j] > z) && x < (pointsX[j] - pointsX[i]) * (z - pointsZ[i]) / (pointsZ[j] - pointsZ[i]) + pointsX[i]) {
                    inside = !inside;
                }
            }
            return inside ? distance : -distance;
        } else if (shape instanceof final EllipseBorderShape ellipse) {
            final double centerX = ellipse.getCenterX();
            final double centerZ = ellipse.getCenterZ();
            final double radiusX = ellipse.getRadiusX();
            final double radiusZ = ellipse.getRadiusZ();
            if (radiusX <= 0 || radiusZ <= 0) {
                return Double.MAX_VALUE;
            }
            final double angle = closestEllipseAngle(radiusX, radiusZ, x - centerX, z - centerZ);
            final double distance = Math.hypot(x - (centerX + radiusX * Math.cos(angle)), z - (centerZ + radiusZ * Math.sin(angle)));
            final double normalizedX = (x - centerX) / radiusX;
            final double normalizedZ = (z - centerZ) / radiusZ;
            return normalizedX * normalizedX + normalizedZ * normalizedZ <= 1 ? distance : -distance;
        }
        return Double.MAX_VALUE;
    }

    public static List<Wall> walls(final BorderShape shape, final double x, final double z, final double renderDistance) {
        final Window window = new Window(Math.floor(x - renderDistance), Math.ceil(x + renderDistance), Math.floor(z - renderDistance), Math.ceil(z + renderDistance));
        final List<Wall> walls = new ArrayList<>();
        if (shape instanceof final PolygonBorderShape polygon) {
            final double[] pointsX = polygon.getPointsX();
            final double[] pointsZ = polygon.getPointsZ();
            final int points = pointsX.length;
            double area = 0;
            for (int i = 0, j = points - 1; i < points; j = i++) {
                area += pointsX[j] * pointsZ[i] - pointsX[i] * pointsZ[j];
            }
            final float uSign = area > 0 ? -1 : 1;
            for (int i = 0; i < points; ++i) {
                final int next = i + 1 == points ? 0 : i + 1;
                addWall(walls, window, pointsX[i], pointsZ[i], pointsX[next], pointsZ[next], uSign, Double.NaN);
            }
        } else if (shape instanceof final EllipseBorderShape ellipse) {
            final double centerX = ellipse.getCenterX();
            final double centerZ = ellipse.getCenterZ();
            final double radiusX = ellipse.getRadiusX();
            final double radiusZ = ellipse.getRadiusZ();
            final double radius = Math.min(radiusX, radiusZ);
            if (radius <= 0) {
                return walls;
            }
            final double fullStep = radius > 0.5 ? 2 * Math.asin(0.5 / radius) : Math.PI / 2;
            final long fullSegments = Math.max(4, (long) Math.ceil(2 * Math.PI / fullStep));
            final double step;
            final long first;
            final long last;
            double arcLength;
            if (fullSegments <= MAX_ELLIPSE_SEGMENTS) {
                step = 2 * Math.PI / fullSegments;
                first = 0;
                last = fullSegments;
                arcLength = 0;
            } else {
                step = fullStep;
                final double cameraAngle = closestEllipseAngle(radiusX, radiusZ, x - centerX, z - centerZ);
                final double arcAngle = Math.min(Math.PI, renderDistance * Math.sqrt(2) / radius);
                first = (long) Math.floor((cameraAngle - arcAngle) / step);
                last = (long) Math.ceil((cameraAngle + arcAngle) / step);
                arcLength = ellipseArcLength(radiusX, radiusZ, first * step);
            }
            for (long i = first; i < last; ++i) {
                final double a = i * step;
                final double b = (i + 1) * step;
                final double x1 = centerX + radiusX * Math.cos(a);
                final double z1 = centerZ + radiusZ * Math.sin(a);
                final double x2 = centerX + radiusX * Math.cos(b);
                final double z2 = centerZ + radiusZ * Math.sin(b);
                addWall(walls, window, x1, z1, x2, z2, -1, (arcLength - Math.floor(arcLength / 2) * 2) * 0.5);
                arcLength += Math.hypot(x2 - x1, z2 - z1);
            }
        }
        return walls;
    }

    private static void addWall(final List<Wall> walls, final Window window, final double x1, final double z1, final double x2, final double z2, final float uSign, final double u) {
        final double dx = x2 - x1;
        final double dz = z2 - z1;
        final double length = Math.hypot(dx, dz);
        if (length == 0) {
            return;
        }
        final double[] t = {0, 1};
        if (!clip(-dx, x1 - window.minX, t) || !clip(dx, window.maxX - x1, t) || !clip(-dz, z1 - window.minZ, t) || !clip(dz, window.maxZ - z1, t) || t[0] >= t[1]) {
            return;
        }
        final double unitX = dx / length;
        final double unitZ = dz / length;
        final double normalX = unitZ * uSign;
        final double normalZ = -unitX * uSign;
        final double start = t[0] * length;
        final double position = x1 * unitX + z1 * unitZ + start;
        final double u0 = Double.isNaN(u) ? (position - Math.floor(position / 2) * 2) * 0.5 : u + start * 0.5;
        walls.add(new Wall(x1 + unitX * start, z1 + unitZ * start, unitX, unitZ, (t[1] - t[0]) * length, (float) u0, uSign, normalX, normalZ, normalX * x1 + normalZ * z1));
    }

    private static boolean clip(final double p, final double q, final double[] t) {
        if (p == 0) {
            return q >= 0;
        }
        final double r = q / p;
        if (p < 0) {
            if (r > t[1]) {
                return false;
            }
            t[0] = Math.max(t[0], r);
        } else {
            if (r < t[0]) {
                return false;
            }
            t[1] = Math.min(t[1], r);
        }
        return true;
    }

    private static double distanceToSegment(final double x, final double z, final double x1, final double z1, final double x2, final double z2) {
        final double dx = x2 - x1;
        final double dz = z2 - z1;
        final double lengthSquared = dx * dx + dz * dz;
        final double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((x - x1) * dx + (z - z1) * dz) / lengthSquared));
        return Math.hypot(x - (x1 + t * dx), z - (z1 + t * dz));
    }

    private static double closestEllipseAngle(final double radiusX, final double radiusZ, final double x, final double z) {
        final boolean swap = radiusZ > radiusX;
        final double e0 = swap ? radiusZ : radiusX;
        final double e1 = swap ? radiusX : radiusZ;
        final double y0 = Math.abs(swap ? z : x);
        final double y1 = Math.abs(swap ? x : z);
        final double x0;
        final double x1;
        if (y1 > 0) {
            if (y0 > 0) {
                final double z0 = y0 / e0;
                final double z1 = y1 / e1;
                final double g = z0 * z0 + z1 * z1 - 1;
                if (g != 0) {
                    final double r0 = (e0 / e1) * (e0 / e1);
                    final double s = ellipseRoot(r0, z0, z1, g);
                    x0 = r0 * y0 / (s + r0);
                    x1 = y1 / (s + 1);
                } else {
                    x0 = y0;
                    x1 = y1;
                }
            } else {
                x0 = 0;
                x1 = e1;
            }
        } else {
            final double numerator = e0 * y0;
            final double denominator = e0 * e0 - e1 * e1;
            if (numerator < denominator) {
                final double ratio = numerator / denominator;
                x0 = e0 * ratio;
                x1 = e1 * Math.sqrt(1 - ratio * ratio);
            } else {
                x0 = e0;
                x1 = 0;
            }
        }
        final double cos = Math.copySign(x0 / e0, swap ? z : x);
        final double sin = Math.copySign(x1 / e1, swap ? x : z);
        return swap ? Math.atan2(cos, sin) : Math.atan2(sin, cos);
    }

    private static double ellipseRoot(final double r0, final double z0, final double z1, final double g) {
        final double n0 = r0 * z0;
        double s0 = z1 - 1;
        double s1 = g < 0 ? 0 : Math.hypot(n0, z1) - 1;
        double s = 0;
        for (int i = 0; i < 1100; ++i) {
            s = (s0 + s1) / 2;
            if (s == s0 || s == s1) {
                break;
            }
            final double ratio0 = n0 / (s + r0);
            final double ratio1 = z1 / (s + 1);
            final double value = ratio0 * ratio0 + ratio1 * ratio1 - 1;
            if (value > 0) {
                s0 = s;
            } else if (value < 0) {
                s1 = s;
            } else {
                break;
            }
        }
        return s;
    }

    private static double ellipseArcLength(final double radiusX, final double radiusZ, final double angle) {
        final double h = angle / ARC_LENGTH_STEPS;
        double sum = 0;
        for (int i = 0; i <= ARC_LENGTH_STEPS; ++i) {
            final double a = i * h;
            final double value = Math.hypot(radiusX * Math.sin(a), radiusZ * Math.cos(a));
            sum += (i == 0 || i == ARC_LENGTH_STEPS ? 1 : i % 2 == 0 ? 2 : 4) * value;
        }
        return sum * h / 3;
    }

    private record Window(double minX, double maxX, double minZ, double maxZ) {
    }

    public record Wall(double startX, double startZ, double unitX, double unitZ, double length, float u, float uSign, double normalX, double normalZ, double lineOffset) {
        public double x(final double distance) {
            return startX + unitX * distance;
        }

        public double z(final double distance) {
            return startZ + unitZ * distance;
        }

        public double distanceToWall(final double x, final double z) {
            return normalX * x + normalZ * z - lineOffset;
        }
    }
}
