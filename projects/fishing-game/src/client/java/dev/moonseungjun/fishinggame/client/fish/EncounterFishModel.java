package dev.moonseungjun.fishinggame.client.fish;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Tail animation shell for encounter-only fish.
 *
 * Geometry used by subclasses is adapted from Sea Life's MIT-licensed fish models.
 * See META-INF/licenses/fishinggame/sea-life-mit.txt and THIRD_PARTY_ASSETS.md.
 */
public class EncounterFishModel extends EntityModel<EncounterFishRenderState> {
    private final ModelPart tail;

    public EncounterFishModel(ModelPart root) {
        super(root);
        this.tail = root.hasChild("body_back") ? root.getChild("body_back") : root.getChild("tail");
    }

    @Override
    public void setupAnim(EncounterFishRenderState state) {
        super.setupAnim(state);
        float speed = state.isInWater ? 1.0f : 1.7f;
        float amount = state.isInWater ? 1.0f : 1.3f;
        tail.yRot = -amount * 0.35f * Mth.sin(speed * 0.6f * state.ageInTicks);
    }
}
