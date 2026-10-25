package dev.moonseungjun.augmentationsurvival.client.render;

import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;

/** Owner-shaped presentation for the temporary combat clone. */
public final class CombatCloneRenderer extends HumanoidMobRenderer<
        CombatCloneEntity,
        AvatarRenderState,
        PlayerModel> {
    private final PlayerModel wideModel;
    private final PlayerModel slimModel;

    public CombatCloneRenderer(EntityRendererProvider.Context context) {
        this(context,
                new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false),
                new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true));
    }

    private CombatCloneRenderer(EntityRendererProvider.Context context, PlayerModel wideModel, PlayerModel slimModel) {
        super(context, wideModel, 0.45F);
        this.wideModel = wideModel;
        this.slimModel = slimModel;
        addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()
        ));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    public void extractRenderState(CombatCloneEntity entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = entity.ownerId() == null ? DefaultPlayerSkin.getDefaultSkin() : DefaultPlayerSkin.get(entity.ownerId());
        if (entity.ownerId() != null && Minecraft.getInstance().getConnection() != null) {
            var playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(entity.ownerId());
            if (playerInfo != null) state.skin = playerInfo.getSkin();
        }

        state.showHat = entity.modelPartShown(PlayerModelPart.HAT);
        state.showJacket = entity.modelPartShown(PlayerModelPart.JACKET);
        state.showLeftSleeve = entity.modelPartShown(PlayerModelPart.LEFT_SLEEVE);
        state.showRightSleeve = entity.modelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        state.showLeftPants = entity.modelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        state.showRightPants = entity.modelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        state.showCape = false;

        model = state.skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
    }
}
