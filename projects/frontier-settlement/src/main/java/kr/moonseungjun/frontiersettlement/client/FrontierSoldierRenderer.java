package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client-only human soldier presentation for the supplied Frontier military body.
 *
 * The visible iron sword exists only as a render-state ItemStack. It is never inserted into the
 * server entity, settlement storage or loot tables, so Alpha.48 cannot mint an economic weapon or
 * alter the inherited Iron Golem combat attributes. Companion weapon visuals can be revisited later
 * only through an explicit physical armory contract.
 */
public final class FrontierSoldierRenderer extends HumanoidMobRenderer<FrontierSoldierEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ItemStack VISUAL_SERVICE_SWORD = new ItemStack(Items.IRON_SWORD);

    public FrontierSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(FrontierSoldierEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // Presentation-only service sword. Never call entity.setItemSlot: server/economy state stays empty.
        state.rightHandItemStack = VISUAL_SERVICE_SWORD;
        state.rightArmPose = HumanoidModel.ArmPose.ITEM;
        this.itemModelResolver.updateForLiving(
                state.rightHandItemState,
                VISUAL_SERVICE_SWORD,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                entity);
        int attackTicks = entity.getAttackAnimationTick();
        if (attackTicks > 0) state.attackTime = Math.max(state.attackTime, 1.0F - Math.min(1.0F, attackTicks / 10.0F));
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
