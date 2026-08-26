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
 * Alpha.48's iron service sword remains a client-only fallback for an un-upgraded soldier. Alpha.57
 * renders the entity's real synced MAINHAND ItemStack when the automated barracks armory has physically
 * assigned one. The renderer itself never creates or inserts economic equipment.
 */
public final class FrontierSoldierRenderer extends HumanoidMobRenderer<FrontierSoldierEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    // Alpha.80 canonical trigger: keep registry-backed presentation stacks out of class initialization.
    // Do not construct registry-backed ItemStacks during class initialization. Minecraft 26.2 can load
    // entity renderer classes while item components are still being bound during the resource reload.
    private ItemStack visualServiceSword;

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
        // Renderer rule: Never call entity.setItemSlot here; the server armory owns real equipment.
        ItemStack physicalWeapon = entity.getMainHandItem();
        if (physicalWeapon.isEmpty()) {
            ItemStack serviceSword = visualServiceSword();
            state.rightHandItemStack = serviceSword;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    serviceSword,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        } else {
            state.rightHandItemStack = physicalWeapon;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    physicalWeapon,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        }
        int attackTicks = entity.getAttackAnimationTick();
        if (attackTicks > 0) state.attackTime = Math.max(state.attackTime, 1.0F - Math.min(1.0F, attackTicks / 10.0F));
    }

    private ItemStack visualServiceSword() {
        if (visualServiceSword == null) {
            // First real entity-render extraction occurs after registry/component bootstrap. This stack is
            // presentation-only and is never inserted into the entity, world, inventory, or settlement economy.
            visualServiceSword = new ItemStack(Items.IRON_SWORD);
        }
        return visualServiceSword;
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
