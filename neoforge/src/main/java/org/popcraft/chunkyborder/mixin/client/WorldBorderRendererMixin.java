package org.popcraft.chunkyborder.mixin.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {
    @Shadow
    @Final
    private static Identifier FORCEFIELD_LOCATION;

    @Unique
    private GpuBuffer chunkyborder$vertexBuffer;

    @Unique
    private RenderSystem.AutoStorageIndexBuffer chunkyborder$indices;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("java:S3776")
    private void renderWorldBorder(WorldBorderRenderState state, Vec3 pos, double renderDistanceBlocks, double height, CallbackInfo ci) {
        final ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return;
        }
        final BorderShape borderShape = ChunkyBorderNeoForge.getBorderShape(clientLevel.dimension().identifier());
        if (borderShape == null) {
            return;
        }
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
        if (distanceInsideBorder >= renderDistanceBlocks) {
            ci.cancel();
            return;
        }

        double alpha = 1.0D - distanceInsideBorder / renderDistanceBlocks;
        alpha = Mth.clamp(Math.pow(alpha, 4.0D), 0.0D, 1.0D);
        final float offset = (Util.getMillis() % 3000L) / 3000.0F;
        final float textureVertical = (float) (height - Mth.frac(pos.y()));
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 1024)) {
            final BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
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
            final MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                try (meshData) {
                    drawCustomBorder(meshData, pos, alpha);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeCustomBorderBuffer(CallbackInfo ci) {
        if (chunkyborder$vertexBuffer != null) {
            chunkyborder$vertexBuffer.close();
            chunkyborder$vertexBuffer = null;
        }
    }

    @Unique
    private void drawCustomBorder(final MeshData meshData, final Vec3 pos, final double alpha) {
        final Minecraft minecraft = Minecraft.getInstance();
        final int indexCount = meshData.drawState().indexCount();
        if (indexCount <= 0) {
            return;
        }
        final long vertexBufferSize = meshData.vertexBuffer().remaining();
        if (chunkyborder$vertexBuffer == null || chunkyborder$vertexBuffer.isClosed() || chunkyborder$vertexBuffer.size() < vertexBufferSize) {
            if (chunkyborder$vertexBuffer != null && !chunkyborder$vertexBuffer.isClosed()) {
                chunkyborder$vertexBuffer.close();
            }
            chunkyborder$vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "ChunkyBorder custom border vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    vertexBufferSize
            );
        }
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(chunkyborder$vertexBuffer.slice(0, vertexBufferSize), meshData.vertexBuffer());
        if (chunkyborder$indices == null) {
            chunkyborder$indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        }
        final GpuBuffer indexBuffer = chunkyborder$indices.getBuffer(indexCount);
        final AbstractTexture texture = minecraft.getTextureManager().getTexture(FORCEFIELD_LOCATION);
        final RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        final RenderTarget weatherTarget = minecraft.levelRenderer.weatherTarget();
        final GpuTextureView colorTexture = weatherTarget == null ? mainTarget.getColorTextureView() : weatherTarget.getColorTextureView();
        final GpuTextureView depthTexture = weatherTarget == null ? mainTarget.getDepthTextureView() : weatherTarget.getDepthTextureView();
        final int color = BorderColor.getColor();
        final GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(),
                new Vector4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (float) alpha),
                new Vector3f(0.0F, (float) -pos.y(), 0.0F),
                new Matrix4f()
        );
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "ChunkyBorder custom world border",
                colorTexture,
                Optional.empty(),
                depthTexture,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(RenderPipelines.WORLD_BORDER);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.setIndexBuffer(indexBuffer, chunkyborder$indices.type());
            renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            renderPass.setVertexBuffer(0, chunkyborder$vertexBuffer.slice(0, vertexBufferSize));
            renderPass.drawIndexed(0, 0, indexCount, 1, 0);
        }
    }

    @Unique
    private double clamp(final double value, final double p1, final double p2) {
        final double min = Math.min(p1, p2);
        final double max = Math.max(p1, p2);
        return Math.max(min, Math.min(max, value));
    }

    @Unique
    private void addWall(final BufferBuilder bufferBuilder, final double height, final double posX, final double posZ, final double x1, final double z1, final double x2, final double z2, final float offset, final float texturePosition, final float textureHorizontal, final float textureVertical) {
        addVertex(bufferBuilder, -height, posX, posZ, x1, z1, offset + texturePosition, offset + textureVertical);
        addVertex(bufferBuilder, -height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset + textureVertical);
        addVertex(bufferBuilder, height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset);
        addVertex(bufferBuilder, height, posX, posZ, x1, z1, offset + texturePosition, offset);
    }

    @Unique
    private void addVertex(final BufferBuilder bufferBuilder, final double height, final double x1, final double z1, final double x2, final double z2, final float u, final float v) {
        bufferBuilder.addVertex((float) (x2 - x1), (float) height, (float) (z2 - z1)).setUv(u, v);
    }
}
