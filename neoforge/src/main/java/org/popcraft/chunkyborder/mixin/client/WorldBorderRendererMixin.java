package org.popcraft.chunkyborder.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.popcraft.chunky.platform.util.Vector2;
import org.popcraft.chunky.shape.ShapeUtil;
import org.popcraft.chunkyborder.ChunkyBorderNeoForge;
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

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {
    @Shadow @Final
    public static ResourceLocation FORCEFIELD_LOCATION;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void chunky$renderCustomBorder(WorldBorder border, Vec3 pos, double renderDistanceBlocks, double height, CallbackInfo ci) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        final BorderShape borderShape = ChunkyBorderNeoForge.getBorderShape(mc.level.dimension().location());
        if (borderShape == null) return;

        final double posX = pos.x();
        final double posZ = pos.z();
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
            alpha = Mth.clamp(alpha, 0.0D, 1.0D);
            final int color = BorderColor.getColor();
            final float red = (color >> 16 & 255) / 255.0F;
            final float green = (color >> 8 & 255) / 255.0F;
            final float blue = (color & 255) / 255.0F;

            // Get forcefield texture and views
            TextureManager textureManager = mc.getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(FORCEFIELD_LOCATION);
            abstractTexture.setUseMipmaps(false);
            GpuTextureView textureView = abstractTexture.getTextureView();

            // Get framebuffer attachments
            RenderTarget fb = mc.getMainRenderTarget();
            RenderTarget weatherFb = mc.levelRenderer.getWeatherTarget();
            GpuTextureView colorView, depthView;
            if (weatherFb != null) {
                colorView = weatherFb.getColorTextureView();
                depthView = weatherFb.getDepthTextureView();
            } else {
                colorView = fb.getColorTextureView();
                depthView = fb.getDepthTextureView();
            }

            // Animation and texture offsets
            final float offset = (float) (Util.getMillis() % 3000L) / 3000.0F;
            float textureVertical = (float) (height - Mth.frac(pos.y()));
            final float textureSize = 0.5F;

            // Build the vertex mesh
            try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4096)) {
                BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                if (borderShape instanceof final PolygonBorderShape polygon) {
                    final double[] pointsX = polygon.getPointsX();
                    final double[] pointsZ = polygon.getPointsZ();
                    for (int i = 0; i < pointsX.length; ++i) {
                        final double p1x = pointsX[i];
                        final double p1z = pointsZ[i];
                        final double p2x = pointsX[i + 1 == pointsX.length ? 0 : i + 1];
                        final double p2z = pointsZ[i + 1 == pointsZ.length ? 0 : i + 1];
                        final Vector2 closestPoint = ShapeUtil.closestPointOnLine(posX, posZ, p1x, p1z, p2x, p2z);
                        if (ShapeUtil.distanceBetweenPoints(posX, posZ, closestPoint.getX(), closestPoint.getZ()) > renderDistanceBlocks)
                            continue;
                        final double dx = p2x - p1x;
                        final double dz = p2z - p1z;
                        final double distance = Math.sqrt(dx * dx + dz * dz);
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
                        double remainingX, remainingZ, x2, z2;
                        float texturePosition = 0.0F;
                        float textureDistance;
                        double x = startX, z = startZ;
                        while (true) {
                            remainingX = stopX - x;
                            remainingZ = stopZ - z;
                            if (Math.abs(remainingX) <= absUnitX || Math.abs(remainingZ) <= absUnitZ) {
                                float remainingDistance = (float) Math.sqrt(remainingX * remainingX + remainingZ * remainingZ);
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
                    final double minAngle, maxAngle;
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
                    float texturePosition = 0.0F;
                    float textureHorizontal;
                    double a = minAngle, b = minAngle + angle;
                    while (a < maxAngle) {
                        final Vector2 pointA = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, a);
                        if (b >= maxAngle) {
                            final Vector2 pointB = ShapeUtil.pointOnEllipse(centerX, centerZ, radiusX, radiusZ, maxAngle);
                            float remainingDistance = (float) ShapeUtil.distanceBetweenPoints(pointA.getX(), pointA.getZ(), pointB.getX(), pointB.getZ());
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

                try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                    GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "ChunkyBorder VertexBuffer",
                            40,
                            meshData.vertexBuffer().remaining()
                    );
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(), meshData.vertexBuffer());

                    GpuBufferSlice dynamicUniform = RenderSystem.getDynamicUniforms().writeTransform(
                            RenderSystem.getModelViewMatrix(),
                            new Vector4f(red, green, blue, (float) alpha),
                            new Vector3f(0.0F, (float) -pos.y(), 0.0F),
                            new Matrix4f().translation(offset, offset, 0.0F),
                            0.0F
                    );

                    RenderPipeline pipeline = RenderPipelines.WORLD_BORDER;

                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "ChunkyBorder Border Pass", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                        pass.setPipeline(pipeline);
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", dynamicUniform);
                        pass.setVertexBuffer(0, gpuBuffer);
                        pass.bindSampler("Sampler0", textureView);

                        MeshData.DrawState drawState = meshData.drawState();
                        int vertexCount = drawState.vertexCount();
                        int indexCount = drawState.indexCount();
                        RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(drawState.mode());
                        pass.setIndexBuffer(indexBuffer.getBuffer(vertexCount), indexBuffer.type());
                        pass.drawIndexed(0, indexCount, 0, 1);
                    }
                }
            }
        }
        ci.cancel();
    }

    private double clamp(final double value, final double p1, final double p2) {
        final double min = Math.min(p1, p2);
        final double max = Math.max(p1, p2);
        return Math.max(min, Math.min(max, value));
    }

    private void addWall(BufferBuilder bufferBuilder, double height, double posX, double posZ, double x1, double z1, double x2, double z2, float offset, float texturePosition, float textureHorizontal, float textureVertical) {
        addVertex(bufferBuilder, -height, posX, posZ, x1, z1, offset + texturePosition, offset + textureVertical);
        addVertex(bufferBuilder, -height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset + textureVertical);
        addVertex(bufferBuilder, height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset);
        addVertex(bufferBuilder, height, posX, posZ, x1, z1, offset + texturePosition, offset);
    }

    private void addVertex(BufferBuilder bufferBuilder, double height, double x1, double z1, double x2, double z2, float u, float v) {
        bufferBuilder.addVertex((float) (x2 - x1), (float) height, (float) (z2 - z1)).setUv(u, v);
    }
}