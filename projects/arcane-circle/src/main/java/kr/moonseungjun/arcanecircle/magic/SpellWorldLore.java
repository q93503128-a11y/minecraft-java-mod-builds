
package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.MagicTradition;
import kr.moonseungjun.arcanecircle.magic.SpellWorldLore;

import java.util.Set;

/**
 * Licensing-safe world layer: spell names and core roles come from the D&D SRD released under
 * CC BY 4.0. The four faculty structure and Minecraft balance are original to this mod; no
 * proprietary setting characters, places, gods, maps or artwork are copied.
 */
public final class SpellWorldLore {
    public enum Discipline {
        ABJURATION("방호술"), CONJURATION("소환술"), DIVINATION("예지술"), ENCHANTMENT("정신술"),
        EVOCATION("방출술"), ILLUSION("환영술"), NECROMANCY("사령술"), TRANSMUTATION("변환술");
        private final String display;
        Discipline(String display) { this.display = display; }
        public String displayName() { return display; }
    }

    public enum SigilFamily { LANCE, STAR, HEX, PORTAL, EYE, SEAL, CLOCK, SPIRAL, STORM, CROWN }

    private static final Set<String> DIVINE = Set.of(
            "shield", "mage_armor", "mass_cure_wounds", "flame_strike", "sunbeam", "sunburst",
            "globe_of_invulnerability", "foresight", "gate", "clone", "shapechange");
    private static final Set<String> OCCULT = Set.of(
            "sleep", "mirror_image", "invisibility", "blur", "hold_person", "confusion",
            "phantasmal_killer", "dominate_person", "hold_monster", "mass_suggestion", "eyebite",
            "finger_of_death", "feeblemind", "dominate_monster", "maze", "power_word_kill", "weird");
    private static final Set<String> PRIMAL = Set.of(
            "fire_bolt", "ray_of_frost", "feather_fall", "grease", "thunderwave", "scorching_ray",
            "web", "gust_of_wind", "levitate", "fireball", "lightning_bolt", "fly", "sleet_storm",
            "wall_of_fire", "ice_storm", "stoneskin", "blight", "freedom_of_movement", "cone_of_cold",
            "cloudkill", "insect_plague", "move_earth", "freezing_sphere", "flesh_to_stone",
            "delayed_blast_fireball", "fire_storm", "reverse_gravity", "control_weather", "earthquake",
            "incendiary_cloud", "meteor_swarm");

    private SpellWorldLore() {}

    public static MagicTradition tradition(String spellId) {
        if (DIVINE.contains(spellId)) return MagicTradition.DIVINE;
        if (OCCULT.contains(spellId)) return MagicTradition.OCCULT;
        if (PRIMAL.contains(spellId)) return MagicTradition.PRIMAL;
        return MagicTradition.ARCANE;
    }

    public static Discipline discipline(String id) {
        return switch (id) {
            case "shield", "mage_armor", "protection_from_energy", "wall_of_force",
                    "globe_of_invulnerability", "forcecage", "antimagic_field", "prismatic_wall",
                    "counterspell", "fire_shield" -> Discipline.ABJURATION;
            case "misty_step", "web", "dimension_door", "passwall", "plane_shift", "teleport",
                    "demiplane", "gate", "teleportation_circle", "arcane_hand", "simulacrum" -> Discipline.CONJURATION;
            case "light", "true_seeing", "foresight" -> Discipline.DIVINATION;
            case "sleep", "hold_person", "confusion", "slow", "hold_monster", "dominate_person",
                    "mass_suggestion", "dominate_monster", "feeblemind", "eyebite" -> Discipline.ENCHANTMENT;
            case "mirror_image", "invisibility", "blur", "greater_invisibility", "phantasmal_killer",
                    "weird" -> Discipline.ILLUSION;
            case "vampiric_touch", "blight", "circle_of_death", "finger_of_death", "power_word_kill",
                    "clone" -> Discipline.NECROMANCY;
            case "feather_fall", "levitate", "fly", "haste", "stoneskin", "freedom_of_movement",
                    "telekinesis", "move_earth", "flesh_to_stone", "etherealness", "reverse_gravity",
                    "shapechange", "true_polymorph", "time_stop" -> Discipline.TRANSMUTATION;
            default -> Discipline.EVOCATION;
        };
    }

    public static SigilFamily sigilFamily(String id) {
        return switch (discipline(id)) {
            case ABJURATION -> SigilFamily.HEX;
            case CONJURATION -> SigilFamily.PORTAL;
            case DIVINATION -> id.equals("foresight") ? SigilFamily.CLOCK : SigilFamily.EYE;
            case ENCHANTMENT -> SigilFamily.EYE;
            case ILLUSION -> SigilFamily.SPIRAL;
            case NECROMANCY -> SigilFamily.SEAL;
            case TRANSMUTATION -> Set.of("time_stop", "reverse_gravity").contains(id)
                    ? SigilFamily.CLOCK : SigilFamily.SPIRAL;
            case EVOCATION -> Set.of("meteor_swarm", "sunburst", "fire_storm", "earthquake",
                    "control_weather", "incendiary_cloud", "sleet_storm", "ice_storm").contains(id)
                    ? SigilFamily.STORM
                    : Set.of("fireball", "delayed_blast_fireball", "freezing_sphere", "flame_strike",
                    "circle_of_death", "shatter").contains(id) ? SigilFamily.STAR : SigilFamily.LANCE;
        };
    }

    public static String provenance(String spellId) {
        return "D&D SRD CC BY 4.0 주문 역할 · Ninefold Arcana Minecraft 재해석";
    }
}
