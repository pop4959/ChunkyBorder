package org.popcraft.chunkyborder.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
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

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {
    @Unique
    private static final double CHUNKYBORDER$RENDERING_OFFSET = 0.01;
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
    private AbstractTexture chunkyborder$texture;
    @Unique
    private GpuBuffer chunkyborder$worldBorderBuffer;

    @Unique
    private void chunkyborder$rebuildWorldBorderBuffer(final BorderShape borderShape, final double renderDistance, final double z, final double x, final float halfHeight, final float v1, final float v0) {
        final double minX = Math.floor(x - renderDistance);
        final double minZ = Math.floor(z - renderDistance);
        final List<BorderGeometry.Wall> walls = BorderGeometry.walls(borderShape, x, z, renderDistance, CHUNKYBORDER$RENDERING_OFFSET);
        if (!walls.isEmpty()) {
            try (final ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * walls.size())) {
                final BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
                for (final BorderGeometry.Wall wall : walls) {
                    final float x1 = (float) (wall.x(0) - minX);
                    final float z1 = (float) (wall.z(0) - minZ);
                    final float x2 = (float) (wall.x(wall.length()) - minX);
                    final float z2 = (float) (wall.z(wall.length()) - minZ);
                    final float u1 = wall.uSign() * wall.u();
                    final float u2 = wall.uSign() * (wall.u() + (float) wall.length() * 0.5F);
                    bufferBuilder.addVertex(x1, -halfHeight, z1).setUv(u1, v1);
                    bufferBuilder.addVertex(x2, -halfHeight, z2).setUv(u2, v1);
                    bufferBuilder.addVertex(x2, halfHeight, z2).setUv(u2, v0);
                    bufferBuilder.addVertex(x1, halfHeight, z1).setUv(u1, v0);
                }
                try (final MeshData meshData = bufferBuilder.buildOrThrow()) {
                    final int size = meshData.vertexBuffer().remaining();
                    if (this.chunkyborder$worldBorderBuffer == null || this.chunkyborder$worldBorderBuffer.size() < size) {
                        if (this.chunkyborder$worldBorderBuffer != null) {
                            this.chunkyborder$worldBorderBuffer.close();
                        }
                        this.chunkyborder$worldBorderBuffer = RenderSystem.getDevice().createBuffer(() -> "ChunkyBorder world border vertex buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, size);
                    }
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.chunkyborder$worldBorderBuffer.slice(0, size), meshData.vertexBuffer());
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

    @Inject(method = "prepare", at = @At("HEAD"), cancellable = true)
    private void prepare(final WorldBorderRenderState state, final Vec3 pos, final double renderDistance, final double depthFar, final CallbackInfo ci) {
        final BorderShape borderShape = this.chunkyborder$borderShape;
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        if (!(state.alpha <= 0.0)) {
            final double x = pos.x;
            final double z = pos.z;
            final float halfHeight = (float) depthFar;
            final float v0 = (float) (-Mth.frac(pos.y * 0.5));
            final float v1 = v0 + halfHeight;
            if (this.chunkyborder$needsRebuild || borderShape != this.chunkyborder$lastBorderShape) {
                this.chunkyborder$rebuildWorldBorderBuffer(borderShape, renderDistance, z, x, halfHeight, v1, v0);
            }
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(6 * this.chunkyborder$walls.size());
            this.chunkyborder$texture = Minecraft.getInstance().getTextureManager().getTexture(WorldBorderRenderer.FORCEFIELD_LOCATION);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(final WorldBorderRenderState state, final RenderPass renderPass, final Vec3 pos, final double renderDistance, final CallbackInfo ci) {
        if (this.chunkyborder$borderShape == null) {
            return;
        }
        ci.cancel();
        if (!(state.alpha <= 0.0) && this.chunkyborder$texture != null && !this.chunkyborder$walls.isEmpty()) {
            final RenderPipeline renderPipeline = RenderPipelines.WORLD_BORDER;
            final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            final GpuBuffer indexBuffer = indices.getBuffer();
            final GpuBufferSlice dynamicTransforms = this.chunkyborder$prepareDynamicTransforms(state, pos);
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
            this.chunkyborder$prepareRenderPass(renderPass, dynamicTransforms, indexBuffer, indices, this.chunkyborder$texture);
            this.chunkyborder$draw(renderDistance, pos.x, pos.z, indexBuffer, indices, renderPass);
        }
    }

    @Inject(method = "renderOit", at = @At("HEAD"), cancellable = true)
    private void renderOit(final WorldBorderRenderState state, final Vec3 pos, final double renderDistance, final OitStage stage, final RenderPass renderPass, final CallbackInfo ci) {
        if (this.chunkyborder$borderShape == null) {
            return;
        }
        ci.cancel();
        if (!(state.alpha <= 0.0) && this.chunkyborder$texture != null && !this.chunkyborder$walls.isEmpty()) {
            final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            final GpuBuffer indexBuffer = indices.getBuffer();
            final GpuBufferSlice dynamicTransforms = this.chunkyborder$prepareDynamicTransforms(state, pos);
            final RenderPipeline renderPipeline = RenderPipelines.OIT_WORLD_BORDER.getPipeline(stage);
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
            this.chunkyborder$prepareRenderPass(renderPass, dynamicTransforms, indexBuffer, indices, this.chunkyborder$texture);
            this.chunkyborder$draw(renderDistance, pos.x, pos.z, indexBuffer, indices, renderPass);
        }
    }

    @Unique
    private GpuBufferSlice chunkyborder$prepareDynamicTransforms(final WorldBorderRenderState state, final Vec3 pos) {
        final float red = ARGB.red(state.tint) / 255.0F;
        final float green = ARGB.green(state.tint) / 255.0F;
        final float blue = ARGB.blue(state.tint) / 255.0F;
        final float offset = (float) (Util.getMillis() % 3000L) / 3000.0F;
        return RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), new Vector4f(red, green, blue, (float) state.alpha), new Vector3f((float) (this.chunkyborder$lastMinX - pos.x), (float) (-pos.y), (float) (this.chunkyborder$lastMinZ - pos.z)), new Matrix4f().translation(offset, offset, 0.0F));
    }

    @Unique
    private void chunkyborder$prepareRenderPass(final RenderPass renderPass, final GpuBufferSlice dynamicTransforms, final GpuBuffer indexBuffer, final RenderSystem.AutoStorageIndexBuffer indices, final AbstractTexture abstractTexture) {
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", dynamicTransforms);
        renderPass.setIndexBuffer(indexBuffer, indices.type());
        renderPass.setUniform("Sampler0", abstractTexture.getTextureView(), abstractTexture.getSampler());
        renderPass.setVertexBuffer(0, this.chunkyborder$worldBorderBuffer.slice());
    }

    @Unique
    private void chunkyborder$draw(final double renderDistance, final double x, final double z, final GpuBuffer indexBuffer, final RenderSystem.AutoStorageIndexBuffer indices, final RenderPass renderPass) {
        renderPass.pushDebugGroup(() -> "World Border");
        final ArrayList<RenderPass.Draw<WorldBorderRenderer>> draws = new ArrayList<>();
        int first = -1;
        for (int i = 0; i <= this.chunkyborder$walls.size(); ++i) {
            final boolean visible = i < this.chunkyborder$walls.size() && this.chunkyborder$walls.get(i).distanceToWall(x, z) < renderDistance;
            if (visible && first < 0) {
                first = i;
            } else if (!visible && first >= 0) {
                draws.add(new RenderPass.Draw<>(0, this.chunkyborder$worldBorderBuffer, indexBuffer, indices.type(), 6 * first, 6 * (i - first), 0));
                first = -1;
            }
        }
        renderPass.drawMultipleIndexed(draws, null, null, Collections.emptyList(), (WorldBorderRenderer) (Object) this);
        renderPass.popDebugGroup();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void close(final CallbackInfo ci) {
        if (this.chunkyborder$worldBorderBuffer != null) {
            this.chunkyborder$worldBorderBuffer.close();
            this.chunkyborder$worldBorderBuffer = null;
        }
        this.chunkyborder$walls = List.of();
        this.chunkyborder$needsRebuild = true;
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
