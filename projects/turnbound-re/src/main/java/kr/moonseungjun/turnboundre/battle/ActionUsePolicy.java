package kr.moonseungjun.turnboundre.battle;

public record ActionUsePolicy(
        boolean owned,
        boolean cooldownReady,
        boolean statusEligible,
        int minTargets,
        int maxTargets,
        TargetRule targetRule
) {
    public enum TargetRule {
        SELF,
        ALLY,
        ENEMY,
        ANY
    }

    public ActionUsePolicy {
        if (minTargets < 0) throw new IllegalArgumentException("minTargets must be >= 0");
        if (maxTargets < minTargets) throw new IllegalArgumentException("maxTargets must be >= minTargets");
        if (targetRule == null) throw new IllegalArgumentException("targetRule must not be null");
    }

    public static ActionUsePolicy self() {
        return new ActionUsePolicy(true, true, true, 1, 1, TargetRule.SELF);
    }

    public static ActionUsePolicy singleEnemy() {
        return new ActionUsePolicy(true, true, true, 1, 1, TargetRule.ENEMY);
    }
}
