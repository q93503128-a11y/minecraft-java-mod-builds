package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentLookup;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.List;
import java.util.Objects;

/** Read-only combat runtime adapter over an already validated/published content snapshot. */
public final class CombatRuntimeCatalog {
    private final ContentLookup content;

    public CombatRuntimeCatalog(ContentLookup content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    public CoreDefinition.AttackPattern requireAttackPattern(ContentId id) {
        Objects.requireNonNull(id, "id");
        CoreDefinition definition = content.find(CoreDefinition.Kind.ATTACK_PATTERN, id)
            .orElseThrow(() -> new IllegalStateException("Missing attack pattern in published content: " + id));
        if (!(definition instanceof CoreDefinition.AttackPattern pattern)) {
            throw new IllegalStateException("Content kind mismatch for attack pattern: " + id);
        }
        return pattern;
    }

    public CoreDefinition.BossProfile requireBossProfile(ContentId id) {
        Objects.requireNonNull(id, "id");
        CoreDefinition definition = content.find(CoreDefinition.Kind.BOSS_PROFILE, id)
            .orElseThrow(() -> new IllegalStateException("Missing boss profile in published content: " + id));
        if (!(definition instanceof CoreDefinition.BossProfile boss)) {
            throw new IllegalStateException("Content kind mismatch for boss profile: " + id);
        }
        return boss;
    }

    public AttackExecution startAttack(ContentId patternId, long gameTick) {
        return AttackExecution.start(requireAttackPattern(patternId), gameTick);
    }

    /**
     * Resolves a boss' attack set to immutable runtime timelines. Stable ID order keeps selection
     * inputs deterministic even though authored selection policy is intentionally a later layer.
     */
    public List<AttackTimeline> bossAttackTimelines(ContentId bossId) {
        CoreDefinition.BossProfile boss = requireBossProfile(bossId);
        return boss.attackPatterns().stream()
            .sorted()
            .map(this::requireAttackPattern)
            .map(AttackTimeline::new)
            .toList();
    }
}
