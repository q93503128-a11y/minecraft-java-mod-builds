package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.Map;

/**
 * Explicit GeckoLib-capable render state for Campfire residents.
 *
 * <p>GeckoLib normally injects GeoRenderState into vanilla render-state classes
 * at runtime. Declaring it directly here also gives Java's type system the
 * bound required by RenderPassInfo during compilation.</p>
 */
public final class ResidentEntityRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new Reference2ObjectOpenHashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }
}
