package kr.moonseungjun.earthtostars.ship.client;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipExteriorEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * The authoritative ride shell intentionally renders nothing itself. A separate
 * model-backed visual is kept at the exact same transform by ShipRuntimeManager.
 * This avoids coupling gameplay collision/seat behavior to the chosen art mesh.
 */
public final class ShipExteriorEntityRenderer extends EntityRenderer<ShipExteriorEntity, EntityRenderState> {
    public ShipExteriorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
