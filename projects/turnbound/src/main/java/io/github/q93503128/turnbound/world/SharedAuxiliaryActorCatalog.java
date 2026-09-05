package io.github.q93503128.turnbound.world;

/** Minecraft-independent identity contract for shared authored auxiliary world actors. */
final class SharedAuxiliaryActorCatalog {
    static final String COMMON_TAG = "turnbound_aux_shared_actor";
    static final String ROLE_PREFIX = "turnbound_aux_role:";

    private SharedAuxiliaryActorCatalog() {}

    static String roleTag(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Auxiliary actor key must not be blank");
        if (key.indexOf(' ') >= 0) throw new IllegalArgumentException("Auxiliary actor key must not contain spaces: " + key);
        return ROLE_PREFIX + key;
    }

    static String fromTag(String tag) {
        if (tag == null || !tag.startsWith(ROLE_PREFIX)) return null;
        String key = tag.substring(ROLE_PREFIX.length());
        return key.isBlank() || key.indexOf(' ') >= 0 ? null : key;
    }
}
