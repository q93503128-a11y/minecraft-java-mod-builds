package kr.moonseungjun.earthtostars.ship.client;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders the same ItemDisplay entity that owns the pilot seat and interaction hitbox. */
public final class ShipExteriorEntityRenderer extends DisplayRenderer.ItemDisplayRenderer {
    public ShipExteriorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
