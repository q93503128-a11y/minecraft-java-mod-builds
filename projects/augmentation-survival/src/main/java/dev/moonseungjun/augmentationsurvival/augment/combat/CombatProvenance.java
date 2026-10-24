package dev.moonseungjun.augmentationsurvival.augment.combat;

/** Canonical damage provenance categories used by shared combat hooks. */
public enum CombatProvenance {
    ROOT_MELEE,
    ROOT_PROJECTILE,
    DERIVED_PROJECTILE,
    DERIVED_CHAIN,
    DERIVED_AOE,
    ECHO,
    PATH_ECHO,
    ACTIVE_SPECIAL,
    REPLAY,
    AUTHORITY,
    COMPANION,
    REFLECTED,
    SPECIAL_PERCENT_HP,
    DELAYED_DAMAGE,
    OTHER;

    public boolean isRootDirect() {
        return this == ROOT_MELEE || this == ROOT_PROJECTILE;
    }
}
