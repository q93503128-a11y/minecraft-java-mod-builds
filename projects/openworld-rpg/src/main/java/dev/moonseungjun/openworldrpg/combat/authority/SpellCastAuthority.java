package dev.moonseungjun.openworldrpg.combat.authority;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-owned admission seam for Spell Engine casts.
 *
 * <p>Spell Engine remains execution infrastructure. A spell in the {@code openworld_rpg} namespace
 * is not allowed to execute on the server until the project has registered an explicit authority
 * policy for that spell. This prevents a future project spell from silently inheriting donor
 * resource, cooldown, progression or reward ownership while those domain systems are still being
 * implemented.</p>
 */
public final class SpellCastAuthority {
    public enum AttemptDecision {
        PASS_THROUGH,
        ALLOW,
        BLOCK
    }

    public interface Policy {
        boolean authorizeAttempt(UUID playerId, String spellId);

        void onEngineCostConsumed(UUID playerId, String spellId);

        void onEngineCastCompleted(UUID playerId, String spellId, String action, float progress);
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

    public AttemptDecision authorizeAttempt(UUID playerId, String spellId) {
        Objects.requireNonNull(playerId, "playerId");
        String normalized = normalizeSpellId(spellId);
        if (!isOwned(normalized)) {
            return AttemptDecision.PASS_THROUGH;
        }

        Policy policy = policies.get(normalized);
        if (policy == null) {
            return AttemptDecision.BLOCK;
        }
        return policy.authorizeAttempt(playerId, normalized)
                ? AttemptDecision.ALLOW
                : AttemptDecision.BLOCK;
    }

    public void onEngineCostConsumed(UUID playerId, String spellId) {
        policyForCommittedProjectSpell(spellId).onEngineCostConsumed(playerId, normalizeSpellId(spellId));
    }

    public void onEngineCastCompleted(UUID playerId, String spellId, String action, float progress) {
        if (!Float.isFinite(progress) || progress < 0.0F || progress > 1.0F) {
            throw new IllegalArgumentException("Spell cast progress must be finite and inside [0, 1].");
        }
        policyForCommittedProjectSpell(spellId)
                .onEngineCastCompleted(playerId, normalizeSpellId(spellId), Objects.requireNonNull(action, "action"), progress);
    }

    public boolean owns(String spellId) {
        return isOwned(normalizeSpellId(spellId));
    }

    public int registeredPolicyCount() {
        return policies.size();
    }

    private Policy policyForCommittedProjectSpell(String spellId) {
        String normalized = normalizeSpellId(spellId);
        requireOwned(normalized);
        Policy policy = policies.get(normalized);
        if (policy == null) {
            throw new IllegalStateException(
                    "Spell Engine reached a committed project-spell stage without a registered authority policy: "
                            + normalized
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
}
