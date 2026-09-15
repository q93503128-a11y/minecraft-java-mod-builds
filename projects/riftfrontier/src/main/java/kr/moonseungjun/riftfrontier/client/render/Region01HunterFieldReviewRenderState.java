package kr.moonseungjun.riftfrontier.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Local-only clock for the explicit Hunter Alien field-review actor. */
public final class Region01HunterFieldReviewRenderState extends EntityRenderState {
    private float previewTimeSeconds;
    public float previewTimeSeconds() { return previewTimeSeconds; }
    void setPreviewTimeSeconds(float value) {
        if (!Float.isFinite(value) || value < 0.0F) throw new IllegalArgumentException("preview time must be finite and >= 0");
        previewTimeSeconds = value;
    }
}
