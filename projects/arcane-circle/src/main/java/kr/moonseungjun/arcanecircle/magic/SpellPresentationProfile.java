package kr.moonseungjun.arcanecircle.magic;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual/kinetic identity of a spell. Circle rank limits how sophisticated a formula may be, but
 * it does not dictate physical size: a 9C death command can be compact while Meteor Strike owns
 * the sky. Values are authored per spell where silhouette matters and fall back conservatively.
 */
public final class SpellPresentationProfile {
    public enum SigilStyle {
        FRONT_COMPACT, FRONT_LANCE, GROUND_SEAL, TARGET_SEAL, BODY_HALO, FEET_RUNE,
        SKY_RITUAL, QUAD_ARRAY, WALL_MATRIX, PORTAL_GATE
    }
    public enum MotionStyle {
        SNAP, DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE, BEAM, WAVE,
        FIELD, SKY_DROP, STORM, PORTAL, PRISON, WALL, TARGET_BURST, AURA
    }

    public record Profile(SigilStyle sigil, MotionStyle motion, double radius, int complexity,
                          int satellites, double projectileSpeed, double skyHeight,
                          double releaseScale, int fixedImpactTicks) {
        public Profile {
            radius = Math.max(0.35, radius);
            complexity = Math.max(1, Math.min(6, complexity));
            satellites = Math.max(0, Math.min(12, satellites));
            projectileSpeed = Math.max(0.0, projectileSpeed);
            skyHeight = Math.max(0.0, skyHeight);
            releaseScale = Math.max(0.45, releaseScale);
            fixedImpactTicks = Math.max(0, fixedImpactTicks);
        }
    }

    private static final Map<String, Profile> AUTHORED = new HashMap<>();

    static {
        // Low/mid projectile language: deliberately different speeds and launch silhouettes.
        put("magic_missile", SigilStyle.FRONT_COMPACT, MotionStyle.MISSILE_SWARM, 0.92, 2, 3, 56, 0, 0.90, 0);
        put("fire_bolt", SigilStyle.FRONT_COMPACT, MotionStyle.BOLT, 0.72, 1, 0, 44, 0, 0.86, 0);
        put("ray_of_frost", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 0.68, 2, 0, 0, 0, 0.86, 0);
        put("scorching_ray", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.10, 3, 3, 0, 0, 1.00, 0);
        put("fireball", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.38, 3, 0, 28, 0, 1.15, 0);
        put("lightning_bolt", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.18, 3, 0, 0, 0, 1.10, 0);
        put("ice_knife", SigilStyle.FRONT_LANCE, MotionStyle.DART, 0.88, 2, 0, 48, 0, 0.92, 0);
        put("chromatic_orb", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.18, 3, 0, 34, 0, 1.08, 0);
        put("cone_of_cold", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.85, 4, 0, 0, 0, 1.18, 0);
        put("chain_lightning", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.45, 4, 4, 0, 0, 1.16, 0);

        // Signature mid-circle identities. These keep the same spell school from collapsing into one silhouette.
        put("burning_hands", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.15, 2, 0, 0, 0, 0.92, 0);
        put("thunderwave", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.35, 2, 4, 0, 0, 1.02, 0);
        put("gust_of_wind", SigilStyle.FRONT_LANCE, MotionStyle.WAVE, 1.05, 3, 2, 0, 0, 1.00, 0);
        put("shatter", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.90, 3, 6, 0, 0, 1.10, 4);
        put("sleet_storm", SigilStyle.GROUND_SEAL, MotionStyle.STORM, 6.2, 4, 4, 0, 0, 1.30, 4);
        put("ice_storm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 6.8, 4, 5, 0, 12, 1.44, 8);
        put("cloudkill", SigilStyle.GROUND_SEAL, MotionStyle.STORM, 7.0, 4, 3, 0, 0, 1.38, 3);
        put("insect_plague", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 7.8, 4, 8, 0, 8, 1.42, 4);
        put("wall_of_fire", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.0, 4, 4, 0, 0, 1.42, 2);
        put("wall_of_ice", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.2, 4, 4, 0, 0, 1.38, 2);
        put("wall_of_force", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.6, 5, 6, 0, 0, 1.52, 2);
        put("misty_step", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.55, 2, 1, 0, 0, 0.92, 0);
        put("dimension_door", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 2.8, 4, 2, 0, 0, 1.18, 0);
        put("plane_shift", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 4.4, 5, 4, 0, 0, 1.48, 0);
        put("demiplane", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 6.6, 5, 6, 0, 0, 1.72, 0);
        put("hold_person", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 1.35, 3, 4, 0, 0, 1.08, 2);
        put("hold_monster", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 2.0, 4, 6, 0, 0, 1.26, 2);
        put("resilient_sphere", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 2.35, 4, 3, 0, 0, 1.30, 2);
        put("maze", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 3.2, 6, 8, 0, 0, 1.58, 3);

        // High-circle spells: size follows fiction, not rank.
        put("flame_strike", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 6.4, 4, 1, 0, 13, 1.45, 10);
        put("disintegrate", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.25, 5, 0, 0, 0, 1.16, 0);
        put("sunbeam", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 2.10, 4, 1, 0, 0, 1.34, 0);
        put("freezing_sphere", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.72, 5, 0, 23, 0, 1.42, 0);
        put("circle_of_death", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 5.2, 5, 0, 0, 0, 1.35, 0);
        put("delayed_blast_fireball", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.4, 5, 4, 0, 0, 1.65, 18);
        put("finger_of_death", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.15, 5, 0, 0, 0, 1.18, 2);
        put("fire_storm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 10.5, 5, 6, 0, 19, 1.78, 14);
        put("forcecage", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 3.25, 5, 4, 0, 0, 1.28, 2);
        put("prismatic_spray", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 2.55, 5, 7, 0, 0, 1.44, 0);
        put("reverse_gravity", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 8.2, 5, 4, 0, 0, 1.48, 4);
        put("teleport", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 4.2, 5, 2, 0, 0, 1.36, 0);
        put("antimagic_field", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 6.8, 5, 0, 0, 0, 1.34, 0);
        put("control_weather", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 16.0, 5, 8, 0, 24, 1.82, 8);
        put("earthquake", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 11.0, 5, 4, 0, 0, 1.74, 4);
        put("incendiary_cloud", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 10.0, 5, 5, 0, 10, 1.62, 6);
        put("sunburst", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 8.5, 5, 1, 0, 12, 1.64, 8);

        // 9C: deliberately non-monotonic physical scale.
        put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.0, 6, 4, 0, 30, 2.55, 30);
        put("power_word_kill", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35, 6, 0, 0, 0, 1.28, 2);
        put("prismatic_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 10.5, 6, 4, 0, 0, 2.05, 4);
        put("shapechange", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.85, 6, 3, 0, 0, 1.44, 0);
        put("time_stop", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 8.8, 6, 12, 0, 0, 1.72, 3);
        put("true_polymorph", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.6, 6, 4, 0, 0, 1.44, 3);
        put("weird", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 7.4, 6, 4, 0, 0, 1.62, 3);
        put("wish", SigilStyle.BODY_HALO, MotionStyle.AURA, 3.1, 6, 6, 0, 0, 1.55, 0);
        put("gate", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 10.5, 6, 4, 0, 0, 2.20, 0);
        put("foresight", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.75, 6, 2, 0, 0, 1.20, 0);

        // Fusion identities.
        put("void_lance", SigilStyle.FRONT_LANCE, MotionStyle.LANCE, 1.45, 6, 2, 72, 0, 1.55, 0);
        put("winter_domain", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 9.2, 6, 4, 0, 0, 1.65, 2);
        put("astral_prison", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 4.2, 6, 6, 0, 0, 1.62, 2);
        put("phoenix_requiem", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 10.8, 6, 6, 0, 14, 1.90, 6);
        put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 14.0, 6, 4, 0, 0, 2.25, 5);
        put("solar_guard", SigilStyle.BODY_HALO, MotionStyle.AURA, 4.4, 5, 6, 0, 0, 1.46, 0);
        put("teleportation_circle", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 5.0, 4, 4, 0, 0, 1.45, 0);
        put("thunder_cage", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 2.8, 4, 4, 0, 0, 1.26, 2);

        // alpha.23 phase 2B: canonical authored profiles for every 5C formula.
        put("cloudkill", SigilStyle.GROUND_SEAL, MotionStyle.STORM, 7.20, 5, 5, 0, 0, 1.42, 3);
        put("wall_of_force", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.80, 5, 7, 0, 0, 1.54, 2);
        put("hold_monster", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 2.10, 5, 6, 0, 0, 1.30, 2);
        put("passwall", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 3.40, 5, 2, 0, 0, 1.24, 0);
        put("insect_plague", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 8.00, 5, 8, 0, 8, 1.44, 4);
        put("telekinesis", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.95, 5, 5, 0, 0, 1.22, 2);
        put("cone_of_cold", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.90, 5, 6, 0, 0, 1.22, 0);
        put("flame_strike", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 6.60, 5, 1, 0, 13, 1.48, 10);
        put("dominate_person", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.85, 5, 5, 0, 0, 1.18, 2);
        put("mass_cure_wounds", SigilStyle.BODY_HALO, MotionStyle.AURA, 3.60, 5, 8, 0, 0, 1.34, 0);
        put("chain_lightning", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.55, 5, 4, 0, 0, 1.20, 0);
        put("arcane_hand", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35, 5, 5, 0, 0, 1.28, 2);
        put("teleportation_circle", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 5.20, 5, 6, 0, 0, 1.48, 0);

        // alpha.21 phase 1: canonical authored profiles for every 1C-3C formula.
        // Rank does not define size; each value follows the spell fiction and its launch device.
        put("magic_missile", SigilStyle.FRONT_COMPACT, MotionStyle.MISSILE_SWARM, 0.96, 2, 3, 62, 0, 0.92, 0);
        put("fire_bolt", SigilStyle.FRONT_LANCE, MotionStyle.BOLT, 0.68, 1, 0, 50, 0, 0.88, 0);
        put("ray_of_frost", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 0.76, 2, 0, 0, 0, 0.90, 0);
        put("shield", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.12, 2, 0, 0, 0, 1.02, 0);
        put("feather_fall", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.18, 2, 0, 0, 0, 0.96, 0);
        put("light", SigilStyle.BODY_HALO, MotionStyle.AURA, 0.78, 1, 0, 0, 0, 0.84, 0);
        put("grease", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 2.45, 2, 4, 0, 0, 1.04, 1);
        put("sleep", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 2.85, 2, 3, 0, 0, 1.08, 1);
        put("thunderwave", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.48, 2, 4, 0, 0, 1.06, 0);
        put("mage_armor", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.34, 2, 2, 0, 0, 1.08, 0);

        put("scorching_ray", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.08, 3, 3, 0, 0, 1.02, 0);
        put("misty_step", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.62, 2, 1, 0, 0, 0.94, 0);
        put("web", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 3.20, 3, 0, 0, 0, 1.10, 1);
        put("mirror_image", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.65, 3, 3, 0, 0, 1.04, 0);
        put("invisibility", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.10, 2, 0, 0, 0, 0.96, 0);
        put("gust_of_wind", SigilStyle.FRONT_LANCE, MotionStyle.WAVE, 1.18, 3, 2, 0, 0, 1.02, 0);
        put("hold_person", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 1.42, 3, 4, 0, 0, 1.10, 2);
        put("shatter", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.88, 3, 7, 0, 0, 1.12, 4);
        put("blur", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.18, 2, 3, 0, 0, 0.98, 0);
        put("levitate", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.34, 3, 3, 0, 0, 1.00, 0);

        put("fireball", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.46, 3, 0, 27, 0, 1.18, 0);
        put("lightning_bolt", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.22, 3, 0, 0, 0, 1.12, 0);
        put("fly", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.58, 3, 2, 0, 0, 1.08, 0);
        put("haste", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.36, 3, 0, 0, 0, 1.04, 0);
        put("dispel_magic", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.38, 3, 0, 0, 0, 1.02, 2);
        put("vampiric_touch", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.46, 3, 2, 0, 0, 1.08, 2);
        put("slow", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 3.65, 3, 0, 0, 0, 1.14, 2);
        put("protection_from_energy", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.68, 3, 4, 0, 0, 1.12, 0);
        put("sleet_storm", SigilStyle.GROUND_SEAL, MotionStyle.STORM, 6.20, 4, 4, 0, 0, 1.30, 4);
        put("blink", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.70, 3, 2, 0, 0, 1.02, 0);

        put("burning_hands", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.18, 2, 5, 0, 0, 0.96, 0);
        put("ice_knife", SigilStyle.FRONT_LANCE, MotionStyle.DART, 0.92, 2, 0, 54, 0, 0.94, 0);
        put("chromatic_orb", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.22, 3, 7, 36, 0, 1.10, 0);
        put("wind_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 4.60, 3, 5, 0, 0, 1.18, 2);
        put("counterspell", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.45, 3, 0, 0, 0, 1.08, 1);
        put("steam_burst", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.32, 2, 2, 0, 0, 1.04, 0);
        put("frost_step", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.88, 3, 6, 0, 0, 1.08, 0);

        // alpha.22 phase 2A: all 4C normal/fusion formulas get authored scale and duration semantics.
        // The MidCircleVisualIdentity director owns their geometry; these profiles define only the
        // spell-specific staging footprint and lifetime, never a rank-wide circle template.
        put("wall_of_fire", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.00, 4, 9, 0, 0, 1.46, 2);
        put("ice_storm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 7.20, 4, 7, 0, 11, 1.48, 8);
        put("greater_invisibility", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.55, 4, 6, 0, 0, 1.02, 0);
        put("resilient_sphere", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.45, 4, 4, 0, 0, 1.30, 0);
        put("dimension_door", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 2.90, 4, 7, 0, 0, 1.22, 0);
        put("stoneskin", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.75, 4, 5, 0, 0, 1.08, 0);
        put("confusion", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 3.10, 4, 5, 0, 0, 1.16, 2);
        put("blight", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.85, 4, 7, 0, 0, 1.18, 2);
        put("freedom_of_movement", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.90, 4, 5, 0, 0, 1.08, 0);
        put("phantasmal_killer", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.20, 4, 4, 0, 0, 1.22, 2);
        put("fire_shield", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.35, 4, 8, 0, 0, 1.30, 0);
        put("wall_of_ice", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 6.30, 4, 8, 0, 0, 1.42, 2);
        put("thunder_cage", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 3.00, 4, 4, 0, 0, 1.30, 2);

        // alpha.24 completion: canonical authored profiles for every 6C-9C formula.
        put("disintegrate", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.35, 6, 0, 0, 0, 1.22, 0);
        put("globe_of_invulnerability", SigilStyle.BODY_HALO, MotionStyle.AURA, 3.10, 6, 6, 0, 0, 1.48, 0);
        put("mass_suggestion", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 6.20, 6, 8, 0, 0, 1.42, 2);
        put("move_earth", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 8.40, 6, 4, 0, 0, 1.56, 4);
        put("sunbeam", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 2.20, 6, 1, 0, 0, 1.38, 0);
        put("true_seeing", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.80, 6, 6, 0, 0, 1.16, 0);
        put("freezing_sphere", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.90, 6, 0, 22, 0, 1.48, 0);
        put("eyebite", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.85, 6, 5, 0, 0, 1.26, 2);
        put("flesh_to_stone", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.20, 6, 6, 0, 0, 1.36, 2);
        put("circle_of_death", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 6.80, 6, 8, 0, 0, 1.52, 2);
        put("solar_guard", SigilStyle.BODY_HALO, MotionStyle.AURA, 4.40, 6, 8, 0, 0, 1.50, 0);
        put("delayed_blast_fireball", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.60, 6, 6, 0, 0, 1.72, 18);
        put("etherealness", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.70, 6, 6, 0, 0, 1.36, 0);
        put("finger_of_death", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.10, 6, 0, 0, 0, 1.24, 2);
        put("fire_storm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 10.50, 6, 6, 0, 19, 1.82, 14);
        put("forcecage", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 3.40, 6, 8, 0, 0, 1.48, 2);
        put("plane_shift", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 4.60, 6, 5, 0, 0, 1.52, 0);
        put("prismatic_spray", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 2.60, 6, 7, 0, 0, 1.48, 0);
        put("reverse_gravity", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 8.40, 6, 8, 0, 0, 1.56, 4);
        put("simulacrum", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.80, 6, 2, 0, 0, 1.40, 0);
        put("teleport", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 4.40, 6, 6, 0, 0, 1.42, 0);
        put("void_lance", SigilStyle.FRONT_LANCE, MotionStyle.LANCE, 1.55, 6, 2, 72, 0, 1.60, 0);
        put("winter_domain", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 9.40, 6, 12, 0, 0, 1.72, 2);
        put("antimagic_field", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 7.00, 6, 10, 0, 0, 1.42, 0);
        put("clone", SigilStyle.BODY_HALO, MotionStyle.AURA, 2.80, 6, 3, 0, 0, 1.38, 0);
        put("control_weather", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 16.00, 6, 8, 0, 24, 1.88, 8);
        put("demiplane", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 6.80, 6, 8, 0, 0, 1.78, 0);
        put("dominate_monster", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.10, 6, 8, 0, 0, 1.48, 2);
        put("earthquake", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 11.00, 6, 12, 0, 0, 1.80, 4);
        put("feeblemind", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.80, 6, 9, 0, 0, 1.44, 2);
        put("incendiary_cloud", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 10.00, 6, 7, 0, 10, 1.68, 6);
        put("maze", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 3.40, 6, 8, 0, 0, 1.62, 3);
        put("sunburst", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 8.80, 6, 1, 0, 12, 1.70, 8);
        put("astral_prison", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 4.40, 6, 8, 0, 0, 1.68, 2);
        put("phoenix_requiem", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 11.00, 6, 6, 0, 14, 1.96, 6);
        put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.00, 6, 4, 0, 30, 2.60, 30);
        put("power_word_kill", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35, 6, 0, 0, 0, 1.34, 2);
        put("prismatic_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 10.80, 6, 7, 0, 0, 2.10, 4);
        put("shapechange", SigilStyle.BODY_HALO, MotionStyle.AURA, 3.00, 6, 6, 0, 0, 1.52, 0);
        put("time_stop", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 9.00, 6, 12, 0, 0, 1.80, 3);
        put("true_polymorph", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.80, 6, 8, 0, 0, 1.52, 3);
        put("weird", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 7.60, 6, 9, 0, 0, 1.70, 3);
        put("wish", SigilStyle.BODY_HALO, MotionStyle.AURA, 3.20, 6, 6, 0, 0, 1.62, 0);
        put("gate", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 10.80, 6, 8, 0, 0, 2.25, 0);
        put("foresight", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.80, 6, 7, 0, 0, 1.24, 0);
        put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 14.50, 6, 12, 0, 0, 2.32, 5);

    }

    private SpellPresentationProfile() {}

    private static void put(String id, SigilStyle sigil, MotionStyle motion, double radius,
                            int complexity, int satellites, double speed, double skyHeight,
                            double releaseScale, int impactTicks) {
        AUTHORED.put(id, new Profile(sigil, motion, radius, complexity, satellites, speed,
                skyHeight, releaseScale, impactTicks));
    }

    public static Profile profile(SpellDefinition spell) {
        Profile explicit = AUTHORED.get(spell.id());
        if (explicit != null) return explicit;
        int c = Math.max(1, Math.min(9, spell.circle()));
        int complexity = Math.max(1, Math.min(6, 1 + c / 2));
        double radius = 0.58 + c * 0.18;
        SigilStyle sigil = switch (spell.sigilAnchor()) {
            case FRONT -> SigilStyle.FRONT_COMPACT;
            case FEET -> SigilStyle.FEET_RUNE;
            case GROUND_SELF, GROUND_TARGET -> SigilStyle.GROUND_SEAL;
            case BODY -> SigilStyle.BODY_HALO;
            case TARGET -> SigilStyle.TARGET_SEAL;
        };
        MotionStyle motion = switch (SpellArchetype.mode(spell.id())) {
            case PROJECTILE -> MotionStyle.BOLT;
            case CHANNEL -> MotionStyle.BEAM;
            case FIELD -> MotionStyle.FIELD;
            case INSTANT -> switch (spell.sigilAnchor()) {
                case BODY, FEET -> MotionStyle.AURA;
                case GROUND_SELF, GROUND_TARGET -> MotionStyle.FIELD;
                case TARGET -> MotionStyle.TARGET_BURST;
                case FRONT -> MotionStyle.SNAP;
            };
        };
        double speed = motion == MotionStyle.BOLT ? Math.max(30.0, 48.0 - c * 1.4) : 0.0;
        return new Profile(sigil, motion, radius, complexity, 0, speed, 0.0,
                1.0 + Math.max(0, c - 5) * 0.05, 0);
    }

    public static int impactDelayTicks(SpellDefinition spell, double distance) {
        Profile profile = profile(spell);
        if (profile.fixedImpactTicks() > 0) return profile.fixedImpactTicks();
        if (profile.projectileSpeed() <= 0.0) return 0;
        double seconds = Math.max(0.0, distance) / profile.projectileSpeed();
        return Math.max(1, Math.min(34, (int) Math.round(seconds * 20.0)));
    }

    public static int releaseDurationTicks(SpellDefinition spell, double distance) {
        Profile profile = profile(spell);
        int impact = impactDelayTicks(spell, distance);
        return switch (profile.motion()) {
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE -> Math.max(6, impact + 7);
            case SKY_DROP -> Math.max(18, impact + 16);
            case BEAM -> 12;
            case WAVE -> 18;
            case TARGET_BURST -> Math.max(14, impact + 12);
            case STORM -> 42;
            case PORTAL -> 34;
            case PRISON, WALL -> 30;
            case FIELD -> 34;
            case AURA -> 28;
            case SNAP -> 12;
        };
    }
}
