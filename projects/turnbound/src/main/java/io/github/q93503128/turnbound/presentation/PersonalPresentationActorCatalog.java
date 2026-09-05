package io.github.q93503128.turnbound.presentation;

import java.util.UUID;

/** Pure identity contract for presentation entities that must only be visible to one player. */
public final class PersonalPresentationActorCatalog {
    public static final String COMMON_TAG = "turnbound_private_presentation";
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
