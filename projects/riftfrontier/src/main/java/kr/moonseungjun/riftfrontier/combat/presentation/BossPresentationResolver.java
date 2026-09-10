package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable Cobblemon-style logical presentation resolver.
 * It maps authoritative semantic state to authored logical asset keys and deliberately owns no timing.
 */
public final class BossPresentationResolver {
    private final Map<BossPresentationProfile.Context, BossPresentationProfile> profilesByContext;
    private final ValidatedBossCombatSemantics semanticAuthority;

    public BossPresentationResolver(Collection<BossPresentationProfile> profiles) {
        this(profiles, null);
    }

    private BossPresentationResolver(
        Collection<BossPresentationProfile> profiles,
        ValidatedBossCombatSemantics semanticAuthority
    ) {
        Map<BossPresentationProfile.Context, BossPresentationProfile> indexed = new LinkedHashMap<>();
        for (BossPresentationProfile profile : Objects.requireNonNull(profiles, "profiles")) {
            Objects.requireNonNull(profile, "profile");
            BossPresentationProfile existing = indexed.putIfAbsent(profile.context(), profile);
            if (existing != null) {
                throw new IllegalArgumentException(
                    "duplicate boss presentation context " + profile.context() + " for " + existing.id() + " and " + profile.id()
                );
            }
        }
        profilesByContext = Map.copyOf(indexed);
        this.semanticAuthority = semanticAuthority;
    }

    /**
     * Creates a resolver that can only resolve the exact presentation profile proven by server combat semantics.
     * Pattern id, cue and delivery are cross-checked through the validated server attack before any asset key is emitted.
     */
    public static BossPresentationResolver validated(ValidatedBossCombatSemantics semantics) {
        ValidatedBossCombatSemantics authority = Objects.requireNonNull(semantics, "semantics");
        return new BossPresentationResolver(List.of(authority.presentationProfile()), authority);
    }

    public Optional<ResolvedPresentation> resolve(
        ContentId bossProfile,
        String variant,
        BossPresentationSemanticState state
    ) {
        Objects.requireNonNull(bossProfile, "bossProfile");
        Objects.requireNonNull(state, "state");
        if (!state.active()) return Optional.empty();

        BossPresentationProfile profile = profilesByContext.get(new BossPresentationProfile.Context(bossProfile, variant));
        if (profile == null) return Optional.empty();

        if (semanticAuthority != null) {
            if (!bossProfile.equals(semanticAuthority.bossProfile())
                || profile != semanticAuthority.presentationProfile()) {
                return Optional.empty();
            }
            final ValidatedBossCombatSemantics.ValidatedPresentation validated;
            try {
                validated = semanticAuthority.validatePresentationState(state);
            } catch (IllegalArgumentException | IllegalStateException rejected) {
                return Optional.empty();
            }
            if (!validated.presentationProfile().equals(profile.id())
                || !validated.modelKey().equals(profile.modelKey())) {
                throw new IllegalStateException("validated boss presentation profile changed after resolver construction");
            }
            return Optional.of(new ResolvedPresentation(
                validated.presentationProfile(),
                validated.modelKey(),
                validated.binding().animationKey(),
                validated.binding().vfxKey(),
                validated.binding().soundKey(),
                validated.phase(),
                state.phaseProgress(),
                state.hitWindowOpen()
            ));
        }

        AttackTimeline.Phase phase;
        try {
            phase = AttackTimeline.Phase.valueOf(state.attackPhase());
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
        if (phase == AttackTimeline.Phase.COMPLETE) return Optional.empty();

        var key = new BossPresentationProfile.BindingKey(state.presentationCue(), state.delivery(), phase);
        var binding = profile.bindings().get(key);
        if (binding == null) return Optional.empty();

        return Optional.of(new ResolvedPresentation(
            profile.id(),
            profile.modelKey(),
            binding.animationKey(),
            binding.vfxKey(),
            binding.soundKey(),
            phase,
            state.phaseProgress(),
            state.hitWindowOpen()
        ));
    }

    /** Identity-level provenance check used by prepared renderer publication. */
    public boolean isValidatedBy(ValidatedBossCombatSemantics semantics) {
        return semanticAuthority != null && semanticAuthority == semantics;
    }

    public record ResolvedPresentation(
        ContentId presentationProfile,
        ContentId modelKey,
        ContentId animationKey,
        ContentId vfxKey,
        ContentId soundKey,
        AttackTimeline.Phase phase,
        double phaseProgress,
        boolean hitWindowOpen
    ) {
        public ResolvedPresentation {
            Objects.requireNonNull(presentationProfile, "presentationProfile");
            Objects.requireNonNull(modelKey, "modelKey");
            Objects.requireNonNull(animationKey, "animationKey");
            Objects.requireNonNull(vfxKey, "vfxKey");
            Objects.requireNonNull(soundKey, "soundKey");
            Objects.requireNonNull(phase, "phase");
            if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
                throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
            }
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("resolved hit window must exactly match ACTIVE phase");
            }
        }
    }
}
