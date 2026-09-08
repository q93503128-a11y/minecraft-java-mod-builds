package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;

import java.util.Objects;
import java.util.Optional;

/**
 * Final fail-closed bridge from authoritative semantic boss state to physically validated render resources.
 *
 * <p>This boundary deliberately owns no timers or animation state. It consumes the current logical content
 * resolver and the atomically published client asset selection only when both were produced for the exact same
 * content generation. A content reload can therefore never combine a new logical presentation profile with a
 * stale physical asset selection. Missing profiles, missing bindings, inactive assets, generation mismatch, and
 * unknown logical keys all resolve to an empty result rather than guessing a fallback.</p>
 */
public final class BossPresentationRenderResolver {
    private BossPresentationRenderResolver() {}

    /** Resolves against the currently published content and client-resource snapshots. */
    public static Optional<BossPresentationAssetSelection.PhysicalPresentation> resolveCurrent(
        ContentId bossProfile,
        String variant,
        BossPresentationSemanticState state
    ) {
        Objects.requireNonNull(bossProfile, "bossProfile");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(state, "state");

        return ContentRuntime.current().flatMap(content -> resolve(
            content.generation(),
            content.bossPresentationResolver(),
            BossPresentationClientAssetRuntime.current(),
            bossProfile,
            variant,
            state
        ));
    }

    /**
     * Pure/testable resolution boundary. Generation equality is mandatory before logical or physical resolution.
     */
    public static Optional<BossPresentationAssetSelection.PhysicalPresentation> resolve(
        long contentGeneration,
        BossPresentationResolver logicalResolver,
        BossPresentationClientAssetRuntime.Snapshot clientAssets,
        ContentId bossProfile,
        String variant,
        BossPresentationSemanticState state
    ) {
        if (contentGeneration < 0) throw new IllegalArgumentException("contentGeneration must be >= 0");
        Objects.requireNonNull(logicalResolver, "logicalResolver");
        Objects.requireNonNull(clientAssets, "clientAssets");
        Objects.requireNonNull(bossProfile, "bossProfile");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(state, "state");

        if (clientAssets.contentGeneration() != contentGeneration) return Optional.empty();

        return clientAssets.selection().flatMap(selection ->
            logicalResolver.resolve(bossProfile, variant, state).flatMap(selection::resolve)
        );
    }
}
