package kr.moonseungjun.arcanecircle.magic;

/** One geometry/hitbox metric table shared by effects and the world renderer. */
public final class SpellMetrics {
    private SpellMetrics() {}

    public static double effectRadius(String spellId, double range, int circle) {
        return switch (spellId) {
            case "shatter" -> Math.max(3.5, range * 0.28);
            case "sleet_storm", "cloudkill" -> Math.max(spellId.equals("cloudkill") ? 6.0 : 5.0, range * 0.34);
            case "insect_plague" -> Math.max(6.0, range * 0.32);
            case "fireball", "ice_knife", "chromatic_orb" -> Math.max(3.2, range * 0.22);
            case "wall_of_fire", "wall_of_ice", "wind_wall", "prismatic_wall" -> Math.max(5.0, range * 0.38);
            case "antimagic_field", "incendiary_cloud", "meteor_swarm", "storm_of_vengeance" ->
                    Math.max(7.0, range * 0.36);
            case "frost_nova", "phoenix_field", "blizzard_field", "thunder_prison",
                    "inferno_domain", "absolute_zero", "tempest_domain", "aegis_citadel" ->
                    Math.max(3.0, range * 0.32);
            default -> Math.max(2.4, range * (0.20 + Math.min(9, circle) * 0.012));
        };
    }

    public static double waveLength(double range) { return Math.max(4.0, range); }

    public static double waveEndRadius(String spellId, double range, int circle) {
        double spread = switch (spellId) {
            case "burning_hands" -> 0.34;
            case "cone_of_cold" -> 0.46;
            default -> 0.40;
        };
        return Math.max(1.4, range * spread + circle * 0.10);
    }
}
