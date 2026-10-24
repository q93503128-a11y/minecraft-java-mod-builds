package dev.moonseungjun.augmentationsurvival.combat;

/** Runtime provenance categories from the canonical interaction contract. */
public enum CombatProvenance {
    ROOT_MELEE(true, true),
    ROOT_PROJECTILE(true, true),
    DERIVED_PROJECTILE(false, true),
    DERIVED_CHAIN(false, true),
    DERIVED_AOE(false, true),
    ECHO(false, true),
    PATH_ECHO(false, true),
    ACTIVE_SPECIAL(false, true),
    REPLAY(false, true),
    AUTHORITY(false, true),
    COMPANION(false, false),
    REFLECTED(false, true),
    SPECIAL_PERCENT_HP(false, true);

    private final boolean rootDirectHit;
    private final boolean playerAttributedByDefault;

    CombatProvenance(boolean rootDirectHit, boolean playerAttributedByDefault) {
        this.rootDirectHit = rootDirectHit;
        this.playerAttributedByDefault = playerAttributedByDefault;
    }

    public boolean isRootDirectHit() {
        return rootDirectHit;
    }

    public boolean isPlayerAttributedByDefault() {
        return playerAttributedByDefault;
    }
}
