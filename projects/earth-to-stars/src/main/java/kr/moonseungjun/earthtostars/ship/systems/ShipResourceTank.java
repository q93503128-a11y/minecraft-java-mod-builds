package kr.moonseungjun.earthtostars.ship.systems;

public final class ShipResourceTank {
    private static final double EPSILON = 1.0E-9D;

    private final double capacity;
    private double stored;

    public ShipResourceTank(double capacity, double initialStored) {
        if (!Double.isFinite(capacity) || capacity <= 0.0D) {
            throw new IllegalArgumentException("capacity must be finite and > 0");
        }
        if (!Double.isFinite(initialStored) || initialStored < 0.0D || initialStored > capacity) {
            throw new IllegalArgumentException("initialStored must be within tank capacity");
        }
        this.capacity = capacity;
        this.stored = initialStored;
    }

    public synchronized boolean canConsume(double amount) {
        validateAmount(amount);
        return stored + EPSILON >= amount;
    }

    public synchronized boolean tryConsume(double amount) {
        if (!canConsume(amount)) {
            return false;
        }
        stored = Math.max(0.0D, stored - amount);
        return true;
    }

    public synchronized double drain(double amount) {
        validateAmount(amount);
        double drained = Math.min(stored, amount);
        stored -= drained;
        return drained;
    }

    public synchronized double fill(double amount) {
        validateAmount(amount);
        double accepted = Math.min(amount, capacity - stored);
        stored += accepted;
        return accepted;
    }

    public synchronized double stored() {
        return stored;
    }

    public double capacity() {
        return capacity;
    }

    private static void validateAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException("resource amount must be finite and >= 0");
        }
    }
}
