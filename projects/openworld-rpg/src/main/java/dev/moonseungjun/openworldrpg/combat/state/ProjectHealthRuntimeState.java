package dev.moonseungjun.openworldrpg.combat.state;

/**
 * Canonical server-owned HP state when project actor HP exceeds Minecraft's vanilla health attribute range.
 *
 * <p>Minecraft health is treated as a proportional presentation/proxy value. Combat math, death threshold
 * and future UI read this canonical state instead of silently accepting the vanilla attribute clamp.</p>
 */
public final class ProjectHealthRuntimeState {
    private final double maxHealth;
    private double currentHealth;

    private ProjectHealthRuntimeState(double maxHealth, double currentHealth) {
        requireFinitePositive("maxHealth", maxHealth);
        if (!Double.isFinite(currentHealth) || currentHealth < 0.0 || currentHealth > maxHealth) {
            throw new IllegalArgumentException("currentHealth must be finite and inside [0, maxHealth].");
        }
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
    }

    public static ProjectHealthRuntimeState atFraction(double maxHealth, double healthFraction) {
        requireFinitePositive("maxHealth", maxHealth);
        if (!Double.isFinite(healthFraction) || healthFraction < 0.0 || healthFraction > 1.0) {
            throw new IllegalArgumentException("healthFraction must be finite and inside [0, 1].");
        }
        return new ProjectHealthRuntimeState(maxHealth, maxHealth * healthFraction);
    }

    public Snapshot snapshot() {
        return new Snapshot(currentHealth, maxHealth);
    }

    public double previewAppliedDamage(double requestedDamage) {
        requireFiniteNonNegative("requestedDamage", requestedDamage);
        return Math.min(requestedDamage, currentHealth);
    }

    public Application applyDamage(double requestedDamage) {
        double appliedDamage = previewAppliedDamage(requestedDamage);
        currentHealth = Math.max(0.0, currentHealth - appliedDamage);
        return new Application(appliedDamage, currentHealth, maxHealth, currentHealth <= 0.0);
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    public record Snapshot(double currentHealth, double maxHealth) {
        public double fraction() {
            return maxHealth <= 0.0 ? 0.0 : currentHealth / maxHealth;
        }
    }

    public record Application(
            double appliedDamage,
            double currentHealth,
            double maxHealth,
            boolean killed
    ) {
        public double fraction() {
            return maxHealth <= 0.0 ? 0.0 : currentHealth / maxHealth;
        }
    }
}
