package org.popcraft.chunkyborder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.popcraft.chunkyborder.packet.BorderPayload;
import org.popcraft.chunkyborder.platform.Config;
import org.popcraft.chunkyborder.shape.BorderShape;
import org.popcraft.chunkyborder.util.BorderColor;
import org.popcraft.chunkyborder.util.ClientBorder;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ChunkyBorderFabricClient implements ClientModInitializer {
    private static final Map<Identifier, BorderShape> borderShapes = new ConcurrentHashMap<>();
    private static Config config;

    public static BorderShape getBorderShape(final Identifier identifier) {
        return ChunkyBorderFabricClient.borderShapes.get(identifier);
    }

    public static Config getConfig() {
        return ChunkyBorderFabricClient.config;
    }

    public static void setConfig(final Config config) {
        ChunkyBorderFabricClient.config = config;
    }

    @Override
    public void onInitializeClient() {
        final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chunkyborder/config.json");
        ChunkyBorderFabricClient.setConfig(new FabricConfig(configPath));
        BorderColor.parseColor(config.visualizerColor());
        PayloadTypeRegistry.playS2C().register(BorderPayload.ID, CustomPayload.codecOf(BorderPayload::write, BorderPayload::new));
        ClientPlayNetworking.registerGlobalReceiver(BorderPayload.ID, (borderPayload, context) -> {
            final ClientBorder clientBorder = borderPayload.getBorder();
            if (clientBorder.worldKey() == null) {
                return;
            }
            final Identifier identifier = Identifier.tryParse(clientBorder.worldKey());
            if (identifier == null) {
                return;
            }
            final BorderShape borderShape = clientBorder.borderShape();
            if (borderShape == null) {
                ChunkyBorderFabricClient.borderShapes.remove(identifier);
            } else {
                ChunkyBorderFabricClient.borderShapes.put(identifier, borderShape);
            }
        });
    }
}
