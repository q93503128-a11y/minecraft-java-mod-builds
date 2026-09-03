package io.github.q93503128.turnbound.content;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Canon-safe authoring contract for Signature Trial encounters.
 *
 * <p>The v0.4 character wiki already defines Trial objectives, but it does not provide complete encounter
 * authoring. This class defines the minimum data that must exist before a Trial may become CANON_READY.
 * The canonical map intentionally starts empty: missing content remains a visible CANON GAP instead of being
 * filled with invented enemies, bosses or NPCs.</p>
 */
public final class SignatureTrialEncounterAuthoring {
    public record EncounterSpec(
            String characterId,
            String encounterId,
            List<String> enemyIds,
            String specialEliteId,
            String trialBossId,
            String protectedNpcId
    ) {
        public EncounterSpec {
            characterId = clean(characterId);
            encounterId = clean(encounterId);
            enemyIds = enemyIds == null ? List.of() : enemyIds.stream().map(SignatureTrialEncounterAuthoring::clean).toList();
            specialEliteId = clean(specialEliteId);
            trialBossId = clean(trialBossId);
            protectedNpcId = clean(protectedNpcId);
        }
    }

    public record Validation(boolean ready, List<String> missingOrInvalid) {
        public Validation {
            missingOrInvalid = List.copyOf(missingOrInvalid);
        }

        public String blockReason() {
            return ready ? "" : String.join(" · ", missingOrInvalid);
        }
    }

    /**
     * No v0.4 Signature Trial has a complete canonical encounter definition yet.
     * Future canon may populate this map without changing the validation contract.
     */
    private static final Map<String, EncounterSpec> CANONICAL = Map.of();

    private SignatureTrialEncounterAuthoring() { }

    public static Optional<EncounterSpec> canonical(String characterId) {
        if (!SignatureTrialCatalog.contains(characterId)) {
            throw new IllegalArgumentException("No Signature Trial for " + characterId);
        }
        return Optional.ofNullable(CANONICAL.get(characterId));
    }

    public static Validation readiness(String characterId) {
        SignatureTrialCatalog.Spec trial = SignatureTrialCatalog.forCharacter(characterId);
        if (trial.canonState() == SignatureTrialCatalog.CanonState.CANON_CONTRADICTION) {
            return blocked(trial.authoringBlockReason());
        }
        EncounterSpec spec = CANONICAL.get(characterId);
        if (spec == null) {
            return blocked(trial.authoringBlockReason() + " · 필요 필드: " + requiredFields(trial.gapKind()));
        }
        return validate(characterId, spec);
    }

    public static boolean canonReady(String characterId) {
        return readiness(characterId).ready();
    }

    /** Validates a future authored encounter without registering it as canon. */
    public static Validation validate(String characterId, EncounterSpec draft) {
        SignatureTrialCatalog.Spec trial = SignatureTrialCatalog.forCharacter(characterId);
        List<String> problems = new ArrayList<>();

        if (trial.canonState() == SignatureTrialCatalog.CanonState.CANON_CONTRADICTION) {
            problems.add(trial.authoringBlockReason());
            return new Validation(false, problems);
        }
        if (draft == null) return blocked("Encounter authoring row가 없습니다.");
        if (!characterId.equals(draft.characterId())) problems.add("characterId가 대상 Trial과 일치하지 않습니다.");
        if (draft.encounterId().isBlank()) problems.add("encounterId가 필요합니다.");
        if (draft.enemyIds().isEmpty()) problems.add("enemyIds 전투 편성이 비어 있습니다.");
        if (draft.enemyIds().stream().anyMatch(String::isBlank)) problems.add("enemyIds에 빈 canonical ID가 있습니다.");

        Set<String> unique = new LinkedHashSet<>(draft.enemyIds());
        if (unique.size() != draft.enemyIds().size()) problems.add("enemyIds에 중복 canonical ID가 있습니다.");

        switch (trial.gapKind()) {
            case SPECIAL_ELITE_IDENTITY -> {
                if (draft.specialEliteId().isBlank()) {
                    problems.add("specialEliteId가 필요합니다.");
                } else if (!draft.enemyIds().contains(draft.specialEliteId())) {
                    problems.add("specialEliteId는 enemyIds 편성에 포함되어야 합니다.");
                }
            }
            case TRIAL_BOSS_IDENTITY -> {
                if (draft.trialBossId().isBlank()) {
                    problems.add("trialBossId가 필요합니다.");
                } else if (!draft.enemyIds().contains(draft.trialBossId())) {
                    problems.add("trialBossId는 enemyIds 편성에 포함되어야 합니다.");
                }
            }
            case PROTECTED_NPC_IDENTITY -> {
                if (draft.protectedNpcId().isBlank()) {
                    problems.add("protectedNpcId가 필요합니다.");
                } else if (draft.enemyIds().contains(draft.protectedNpcId())) {
                    problems.add("protectedNpcId를 enemyIds로 사용할 수 없습니다.");
                }
            }
            case ENCOUNTER_ROSTER -> { }
            case PREREQUISITE_CONTRADICTION -> problems.add(trial.authoringBlockReason());
        }
        return new Validation(problems.isEmpty(), problems);
    }

    private static String requiredFields(SignatureTrialCatalog.GapKind gapKind) {
        return switch (gapKind) {
            case SPECIAL_ELITE_IDENTITY -> "encounterId, enemyIds, specialEliteId";
            case TRIAL_BOSS_IDENTITY -> "encounterId, enemyIds, trialBossId";
            case PROTECTED_NPC_IDENTITY -> "encounterId, enemyIds, protectedNpcId";
            case ENCOUNTER_ROSTER -> "encounterId, enemyIds";
            case PREREQUISITE_CONTRADICTION -> "기획서 모순 수정";
        };
    }

    private static Validation blocked(String reason) {
        return new Validation(false, List.of(reason));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
