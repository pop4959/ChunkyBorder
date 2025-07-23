package org.popcraft.chunkyborder.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(WorldBorderRendering.class)
public class WorldBorderRenderingMixin {
    @Shadow
    @Final
    private static Identifier FORCEFIELD;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("java:S3776")
    private void render(WorldBorder border, Vec3d pos, double renderDistanceBlocks, double height, CallbackInfo ci) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        final BorderShape borderShape = ChunkyBorderFabricClient.getBorderShape(client.world.getRegistryKey().getValue());
        if (borderShape == null) return;

        final double posX = pos.getX();
        final double posZ = pos.getZ();
        double distanceInsideBorder = Double.MAX_VALUE;

        // Calculate distance to polygon/ellipse border
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
            // Calculate border color and alpha
            double alpha = 1.0D - distanceInsideBorder / renderDistanceBlocks;
            alpha = Math.pow(alpha, 4.0D);
            alpha = clamp(alpha, 0.0D, 1.0D);
            final int color = BorderColor.getColor();
            final float red = (color >> 16 & 255) / 255.0F;
            final float green = (color >> 8 & 255) / 255.0F;
            final float blue = (color & 255) / 255.0F;

            // Get forcefield texture as GpuTextureView
            TextureManager textureManager = client.getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(FORCEFIELD);
            abstractTexture.setUseMipmaps(false);
            GpuTextureView textureView = abstractTexture.getGlTextureView();

            // Get framebuffer attachments
            Framebuffer framebuffer = client.getFramebuffer();
            Framebuffer weatherFramebuffer = client.worldRenderer.getWeatherFramebuffer();
            GpuTextureView colorView, depthView;
            if (weatherFramebuffer != null) {
                colorView = weatherFramebuffer.getColorAttachmentView();
                depthView = weatherFramebuffer.getDepthAttachmentView();
            } else {
                colorView = framebuffer.getColorAttachmentView();
                depthView = framebuffer.getDepthAttachmentView();
            }

            // Animation and texture offsets
            final float offset = (Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
            float textureVertical = (float) (height - MathHelper.fractionalPart(pos.getY()));
            final float textureSize = 0.5F;

            // Build the vertex buffer for border walls
            try (BufferAllocator allocator = BufferAllocator.method_72201(VertexFormats.POSITION_TEXTURE.getVertexSize() * 4096)) {
                BufferBuilder bufferBuilder = new BufferBuilder(allocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

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

                try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                    // Upload buffer to GPU
                    GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "ChunkyBorder VertexBuffer",
                            40, // alignment
                            builtBuffer.getBuffer().remaining()
                    );
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(), builtBuffer.getBuffer());

                    // Uniform data: modelview, color, translation, tex offset, unused
                    GpuBufferSlice dynamicUniform = RenderSystem.getDynamicUniforms().write(
                            RenderSystem.getModelViewMatrix(),
                            new Vector4f(red, green, blue, (float) alpha),
                            new Vector3f(0.0F, (float) -pos.getY(), 0.0F),
                            new Matrix4f().translation(offset, offset, 0.0F),
                            0.0F
                    );

                    // Setup pipeline
                    RenderPipeline pipeline = RenderPipelines.RENDERTYPE_WORLD_BORDER;

                    // Open render pass and draw
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "ChunkyBorder Border Pass", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                        pass.setPipeline(pipeline);
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", dynamicUniform);
                        pass.setVertexBuffer(0, gpuBuffer);
                        pass.bindSampler("Sampler0", textureView);

                        // --- Indexed drawing for QUADS ---
                        int vertexCount = builtBuffer.getDrawParameters().vertexCount();
                        int indexCount = (vertexCount / 4) * 6;
                        RenderSystem.ShapeIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                        pass.setIndexBuffer(indexBuffer.getIndexBuffer(vertexCount), indexBuffer.getIndexType());
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

    private void addWall(final BufferBuilder bufferBuilder, final double height, final double posX, final double posZ, final double x1, final double z1, final double x2, final double z2, final float offset, final float texturePosition, final float textureHorizontal, final float textureVertical) {
        addVertex(bufferBuilder, -height, posX, posZ, x1, z1, offset + texturePosition, offset + textureVertical);
        addVertex(bufferBuilder, -height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset + textureVertical);
        addVertex(bufferBuilder, height, posX, posZ, x2, z2, offset + texturePosition + textureHorizontal, offset);
        addVertex(bufferBuilder, height, posX, posZ, x1, z1, offset + texturePosition, offset);
    }

    private void addVertex(final BufferBuilder bufferBuilder, final double height, final double x1, final double z1, final double x2, final double z2, final float u, final float v) {
        bufferBuilder.vertex((float) (x2 - x1), (float) height, (float) (z2 - z1)).texture(u, v);
    }
}