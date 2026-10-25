package dev.moonseungjun.augmentationsurvival.client.render;

import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Player-shaped presentation baseline for the temporary combat clone. */
public final class CombatCloneRenderer extends HumanoidMobRenderer<
        CombatCloneEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/entity/player/wide/steve.png"
    );

    public CombatCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.45F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
