package kr.moonseungjun.turnboundre.battle;

/** Battle-owned enemy telegraph. The actionId must match the action the AI actually resolves. */
public record EnemyIntent(
        String actionId,
        Type type,
        Targeting targeting,
        Risk risk,
        boolean breakCancelable,
        String breakDowngradeAction
) {
    public enum Type { ATTACK, DEFEND, BUFF, DEBUFF, SPECIAL }
    public enum Targeting { SINGLE, ALL, SELF, RANDOM }
    public enum Risk { NORMAL, DANGEROUS, ULTIMATE }

    public EnemyIntent {
        if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId must not be blank");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (targeting == null) throw new IllegalArgumentException("targeting must not be null");
        if (risk == null) throw new IllegalArgumentException("risk must not be null");
        if (breakDowngradeAction != null && breakDowngradeAction.isBlank()) breakDowngradeAction = null;
    }

    public EnemyIntent withAction(String newActionId, Risk newRisk) {
        return new EnemyIntent(newActionId, type, targeting, newRisk, false, null);
    }

    public static EnemyIntent basic() {
        return new EnemyIntent("basic", Type.ATTACK, Targeting.SINGLE, Risk.NORMAL, true, null);
    }

    public static EnemyIntent recover() {
        return new EnemyIntent("recover", Type.SPECIAL, Targeting.SELF, Risk.NORMAL, false, null);
    }
}
