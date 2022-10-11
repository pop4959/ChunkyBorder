package org.popcraft.chunkyborder.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunkyborder.ChunkyBorderFabricClient;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.shape.EllipseBorderShape;
import org.popcraft.chunkyborder.shape.PolygonBorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow
    @Final
    @SuppressWarnings("java:S3008")
    private static Identifier FORCEFIELD;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private ClientWorld world;

    @Inject(method = "renderWorldBorder", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("java:S3776")
    private void renderWorldBorder(final Camera camera, final CallbackInfo ci) {
        final BorderShape borderShape = ChunkyBorderFabricClient.getBorderShape(this.world.getDimensionKey().getValue());
        if (borderShape == null) {
            return;
        }
        final BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        final double renderDistanceBlocks = this.client.options.getClampedViewDistance() * 16D;
        final double posX = camera.getPos().x;
        final double posZ = camera.getPos().z;
        final double height = this.client.gameRenderer.method_32796();
        double distanceInsideBorder = Double.MAX_VALUE;
        if (borderShape instanceof final PolygonBorderShape polygon) {
            final double[] pointsX = polygon.getPointsX();
            final double[] pointsZ = polygon.getPointsZ();
            for (int i = 0; i < pointsX.length; ++i) {
                final double p1x = pointsX[i];
                final double p1z = pointsZ[i];
                final double p2x = pointsX[i + 1 == pointsX.length ? 0 : i + 1];
                final double p2z = pointsZ[i + 1 == pointsZ.length ? 0 : i + 1];
                final Vector2 closestPoint = ShapeUtil.closestPointOnLine(posX, posZ, p1x, p1z, p2x, p2z);
                final double distanceToBorder = ShapeUtil.distanceBetweenPoints(posX, posZ, closestPoint.getX(), closestPoint.getZ());
                if (distanceToBorder < distanceInsideBorder) {
                    distanceInsideBorder = distanceToBorder;
                }
            }
        } else if (borderShape instanceof final EllipseBorderShape ellipse) {
            final double centerX = ellipse.getCenterX();
            final double centerZ = ellipse.getCenterZ();
            final double radiusX = ellipse.getRadiusX();
            final double radiusZ = ellipse.getRadiusZ();
            final double cameraAngle = Math.atan2(radiusX * (posZ - centerZ), radiusZ * (posX - centerX));
            final Vector2 pointOnBorder = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, cameraAngle);
            distanceInsideBorder = ShapeUtil.distanceBetweenPoints(posX, posZ, pointOnBorder.getX(), pointOnBorder.getZ());
        }
        if (distanceInsideBorder < renderDistanceBlocks) {
            double alpha = 1.0D - distanceInsideBorder / renderDistanceBlocks;
            alpha = Math.pow(alpha, 4.0D);
            alpha = clamp(alpha, 0.0D, 1.0D);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
            RenderSystem.setShaderTexture(0, FORCEFIELD);
            RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();
            RenderSystem.applyModelViewMatrix();
            final int color = BorderColor.getColor();
            final float red = (color >> 16 & 255) / 255.0F;
            final float green = (color >> 8 & 255) / 255.0F;
            final float blue = (color & 255) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, (float) alpha);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.polygonOffset(-3.0F, -3.0F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.disableCull();
            final float offset = (Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
            float textureVertical = (float) (height - MathHelper.fractionalPart(camera.getPos().y));
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            final float textureSize = 0.5F;
            if (borderShape instanceof final PolygonBorderShape polygon) {
                final double[] pointsX = polygon.getPointsX();
                final double[] pointsZ = polygon.getPointsZ();
                for (int i = 0; i < pointsX.length; ++i) {
                    final double p1x = pointsX[i];
                    final double p1z = pointsZ[i];
                    final double p2x = pointsX[i + 1 == pointsX.length ? 0 : i + 1];
                    final double p2z = pointsZ[i + 1 == pointsZ.length ? 0 : i + 1];
                    final Vector2 closestPoint = ShapeUtil.closestPointOnLine(posX, posZ, p1x, p1z, p2x, p2z);
                    if (ShapeUtil.distanceBetweenPoints(posX, posZ, closestPoint.getX(), closestPoint.getZ()) > renderDistanceBlocks) {
                        continue;
                    }
                    final double dx = p2x - p1x;
                    final double dz = p2z - p1z;
                    final double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
                    final double unitX = distance == 0 ? 0 : dx / distance;
                    final double unitZ = distance == 0 ? 0 : dz / distance;
                    final double absUnitX = Math.abs(unitX);
                    final double absUnitZ = Math.abs(unitZ);
                    final double distanceFromStartX = Math.abs(closestPoint.getX() - p1x);
                    final double distanceFromStartZ = Math.abs(closestPoint.getZ() - p1z);
                    final long unitsFromStartX = absUnitX == 0 ? 0 : (long) (distanceFromStartX / absUnitX);
                    final long unitsFromStartZ = absUnitZ == 0 ? 0 : (long) (distanceFromStartZ / absUnitZ);
                    final double closestPointAdjustedX = p1x + unitsFromStartX * unitX;
                    final double closestPointAdjustedZ = p1z + unitsFromStartZ * unitZ;
                    final double startX = clamp(closestPointAdjustedX - renderDistanceBlocks * unitX, p1x, p2x);
                    final double startZ = clamp(closestPointAdjustedZ - renderDistanceBlocks * unitZ, p1z, p2z);
                    final double stopX = clamp(closestPointAdjustedX + renderDistanceBlocks * unitX, p1x, p2x);
                    final double stopZ = clamp(closestPointAdjustedZ + renderDistanceBlocks * unitZ, p1z, p2z);
                    double remainingX;
                    double remainingZ;
                    double x2;
                    double z2;
                    float texturePosition = 0.0F;
                    float textureDistance;
                    double x = startX;
                    double z = startZ;
                    while (true) {
                        remainingX = stopX - x;
                        remainingZ = stopZ - z;
                        if (Math.abs(remainingX) <= absUnitX || Math.abs(remainingZ) <= absUnitZ) {
                            final float remainingDistance = (float) Math.sqrt(Math.pow(remainingX, 2) + Math.pow(remainingZ, 2));
                            textureDistance = remainingDistance * textureSize;
                            x2 = x + remainingX;
                            z2 = z + remainingZ;
                            addWall(bufferBuilder, height, posX, posZ, x, z, x2, z2, offset, texturePosition, textureDistance, textureVertical);
                            break;
                        } else {
                            textureDistance = textureSize;
                            x2 = x + unitX;
                            z2 = z + unitZ;
                            addWall(bufferBuilder, height, posX, posZ, x, z, x2, z2, offset, texturePosition, textureDistance, textureVertical);
                        }
                        x += unitX;
                        z += unitZ;
                        texturePosition += textureSize;
                    }
                }
            } else if (borderShape instanceof final EllipseBorderShape ellipse) {
                final double centerX = ellipse.getCenterX();
                final double centerZ = ellipse.getCenterZ();
                final double radiusX = ellipse.getRadiusX();
                final double radiusZ = ellipse.getRadiusZ();
                final double radius = Math.min(radiusX, radiusZ);
                final double angle = Math.acos((2 * radius * radius - 1) / (2 * radius * radius));
                final double minAngle;
                final double maxAngle;
                if (radius > renderDistanceBlocks) {
                    final double cameraAngle = Math.atan2(radiusX * (posZ - centerZ), radiusZ * (posX - centerX));
                    final double cameraAngleAdjusted = Math.floor(cameraAngle / angle) * angle;
                    final double arcAngle = renderDistanceBlocks / radius;
                    minAngle = cameraAngleAdjusted - arcAngle;
                    maxAngle = cameraAngleAdjusted + arcAngle;
                } else {
                    minAngle = 0D;
                    maxAngle = 2 * Math.PI;
                }
                final float texturePosition = 0.0F;
                float textureHorizontal;
                double a = minAngle;
                double b = minAngle + angle;
                while (a < maxAngle) {
                    final Vector2 pointA = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, a);
                    if (b >= maxAngle) {
                        final Vector2 pointB = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, maxAngle);
                        final float remainingDistance = (float) ShapeUtil.distanceBetweenPoints(pointA.getX(), pointA.getZ(), pointB.getX(), pointB.getZ());
                        textureHorizontal = remainingDistance * textureSize;
                        addWall(bufferBuilder, height, posX, posZ, pointB.getX(), pointB.getZ(), pointA.getX(), pointA.getZ(), offset, texturePosition, textureHorizontal, textureVertical);
                        break;
                    } else {
                        final Vector2 pointB = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, b);
                        textureHorizontal = textureSize;
                        addWall(bufferBuilder, height, posX, posZ, pointB.getX(), pointB.getZ(), pointA.getX(), pointA.getZ(), offset, texturePosition, textureHorizontal, textureVertical);
                    }
                    a += angle;
                    b += angle;
                }
            }
            BufferRenderer.drawWithShader(bufferBuilder.end());
            RenderSystem.enableCull();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.disableBlend();
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);
        }
        ci.cancel();
    }

    private double clamp(final double value, final double p1, final double p2) {
        final double min = Math.min(p1, p2);
        final double max = Math.max(p1, p2);
        return Math.max(min, Math.min(max, value));
    }

    private void addWall(final BufferBuilder bufferBuilder, final double height, final double posX, final double posZ, final double x1, final double z1, final double x2, final double z2, final float offset, final float texturePosition, final float textureHorizontal, final float textureVertical) {
        addVertex(bufferBuilder, -height, posX, posZ, x1, z1, offset + texturePosition, offset + textureVertical);
        addVertex(bufferBuilder, -height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset + textureVertical);
        addVertex(bufferBuilder, height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset);
        addVertex(bufferBuilder, height, posX, posZ, x1, z1, offset + texturePosition, offset);
    }

    private void addVertex(final BufferBuilder bufferBuilder, final double height, final double x1, final double z1, final double x2, final double z2, final float u, final float v) {
        bufferBuilder.vertex(x2 - x1, height, z2 - z1).texture(u, v).next();
    }
}
