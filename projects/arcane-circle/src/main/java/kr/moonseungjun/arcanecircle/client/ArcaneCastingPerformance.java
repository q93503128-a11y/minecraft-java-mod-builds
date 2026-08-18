package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Casting pose hook retained for compatibility with the regalia renderer.
 *
 * This hook deliberately draws no synthetic player-body geometry.  Filled-box limbs, straps and
 * blade-like quads are not attached to the avatar skeleton and create third-person artifacts.
 * Actual spell presentation is owned exclusively by the explicit ManualSpellVisuals registry;
 * robe motion may still consume the manually assigned casting-family state.
 */
final class ArcaneCastingPerformance {
    private ArcaneCastingPerformance() {}

    static void render(PoseStack stack, RenderPlayerEvent.Post<?> event, int style,
                       int family, float progress, boolean release) {
        // Intentionally empty: never overlay fake limbs or debug-filled geometry on the player.
    }
}
