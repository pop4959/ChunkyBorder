package org.popcraft.chunkyborder.mixin;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.joml.Matrix4f;
import org.popcraft.chunkyborder.ChunkyBorderFabricClient;
import org.popcraft.chunkyborder.shape.BorderGeometry;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(WorldBorderRendering.class)
public class WorldBorderRenderingMixin {
    @Unique
    private boolean chunkyborder$forceRefreshBuffers = true;
    @Unique
    private BorderShape chunkyborder$lastBorderShape;
    @Unique
    private double chunkyborder$lastUploadedBoundWest;
    @Unique
    private double chunkyborder$lastUploadedBoundNorth;
    @Unique
    private List<BorderGeometry.Wall> chunkyborder$walls = List.of();
    @Unique
    private GpuBuffer chunkyborder$vertexBuffer;

    @Unique
    private void chunkyborder$refreshDirectionBuffer(final BorderShape borderShape, final double viewDistanceBlocks, final double z, final double x, final float farPlaneDistance, final float v1, final float v0) {
        final double boundWest = Math.floor(x - viewDistanceBlocks);
        final double boundNorth = Math.floor(z - viewDistanceBlocks);
        final List<BorderGeometry.Wall> walls = BorderGeometry.walls(borderShape, x, z, viewDistanceBlocks);
        if (!walls.isEmpty()) {
            try (final BufferAllocator bufferAllocator = new BufferAllocator(VertexFormats.POSITION_TEXTURE.getVertexSize() * 4 * walls.size())) {
                final BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                for (final BorderGeometry.Wall wall : walls) {
                    final float x1 = (float) (wall.x(0) - boundWest);
                    final float z1 = (float) (wall.z(0) - boundNorth);
                    final float x2 = (float) (wall.x(wall.length()) - boundWest);
                    final float z2 = (float) (wall.z(wall.length()) - boundNorth);
                    final float u1 = wall.uSign() * wall.u();
                    final float u2 = wall.uSign() * (wall.u() + (float) wall.length() * 0.5F);
                    bufferBuilder.vertex(x1, -farPlaneDistance, z1).texture(u1, v1);
                    bufferBuilder.vertex(x2, -farPlaneDistance, z2).texture(u2, v1);
                    bufferBuilder.vertex(x2, farPlaneDistance, z2).texture(u2, v0);
                    bufferBuilder.vertex(x1, farPlaneDistance, z1).texture(u1, v0);
                }
                try (final BuiltBuffer builtBuffer = bufferBuilder.end()) {
                    final int size = builtBuffer.getBuffer().remaining();
                    if (chunkyborder$vertexBuffer == null || chunkyborder$vertexBuffer.size() < size) {
                        if (chunkyborder$vertexBuffer != null) {
                            chunkyborder$vertexBuffer.close();
                        }
                        chunkyborder$vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "ChunkyBorder world border vertex buffer", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, size);
                    }
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(chunkyborder$vertexBuffer, builtBuffer.getBuffer(), 0);
                }
            }
        }
        this.chunkyborder$walls = walls;
        this.chunkyborder$lastBorderShape = borderShape;
        this.chunkyborder$lastUploadedBoundWest = boundWest;
        this.chunkyborder$lastUploadedBoundNorth = boundNorth;
        this.chunkyborder$forceRefreshBuffers = false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(final WorldBorder border, final Vec3d cameraPos, final double viewDistanceBlocks, final double farPlaneDistance, final CallbackInfo ci) {
        final ClientWorld clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld == null) {
            return;
        }
        final BorderShape borderShape = ChunkyBorderFabricClient.getBorderShape(clientWorld.getRegistryKey().getValue());
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        final double distanceToBorder = BorderGeometry.distanceToBorder(borderShape, cameraPos.x, cameraPos.z);
        if (distanceToBorder < viewDistanceBlocks && distanceToBorder > -viewDistanceBlocks) {
            double alpha = 1.0 - distanceToBorder / viewDistanceBlocks;
            alpha = Math.pow(alpha, 4.0);
            alpha = MathHelper.clamp(alpha, 0.0, 1.0);
            final double x = cameraPos.x;
            final double z = cameraPos.z;
            final float farPlane = (float) farPlaneDistance;
            final int color = BorderColor.getColor();
            final float red = ColorHelper.getRed(color) / 255.0F;
            final float green = ColorHelper.getGreen(color) / 255.0F;
            final float blue = ColorHelper.getBlue(color) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, (float) alpha);
            final float offset = (float) (Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
            RenderSystem.setTextureMatrix(new Matrix4f().translation(offset, offset, 0.0F));
            final float v0 = (float) (-MathHelper.fractionalPart(cameraPos.y * 0.5));
            final float v1 = v0 + farPlane;
            if (this.chunkyborder$forceRefreshBuffers || borderShape != this.chunkyborder$lastBorderShape) {
                this.chunkyborder$refreshDirectionBuffer(borderShape, viewDistanceBlocks, z, x, farPlane, v1, v0);
            }
            if (!this.chunkyborder$walls.isEmpty()) {
                RenderSystem.setModelOffset((float) (this.chunkyborder$lastUploadedBoundWest - x), (float) (-cameraPos.y), (float) (this.chunkyborder$lastUploadedBoundNorth - z));
                final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
                final AbstractTexture abstractTexture = textureManager.getTexture(WorldBorderRendering.FORCEFIELD);
                abstractTexture.setFilter(TriState.FALSE, false);
                final RenderPipeline renderPipeline = RenderPipelines.RENDERTYPE_WORLD_BORDER;
                final Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                final Framebuffer weatherFramebuffer = MinecraftClient.getInstance().worldRenderer.getWeatherFramebuffer();
                final GpuTexture colorAttachment;
                final GpuTexture depthAttachment;
                if (weatherFramebuffer != null) {
                    colorAttachment = weatherFramebuffer.getColorAttachment();
                    depthAttachment = weatherFramebuffer.getDepthAttachment();
                } else {
                    colorAttachment = framebuffer.getColorAttachment();
                    depthAttachment = framebuffer.getDepthAttachment();
                }
                final RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                final GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(6 * this.chunkyborder$walls.size());
                try (final RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(colorAttachment, OptionalInt.empty(), depthAttachment, OptionalDouble.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.getIndexType());
                    renderPass.bindSampler("Sampler0", abstractTexture.getGlTexture());
                    renderPass.setVertexBuffer(0, this.chunkyborder$vertexBuffer);
                    final ArrayList<RenderPass.RenderObject> renderObjects = new ArrayList<>();
                    int first = -1;
                    for (int i = 0; i <= this.chunkyborder$walls.size(); ++i) {
                        final boolean visible = i < this.chunkyborder$walls.size() && this.chunkyborder$walls.get(i).distanceToWall(x, z) < viewDistanceBlocks;
                        if (visible && first < 0) {
                            first = i;
                        } else if (!visible && first >= 0) {
                            renderObjects.add(new RenderPass.RenderObject(0, this.chunkyborder$vertexBuffer, indexBuffer, shapeIndexBuffer.getIndexType(), 6 * first, 6 * (i - first)));
                            first = -1;
                        }
                    }
                    renderPass.drawMultipleIndexed(renderObjects, null, null);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.resetTextureMatrix();
            RenderSystem.resetModelOffset();
        }
    }

    @Inject(method = "markBuffersDirty", at = @At("HEAD"))
    private void markBuffersDirty(final CallbackInfo ci) {
        this.chunkyborder$forceRefreshBuffers = true;
    }
}
