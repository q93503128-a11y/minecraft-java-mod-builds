package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-only pre-mutation authority gate for network commands.
 * The network layer never mutates BattleInstance directly; accepted commands must still pass the M1 BattleCommandService.
 */
public final class BattleNetworkGateway {
    public enum ResultCode {
        AUTHORIZED,
        BATTLE_NOT_FOUND,
        SENDER_NOT_BOUND,
        ACTOR_NOT_OWNED,
        STALE_REVISION,
        WRONG_ACTOR_WINDOW
    }

    public record Result(ResultCode code, BattleInstance battle, String detail) {
        public boolean authorized() { return code == ResultCode.AUTHORIZED; }
    }

    private final BattleManager manager;

    public BattleNetworkGateway(BattleManager manager) {
        if (manager == null) throw new IllegalArgumentException("manager must not be null");
        this.manager = manager;
    }

    public Result authorize(UUID senderEntityId, BattleNetworkPayloads.DecodedCommand incoming) {
        if (senderEntityId == null) throw new IllegalArgumentException("senderEntityId must not be null");
        if (incoming == null) throw new IllegalArgumentException("incoming must not be null");

        Optional<BattleInstance> battleOpt = manager.battle(incoming.battleId());
        if (battleOpt.isEmpty()) return new Result(ResultCode.BATTLE_NOT_FOUND, null, "battle_not_found");
        BattleInstance battle = battleOpt.get();

        Optional<EntityParticipantBinding> senderBinding = manager.binding(incoming.battleId(), incoming.actorId());
        if (senderBinding.isEmpty() || !senderBinding.get().entityId().equals(senderEntityId)) {
            return new Result(ResultCode.SENDER_NOT_BOUND, battle, "sender_not_bound");
        }
        if (!senderBinding.get().participantId().equals(incoming.actorId())) {
            return new Result(ResultCode.ACTOR_NOT_OWNED, battle, "actor_not_owned");
        }
        if (incoming.expectedRevision() != battle.revision()) {
            return new Result(ResultCode.STALE_REVISION, battle,
                    "expected=" + incoming.expectedRevision() + " actual=" + battle.revision());
        }
        if (!battle.currentActorId().equals(incoming.actorId())) {
            return new Result(ResultCode.WRONG_ACTOR_WINDOW, battle,
                    "current=" + battle.currentActorId() + " requested=" + incoming.actorId());
        }
        return new Result(ResultCode.AUTHORIZED, battle, "authorized_for_strict_command_service");
    }
}
