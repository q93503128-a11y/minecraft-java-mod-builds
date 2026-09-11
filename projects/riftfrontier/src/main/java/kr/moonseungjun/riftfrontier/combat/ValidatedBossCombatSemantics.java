package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.*;

/**
 * Fail-closed validation capability joining server boss semantics to logical presentation semantics.
 *
 * <p>Validation proves only references, phase coverage and logical animation-key coverage. It never
 * authorizes timing, damage, hit volumes, source clips, materials or render state.</p>
 */
public final class ValidatedBossCombatSemantics {
    private final BossCombatSemanticProfile semantics;
    private final BossPresentationProfile presentation;
    private final Map<ContentId, CoreDefinition.AttackPattern> attacks;
    private final OptionalLong publishedGeneration;

    private ValidatedBossCombatSemantics(
        BossCombatSemanticProfile semantics,
        BossPresentationProfile presentation,
        Map<ContentId, CoreDefinition.AttackPattern> attacks,
        OptionalLong publishedGeneration
    ) {
        this.semantics = semantics;
        this.presentation = presentation;
        this.attacks = Map.copyOf(attacks);
        this.publishedGeneration = Objects.requireNonNull(publishedGeneration, "publishedGeneration");
    }

    public static ValidatedBossCombatSemantics validate(
        CombatRuntimeCatalog catalog,
        BossCombatSemanticProfile semantics,
        BossPresentationProfile presentation
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(semantics, "semantics");
        Objects.requireNonNull(presentation, "presentation");

        CoreDefinition.BossProfile boss = catalog.requireBossProfile(semantics.bossProfile());
        if (!presentation.bossProfile().equals(boss.id())) {
            throw new IllegalArgumentException("presentation profile targets a different boss: " + presentation.bossProfile());
        }

        Set<Integer> expectedPhases = new TreeSet<>();
        for (int phase = 1; phase <= boss.phaseCount(); phase++) expectedPhases.add(phase);
        if (!semantics.phaseAttackPatterns().keySet().equals(expectedPhases)) {
            throw new IllegalArgumentException(
                "phase attack pools must exactly cover 1.." + boss.phaseCount() + ": " + semantics.phaseAttackPatterns().keySet()
            );
        }

        Map<ContentId, CoreDefinition.AttackPattern> resolved = new LinkedHashMap<>();
        for (Map.Entry<Integer, Set<ContentId>> entry : semantics.phaseAttackPatterns().entrySet()) {
            for (ContentId attackId : entry.getValue()) {
                if (!boss.attackPatterns().contains(attackId)) {
                    throw new IllegalArgumentException("phase " + entry.getKey() + " uses attack outside boss profile: " + attackId);
                }
                CoreDefinition.AttackPattern attack = catalog.requireAttackPattern(attackId);
                resolved.putIfAbsent(attackId, attack);
                requireLogicalAnimationCoverage(presentation, attack);
            }
        }
        return new ValidatedBossCombatSemantics(
            semantics,
            presentation,
            resolved,
            catalog.publishedGeneration()
        );
    }

    private static void requireLogicalAnimationCoverage(
        BossPresentationProfile presentation,
        CoreDefinition.AttackPattern attack
    ) {
        for (AttackTimeline.Phase phase : presentationPhases()) {
            BossPresentationProfile.BindingKey selector = new BossPresentationProfile.BindingKey(
                attack.presentationCue(), attack.delivery(), phase
            );
            BossPresentationProfile.AssetBinding binding = presentation.bindings().get(selector);
            if (binding == null) {
                throw new IllegalArgumentException("missing logical presentation binding for " + attack.id() + " / " + phase);
            }
        }
    }

    private static List<AttackTimeline.Phase> presentationPhases() {
        return List.of(
            AttackTimeline.Phase.TELEGRAPH,
            AttackTimeline.Phase.ACTIVE,
            AttackTimeline.Phase.RECOVERY
        );
    }

    public ContentId bossProfile() { return semantics.bossProfile(); }

    public BossPresentationProfile presentationProfile() { return presentation; }

    /**
     * Generation of the atomically published content snapshot that produced this semantic capability.
     * Detached fixture catalogs intentionally return empty and therefore do not masquerade as published authority.
     */
    public OptionalLong publishedGeneration() { return publishedGeneration; }

    /**
     * Final retained-capability fence used immediately before presentation delivery.
     * A capability derived from a published snapshot is invalid as soon as another generation becomes authoritative.
     */
    public void requirePublishedGeneration(long currentGeneration) {
        if (publishedGeneration.isEmpty()) return;
        long expected = publishedGeneration.getAsLong();
        if (currentGeneration != expected) {
            throw new IllegalStateException(
                "Validated boss semantics generation is stale: expected " + expected + ", current " + currentGeneration
            );
        }
    }

    public List<ContentId> candidateAttacks(int phase) {
        return semantics.attacksForPhase(phase).stream().sorted().toList();
    }

    /**
     * Exact logical animation-key set required by the already validated server-authoritative attack semantics.
     *
     * <p>This is intentionally narrower than a whole boss animation inventory: idle, hit-react, death and other
     * non-attack presentation may exist separately. Callers preparing attack animation publication must at least
     * prove coverage of this immutable set.</p>
     */
    public Set<ContentId> requiredLogicalAnimationKeys() {
        TreeSet<ContentId> required = new TreeSet<>();
        for (CoreDefinition.AttackPattern attack : attacks.values()) {
            for (AttackTimeline.Phase phase : presentationPhases()) {
                BossPresentationProfile.BindingKey selector = new BossPresentationProfile.BindingKey(
                    attack.presentationCue(), attack.delivery(), phase
                );
                BossPresentationProfile.AssetBinding binding = presentation.bindings().get(selector);
                if (binding == null) {
                    throw new IllegalStateException("validated presentation binding disappeared: " + selector.selector());
                }
                required.add(binding.animationKey());
            }
        }
        return Collections.unmodifiableSet(required);
    }

    public ContentId logicalAnimationKey(ContentId attackId, AttackTimeline.Phase phase) {
        return validatedPresentation(attackId, phase).binding().animationKey();
    }

    /**
     * Resolves one network/client semantic state only when it still describes the exact validated server attack.
     *
     * <p>The pattern id is authoritative. Cue and delivery are redundant communication fields and must match that
     * pattern rather than being allowed to select another profile entry independently. This prevents a client render
     * resolver from accepting a semantically unrelated but otherwise valid cue/delivery pair.</p>
     */
    public ValidatedPresentation validatePresentationState(BossPresentationSemanticState state) {
        Objects.requireNonNull(state, "state");
        if (!state.active()) throw new IllegalArgumentException("inactive boss presentation has no attack binding");

        ContentId attackId = ContentId.parse(state.patternId());
        CoreDefinition.AttackPattern attack = attacks.get(attackId);
        if (attack == null) {
            throw new IllegalArgumentException("presentation attack was not validated for this boss semantic profile: " + attackId);
        }

        final AttackTimeline.Phase phase;
        try {
            phase = AttackTimeline.Phase.valueOf(state.attackPhase());
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("invalid boss presentation attack phase: " + state.attackPhase(), invalid);
        }
        if (phase == AttackTimeline.Phase.COMPLETE) {
            throw new IllegalArgumentException("COMPLETE has no active presentation binding");
        }
        if (!attack.presentationCue().equals(state.presentationCue())) {
            throw new IllegalArgumentException(
                "presentation cue does not match authoritative attack " + attackId + ": " + state.presentationCue()
            );
        }
        if (!attack.delivery().equals(state.delivery())) {
            throw new IllegalArgumentException(
                "presentation delivery does not match authoritative attack " + attackId + ": " + state.delivery()
            );
        }
        return validatedPresentation(attackId, phase);
    }

    private ValidatedPresentation validatedPresentation(ContentId attackId, AttackTimeline.Phase phase) {
        Objects.requireNonNull(attackId, "attackId");
        Objects.requireNonNull(phase, "phase");
        CoreDefinition.AttackPattern attack = attacks.get(attackId);
        if (attack == null) throw new IllegalArgumentException("attack was not validated for this boss semantic profile: " + attackId);
        if (phase == AttackTimeline.Phase.COMPLETE) throw new IllegalArgumentException("COMPLETE has no animation key");
        BossPresentationProfile.BindingKey selector = new BossPresentationProfile.BindingKey(
            attack.presentationCue(), attack.delivery(), phase
        );
        BossPresentationProfile.AssetBinding binding = presentation.bindings().get(selector);
        if (binding == null) throw new IllegalStateException("validated presentation binding disappeared: " + selector.selector());
        return new ValidatedPresentation(presentation.id(), presentation.modelKey(), selector, binding, phase);
    }

    /** Immutable result proving pattern/cue/delivery/phase were resolved through this validated server semantic set. */
    public record ValidatedPresentation(
        ContentId presentationProfile,
        ContentId modelKey,
        BossPresentationProfile.BindingKey selector,
        BossPresentationProfile.AssetBinding binding,
        AttackTimeline.Phase phase
    ) {
        public ValidatedPresentation {
            Objects.requireNonNull(presentationProfile, "presentationProfile");
            Objects.requireNonNull(modelKey, "modelKey");
            Objects.requireNonNull(selector, "selector");
            Objects.requireNonNull(binding, "binding");
            Objects.requireNonNull(phase, "phase");
            if (selector.phase() != phase) throw new IllegalArgumentException("selector phase must match validated phase");
        }
    }
}
