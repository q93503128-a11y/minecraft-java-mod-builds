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
import net.minecraft.world.entity.player.PlayerSkin;

/** Owner-shaped presentation for the temporary combat clone. */
public final class CombatCloneRenderer extends HumanoidMobRenderer<
        CombatCloneEntity,
        CombatCloneRenderer.CloneRenderState,
        HumanoidModel<CombatCloneRenderer.CloneRenderState>> {

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
    public CloneRenderState createRenderState() {
        return new CloneRenderState();
    }

    @Override
    public Identifier getTextureLocation(CloneRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    public void extractRenderState(CombatCloneEntity entity, CloneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = entity.ownerId() == null ? DefaultPlayerSkin.getDefaultSkin() : DefaultPlayerSkin.get(entity.ownerId());
        if (entity.ownerId() == null || Minecraft.getInstance().getConnection() == null) return;
        var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(entity.ownerId());
        if (playerInfo != null) state.skin = playerInfo.getSkin();
    }

    public static final class CloneRenderState extends HumanoidRenderState {
        private PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
    }
}
