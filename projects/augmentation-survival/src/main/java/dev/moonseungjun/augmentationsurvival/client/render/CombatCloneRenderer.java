package dev.moonseungjun.augmentationsurvival.client.render;

import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

/** Owner-shaped presentation for the temporary combat clone. */
public final class CombatCloneRenderer extends HumanoidMobRenderer<
        CombatCloneEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>> {
    private static final Identifier FALLBACK_TEXTURE = DefaultPlayerSkin.getDefaultTexture();

    public CombatCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.45F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()
        ));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public void extractRenderState(CombatCloneEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.ownerId() == null || Minecraft.getInstance().getConnection() == null) return;
        var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(entity.ownerId());
        if (playerInfo != null) {
            state.skin = playerInfo.getSkin();
        }
    }
}
