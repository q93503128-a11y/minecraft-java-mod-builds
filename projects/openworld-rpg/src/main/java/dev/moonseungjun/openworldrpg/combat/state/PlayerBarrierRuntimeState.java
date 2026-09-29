package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-owned transient barrier layers for one player.
 *
 * <p>Same-source grants replace/refresh. Different sources share the canonical 40% MaxHP total
 * cap. Barrier layers are depleted by soonest expiry first; source id is only a deterministic
 * tie-breaker when expiries match. No source receives hidden strength priority.</p>
 */
public final class PlayerBarrierRuntimeState {
    private static final double EPSILON = 1.0e-9;
    private static final double ULTIMATE_STEP_MAX_HP_FRACTION = 0.05;
    private static final int MAX_ULTIMATE_SUPPORT_STEPS = 3;
    private final Map<String, Layer> layers = new HashMap<>();

    public GrantResult grant(
            String sourceId,
            UUID sourcePlayerId,
            double requestedAmount,
            double recipientMaxHp,
            int durationTicks,
            boolean clericGraceSource,
            long nowTick
    ) {
        requireStableId(sourceId);
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId");
        requireFinitePositive("requestedAmount", requestedAmount);
        requireFinitePositive("recipientMaxHp", recipientMaxHp);
        if (durationTicks <= 0 || nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Barrier duration/time must be positive and non-negative."
            );
        }

        refresh(nowTick);
        trimToCurrentCap(recipientMaxHp);

        Layer previous = layers.get(sourceId);
        double previousAmount = previous == null
                ? 0.0
                : previous.amount();
        double totalWithoutSource = Math.max(
                0.0,
                totalRaw() - previousAmount
        );
        double sourceCapacity = Math.max(
                0.0,
                PlayerBarrierAuthority.totalBarrierCap(recipientMaxHp)
                        - totalWithoutSource
        );
        double appliedAmount = Math.min(
                requestedAmount,
                sourceCapacity
        );
        long expiresAtTick = Math.addExact(
                nowTick,
                durationTicks
        );

        if (appliedAmount <= EPSILON) {
            layers.remove(sourceId);
            return new GrantResult(
                    requestedAmount,
                    0.0,
                    0.0,
                    previousAmount,
                    totalRaw(),
                    expiresAtTick
            );
        }

        layers.put(
                sourceId,
                new Layer(
                        sourceId,
                        sourcePlayerId,
                        appliedAmount,
                        expiresAtTick,
                        clericGraceSource,
                        0.0,
                        false,
                        0
                )
        );
        double effectiveGranted = Math.max(
                0.0,
                appliedAmount - previousAmount
        );
        return new GrantResult(
                requestedAmount,
                appliedAmount,
                effectiveGranted,
                previousAmount,
                totalRaw(),
                expiresAtTick
        );
    }

    public Absorption absorbHostileDamage(
            double incomingDamage,
            double recipientMaxHp,
            long nowTick
    ) {
        requireFiniteNonNegative("incomingDamage", incomingDamage);
        requireFinitePositive("recipientMaxHp", recipientMaxHp);
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Barrier time must be non-negative."
            );
        }

        refresh(nowTick);
        trimToCurrentCap(recipientMaxHp);
        if (incomingDamage <= EPSILON || layers.isEmpty()) {
            return new Absorption(
                    incomingDamage,
                    0.0,
                    totalRaw(),
                    List.of()
            );
        }

        List<Layer> ordered = layers.values().stream()
                .sorted(
                        Comparator.comparingLong(Layer::expiresAtTick)
                                .thenComparing(Layer::sourceId)
                )
                .toList();

        double remaining = incomingDamage;
        double absorbedTotal = 0.0;
        List<SourceConsumption> consumptions =
                new ArrayList<>();
        double graceThreshold = recipientMaxHp
                * ClericGraceRuntimeState
                        .SUPPORT_THRESHOLD_MAX_HP_FRACTION;

        for (Layer layer : ordered) {
            if (remaining <= EPSILON) {
                break;
            }

            double absorbed = Math.min(
                    layer.amount(),
                    remaining
            );
            if (absorbed <= EPSILON) {
                continue;
            }

            double amountAfter = Math.max(
                    0.0,
                    layer.amount() - absorbed
            );
            double consumedSinceGrant =
                    layer.consumedSinceGrant() + absorbed;
            boolean graceThresholdReached =
                    layer.clericGraceSource()
                            && !layer.graceEventEmitted()
                            && consumedSinceGrant + EPSILON
                                    >= graceThreshold;
            boolean graceEventEmitted =
                    layer.graceEventEmitted()
                            || graceThresholdReached;
            int qualifiedUltimateSteps = layer.clericGraceSource()
                    ? Math.min(
                            MAX_ULTIMATE_SUPPORT_STEPS,
                            (int) Math.floor(
                                    (consumedSinceGrant + EPSILON)
                                            / (recipientMaxHp
                                            * ULTIMATE_STEP_MAX_HP_FRACTION)
                            )
                    )
                    : 0;
            int newUltimateChargeSteps = Math.max(
                    0,
                    qualifiedUltimateSteps
                            - layer.ultimateChargeStepsEmitted()
            );
            int ultimateChargeStepsEmitted =
                    layer.ultimateChargeStepsEmitted()
                            + newUltimateChargeSteps;

            if (amountAfter <= EPSILON) {
                layers.remove(layer.sourceId());
            } else {
                layers.put(
                        layer.sourceId(),
                        new Layer(
                                layer.sourceId(),
                                layer.sourcePlayerId(),
                                amountAfter,
                                layer.expiresAtTick(),
                                layer.clericGraceSource(),
                                consumedSinceGrant,
                                graceEventEmitted,
                                ultimateChargeStepsEmitted
                        )
                );
            }

            consumptions.add(
                    new SourceConsumption(
                            layer.sourceId(),
                            layer.sourcePlayerId(),
                            absorbed,
                            consumedSinceGrant,
                            graceThresholdReached,
                            newUltimateChargeSteps
                    )
            );
            remaining -= absorbed;
            absorbedTotal += absorbed;
        }

        return new Absorption(
                Math.max(0.0, remaining),
                absorbedTotal,
                totalRaw(),
                List.copyOf(consumptions)
        );
    }

    public double totalBarrier(
            double recipientMaxHp,
            long nowTick
    ) {
        requireFinitePositive("recipientMaxHp", recipientMaxHp);
        refresh(nowTick);
        trimToCurrentCap(recipientMaxHp);
        return totalRaw();
    }

    public double layerAmount(
            String sourceId,
            long nowTick
    ) {
        requireStableId(sourceId);
        refresh(nowTick);
        Layer layer = layers.get(sourceId);
        return layer == null ? 0.0 : layer.amount();
    }

    public int activeLayerCount(long nowTick) {
        refresh(nowTick);
        return layers.size();
    }

    private void refresh(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Barrier time must be non-negative."
            );
        }
        layers.entrySet().removeIf(
                entry -> nowTick >= entry.getValue().expiresAtTick()
        );
    }

    /**
     * A MaxHP decrease cannot be used to keep an over-cap pool. Excess is removed from the
     * longest-lived layers first, preserving the layers that would naturally disappear sooner.
     */
    private void trimToCurrentCap(double recipientMaxHp) {
        double cap = PlayerBarrierAuthority.totalBarrierCap(
                recipientMaxHp
        );
        double excess = totalRaw() - cap;
        if (excess <= EPSILON) {
            return;
        }

        List<Layer> ordered = layers.values().stream()
                .sorted(
                        Comparator.comparingLong(Layer::expiresAtTick)
                                .reversed()
                                .thenComparing(
                                        Layer::sourceId,
                                        Comparator.reverseOrder()
                                )
                )
                .toList();
        for (Layer layer : ordered) {
            if (excess <= EPSILON) {
                break;
            }
            double removed = Math.min(
                    layer.amount(),
                    excess
            );
            double amountAfter = layer.amount() - removed;
            if (amountAfter <= EPSILON) {
                layers.remove(layer.sourceId());
            } else {
                layers.put(
                        layer.sourceId(),
                        new Layer(
                                layer.sourceId(),
                                layer.sourcePlayerId(),
                                amountAfter,
                                layer.expiresAtTick(),
                                layer.clericGraceSource(),
                                layer.consumedSinceGrant(),
                                layer.graceEventEmitted(),
                                layer.ultimateChargeStepsEmitted()
                        )
                );
            }
            excess -= removed;
        }
    }

    private double totalRaw() {
        double total = 0.0;
        for (Layer layer : layers.values()) {
            total += layer.amount();
        }
        return total;
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced barrier source id."
            );
        }
    }

    private static void requireFinitePositive(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and positive."
            );
        }
    }

    private static void requireFiniteNonNegative(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative."
            );
        }
    }

    private record Layer(
            String sourceId,
            UUID sourcePlayerId,
            double amount,
            long expiresAtTick,
            boolean clericGraceSource,
            double consumedSinceGrant,
            boolean graceEventEmitted,
            int ultimateChargeStepsEmitted
    ) {
        private Layer {
            requireStableId(sourceId);
            Objects.requireNonNull(
                    sourcePlayerId,
                    "sourcePlayerId"
            );
            requireFinitePositive("amount", amount);
            if (expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Barrier expiry must be non-negative."
                );
            }
            requireFiniteNonNegative(
                    "consumedSinceGrant",
                    consumedSinceGrant
            );
            if (ultimateChargeStepsEmitted < 0
                    || ultimateChargeStepsEmitted
                    > MAX_ULTIMATE_SUPPORT_STEPS) {
                throw new IllegalArgumentException(
                        "Invalid barrier Ultimate-charge step count."
                );
            }
        }
    }

    public record GrantResult(
            double requestedAmount,
            double appliedAmount,
            double effectiveGranted,
            double replacedAmount,
            double totalBarrierAfter,
            long expiresAtTick
    ) {
        public GrantResult {
            requireFiniteNonNegative(
                    "requestedAmount",
                    requestedAmount
            );
            requireFiniteNonNegative(
                    "appliedAmount",
                    appliedAmount
            );
            requireFiniteNonNegative(
                    "effectiveGranted",
                    effectiveGranted
            );
            requireFiniteNonNegative(
                    "replacedAmount",
                    replacedAmount
            );
            requireFiniteNonNegative(
                    "totalBarrierAfter",
                    totalBarrierAfter
            );
            if (expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Barrier expiry must be non-negative."
                );
            }
        }
    }

    public record SourceConsumption(
            String sourceId,
            UUID sourcePlayerId,
            double absorbedDamage,
            double consumedSinceGrant,
            boolean clericGraceThresholdReached,
            int clericUltimateChargeStepsReached
    ) {
        public SourceConsumption {
            requireStableId(sourceId);
            Objects.requireNonNull(
                    sourcePlayerId,
                    "sourcePlayerId"
            );
            requireFinitePositive(
                    "absorbedDamage",
                    absorbedDamage
            );
            requireFinitePositive(
                    "consumedSinceGrant",
                    consumedSinceGrant
            );
            if (clericUltimateChargeStepsReached < 0
                    || clericUltimateChargeStepsReached
                    > MAX_ULTIMATE_SUPPORT_STEPS) {
                throw new IllegalArgumentException(
                        "Invalid barrier Ultimate-charge step delta."
                );
            }
        }
    }

    public record Absorption(
            double remainingDamage,
            double absorbedDamage,
            double remainingBarrier,
            List<SourceConsumption> sourceConsumptions
    ) {
        public Absorption {
            requireFiniteNonNegative(
                    "remainingDamage",
                    remainingDamage
            );
            requireFiniteNonNegative(
                    "absorbedDamage",
                    absorbedDamage
            );
            requireFiniteNonNegative(
                    "remainingBarrier",
                    remainingBarrier
            );
            sourceConsumptions = List.copyOf(
                    Objects.requireNonNull(
                            sourceConsumptions,
                            "sourceConsumptions"
                    )
            );
        }
    }
}
