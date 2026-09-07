package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.ActionUsePolicy;
import kr.moonseungjun.turnboundre.battle.BattleActionExecutor;
import kr.moonseungjun.turnboundre.battle.BattleCommandService;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.data.ActionDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-only authority and strict-command gateway for C2S battle commands. */
public final class BattleNetworkGateway {
    public enum ResultCode {
        ACCEPTED,
        BATTLE_NOT_FOUND,
        SENDER_NOT_BOUND,
        ACTOR_NOT_OWNED,
        STALE_REVISION,
        WRONG_ACTOR_WINDOW,
        STRICT_GATE_NOT_REGISTERED,
        DATA_ACTION_NOT_RESOLVED,
        COMMAND_REJECTED
    }

    public record Result(ResultCode code, BattleInstance battle, int eventStartIndex, String detail) {
        public boolean accepted() { return code == ResultCode.ACCEPTED; }
    }

    private record ResolvedAction(ActionDefinition definition, ActionUsePolicy policy, BattleDefinitionContext context) {}

    private final BattleManager manager;

    public BattleNetworkGateway(BattleManager manager) {
        if (manager == null) throw new IllegalArgumentException("manager must not be null");
        this.manager = manager;
    }

    public Result submit(UUID senderEntityId, BattleNetworkPayloads.DecodedCommand incoming) {
        if (senderEntityId == null) throw new IllegalArgumentException("senderEntityId must not be null");
        if (incoming == null) throw new IllegalArgumentException("incoming must not be null");

        Optional<BattleInstance> battleOpt = manager.battle(incoming.battleId());
        if (battleOpt.isEmpty()) return new Result(ResultCode.BATTLE_NOT_FOUND, null, -1, "battle_not_found");
        BattleInstance battle = battleOpt.get();
        int eventStart = battle.eventLog().size();

        Optional<EntityParticipantBinding> senderBinding = manager.binding(incoming.battleId(), incoming.actorId());
        if (senderBinding.isEmpty() || !senderBinding.get().entityId().equals(senderEntityId)) {
            return new Result(ResultCode.SENDER_NOT_BOUND, battle, eventStart, "sender_not_bound");
        }
        if (!senderBinding.get().participantId().equals(incoming.actorId())) {
            return new Result(ResultCode.ACTOR_NOT_OWNED, battle, eventStart, "actor_not_owned");
        }
        if (incoming.expectedRevision() != battle.revision()) {
            return new Result(ResultCode.STALE_REVISION, battle, eventStart,
                    "expected=" + incoming.expectedRevision() + " actual=" + battle.revision());
        }
        if (!battle.currentActorId().equals(incoming.actorId())) {
            return new Result(ResultCode.WRONG_ACTOR_WINDOW, battle, eventStart,
                    "current=" + battle.currentActorId() + " requested=" + incoming.actorId());
        }

        Optional<BattleCommandService> strictGate = manager.commandService(incoming.battleId());
        if (strictGate.isEmpty()) {
            return new Result(ResultCode.STRICT_GATE_NOT_REGISTERED, battle, eventStart,
                    "network encounter missing persistent command service");
        }

        ResolvedAction resolved = resolveUniversal(incoming.actionId());
        if (resolved == null) resolved = resolveData(incoming);
        if (resolved == null) {
            return new Result(ResultCode.DATA_ACTION_NOT_RESOLVED, battle, eventStart, incoming.actionId());
        }

        BattleCommandService.Result commandResult = strictGate.get().submit(incoming.toCore(), resolved.definition(), resolved.policy());
        if (commandResult != BattleCommandService.Result.ACCEPTED) {
            return new Result(ResultCode.COMMAND_REJECTED, battle, eventStart, commandResult.name());
        }

        if (resolved.context() != null) {
            BattleDefinitionContext context = resolved.context();
            BattleActionExecutor executor = new BattleActionExecutor(
                    context.definitions(), (battleId, participantId) -> context.characterId(participantId));
            executor.execute(battle, incoming.actorId(), resolved.definition(), incoming.targetIds());
            battle.finishResolution();
        }
        return new Result(ResultCode.ACCEPTED, battle, eventStart, "accepted");
    }

    private ResolvedAction resolveData(BattleNetworkPayloads.DecodedCommand incoming) {
        BattleDefinitionContext context = manager.definitionContext(incoming.battleId()).orElse(null);
        if (context == null) return null;
        DataActionResolver resolver = new DataActionResolver(
                context.definitions(),
                (battleId, participantId) -> context.characterId(participantId),
                DataActionResolver.ALLOW_ALL_RUNTIME);
        return resolver.resolve(incoming.battleId(), incoming.actorId(), incoming.actionId())
                .map(it -> new ResolvedAction(it.definition(), it.policy(), context))
                .orElse(null);
    }

    public static List<kr.moonseungjun.turnboundre.battle.BattleEvent> eventsSince(Result result) {
        if (result == null || result.battle() == null || result.eventStartIndex() < 0) return List.of();
        List<kr.moonseungjun.turnboundre.battle.BattleEvent> all = result.battle().eventLog();
        if (result.eventStartIndex() >= all.size()) return List.of();
        return List.copyOf(all.subList(result.eventStartIndex(), all.size()));
    }

    private static ResolvedAction resolveUniversal(String actionId) {
        return switch (actionId) {
            case "basic" -> new ResolvedAction(
                    new ActionDefinition("basic", "BASIC", 0, 0, 0), ActionUsePolicy.singleEnemy(), null);
            case "guard" -> new ResolvedAction(
                    new ActionDefinition("guard", "GUARD", 0, 0, 0), ActionUsePolicy.self(), null);
            default -> null;
        };
    }
}
