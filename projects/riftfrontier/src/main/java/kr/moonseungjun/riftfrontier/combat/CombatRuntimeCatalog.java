package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentLookup;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public CoreDefinition.WeaponFamily requireWeaponFamily(ContentId id) {
        Objects.requireNonNull(id, "id");
        CoreDefinition definition = content.find(CoreDefinition.Kind.WEAPON_FAMILY, id)
            .orElseThrow(() -> new IllegalStateException("Missing weapon family in published content: " + id));
        if (!(definition instanceof CoreDefinition.WeaponFamily family)) {
            throw new IllegalStateException("Content kind mismatch for weapon family: " + id);
        }
        return family;
    }

    public CoreDefinition.WeaponModule requireWeaponModule(ContentId id) {
        Objects.requireNonNull(id, "id");
        CoreDefinition definition = content.find(CoreDefinition.Kind.WEAPON_MODULE, id)
            .orElseThrow(() -> new IllegalStateException("Missing weapon module in published content: " + id));
        if (!(definition instanceof CoreDefinition.WeaponModule module)) {
            throw new IllegalStateException("Content kind mismatch for weapon module: " + id);
        }
        return module;
    }

    public AttackExecution startAttack(ContentId patternId, long gameTick) {
        return AttackExecution.start(requireAttackPattern(patternId), gameTick);
    }

    public PlayerWeaponRuntimeProfile playerWeaponProfile(ContentId familyId, Optional<ContentId> moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");
        CoreDefinition.WeaponFamily family = requireWeaponFamily(familyId);
        Optional<CoreDefinition.WeaponModule> module = moduleId.map(this::requireWeaponModule);
        return PlayerWeaponRuntimeProfile.assemble(family, module, this::requireAttackPattern);
    }

    public PlayerWeaponCombatController playerWeaponController(ContentId familyId, Optional<ContentId> moduleId) {
        return new PlayerWeaponCombatController(playerWeaponProfile(familyId, moduleId));
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
