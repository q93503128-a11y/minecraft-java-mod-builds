package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectBasicAttackRules;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentLoadoutState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side cadence firewall for Better Combat hit requests against project-owned actors.
 *
 * <p>Better Combat owns input/animation cadence, but project damage authority does not trust a
 * client packet as proof that enough time elapsed. One accepted swing may hit multiple distinct
 * targets in the same server tick, while duplicate hits and early follow-up swings fail closed.</p>
 */
public final class ProjectBasicAttackCadenceRuntime {
    private static final ConcurrentHashMap<UUID, SwingState> STATES =
            new ConcurrentHashMap<>();

    private ProjectBasicAttackCadenceRuntime() {
    }

    public static boolean authorize(
            UUID playerId,
            ProjectWeaponFamily family,
            double attackSpeedBonus,
            int comboCount,
            int targetEntityId,
            long gameTick
    ) {
        if (playerId == null || family == null || comboCount < 0 || gameTick < 0L) {
            return false;
        }
        if (!Double.isFinite(attackSpeedBonus)
                || attackSpeedBonus < 0.0
                || attackSpeedBonus > PlayerEquipmentLoadoutState.ATTACK_SPEED_GEAR_CAP
                || !ProjectBasicAttackRules.supportsBetterCombatMelee(family)) {
            return false;
        }

        double eventsPerSecond =
                family.basicAttackEventsPerSecond() * (1.0 + attackSpeedBonus);
        double periodTicks = 20.0 / eventsPerSecond;
        return STATES.computeIfAbsent(playerId, ignored -> new SwingState())
                .authorize(comboCount, targetEntityId, gameTick, periodTicks);
    }

    public static void disconnect(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }

    static int stateCount() {
        return STATES.size();
    }

    private static final class SwingState {
        private double nextAllowedTick = Double.NEGATIVE_INFINITY;
        private long currentSwingTick = Long.MIN_VALUE;
        private int currentComboCount = -1;
        private final Set<Integer> currentTargets = new HashSet<>();

        private synchronized boolean authorize(
                int comboCount,
                int targetEntityId,
                long gameTick,
                double periodTicks
        ) {
            if (gameTick == currentSwingTick && comboCount == currentComboCount) {
                return currentTargets.add(targetEntityId);
            }
            if (gameTick + 1.0e-9 < nextAllowedTick) {
                return false;
            }

            if (!Double.isFinite(nextAllowedTick)
                    || gameTick - nextAllowedTick > periodTicks) {
                nextAllowedTick = gameTick + periodTicks;
            } else {
                nextAllowedTick += periodTicks;
            }

            currentSwingTick = gameTick;
            currentComboCount = comboCount;
            currentTargets.clear();
            currentTargets.add(targetEntityId);
            return true;
        }
    }
}
