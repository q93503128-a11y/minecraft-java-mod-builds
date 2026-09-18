package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SpellCastAuthorityTest {
    @Test
    void nonProjectSpellPassesThrough() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");

        assertEquals(
                SpellCastAuthority.AttemptDecision.PASS_THROUGH,
                authority.authorizeAttempt(UUID.randomUUID(), "external_mod:spell")
        );
    }

    @Test
    void projectSpellWithoutPolicyIsBlocked() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");

        assertEquals(
                SpellCastAuthority.AttemptDecision.BLOCK,
                authority.authorizeAttempt(UUID.randomUUID(), "openworld_rpg:test_spell")
        );
    }

    @Test
    void registeredProjectPolicyOwnsAttemptCostAndCompletionHooks() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        AtomicInteger costEvents = new AtomicInteger();
        AtomicInteger castEvents = new AtomicInteger();

        authority.registerPolicy("openworld_rpg:test_spell", new SpellCastAuthority.Policy() {
            @Override
            public boolean authorizeAttempt(UUID playerId, String spellId) {
                return true;
            }

            @Override
            public void onEngineCostConsumed(UUID playerId, String spellId) {
                costEvents.incrementAndGet();
            }

            @Override
            public void onEngineCastCompleted(UUID playerId, String spellId, String action, float progress) {
                castEvents.incrementAndGet();
            }
        });

        UUID player = UUID.randomUUID();
        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.authorizeAttempt(player, "openworld_rpg:test_spell")
        );

        authority.onEngineCostConsumed(player, "openworld_rpg:test_spell");
        authority.onEngineCastCompleted(player, "openworld_rpg:test_spell", "RELEASE", 1.0F);

        assertEquals(1, costEvents.get());
        assertEquals(1, castEvents.get());
        assertEquals(1, authority.registeredPolicyCount());
    }

    @Test
    void duplicateOrForeignPoliciesAreRejected() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        SpellCastAuthority.Policy policy = new SpellCastAuthority.Policy() {
            @Override
            public boolean authorizeAttempt(UUID playerId, String spellId) {
                return true;
            }

            @Override
            public void onEngineCostConsumed(UUID playerId, String spellId) {
            }

            @Override
            public void onEngineCastCompleted(UUID playerId, String spellId, String action, float progress) {
            }
        };

        authority.registerPolicy("openworld_rpg:test_spell", policy);

        assertThrows(
                IllegalStateException.class,
                () -> authority.registerPolicy("openworld_rpg:test_spell", policy)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> authority.registerPolicy("external_mod:test_spell", policy)
        );
    }

    @Test
    void committedProjectSpellWithoutPolicyIsInvariantFailure() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");

        assertThrows(
                IllegalStateException.class,
                () -> authority.onEngineCostConsumed(UUID.randomUUID(), "openworld_rpg:test_spell")
        );
    }
}
