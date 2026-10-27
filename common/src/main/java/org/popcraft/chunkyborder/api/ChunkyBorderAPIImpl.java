package org.popcraft.chunkyborder.api;

import org.popcraft.chunky.Selection;
import org.popcraft.chunky.platform.World;
import org.popcraft.chunky.util.Input;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.BorderWrapType;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.api.event.border.BorderChangeEvent;
import org.popcraft.chunkyborder.api.event.border.BorderRedirectEvent;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("ClassCanBeRecord")
public class ChunkyBorderAPIImpl implements ChunkyBorderAPI {
    private final ChunkyBorder chunkyBorder;

    public ChunkyBorderAPIImpl(final ChunkyBorder chunkyBorder) {
        this.chunkyBorder = chunkyBorder;
    }

    @Override
    public int version() {
        return 0;
    }

    @Override
    public Optional<Border> getBorder(final String world) {
        return chunkyBorder.getBorder(world).map(BorderData::toApi);
    }

    @Override
    public boolean addBorder(final String world, final String shape, final double centerX, final double centerZ, final double radiusX, final double radiusZ) {
        final World implWorld = Input.tryWorld(chunkyBorder.getChunky(), world).orElse(null);
        final String implShape = Input.tryShape(shape).orElse(null);
        if (implWorld == null || implShape == null || radiusX < 0 || radiusZ < 0) {
            return false;
        }
        final Selection selection = Selection.builder(chunkyBorder.getChunky(), implWorld)
                .shape(implShape).center(centerX, centerZ)
                .radiusX(radiusX).radiusZ(radiusZ).build();
        chunkyBorder.addBorder(implWorld, new BorderData(selection));
        return true;
    }

    @Override
    public boolean removeBorder(final String world) {
        return Input.tryWorld(chunkyBorder.getChunky(), world)
                .map(chunkyBorder::removeBorder)
                .orElse(false);
    }

    @Override
    public boolean setWrap(final String world, final String wrap) {
        final boolean validWrap = wrap != null && Arrays.stream(BorderWrapType.values()).anyMatch(type -> type.name().equalsIgnoreCase(wrap));
        if (!validWrap) {
            return false;
        }
        return chunkyBorder.getBorder(world)
                .map(borderData -> {
                    chunkyBorder.setWrap(borderData, BorderWrapType.fromString(wrap));
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean isInsideBorder(final String world, final double x, final double z) {
        return chunkyBorder.getBorder(world)
                .map(borderData -> borderData.getBorder().isBounding(x, z))
                .orElse(true);
    }

    @Override
    public boolean isBypassing(final UUID player) {
        return chunkyBorder.getPlayerData(player).isBypassing();
    }

    @Override
    public void setBypassing(final UUID player, final boolean bypassing) {
        chunkyBorder.getPlayerData(player).setBypassing(bypassing);
    }

    @Override
    public void onBorderChange(final Consumer<BorderChangeEvent> listener) {
        chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, listener);
    }

    @Override
    public void onBorderRedirect(final Consumer<BorderRedirectEvent> listener) {
        chunkyBorder.getChunky().getEventBus().subscribe(BorderRedirectEvent.class, listener);
    }
}
