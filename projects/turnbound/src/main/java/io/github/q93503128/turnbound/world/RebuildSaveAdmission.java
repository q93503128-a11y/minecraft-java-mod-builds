package io.github.q93503128.turnbound.world;

/**
 * Pure login admission policy for the blank-content build.
 * Legacy campaign data is never migrated or overwritten here. A profile with both
 * versions present is ambiguous and must not be loaded by either path.
 */
public final class RebuildSaveAdmission {
    public enum Decision {
        START_FRESH,
        RESTORE_REBUILD,
        REJECT_LEGACY,
        REJECT_CONFLICT
    }

    private RebuildSaveAdmission() {}

    public static Decision decide(boolean hasRebuildAttachment,
                                  boolean hasLegacyAttachment,
                                  boolean hasLegacyFile) {
        if (hasRebuildAttachment && (hasLegacyAttachment || hasLegacyFile)) return Decision.REJECT_CONFLICT;
        if (hasLegacyAttachment || hasLegacyFile) return Decision.REJECT_LEGACY;
        return hasRebuildAttachment ? Decision.RESTORE_REBUILD : Decision.START_FRESH;
    }
}
