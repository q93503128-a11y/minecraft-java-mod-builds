package kr.moonseungjun.earthtostars.ship.systems;

public enum PowerPriority {
    ESSENTIAL(0.00D),
    PROPULSION(0.10D),
    WEAPONS(0.25D),
    UTILITY(0.40D);

    private final double reserveFraction;

    PowerPriority(double reserveFraction) {
        this.reserveFraction = reserveFraction;
    }

    public double reserveFraction() {
        return reserveFraction;
    }
}
