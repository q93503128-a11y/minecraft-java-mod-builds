package io.github.q93503128.turnbound.presentation;

/**
 * Authored P01-P08 action-audio grammar.
 *
 * <p>The sample identifies the hero silhouette; pitch/volume distinguish Basic and the two actives.
 * Impact/heal/barrier/revive layers are emitted separately from authoritative battle events.</p>
 */
public final class HeroSkillAudioStyle {
    public record Style(String cueId, float volume, float pitch, int priority) {
        public Style {
            if (cueId == null || cueId.isBlank()) throw new IllegalArgumentException("Blank hero audio cue");
            volume = Math.max(0.0F, Math.min(1.0F, volume));
            pitch = Math.max(0.50F, Math.min(2.00F, pitch));
            priority = Math.max(1, Math.min(3, priority));
        }
    }

    private HeroSkillAudioStyle() {}

    public static Style resolve(String heroId, String skillId) {
        if (heroId == null || skillId == null) return null;
        return switch (heroId + "|" + skillId) {
            case "P01|p01_chase_slash" -> new Style("hero_kyren", 0.76F, 1.08F, 1);
            case "P01|p01_breaker_strike" -> new Style("hero_kyren", 0.94F, 0.86F, 3);
            case "P01|p01_duel_lock" -> new Style("hero_kyren", 0.84F, 1.00F, 2);

            case "P02|p02_accelerate" -> new Style("hero_lumea", 0.64F, 1.14F, 1);
            case "P02|p02_time_leap" -> new Style("hero_lumea", 0.80F, 0.98F, 2);
            case "P02|p02_delay_field" -> new Style("hero_lumea", 0.84F, 0.86F, 2);

            case "P03|p03_guard_stance" -> new Style("hero_bram", 0.80F, 1.00F, 1);
            case "P03|p03_guard_transfer" -> new Style("hero_bram", 0.86F, 0.92F, 2);
            case "P03|p03_shield_pressure" -> new Style("hero_bram", 0.96F, 0.80F, 3);

            case "P04|p04_heal" -> new Style("hero_elysia", 0.64F, 1.12F, 1);
            case "P04|p04_returned_breath" -> new Style("hero_elysia", 0.90F, 0.90F, 3);
            case "P04|p04_resting_light" -> new Style("hero_elysia", 0.78F, 1.00F, 2);

            case "P05|p05_suppressive_shot" -> new Style("hero_lynette", 0.72F, 1.16F, 1);
            case "P05|p05_piercing_shot" -> new Style("hero_lynette", 0.90F, 0.90F, 3);
            case "P05|p05_hunt_signal" -> new Style("hero_lynette", 0.80F, 1.04F, 2);

            case "P06|p06_echo" -> new Style("hero_morwen", 0.66F, 1.08F, 1);
            case "P06|p06_condolence" -> new Style("hero_morwen", 0.80F, 0.94F, 2);
            case "P06|p06_funeral_order" -> new Style("hero_morwen", 0.92F, 0.82F, 3);

            case "P07|p07_command" -> new Style("hero_marion", 0.68F, 1.12F, 1);
            case "P07|p07_summon_toto" -> new Style("hero_marion", 0.80F, 1.00F, 2);
            case "P07|p07_joint_attack" -> new Style("hero_marion", 0.92F, 0.86F, 3);

            case "P08|p08_frenzy" -> new Style("hero_raze", 0.82F, 1.02F, 1);
            case "P08|p08_blood_charge" -> new Style("hero_raze", 0.98F, 0.80F, 3);
            case "P08|p08_battle_mania" -> new Style("hero_raze", 0.92F, 0.90F, 2);
            default -> null;
        };
    }

    public static Style resolveCue(String cueId, String skillId) {
        String heroId = switch (cueId == null ? "" : cueId) {
            case "hero_kyren" -> "P01";
            case "hero_lumea" -> "P02";
            case "hero_bram" -> "P03";
            case "hero_elysia" -> "P04";
            case "hero_lynette" -> "P05";
            case "hero_morwen" -> "P06";
            case "hero_marion" -> "P07";
            case "hero_raze" -> "P08";
            default -> null;
        };
        return heroId == null ? null : resolve(heroId, skillId);
    }
}
