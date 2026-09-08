package kr.moonseungjun.earthtostars.ship.systems;

import java.util.Objects;

public final class ShipPowerGrid {
    private static final double EPSILON = 1.0E-9D;

    private final double capacity;
    private final double generationPerTick;
    private double stored;
    private long lastGenerationTick = Long.MIN_VALUE;

    public ShipPowerGrid(double capacity, double initialStored, double generationPerTick) {
        requireFinitePositive(capacity, "capacity");
        requireFiniteNonNegative(initialStored, "initialStored");
        requireFiniteNonNegative(generationPerTick, "generationPerTick");
        if (initialStored > capacity) {
            throw new IllegalArgumentException("initialStored must not exceed capacity");
        }
        this.capacity = capacity;
        this.stored = initialStored;
        this.generationPerTick = generationPerTick;
    }

    public synchronized void beginTick(long tick) {
        if (lastGenerationTick == Long.MIN_VALUE) {
            lastGenerationTick = tick;
            return;
        }
        if (tick < lastGenerationTick) {
            throw new IllegalArgumentException("power grid tick must be monotonic");
        }
        if (tick == lastGenerationTick) {
            return;
        }
        long elapsed = tick - lastGenerationTick;
        stored = Math.min(capacity, stored + generationPerTick * elapsed);
        lastGenerationTick = tick;
    }

    public synchronized boolean canConsume(double amount, PowerPriority priority) {
        Objects.requireNonNull(priority, "priority");
        requireFiniteNonNegative(amount, "amount");
        if (amount <= EPSILON) {
            return true;
        }
        double reserve = capacity * priority.reserveFraction();
        return stored + EPSILON >= amount && stored - amount + EPSILON >= reserve;
    }

    public synchronized boolean tryConsume(double amount, PowerPriority priority) {
        if (!canConsume(amount, priority)) {
            return false;
        }
        stored = Math.max(0.0D, stored - amount);
        return true;
    }

    public synchronized double stored() {
        return stored;
    }

    public double capacity() {
        return capacity;
    }

    public double generationPerTick() {
        return generationPerTick;
    }

    private static void requireFinitePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and > 0");
        }
    }

    private static void requireFiniteNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }
}
