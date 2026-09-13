package dev.moonseungjun.fishinggame.client.fish;

import java.util.Map;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.entity.EncounterFishEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class EncounterFishRenderer
        extends MobRenderer<EncounterFishEntity, EncounterFishRenderState, EntityModel<EncounterFishRenderState>> {

    private static final Map<String, String> TEXTURES = Map.ofEntries(
            Map.entry("bluegill", "bluegill"),
            Map.entry("crucian", "crucian"),
            Map.entry("perch", "perch"),
            Map.entry("trout", "trout"),
            Map.entry("carp", "carp"),
            Map.entry("largemouth", "largemouth"),
            Map.entry("catfish", "catfish"),
            Map.entry("golden_carp", "golden_carp"),
            Map.entry("mackerel", "mackerel"),
            Map.entry("sea_bream", "sea_bream"),
            Map.entry("salmon", "salmon"),
            Map.entry("tuna", "tuna"),
            Map.entry("angler", "angler"),
            Map.entry("oarfish", "oarfish"),
            Map.entry("ancient_sturgeon", "ancient_sturgeon")
    );

    public EncounterFishRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layer,
            Function<ModelPart, EntityModel<EncounterFishRenderState>> modelFactory
    ) {
        super(context, modelFactory.apply(context.bakeLayer(layer)), 0.28f);
    }

    @Override
    public Identifier getTextureLocation(EncounterFishRenderState state) {
        String texture = TEXTURES.getOrDefault(state.speciesId, "bluegill");
        return FishingGameMod.id("textures/entity/fish/" + texture + ".png");
    }

    @Override
    public EncounterFishRenderState createRenderState() {
        return new EncounterFishRenderState();
    }

    @Override
    public void extractRenderState(EncounterFishEntity entity, EncounterFishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.speciesId = entity.speciesId();
    }

    @Override
    protected void setupRotations(EncounterFishRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        float sway = 3.2f * Mth.sin(0.6f * state.ageInTicks);
        poseStack.mulPose(Axis.YP.rotationDegrees(sway));
        poseStack.translate(0.0f, 0.0f, -0.28f);
    }
}
