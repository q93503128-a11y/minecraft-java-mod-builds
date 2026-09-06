package kr.moonseungjun.turnboundre.battle;

import java.util.SplittableRandom;

/**
 * One deterministic random stream per battle. Callers must not create ad-hoc RNGs
 * during battle resolution; consuming this stream in a fixed order is part of the replay contract.
 */
public final class DeterministicBattleRng {
    private final SplittableRandom random;
    private long draws;

    public DeterministicBattleRng(long seed) {
        this.random = new SplittableRandom(seed);
    }

    public double nextUnitDouble() {
        draws++;
        return random.nextDouble();
    }

    public double nextDouble(double originInclusive, double boundExclusive) {
        if (!(originInclusive < boundExclusive)) {
            throw new IllegalArgumentException("origin must be < bound");
        }
        draws++;
        return random.nextDouble(originInclusive, boundExclusive);
    }

    public long draws() {
        return draws;
    }
}
