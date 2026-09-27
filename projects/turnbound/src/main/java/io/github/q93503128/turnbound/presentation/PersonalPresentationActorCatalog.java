package io.github.q93503128.turnbound.presentation;

import java.util.UUID;

/** Identity tags for player-owned presentation entities. */
public final class PersonalPresentationActorCatalog {
    /** Legacy marker retained as the owner-private visibility tag. */
    public static final String COMMON_TAG = "turnbound_private_presentation";
    public static final String PRIVATE_TAG = COMMON_TAG;
    /** Battle actors carry this tag when they are intentionally spectator-visible. */
    public static final String SHARED_BATTLE_TAG = "turnbound_shared_battle";
    public static final String OWNER_PREFIX = "turnbound_private_owner:";

    private PersonalPresentationActorCatalog() {}

    public static String ownerTag(UUID owner) {
        if (owner == null) throw new IllegalArgumentException("owner");
        return OWNER_PREFIX + owner;
    }

    public static UUID ownerFromTag(String tag) {
        if (tag == null || !tag.startsWith(OWNER_PREFIX)) return null;
        String raw = tag.substring(OWNER_PREFIX.length());
        if (raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
