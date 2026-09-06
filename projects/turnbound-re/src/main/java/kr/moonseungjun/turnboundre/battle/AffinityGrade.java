package kr.moonseungjun.turnboundre.battle;

public enum AffinityGrade {
    WEAK(1.25D, 1.50D),
    NORMAL(1.00D, 1.00D),
    RESIST(0.75D, 0.75D),
    IMMUNE(0.00D, 0.00D);

    private final double hpMultiplier;
    private final double poiseMultiplier;

    AffinityGrade(double hpMultiplier, double poiseMultiplier) {
        this.hpMultiplier = hpMultiplier;
        this.poiseMultiplier = poiseMultiplier;
    }

    public double hpMultiplier() {
        return hpMultiplier;
    }

    public double poiseMultiplier() {
        return poiseMultiplier;
    }
}
