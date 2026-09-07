package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class M4BattleRewardClaimTest {
    private static final String REWARD = "turnbound_re:debug_overworld_patrol";

    @Test
    void victoryRewardCanBeClaimedExactlyOnceAndNeverBeforeRewardState() throws IOException {
        Fixture fixture = fixture(811L);
        AtomicInteger calls = new AtomicInteger();

        assertTrue(fixture.manager.claimVictoryReward(fixture.battle.battleId(), context -> {
            calls.incrementAndGet();
            return context.rewardTableId();
        }).isEmpty());
        assertEquals(0, calls.get());

        win(fixture.battle);
        String claimed = fixture.manager.claimVictoryReward(fixture.battle.battleId(), context -> {
            calls.incrementAndGet();
            assertEquals(fixture.ownerPlayerId, context.ownerPlayerId());
            assertEquals(REWARD, context.rewardTableId());
            assertEquals(811L, context.rewardSeed());
            return context.rewardTableId();
        }).orElseThrow();

        assertEquals(REWARD, claimed);
        assertEquals(1, calls.get());
        assertTrue(fixture.manager.rewardClaimed(fixture.battle.battleId()));
        assertTrue(fixture.manager.claimVictoryReward(fixture.battle.battleId(), context -> {
            calls.incrementAndGet();
            return "duplicate";
        }).isEmpty());
        assertEquals(1, calls.get());
    }

    @Test
    void failedClaimCallbackDoesNotBurnRewardAndCleanupRemovesMetadata() throws IOException {
        Fixture fixture = fixture(812L);
        win(fixture.battle);

        assertThrows(IllegalStateException.class,
                () -> fixture.manager.claimVictoryReward(fixture.battle.battleId(), context -> {
                    throw new IllegalStateException("simulated persistence failure");
                }));
        assertFalse(fixture.manager.rewardClaimed(fixture.battle.battleId()));

        assertEquals("retry-ok", fixture.manager.claimVictoryReward(
                fixture.battle.battleId(), context -> "retry-ok").orElseThrow());
        assertTrue(fixture.manager.rewardClaimed(fixture.battle.battleId()));

        fixture.battle.cleanup();
        fixture.manager.cleanup(fixture.battle.battleId());
        assertFalse(fixture.manager.rewardClaimed(fixture.battle.battleId()));
        assertTrue(fixture.manager.rewardContext(fixture.battle.battleId()).isEmpty());
    }

    @Test
    void registrationRejectsRewardTableOutsideTheCapturedDefinitionSnapshot() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        var zombie = parsed.registry().characters().get("turnbound_re:zombie");
        var skeleton = parsed.registry().characters().get("turnbound_re:skeleton");
        List<BattleParticipant> participants = List.of(
                participant("p", BattleTeam.PLAYER, 0, zombie),
                participant("e", BattleTeam.ENEMY, 1, skeleton));
        BattleInstance battle = new BattleInstance(UUID.randomUUID(), 813L, participants);
        BattleDefinitionContext context = new BattleDefinitionContext(
                parsed.registry(), parsed.hash(), Map.of("p", zombie.id(), "e", skeleton.id()));
        BattleManager manager = new BattleManager();

        assertThrows(IllegalArgumentException.class, () -> manager.register(
                battle,
                List.of(
                        new EntityParticipantBinding("p", UUID.randomUUID()),
                        new EntityParticipantBinding("e", UUID.randomUUID())),
                participants,
                context,
                new BattleRewardContext(UUID.randomUUID(), "turnbound_re:not_loaded", 813L)));
        assertEquals(0, manager.activeBattleCount());
    }

    private static Fixture fixture(long seed) throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        CharacterDefinition zombie = parsed.registry().characters().get("turnbound_re:zombie");
        CharacterDefinition skeleton = parsed.registry().characters().get("turnbound_re:skeleton");
        List<BattleParticipant> participants = List.of(
                participant("p", BattleTeam.PLAYER, 0, zombie),
                participant("e", BattleTeam.ENEMY, 1, skeleton));
        BattleInstance battle = new BattleInstance(UUID.randomUUID(), seed, participants);
        BattleManager manager = new BattleManager();
        UUID owner = UUID.randomUUID();
        manager.register(
                battle,
                List.of(
                        new EntityParticipantBinding("p", owner),
                        new EntityParticipantBinding("e", UUID.randomUUID())),
                participants,
                new BattleDefinitionContext(
                        parsed.registry(), parsed.hash(), Map.of("p", zombie.id(), "e", skeleton.id())),
                new BattleRewardContext(owner, REWARD, seed));
        battle.start();
        return new Fixture(manager, battle, owner);
    }

    private static BattleParticipant participant(
            String id, BattleTeam team, int ordinal, CharacterDefinition character
    ) {
        CharacterDefinition.Stats stats = character.baseStats();
        int speed = team == BattleTeam.PLAYER ? 100 : 1;
        return new BattleParticipant(id, team, ordinal, speed, stats.hp(), stats.atk(), stats.def(), stats.poise());
    }

    private static void win(BattleInstance battle) {
        assertEquals(BattleState.AWAIT_COMMAND, battle.state());
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "p", "basic")));
        battle.resolveDamage("p", "e", new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 1000, 0, 100, 0,
                false, 0.0D, 1.5D, 1.0D));
        battle.finishResolution();
        assertEquals(BattleInstance.Outcome.VICTORY, battle.outcome());
        assertEquals(BattleState.REWARD, battle.state());
    }

    private record Fixture(BattleManager manager, BattleInstance battle, UUID ownerPlayerId) {}
}
