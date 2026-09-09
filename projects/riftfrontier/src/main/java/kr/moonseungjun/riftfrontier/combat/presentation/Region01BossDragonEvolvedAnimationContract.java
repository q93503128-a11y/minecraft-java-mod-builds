package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;

import java.util.List;

/** Exact source-animation contract for the selected Region 01 Dragon Evolved derivation. */
public final class Region01BossDragonEvolvedAnimationContract {
    public static final String SOURCE_SHA256 = "39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c";
    public static final String ACCEPTED_DERIVATION_SHA256 = "ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac";
    public static final List<String> EXPECTED_SOURCE_CLIPS = List.of(
        "Death",
        "Fast_Flying",
        "Flying_Idle",
        "Headbutt",
        "HitReact",
        "No",
        "Punch",
        "Yes"
    );

    private Region01BossDragonEvolvedAnimationContract() {
    }

    /**
     * Verifies only facts already established by direct source inspection. Durations/channel metrics remain runtime
     * facts reported by {@link AnimationClipInventory#metrics()} and are intentionally not guessed here.
     */
    public static AnimationClipInventory verifyImportedClips(List<AnimationClip> clips) {
        return AnimationClipInventory.fromImported(clips).requireExactNames(EXPECTED_SOURCE_CLIPS);
    }
}
