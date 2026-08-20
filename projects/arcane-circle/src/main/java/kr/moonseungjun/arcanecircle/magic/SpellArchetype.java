package kr.moonseungjun.arcanecircle.magic;

import java.util.Set;

/** Server execution cadence. Visual motion lives in SpellPresentationProfile. */
public final class SpellArchetype {
    private static final Set<String> PROJECTILES = Set.of(
            "arcane_dart", "ember", "frost_needle", "flame_lance", "ice_lance", "fireball",
            "triune_barrage", "meteor_shard", "magic_missile", "fire_bolt", "ice_knife",
            "chromatic_orb", "delayed_blast_fireball", "freezing_sphere", "arcane_hand",
            "void_lance");

    private static final Set<String> CHANNELS = Set.of(
            "lightning_arc", "mana_lance", "chain_bolt", "arcane_annihilation");

    private static final Set<String> FIELDS = Set.of(
            "frost_nova", "phoenix_field", "blizzard_field", "thunder_prison",
            "inferno_domain", "absolute_zero", "tempest_domain", "aegis_citadel",
            "wall_of_fire", "cloudkill", "sleet_storm", "antimagic_field",
            "storm_of_vengeance", "winter_domain", "time_stop");

    private SpellArchetype() {}

    public static Mode mode(String spellId) {
        if (PROJECTILES.contains(spellId)) return Mode.PROJECTILE;
        if (CHANNELS.contains(spellId)) return Mode.CHANNEL;
        if (FIELDS.contains(spellId)) return Mode.FIELD;
        return Mode.INSTANT;
    }

    public enum Mode {
        INSTANT("즉", "instant"),
        PROJECTILE("탄", "projectile"),
        CHANNEL("집", "channel"),
        FIELD("장", "field");
        private final String badge;
        private final String key;
        Mode(String badge, String key) { this.badge = badge; this.key = key; }
        public String badge() { return badge; }
        public String key() { return key; }
    }
}
