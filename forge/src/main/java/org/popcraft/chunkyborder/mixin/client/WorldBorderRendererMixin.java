package org.popcraft.chunkyborder.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.popcraft.chunkyborder.ChunkyBorderForge;
import org.popcraft.chunkyborder.shape.BorderGeometry;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {
    @Unique
    private BorderShape chunkyborder$lastBorderShape;
    @Unique
    private double chunkyborder$lastMinX;
    @Unique
    private double chunkyborder$lastMaxX;
    @Unique
    private double chunkyborder$lastMinZ;
    @Unique
    private double chunkyborder$lastMaxZ;
    @Unique
    private List<BorderGeometry.Wall> chunkyborder$walls = List.of();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(final WorldBorder border, final Vec3 pos, final double renderDistance, final double depthFar, final CallbackInfo ci) {
        final ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return;
        }
        final BorderShape borderShape = ChunkyBorderForge.getBorderShape(clientLevel.dimension().location());
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        final double distanceToBorder = BorderGeometry.distanceToBorder(borderShape, pos.x, pos.z);
        if (distanceToBorder < renderDistance) {
            double alpha = 1.0 - distanceToBorder / renderDistance;
            alpha = Math.pow(alpha, 4.0);
            alpha = Mth.clamp(alpha, 0.0, 1.0);
            final double cameraX = pos.x;
            final double cameraZ = pos.z;
            final float halfHeight = (float) depthFar;
            final RenderType renderType = RenderType.worldBorder(Minecraft.useShaderTransparency());
            renderType.setupRenderState();
            final int color = BorderColor.getColor();
            final float red = ARGB.red(color) / 255.0F;
            final float green = ARGB.green(color) / 255.0F;
            final float blue = ARGB.blue(color) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, (float) alpha);
            final float offset = (float) (Util.getMillis() % 3000L) / 3000.0F;
            final float v0 = (float) (-Mth.frac(pos.y * 0.5));
            final float v1 = v0 + halfHeight;
            final BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (final BorderGeometry.Wall wall : this.chunkyborder$walls(borderShape, cameraX, cameraZ, renderDistance)) {
                if (wall.distanceToWall(cameraX, cameraZ) < renderDistance) {
                    float u = wall.u();
                    for (double position = 0; position < wall.length(); u += 0.5F) {
                        final double length = Math.min(1.0, wall.length() - position);
                        final float textureLength = (float) length * 0.5F;
                        final float x1 = (float) (wall.x(position) - cameraX);
                        final float z1 = (float) (wall.z(position) - cameraZ);
                        final float x2 = (float) (wall.x(position + length) - cameraX);
                        final float z2 = (float) (wall.z(position + length) - cameraZ);
                        bufferBuilder.addVertex(x1, -halfHeight, z1).setUv(offset + wall.uSign() * u, offset + v1);
                        bufferBuilder.addVertex(x2, -halfHeight, z2).setUv(offset + wall.uSign() * (textureLength + u), offset + v1);
                        bufferBuilder.addVertex(x2, halfHeight, z2).setUv(offset + wall.uSign() * (textureLength + u), offset + v0);
                        bufferBuilder.addVertex(x1, halfHeight, z1).setUv(offset + wall.uSign() * u, offset + v0);
                        position++;
                    }
                }
            }
            final MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                BufferUploader.drawWithShader(meshData);
            }
            renderType.clearRenderState();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Unique
    private List<BorderGeometry.Wall> chunkyborder$walls(final BorderShape borderShape, final double x, final double z, final double renderDistance) {
        final double minX = Math.floor(x - renderDistance);
        final double maxX = Math.ceil(x + renderDistance);
        final double minZ = Math.floor(z - renderDistance);
        final double maxZ = Math.ceil(z + renderDistance);
        if (borderShape != this.chunkyborder$lastBorderShape || minX != this.chunkyborder$lastMinX || maxX != this.chunkyborder$lastMaxX || minZ != this.chunkyborder$lastMinZ || maxZ != this.chunkyborder$lastMaxZ) {
            this.chunkyborder$walls = BorderGeometry.walls(borderShape, x, z, renderDistance);
            this.chunkyborder$lastBorderShape = borderShape;
            this.chunkyborder$lastMinX = minX;
            this.chunkyborder$lastMaxX = maxX;
            this.chunkyborder$lastMinZ = minZ;
            this.chunkyborder$lastMaxZ = maxZ;
        }
        return this.chunkyborder$walls;
    }
}
