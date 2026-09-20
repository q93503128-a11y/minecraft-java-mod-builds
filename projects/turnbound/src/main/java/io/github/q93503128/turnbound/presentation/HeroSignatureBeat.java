package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleEvent;

/**
 * Pure mapping from authoritative combat events to authored signature payoff beats.
 * This never decides whether a mechanic succeeds; it only recognizes already-resolved events.
 */
public final class HeroSignatureBeat {
    public enum Kind {
        KYREN_FOLLOWUP,
        BRAM_REDIRECT_COUNTER,
        ELYSIA_SANCTUARY,
        LYNETTE_CROSS_SHOT,
        MORWEN_RECORD_SPEND,
        MORWEN_LAST_PAGE,
        MARION_PARTNER_STRIKE,
        MARION_JOINT_STRIKE,
        RAZE_HIGH_FURY,
        RAZE_OVERHEAT
    }

    private HeroSignatureBeat() {}

    public static Kind resolve(BattleEvent event) {
        if (event == null) return null;
        String detail = event.detail() == null ? "" : event.detail();
        return switch (event.type()) {
            case "REACTION_DAMAGE" -> switch (detail) {
                case "P01_BREAKER_FOLLOWUP", "P01_FOCUS_FOLLOWUP", "P01_AWAKEN_FOLLOWUP" -> Kind.KYREN_FOLLOWUP;
                case "P03_REDIRECT_COUNTER" -> Kind.BRAM_REDIRECT_COUNTER;
                case "P05_CROSS_SHOT" -> Kind.LYNETTE_CROSS_SHOT;
                case "P07_PARTNER_BASIC" -> Kind.MARION_PARTNER_STRIKE;
                case "P07_JOINT" -> Kind.MARION_JOINT_STRIKE;
                case "P08_HIGH_FURY" -> Kind.RAZE_HIGH_FURY;
                default -> null;
            };
            case "REACTION_HEAL" -> "P04_SANCTUARY".equals(detail) ? Kind.ELYSIA_SANCTUARY : null;
            case "RESOURCE" -> switch (detail) {
                case "P06_RECORD_SPEND" -> Kind.MORWEN_RECORD_SPEND;
                case "P08_OVERHEAT" -> Kind.RAZE_OVERHEAT;
                default -> null;
            };
            case "SELF_REVIVE" -> "P06_LAST_PAGE".equals(detail) ? Kind.MORWEN_LAST_PAGE : null;
            default -> null;
        };
    }
}
