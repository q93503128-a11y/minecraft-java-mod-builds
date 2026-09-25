package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Server-only persistence boundary for bounded R01 objective class attribution. */
public final class R01QuestAttributionService {
    private R01QuestAttributionService() {
    }

    public static R01QuestAttributionState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01QuestAttributionAttachments.QUEST_ATTRIBUTION,
                R01QuestAttributionState.initial()
        );
    }

    public static R01QuestAttributionState recordDustAction(
            ServerPlayer player,
            R01PlayerState.QuarryRoadAction action,
            RootClass owner
    ) {
        R01QuestAttributionState current = state(player);
        R01QuestAttributionState next = current.recordDustAction(action, owner);
        if (!current.equals(next)) {
            player.setAttached(
                    R01QuestAttributionAttachments.QUEST_ATTRIBUTION,
                    next
            );
        }
        return next;
    }

    public static Optional<RootClass> dustMajorityClass(ServerPlayer player) {
        return state(player).dustMajorityClass();
    }
}
