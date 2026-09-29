package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineProjectSkillAccess;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned mutation/read API for persistent combat Lv, XP, Class Rank/XP and Attributes.
 */
public final class PlayerProgressionService {
    private PlayerProgressionService() {
    }

    public static PlayerProgressionState state(Player player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerProgressionAttachments.COMBAT_PROGRESSION,
                PlayerProgressionState.initial()
        );
    }

    public static PlayerProgressionState setCombatLevel(Player player, int combatLevel) {
        return replace(player, state(player).withCombatLevel(combatLevel));
    }

    /**
     * Exact current-Lv EXP loss bridge used by the automatic death penalty.
     *
     * <p>The caller persists the expected before/after values before invoking this method. Replays
     * therefore accept the already-applied value but fail closed if another mutation diverged.</p>
     */
    public static boolean applyCurrentLevelXpLoss(
            Player player,
            long expectedBefore,
            long expectedAfter
    ) {
        if (expectedBefore < 0L
                || expectedAfter < 0L
                || expectedAfter > expectedBefore) {
            throw new IllegalArgumentException(
                    "Invalid current-Lv EXP loss bounds."
            );
        }
        PlayerProgressionState current = state(player);
        if (current.combatXp() == expectedAfter) {
            return false;
        }
        if (current.combatXp() != expectedBefore) {
            throw new IllegalStateException(
                    "Current-Lv EXP diverged from prepared death penalty."
            );
        }
        replace(
                player,
                current.withCurrentCombatXp(expectedAfter)
        );
        return true;
    }

    public static PlayerProgressionState selectClass(Player player, RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        PlayerProgressionState current = state(player);
        boolean changed = !current.activeClass().equals(
                Optional.of(rootClass)
        );
        PlayerProgressionState next = replace(
                player,
                current.withActiveClass(rootClass)
        );
        if (changed && !player.level().isClientSide()) {
            CombatStateServices.clericGraceStates()
                    .reset(player.getUUID());
            CombatStateServices.clericDoctrineStates()
                    .reset(player.getUUID());
            CombatStateServices.clericSkillCastStates()
                    .reset(player.getUUID());
            long nowTick = player.level().getGameTime();
            CombatStateServices.states()
                    .getOrCreate(player.getUUID(), nowTick)
                    .resetUltimateCharge(nowTick);
        }
        SpellEngineProjectSkillAccess.refreshPublishedSkills(player);
        return next;
    }

    public static PlayerProgressionState setAllocation(
            Player player,
            RootClass rootClass,
            AttributeAllocation allocation
    ) {
        return replace(player, state(player).withAllocation(rootClass, allocation));
    }

    public static PlayerProgressionState creditCombatXpOnce(
            Player player,
            String transactionId,
            long amount
    ) {
        return replace(
                player,
                state(player).grantCombatXpOnce(transactionId, amount)
        );
    }

    public static PlayerProgressionState creditClassXpOnce(
            Player player,
            String transactionId,
            RootClass rewardClass,
            long amount
    ) {
        return replace(
                player,
                state(player).grantClassXpOnce(transactionId, rewardClass, amount)
        );
    }

    public static PlayerProgressionState forgetCombatXpTransaction(
            Player player,
            String transactionId
    ) {
        return replace(
                player,
                state(player).forgetCombatXpTransaction(transactionId)
        );
    }

    public static PlayerProgressionState forgetClassXpTransaction(
            Player player,
            String transactionId
    ) {
        return replace(
                player,
                state(player).forgetClassXpTransaction(transactionId)
        );
    }

    public static Optional<PlayerCombatBuildState> buildWith(
            Player player,
            EquipmentCombatState equipment
    ) {
        return state(player).buildWith(equipment);
    }

    private static PlayerProgressionState replace(
            Player player,
            PlayerProgressionState next
    ) {
        PlayerProgressionState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(PlayerProgressionAttachments.COMBAT_PROGRESSION, next);
        PlayerCombatBuildPublisher.refresh(player);
        return next;
    }
}
