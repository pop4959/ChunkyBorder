package org.popcraft.chunkyborder.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import org.popcraft.chunkyborder.ChunkyBorderFabricClient;
import org.popcraft.chunkyborder.shape.BorderGeometry;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldBorderRendering.class)
public class WorldBorderRenderingMixin {
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
    private void render(final WorldBorder border, final Vec3d pos, final double renderDistance, final double depthFar, final CallbackInfo ci) {
        final ClientWorld clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld == null) {
            return;
        }
        final BorderShape borderShape = ChunkyBorderFabricClient.getBorderShape(clientWorld.getRegistryKey().getValue());
        if (borderShape == null) {
            return;
        }
        ci.cancel();
        final double distanceToBorder = BorderGeometry.distanceToBorder(borderShape, pos.x, pos.z);
        if (distanceToBorder < renderDistance) {
            double alpha = 1.0 - distanceToBorder / renderDistance;
            alpha = Math.pow(alpha, 4.0);
            alpha = MathHelper.clamp(alpha, 0.0, 1.0);
            final double cameraX = pos.x;
            final double cameraZ = pos.z;
            final float halfHeight = (float) depthFar;
            final RenderLayer renderLayer = RenderLayer.getWorldBorder(MinecraftClient.isFabulousGraphicsOrBetter());
            renderLayer.startDrawing();
            final int color = BorderColor.getColor();
            final float red = ColorHelper.getRed(color) / 255.0F;
            final float green = ColorHelper.getGreen(color) / 255.0F;
            final float blue = ColorHelper.getBlue(color) / 255.0F;
            RenderSystem.setShaderColor(red, green, blue, (float) alpha);
            final float offset = (float) (Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
            final float v0 = (float) (-MathHelper.fractionalPart(pos.y * 0.5));
            final float v1 = v0 + halfHeight;
            final BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
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
                        bufferBuilder.vertex(x1, -halfHeight, z1).texture(offset + wall.uSign() * u, offset + v1);
                        bufferBuilder.vertex(x2, -halfHeight, z2).texture(offset + wall.uSign() * (textureLength + u), offset + v1);
                        bufferBuilder.vertex(x2, halfHeight, z2).texture(offset + wall.uSign() * (textureLength + u), offset + v0);
                        bufferBuilder.vertex(x1, halfHeight, z1).texture(offset + wall.uSign() * u, offset + v0);
                        position++;
                    }
                }
            }
            final BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
            renderLayer.endDrawing();
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
