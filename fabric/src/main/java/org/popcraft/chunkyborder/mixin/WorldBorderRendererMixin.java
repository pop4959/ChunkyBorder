package org.popcraft.chunkyborder.mixin;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {
    @Unique
    private boolean chunkyborder$needsRebuild = true;
    @Unique
    private BorderShape chunkyborder$borderShape;
    @Unique
    private BorderShape chunkyborder$lastBorderShape;
    @Unique
    private double chunkyborder$lastMinX;
    @Unique
    private double chunkyborder$lastMinZ;
    @Unique
    private List<BorderGeometry.Wall> chunkyborder$walls = List.of();
    @Unique
    private GpuBuffer chunkyborder$worldBorderBuffer;

    @Unique
    private void chunkyborder$rebuildWorldBorderBuffer(final BorderShape borderShape, final double renderDistance, final double z, final double x, final float depthFar, final float v1, final float v0) {
        final double minX = Math.floor(x - renderDistance);
        final double minZ = Math.floor(z - renderDistance);
        final List<BorderGeometry.Wall> walls = BorderGeometry.walls(borderShape, x, z, renderDistance);
        if (!walls.isEmpty()) {
            try (final ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * walls.size())) {
                final BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                for (final BorderGeometry.Wall wall : walls) {
                    final float x1 = (float) (wall.x(0) - minX);
                    final float z1 = (float) (wall.z(0) - minZ);
                    final float x2 = (float) (wall.x(wall.length()) - minX);
                    final float z2 = (float) (wall.z(wall.length()) - minZ);
                    final float u1 = wall.uSign() * wall.u();
                    final float u2 = wall.uSign() * (wall.u() + (float) wall.length() * 0.5F);
                    bufferBuilder.addVertex(x1, -depthFar, z1).setUv(u1, v1);
                    bufferBuilder.addVertex(x2, -depthFar, z2).setUv(u2, v1);
                    bufferBuilder.addVertex(x2, depthFar, z2).setUv(u2, v0);
                    bufferBuilder.addVertex(x1, depthFar, z1).setUv(u1, v0);
                }
                try (final MeshData meshData = bufferBuilder.buildOrThrow()) {
                    final int size = meshData.vertexBuffer().remaining();
                    if (chunkyborder$worldBorderBuffer == null || chunkyborder$worldBorderBuffer.size() < size) {
                        if (chunkyborder$worldBorderBuffer != null) {
                            chunkyborder$worldBorderBuffer.close();
                        }
                        chunkyborder$worldBorderBuffer = RenderSystem.getDevice().createBuffer(() -> "ChunkyBorder world border vertex buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, size);
                    }
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(chunkyborder$worldBorderBuffer.slice(0, size), meshData.vertexBuffer());
                }
            }
        }
        this.chunkyborder$walls = walls;
        this.chunkyborder$lastBorderShape = borderShape;
        this.chunkyborder$lastMinX = minX;
        this.chunkyborder$lastMinZ = minZ;
        this.chunkyborder$needsRebuild = false;
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void extract(final WorldBorder worldBorder, final float partialTick, final Vec3 pos, final double renderDistance, final WorldBorderRenderState state, final CallbackInfo ci) {
        final BorderShape borderShape = chunkyborder$getBorderShape();
        this.chunkyborder$borderShape = borderShape;
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        state.minX = worldBorder.getMinX(partialTick);
        state.maxX = worldBorder.getMaxX(partialTick);
        state.minZ = worldBorder.getMinZ(partialTick);
        state.maxZ = worldBorder.getMaxZ(partialTick);
        final double distanceToBorder = BorderGeometry.distanceToBorder(borderShape, pos.x, pos.z);
        if (distanceToBorder < renderDistance && distanceToBorder > -renderDistance) {
            state.alpha = 1.0 - distanceToBorder / renderDistance;
            state.alpha = Math.pow(state.alpha, 4.0);
            state.alpha = Mth.clamp(state.alpha, 0.0, 1.0);
            state.tint = BorderColor.getColor();
        } else {
            state.alpha = 0.0;
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(final WorldBorderRenderState state, final Vec3 pos, final double renderDistance, final double depthFar, final CallbackInfo ci) {
        final BorderShape borderShape = this.chunkyborder$borderShape;
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        if (!(state.alpha <= 0.0)) {
            final double x = pos.x;
            final double z = pos.z;
            final float halfHeight = (float) depthFar;
            final float red = ARGB.red(state.tint) / 255.0F;
            final float green = ARGB.green(state.tint) / 255.0F;
            final float blue = ARGB.blue(state.tint) / 255.0F;
            final float offset = (float) (Util.getMillis() % 3000L) / 3000.0F;
            final float v0 = (float) (-Mth.frac(pos.y * 0.5));
            final float v1 = v0 + halfHeight;
            if (this.chunkyborder$needsRebuild || borderShape != this.chunkyborder$lastBorderShape) {
                this.chunkyborder$rebuildWorldBorderBuffer(borderShape, renderDistance, z, x, halfHeight, v1, v0);
            }
            if (!this.chunkyborder$walls.isEmpty()) {
                final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                final AbstractTexture abstractTexture = textureManager.getTexture(WorldBorderRenderer.FORCEFIELD_LOCATION);
                final RenderPipeline renderPipeline = RenderPipelines.WORLD_BORDER;
                final RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
                final RenderTarget weatherTarget = Minecraft.getInstance().levelRenderer.getWeatherTarget();
                final GpuTextureView colorTexture;
                final GpuTextureView depthTexture;
                if (weatherTarget != null) {
                    colorTexture = weatherTarget.getColorTextureView();
                    depthTexture = weatherTarget.getDepthTextureView();
                } else {
                    colorTexture = mainRenderTarget.getColorTextureView();
                    depthTexture = mainRenderTarget.getDepthTextureView();
                }
                final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                final GpuBuffer indexBuffer = indices.getBuffer(6 * this.chunkyborder$walls.size());
                final GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(red, green, blue, (float) state.alpha), new Vector3f((float) (this.chunkyborder$lastMinX - x), (float) (-pos.y), (float) (this.chunkyborder$lastMinZ - z)), new Matrix4f().translation(offset, offset, 0.0F));
                try (final RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "World border", colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                    renderPass.setIndexBuffer(indexBuffer, indices.type());
                    renderPass.bindTexture("Sampler0", abstractTexture.getTextureView(), abstractTexture.getSampler());
                    renderPass.setVertexBuffer(0, this.chunkyborder$worldBorderBuffer);
                    final ArrayList<RenderPass.Draw<WorldBorderRenderer>> draws = new ArrayList<>();
                    int first = -1;
                    for (int i = 0; i <= this.chunkyborder$walls.size(); ++i) {
                        final boolean visible = i < this.chunkyborder$walls.size() && this.chunkyborder$walls.get(i).distanceToWall(x, z) < renderDistance;
                        if (visible && first < 0) {
                            first = i;
                        } else if (!visible && first >= 0) {
                            draws.add(new RenderPass.Draw<>(0, this.chunkyborder$worldBorderBuffer, indexBuffer, indices.type(), 6 * first, 6 * (i - first)));
                            first = -1;
                        }
                    }
                    renderPass.drawMultipleIndexed(draws, null, null, Collections.emptyList(), (WorldBorderRenderer) (Object) this);
                }
            }
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"))
    private void invalidate(final CallbackInfo ci) {
        this.chunkyborder$needsRebuild = true;
    }

    @Unique
    private static BorderShape chunkyborder$getBorderShape() {
        final ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return null;
        }
        return ChunkyBorderFabricClient.getBorderShape(clientLevel.dimension().identifier());
    }
}
