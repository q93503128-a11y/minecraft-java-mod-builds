package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Casting pose hook retained for compatibility with the regalia renderer.
 *
 * Alpha.34 deliberately draws no synthetic player-body geometry here.  The previous implementation
 * built filled-box arms, straps and blade-like quads after the vanilla player render.  Those quads
 * were not attached to the avatar skeleton and appeared as large black/white wedges in third person.
 * Actual casting presentation belongs to ArcaneSigilDirector / SpellCinematicDirector; robe motion
 * can still consume the casting-family state extracted by ArcaneGearRenderer.
 */
final class ArcaneCastingPerformance {
    private ArcaneCastingPerformance() {}

    static void render(PoseStack stack, RenderPlayerEvent.Post<?> event, int style,
                       int family, float progress, boolean release) {
        // Intentionally empty: never overlay fake limbs or debug-filled geometry on the player.
    }
}
