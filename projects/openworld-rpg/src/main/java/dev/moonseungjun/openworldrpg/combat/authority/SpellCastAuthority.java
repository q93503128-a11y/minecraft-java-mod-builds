package dev.moonseungjun.openworldrpg.combat.authority;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-owned admission and impact seam for Spell Engine.
 */
public final class SpellCastAuthority {
    public enum AttemptDecision {
        PASS_THROUGH,
        ALLOW,
        BLOCK
    }

    public interface Policy {
        boolean preflight(CastContext context);

        boolean commitAcceptedCast(CastContext context);

        void onEngineCostConsumed(CastContext context);

        void onEngineCastCompleted(CastCompletion context);

        ImpactDecision onImpact(ImpactContext context);
    }

    private final String ownedNamespace;
    private final Map<String, Policy> policies = new ConcurrentHashMap<>();

    public SpellCastAuthority(String ownedNamespace) {
        String normalized = Objects.requireNonNull(ownedNamespace, "ownedNamespace").trim();
        if (normalized.isEmpty() || normalized.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Owned spell namespace must be a bare non-empty namespace.");
        }
        this.ownedNamespace = normalized;
    }

    public void registerPolicy(String spellId, Policy policy) {
        String normalized = normalizeSpellId(spellId);
        requireOwned(normalized);
        Objects.requireNonNull(policy, "policy");
        if (policies.putIfAbsent(normalized, policy) != null) {
            throw new IllegalStateException("Duplicate spell authority policy: " + normalized);
        }
    }

    public AttemptDecision preflightAttempt(UUID playerId, String spellId, long gameTick) {
        return preflightAttempt(playerId, spellId, gameTick, false);
    }

    public AttemptDecision preflightAttempt(
            UUID playerId,
            String spellId,
            long gameTick,
            boolean engineContinuation
    ) {
        String normalized = normalizeSpellId(spellId);
        if (!isOwned(normalized)) {
            return AttemptDecision.PASS_THROUGH;
        }
        Policy policy = policies.get(normalized);
        if (policy == null) {
            return AttemptDecision.BLOCK;
        }
        CastContext context = new CastContext(
                Objects.requireNonNull(playerId, "playerId"),
                normalized,
                gameTick,
                engineContinuation
        );
        return policy.preflight(context) ? AttemptDecision.ALLOW : AttemptDecision.BLOCK;
    }

    public AttemptDecision commitAcceptedCast(UUID playerId, String spellId, long gameTick) {
        return commitAcceptedCast(playerId, spellId, gameTick, false);
    }

    public AttemptDecision commitAcceptedCast(
            UUID playerId,
            String spellId,
            long gameTick,
            boolean engineContinuation
    ) {
        String normalized = normalizeSpellId(spellId);
        if (!isOwned(normalized)) {
            return AttemptDecision.PASS_THROUGH;
        }
        Policy policy = policies.get(normalized);
        if (policy == null) {
            return AttemptDecision.BLOCK;
        }
        CastContext context = new CastContext(
                Objects.requireNonNull(playerId, "playerId"),
                normalized,
                gameTick,
                engineContinuation
        );
        return policy.commitAcceptedCast(context) ? AttemptDecision.ALLOW : AttemptDecision.BLOCK;
    }

    public void onEngineCostConsumed(UUID playerId, String spellId, long gameTick) {
        String normalized = normalizeSpellId(spellId);
        policyForCommittedProjectSpell(normalized).onEngineCostConsumed(
                new CastContext(playerId, normalized, gameTick, true)
        );
    }

    public void onEngineCastCompleted(
            UUID playerId,
            String spellId,
            long gameTick,
            String action,
            float progress
    ) {
        if (!Float.isFinite(progress) || progress < 0.0F || progress > 1.0F) {
            throw new IllegalArgumentException("Spell cast progress must be finite and inside [0, 1].");
        }
        String normalized = normalizeSpellId(spellId);
        policyForCommittedProjectSpell(normalized).onEngineCastCompleted(
                new CastCompletion(playerId, normalized, gameTick, Objects.requireNonNull(action, "action"), progress)
        );
    }

    public ImpactDecision onImpact(
            UUID playerId,
            String spellId,
            long gameTick,
            int targetEntityId,
            double enginePower,
            double deliveryMultiplier,
            ProjectImpactTransaction.DamageSourceSnapshot sourceSnapshot,
            ProjectImpactTransaction.DamageTargetSnapshot targetSnapshot
    ) {
        if (!Double.isFinite(enginePower) || enginePower < 0.0
                || !Double.isFinite(deliveryMultiplier) || deliveryMultiplier < 0.0) {
            throw new IllegalArgumentException("Impact inputs must be finite and non-negative.");
        }
        String normalized = normalizeSpellId(spellId);
        return policyForCommittedProjectSpell(normalized).onImpact(
                new ImpactContext(
                        playerId,
                        normalized,
                        gameTick,
                        targetEntityId,
                        enginePower,
                        deliveryMultiplier,
                        Objects.requireNonNull(sourceSnapshot, "sourceSnapshot"),
                        Objects.requireNonNull(targetSnapshot, "targetSnapshot")
                )
        );
    }

    public boolean owns(String spellId) {
        return isOwned(normalizeSpellId(spellId));
    }

    public int registeredPolicyCount() {
        return policies.size();
    }

    private Policy policyForCommittedProjectSpell(String spellId) {
        requireOwned(spellId);
        Policy policy = policies.get(spellId);
        if (policy == null) {
            throw new IllegalStateException(
                    "Spell Engine reached a committed project-spell stage without a registered authority policy: "
                            + spellId
            );
        }
        return policy;
    }

    private boolean isOwned(String spellId) {
        int separator = spellId.indexOf(':');
        return separator > 0 && ownedNamespace.equals(spellId.substring(0, separator));
    }

    private void requireOwned(String spellId) {
        if (!isOwned(spellId)) {
            throw new IllegalArgumentException(
                    "Spell authority policy must target namespace " + ownedNamespace + ": " + spellId
            );
        }
    }

    private static String normalizeSpellId(String spellId) {
        String normalized = Objects.requireNonNull(spellId, "spellId").trim();
        int separator = normalized.indexOf(':');
        if (separator <= 0 || separator == normalized.length() - 1 || normalized.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid spell id: " + spellId);
        }
        return normalized;
    }

    public record CastContext(
            UUID playerId,
            String spellId,
            long gameTick,
            boolean engineContinuation
    ) {
    }

    public record CastCompletion(
            UUID playerId,
            String spellId,
            long gameTick,
            String action,
            float progress
    ) {
    }

    public record ImpactContext(
            UUID playerId,
            String spellId,
            long gameTick,
            int targetEntityId,
            double enginePower,
            double deliveryMultiplier,
            ProjectImpactTransaction.DamageSourceSnapshot sourceSnapshot,
            ProjectImpactTransaction.DamageTargetSnapshot targetSnapshot
    ) {
    }

    public record ImpactDecision(
            boolean accepted,
            boolean critical,
            double finalDamage,
            double poiseDamage
    ) {
        public ImpactDecision {
            if (!Double.isFinite(finalDamage) || finalDamage < 0.0
                    || !Double.isFinite(poiseDamage) || poiseDamage < 0.0) {
                throw new IllegalArgumentException("Impact result values must be finite and non-negative.");
            }
            if (!accepted && (critical || finalDamage != 0.0 || poiseDamage != 0.0)) {
                throw new IllegalArgumentException("Rejected impact cannot carry applied combat output.");
            }
        }

        public static ImpactDecision accepted(boolean critical) {
            return new ImpactDecision(true, critical, 0.0, 0.0);
        }

        public static ImpactDecision accepted(boolean critical, double finalDamage, double poiseDamage) {
            return new ImpactDecision(true, critical, finalDamage, poiseDamage);
        }

        public static ImpactDecision rejected() {
            return new ImpactDecision(false, false, 0.0, 0.0);
        }
    }
}
