package kr.moonseungjun.earthtostars.ship.gameplay;

import kr.moonseungjun.earthtostars.ship.runtime.ShipControlInput;

import java.util.Objects;

public final class LaunchReadinessPolicy {
    public static final double DENSE_ATMOSPHERE_CEILING = 256.0D;
    public static final double THIN_ATMOSPHERE_CEILING = 384.0D;
    public static final double EARTH_EXIT_ALTITUDE = 512.0D;

    public static final double MIN_ORBIT_PROPELLANT = 8.0D;
    public static final double MIN_ORBIT_OXYGEN = 20.0D;

    private LaunchReadinessPolicy() {
    }

    public static AtmosphereBand band(boolean inEarth, boolean inOrbit, double altitude) {
        if (!Double.isFinite(altitude)) {
            throw new IllegalArgumentException("altitude must be finite");
        }
        if (inOrbit) {
            return AtmosphereBand.ORBIT;
        }
        if (!inEarth) {
            return AtmosphereBand.OTHER;
        }
        if (altitude < DENSE_ATMOSPHERE_CEILING) {
            return AtmosphereBand.DENSE;
        }
        if (altitude < THIN_ATMOSPHERE_CEILING) {
            return AtmosphereBand.THIN;
        }
        return AtmosphereBand.UPPER;
    }

    public static double propellantPerTick(AtmosphereBand band, ShipControlInput input) {
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(input, "input");
        double activity = Math.max(Math.abs(input.throttle()), Math.max(Math.abs(input.yaw()), Math.abs(input.pitch())));
        if (activity <= 1.0E-6D) {
            return 0.0D;
        }
        double rate = switch (band) {
            case DENSE -> 0.020D;
            case THIN -> 0.040D;
            case UPPER -> 0.070D;
            case ORBIT -> 0.015D;
            case OTHER -> 0.025D;
        };
        return rate * activity;
    }

    public static double oxygenPerTick(AtmosphereBand band, int activeCrew) {
        Objects.requireNonNull(band, "band");
        if (activeCrew < 0) {
            throw new IllegalArgumentException("activeCrew must be >= 0");
        }
        if (activeCrew == 0) {
            return 0.0D;
        }
        double ratePerCrew = switch (band) {
            case DENSE -> 0.0D;
            case THIN -> 0.0025D;
            case UPPER -> 0.010D;
            case ORBIT -> 0.015D;
            case OTHER -> 0.005D;
        };
        return ratePerCrew * activeCrew;
    }

    public static boolean hasOrbitReserve(double propellant, double oxygen, boolean lifeSupportInstalled) {
        return lifeSupportInstalled
                && propellant + 1.0E-9D >= MIN_ORBIT_PROPELLANT
                && oxygen + 1.0E-9D >= MIN_ORBIT_OXYGEN;
    }

    public enum AtmosphereBand {
        DENSE,
        THIN,
        UPPER,
        ORBIT,
        OTHER
    }
}
