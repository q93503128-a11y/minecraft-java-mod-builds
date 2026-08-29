package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/** Villager-shaped presentation only for Frontier's independent civilian entity. */
public final class FrontierWorkerRenderer extends MobRenderer<FrontierWorkerEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier VILLAGER_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public FrontierWorkerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(FrontierWorkerEntity entity, VillagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.isUnhappy = false;
        state.villagerData = null;
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return VILLAGER_TEXTURE;
    }
}
