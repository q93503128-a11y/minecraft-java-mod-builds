package kr.moonseungjun.turnboundre.battle;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
import kr.moonseungjun.turnboundre.progression.ProgressionService;
import kr.moonseungjun.turnboundre.progression.RewardService;
import kr.moonseungjun.turnboundre.progression.TurnboundProgressSavedData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class M4AuthoredEncounterSettlementIntegrationTest {
    private static final String ENCOUNTER = "turnbound_re:debug_overworld_patrol";
    private static final String HERO = "turnbound_re:iron_golem";
    private static final String ZOMBIE = "turnbound_re:zombie";

    @Test
    void authoredEncounterRunsThroughDataActionsRewardClaimProgressionAndSavedDataRoundTrip() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        DefinitionRepository definitions = new DefinitionRepository();
        definitions.install(parsed.registry(), parsed.hash());
        BattleManager manager = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(manager, definitions);
        UUID owner = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        CharacterDefinition heroDefinition = parsed.registry().characters().get(HERO);
        CharacterProgress heroProgress = new CharacterProgress(HERO, heroDefinition.originStar(), 6, 70);

        AuthoredEncounterLauncher.Launch launch = launcher.open(
                ENCOUNTER,
                owner,
                List.of(new AuthoredEncounterLauncher.PlayerSlot("hero", heroProgress, owner)),
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                battleId,
                2026090701L);

        assertSame(parsed.registry().encounters().get(ENCOUNTER), launch.encounter());
        assertSame(parsed.registry().rewards().get(launch.encounter().rewardTable()), launch.rewardContext().rewardTable());
        assertEquals(launch.encounter().rewardTable(), launch.rewardContext().rewardTableId());
        assertEquals(parsed.hash(), launch.definitionContext().definitionHash());
        assertTrue(manager.rewardReadyBattleIds().isEmpty());

        driveAuthoredBattleToVictory(manager, launch);
        assertEquals(BattleInstance.Outcome.VICTORY, launch.battle().outcome());
        assertEquals(BattleState.REWARD, launch.battle().state());
        assertEquals(List.of(battleId), manager.rewardReadyBattleIds());

        var tuning = parsed.registry().progressions().get("turnbound_re:default_progression");
        AtomicReference<PlayerProgress> persisted = new AtomicReference<>(PlayerProgress.fresh(tuning));
        RewardService rewards = new RewardService(launch.definitionContext().definitions());
        RewardService.Applied applied = manager.claimVictoryReward(battleId, context -> {
            RewardService.Applied next = rewards.rollAndApply(persisted.get(), context.rewardTable(), context.rewardSeed());
            persisted.set(next.state());
            return next;
        }).orElseThrow();

        assertEquals(applied.state(), persisted.get());
        assertTrue(applied.grant().coin() >= 60 && applied.grant().coin() <= 90);
        assertTrue(applied.grant().essence() >= 20 && applied.grant().essence() <= 30);
        assertTrue(manager.rewardClaimed(battleId));
        assertTrue(manager.rewardReadyBattleIds().isEmpty());
        assertTrue(manager.claimVictoryReward(battleId, context -> "duplicate").isEmpty());

        PlayerProgress afterEncounterReload = savedDataRoundTrip(owner, persisted.get());
        assertEquals(persisted.get(), afterEncounterReload);

        ProgressionService progression = new ProgressionService(parsed.registry(), tuning);
        PlayerProgress state = progression.grantCurrency(afterEncounterReload, 100_000L, 100_000L);
        state = progression.grantShards(state, ZOMBIE, 100);
        ProgressionService.Result unlock = progression.unlock(state, ZOMBIE);
        assertTrue(unlock.accepted(), unlock.detail());
        state = unlock.state();

        int cap = ProgressionRules.levelCap(state.characters().get(ZOMBIE).currentStar());
        while (state.characters().get(ZOMBIE).level() < cap) {
            ProgressionService.Result level = progression.levelUp(state, ZOMBIE);
            assertTrue(level.accepted(), level.code() + ":" + level.detail());
            state = level.state();
        }
        ProgressionService.Result capped = progression.levelUp(state, ZOMBIE);
        assertEquals(ProgressionService.ResultCode.LEVEL_CAP, capped.code());
        assertSame(state, capped.state());

        ProgressionService.Result ascended = progression.ascend(state, ZOMBIE);
        assertTrue(ascended.accepted(), ascended.detail());
        state = ascended.state();
        assertEquals(3, state.characters().get(ZOMBIE).currentStar());
        assertEquals(cap, state.characters().get(ZOMBIE).level());

        ProgressionService.Result party = progression.setParty(state, List.of(ZOMBIE));
        assertTrue(party.accepted(), party.detail());
        state = party.state();
        assertEquals(List.of(ZOMBIE), state.party());
        assertTrue(parsed.registry().characters().get(ZOMBIE).squadCost() <= state.partyCapacity());

        PlayerProgress afterReconnectEquivalentReload = savedDataRoundTrip(owner, state);
        assertEquals(state, afterReconnectEquivalentReload);
    }

    @Test
    void authoredLauncherRejectsWorldBindingsThatDoNotMatchAuthoredEnemySlotCount() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        DefinitionRepository definitions = new DefinitionRepository();
        definitions.install(parsed.registry(), parsed.hash());
        BattleManager manager = new BattleManager();
        AuthoredEncounterLauncher launcher = new AuthoredEncounterLauncher(manager, definitions);
        CharacterDefinition hero = parsed.registry().characters().get(HERO);

        assertThrows(IllegalArgumentException.class, () -> launcher.open(
                ENCOUNTER,
                UUID.randomUUID(),
                List.of(new AuthoredEncounterLauncher.PlayerSlot(
                        "hero", new CharacterProgress(HERO, hero.originStar(), 6, 70), UUID.randomUUID())),
                List.of(UUID.randomUUID()),
                UUID.randomUUID(),
                22L));
        assertEquals(0, manager.activeBattleCount());
    }

    private static void driveAuthoredBattleToVictory(
            BattleManager manager,
            AuthoredEncounterLauncher.Launch launch
    ) {
        BattleInstance battle = launch.battle();
        BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
        int steps = 0;
        while (battle.outcome() == BattleInstance.Outcome.ONGOING) {
            assertTrue(++steps < 100, "authored battle failed to converge: state=" + battle.state()
                    + " actor=" + battle.currentActorId() + " events=" + battle.eventLog().size());
            String actorId = battle.currentActorId();
            if (battle.participant(actorId).team() == BattleTeam.PLAYER) {
                CharacterDefinition character = launch.definitionContext().definitions().characters()
                        .get(launch.definitionContext().characterId(actorId));
                ActionDefinition basic = launch.definitionContext().definitions().actions().get(character.basicAction());
                String target = firstLiving(battle, BattleTeam.ENEMY);
                BattleCommand command = new BattleCommand(
                        battle.revision(), actorId, basic.id(), "m4-e2e-" + steps, List.of(target));
                BattleNetworkGateway.Result result = gateway.submit(
                        manager.binding(battle.battleId(), actorId).orElseThrow().entityId(),
                        BattleNetworkPayloads.BattleCommandC2S.of(battle.battleId(), command).decode());
                assertTrue(result.accepted(), result.code() + ":" + result.detail());
            } else {
                String intentAction = battle.enemyIntent(actorId).actionId();
                battle.resolveEnemyStub();
                if (!"recover".equals(intentAction)) {
                    ActionDefinition action = launch.definitionContext().definitions().actions().get(intentAction);
                    assertNotNull(action, "missing authored enemy intent action " + intentAction);
                    new BattleActionExecutor(
                            launch.definitionContext().definitions(),
                            (id, participantId) -> launch.definitionContext().characterId(participantId))
                            .execute(battle, actorId, action, List.of(firstLiving(battle, BattleTeam.PLAYER)));
                }
                battle.finishResolution();
                if (battle.outcome() == BattleInstance.Outcome.ONGOING && battle.combatState(actorId).alive()) {
                    publishBasicIntent(battle, launch.definitionContext(), actorId);
                }
            }
        }
    }

    private static void publishBasicIntent(
            BattleInstance battle,
            BattleDefinitionContext context,
            String enemyId
    ) {
        CharacterDefinition enemy = context.definitions().characters().get(context.characterId(enemyId));
        ActionDefinition basic = context.definitions().actions().get(enemy.basicAction());
        EnemyIntent.Targeting targeting = "MULTI".equals(basic.targeting().shape())
                ? EnemyIntent.Targeting.ALL : EnemyIntent.Targeting.SINGLE;
        battle.setEnemyIntent(enemyId, new EnemyIntent(
                basic.id(), EnemyIntent.Type.ATTACK, targeting, EnemyIntent.Risk.NORMAL, true, null));
    }

    private static String firstLiving(BattleInstance battle, BattleTeam team) {
        return battle.actorOrder().stream()
                .filter(id -> battle.participant(id).team() == team)
                .filter(id -> battle.combatState(id).alive())
                .findFirst()
                .orElseThrow();
    }

    private static PlayerProgress savedDataRoundTrip(UUID owner, PlayerProgress state) {
        TurnboundProgressSavedData saved = new TurnboundProgressSavedData();
        saved.put(owner, state);
        JsonElement encoded = TurnboundProgressSavedData.CODEC.encodeStart(JsonOps.INSTANCE, saved).getOrThrow();
        TurnboundProgressSavedData decoded = TurnboundProgressSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        return decoded.get(owner).orElseThrow();
    }
}
