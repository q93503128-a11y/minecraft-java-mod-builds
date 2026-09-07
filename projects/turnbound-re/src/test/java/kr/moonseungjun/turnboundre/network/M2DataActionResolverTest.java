package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M2DataActionResolverTest {
    private static final String BASIC = "turnbound_re:test_basic";
    private static final String SKILL = "turnbound_re:test_skill";
    private static final String BURST = "turnbound_re:test_burst";
    private static final String UNOWNED = "turnbound_re:unowned_skill";
    private static final String HERO = "turnbound_re:test_hero";

    private static BattleParticipant player() {
        return new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30);
    }

    private static BattleParticipant enemy() {
        return new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 10, 5, 30);
    }

    private static DefinitionRegistry definitions() {
        var basic = action(BASIC, "BASIC", 10, 25, 5);
        var skill = action(SKILL, "SKILL", -20, 40, 10);
        var burst = action(BURST, "BURST", -100, 100, 25);
        var unowned = action(UNOWNED, "SKILL", -20, 20, 5);
        var character = new CharacterDefinition(
                HERO, "minecraft:zombie", 5, 5, List.of("STRIKER"),
                new CharacterDefinition.Stats(100, 25, 15, 20, 30),
                new CharacterDefinition.Growth(5, 1.5, 1, 0.2, 0.5), Map.of(),
                Map.of(
                        "MELEE", "NORMAL", "PROJECTILE", "NORMAL", "FIRE", "NORMAL",
                        "BLAST", "NORMAL", "ARCANE", "NORMAL", "VOID", "NORMAL"),
                BASIC, List.of(SKILL), BURST, List.of(),
                new CharacterDefinition.Availability("DEBUG", "turnbound_re:test"), HERO);
        return DefinitionRegistry.create(List.of(basic, skill, burst, unowned), List.of(character));
    }

    private static ActionDefinition action(String id, String kind, int energyDelta, int hpPower, int poisePower) {
        return new ActionDefinition(
                id, kind, energyDelta, hpPower, poisePower, "MELEE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("DAMAGE", "", 1.0D, 0, 1.0D)));
    }

    private static Fixture fixture(DataActionResolver.RuntimeEligibility eligibility) {
        UUID battleId = UUID.randomUUID();
        UUID playerEntity = UUID.randomUUID();
        List<BattleParticipant> participants = List.of(player(), enemy());
        BattleInstance battle = new BattleInstance(battleId, 101L, participants);
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding("p1", playerEntity),
                new EntityParticipantBinding("e1", UUID.randomUUID())), participants);
        battle.start();

        DataActionResolver resolver = new DataActionResolver(
                definitions(),
                (id, participantId) -> "p1".equals(participantId) ? HERO : null,
                eligibility);
        return new Fixture(battleId, playerEntity, battle, new BattleNetworkGateway(manager, resolver));
    }

    @Test
    void ownedSkillResolvesFromCanonicalRegistryAndMutatesThroughStrictGate() {
        Fixture f = fixture(DataActionResolver.ALLOW_ALL_RUNTIME);
        long beforeRevision = f.battle.revision();
        var command = new BattleNetworkPayloads.DecodedCommand(
                f.battleId, beforeRevision, "p1", SKILL, "cmd-skill-ok", List.of("e1"));

        BattleNetworkGateway.Result result = f.gateway.submit(f.playerEntity, command);

        assertTrue(result.accepted(), result.detail());
        assertTrue(f.battle.revision() > beforeRevision);
        assertTrue(f.battle.eventLog().stream().anyMatch(e -> SKILL.equals(e.detail())));
    }

    @Test
    void unownedDataActionIsRejectedWithoutMutation() {
        Fixture f = fixture(DataActionResolver.ALLOW_ALL_RUNTIME);
        long revision = f.battle.revision();
        int events = f.battle.eventLog().size();
        int energy = f.battle.combatState("p1").energy();
        var command = new BattleNetworkPayloads.DecodedCommand(
                f.battleId, revision, "p1", UNOWNED, "cmd-unowned", List.of("e1"));

        BattleNetworkGateway.Result result = f.gateway.submit(f.playerEntity, command);

        assertEquals(BattleNetworkGateway.ResultCode.COMMAND_REJECTED, result.code());
        assertEquals("ACTION_NOT_OWNED", result.detail());
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());
        assertEquals(energy, f.battle.combatState("p1").energy());
    }

    @Test
    void cooldownAndStatusEligibilityStayServerAuthoritativeAndMutationFree() {
        Fixture cooldown = fixture(new DataActionResolver.RuntimeEligibility() {
            @Override public boolean cooldownReady(UUID battleId, String participantId, String actionId) { return false; }
            @Override public boolean statusEligible(UUID battleId, String participantId, String actionId) { return true; }
        });
        long cooldownRevision = cooldown.battle.revision();
        var cooldownCommand = new BattleNetworkPayloads.DecodedCommand(
                cooldown.battleId, cooldownRevision, "p1", SKILL, "cmd-cooldown", List.of("e1"));
        BattleNetworkGateway.Result cooldownResult = cooldown.gateway.submit(cooldown.playerEntity, cooldownCommand);
        assertEquals(BattleNetworkGateway.ResultCode.COMMAND_REJECTED, cooldownResult.code());
        assertEquals("ACTION_ON_COOLDOWN", cooldownResult.detail());
        assertEquals(cooldownRevision, cooldown.battle.revision());

        Fixture status = fixture(new DataActionResolver.RuntimeEligibility() {
            @Override public boolean cooldownReady(UUID battleId, String participantId, String actionId) { return true; }
            @Override public boolean statusEligible(UUID battleId, String participantId, String actionId) { return false; }
        });
        long statusRevision = status.battle.revision();
        var statusCommand = new BattleNetworkPayloads.DecodedCommand(
                status.battleId, statusRevision, "p1", SKILL, "cmd-status", List.of("e1"));
        BattleNetworkGateway.Result statusResult = status.gateway.submit(status.playerEntity, statusCommand);
        assertEquals(BattleNetworkGateway.ResultCode.COMMAND_REJECTED, statusResult.code());
        assertEquals("STATUS_BLOCKED", statusResult.detail());
        assertEquals(statusRevision, status.battle.revision());
    }

    @Test
    void dataTargetPolicyIsEnforcedAndUnknownActionsAreNeverInvented() {
        Fixture f = fixture(DataActionResolver.ALLOW_ALL_RUNTIME);
        long revision = f.battle.revision();
        int events = f.battle.eventLog().size();
        var wrongTarget = new BattleNetworkPayloads.DecodedCommand(
                f.battleId, revision, "p1", SKILL, "cmd-self", List.of("p1"));
        BattleNetworkGateway.Result targetResult = f.gateway.submit(f.playerEntity, wrongTarget);
        assertEquals(BattleNetworkGateway.ResultCode.COMMAND_REJECTED, targetResult.code());
        assertEquals("INVALID_TARGET", targetResult.detail());
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());

        var unknown = new BattleNetworkPayloads.DecodedCommand(
                f.battleId, revision, "p1", "turnbound_re:not_loaded", "cmd-unknown", List.of("e1"));
        BattleNetworkGateway.Result unknownResult = f.gateway.submit(f.playerEntity, unknown);
        assertEquals(BattleNetworkGateway.ResultCode.DATA_ACTION_NOT_RESOLVED, unknownResult.code());
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());
    }

    private record Fixture(
            UUID battleId,
            UUID playerEntity,
            BattleInstance battle,
            BattleNetworkGateway gateway
    ) {}
}
