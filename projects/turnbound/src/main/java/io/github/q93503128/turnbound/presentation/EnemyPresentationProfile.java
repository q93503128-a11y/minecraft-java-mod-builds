package io.github.q93503128.turnbound.presentation;

import java.util.Map;

/**
 * Presentation-only motion/pacing authoring for v0.4 filler, normal-enemy and elite skills.
 *
 * <p>Battle mechanics remain authoritative elsewhere. This table only decides which already-authored GeckoLib
 * clip family is used, how long the action is allowed to read before the next turn, and whether the actor should
 * physically close distance for that exact skill. Keeping this explicit prevents ranged/support units from doing
 * fake melee lunges and prevents special clips from being overwritten by the next turn after the old 8-tick
 * generic delay.</p>
 */
public final class EnemyPresentationProfile {
    public enum Motion { STRIKE, CAST, TELEGRAPH, CHARGE, SUMMON, PHASE }

    public record Skill(Motion motion, int oneXticks, boolean closeDistance) {
        public Skill {
            if (motion == null) throw new IllegalArgumentException("Missing enemy presentation motion");
            if (oneXticks < 1) throw new IllegalArgumentException("Enemy presentation ticks must be positive");
        }
    }

    private static final Map<String, Skill> SKILLS = Map.ofEntries(
            // Story fillers.
            Map.entry("f01_basic", skill(Motion.STRIKE, 13, true)),
            Map.entry("f02_basic", skill(Motion.CAST, 16, false)),
            Map.entry("f03_basic", skill(Motion.STRIKE, 13, false)),
            Map.entry("f03_focus_shot", skill(Motion.CAST, 18, false)),
            Map.entry("f04_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("f04_endure", skill(Motion.CAST, 18, false)),

            // Southgate / early-route enemies.
            Map.entry("e001_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("e002_basic", skill(Motion.STRIKE, 13, false)),
            Map.entry("e002_aimed", skill(Motion.CAST, 18, false)),
            Map.entry("e003_basic", skill(Motion.STRIKE, 14, false)),
            Map.entry("e003_arm", skill(Motion.TELEGRAPH, 18, false)),
            Map.entry("e003_explode", skill(Motion.PHASE, 20, false)),
            Map.entry("e004_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("e004_stab", skill(Motion.CHARGE, 16, true)),
            Map.entry("e005_basic", skill(Motion.CAST, 16, false)),
            Map.entry("e005_reform", skill(Motion.CAST, 19, false)),

            // Gloamwood.
            Map.entry("e006_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("e006_charge", skill(Motion.CHARGE, 18, true)),
            Map.entry("e007_basic", skill(Motion.CAST, 16, false)),
            Map.entry("e007_slow_spores", skill(Motion.CAST, 20, false)),
            Map.entry("e008_basic", skill(Motion.STRIKE, 15, true)),
            Map.entry("e008_barrier", skill(Motion.CAST, 19, false)),

            // Broken Aqueduct.
            Map.entry("e009_basic", skill(Motion.STRIKE, 14, false)),
            Map.entry("e009_delay", skill(Motion.CAST, 19, false)),
            Map.entry("e010_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("e010_flood_rot", skill(Motion.CAST, 19, false)),
            Map.entry("e011_basic", skill(Motion.STRIKE, 14, false)),
            Map.entry("e011_support", skill(Motion.CAST, 19, false)),

            // Ember Quarry.
            Map.entry("e012_basic", skill(Motion.STRIKE, 14, true)),
            Map.entry("e012_pounce", skill(Motion.CHARGE, 18, true)),
            Map.entry("e013_basic", skill(Motion.CAST, 16, false)),
            Map.entry("e013_embers", skill(Motion.CAST, 20, false)),
            Map.entry("e014_basic", skill(Motion.STRIKE, 22, true)),
            Map.entry("e014_crush", skill(Motion.PHASE, 23, true)),

            // Elites. Their own animation JSONs already carry distinct silhouettes; this table ensures specials
            // use the corresponding authored cast/charge/phase language and receive enough screen time.
            Map.entry("el01_basic", skill(Motion.STRIKE, 15, true)),
            Map.entry("el01_command", skill(Motion.CAST, 21, false)),
            Map.entry("el02_basic", skill(Motion.STRIKE, 15, true)),
            Map.entry("el02_piercing_horn", skill(Motion.CAST, 20, true)),
            Map.entry("el03_basic", skill(Motion.STRIKE, 15, true)),
            Map.entry("el03_barrier", skill(Motion.TELEGRAPH, 18, false)),
            Map.entry("el04_basic", skill(Motion.STRIKE, 23, true)),
            Map.entry("el04_collapse", skill(Motion.PHASE, 24, false))
    );

    private EnemyPresentationProfile() { }

    public static boolean handles(String skillId) {
        return skillId != null && SKILLS.containsKey(skillId);
    }

    public static int oneXticks(String skillId) {
        Skill skill = SKILLS.get(skillId);
        return skill == null ? 8 : skill.oneXticks();
    }

    public static boolean closeDistance(String skillId) {
        Skill skill = SKILLS.get(skillId);
        return skill != null && skill.closeDistance();
    }

    public static void play(BattleActorEntity actor, String skillId) {
        if (actor == null) return;
        Skill skill = SKILLS.get(skillId);
        if (skill == null) return;
        switch (skill.motion()) {
            case STRIKE -> actor.playStrike();
            case CAST -> actor.playCast();
            case TELEGRAPH -> actor.playTelegraph();
            case CHARGE -> actor.playCharge();
            case SUMMON -> actor.playSummon();
            case PHASE -> actor.playPhase();
        }
    }

    private static Skill skill(Motion motion, int ticks, boolean closeDistance) {
        return new Skill(motion, ticks, closeDistance);
    }
}
