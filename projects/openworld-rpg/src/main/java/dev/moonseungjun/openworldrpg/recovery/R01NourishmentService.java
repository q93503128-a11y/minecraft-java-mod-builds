package dev.moonseungjun.openworldrpg.recovery;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerHealingAuthority;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildPublisher;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned R01 meal use and one-at-a-time Nourishment authority. */
public final class R01NourishmentService {
    public static final long DURATION_ACTIVE_TICKS = 20L * 60L * 20L;
    public static final int USE_DURATION_TICKS = 24;
    public static final double MEAL_HEAL_MAX_HP_FRACTION = 0.15;
    private static final String ACTION_ID =
            "openworld_rpg:action/meal_use";

    private static final ConcurrentHashMap<UUID, MealUseAction> ACTIVE_USES =
            new ConcurrentHashMap<>();

    private R01NourishmentService() {
    }

    public static R01NourishmentState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01NourishmentAttachments.STATE,
                R01NourishmentState.initial()
        );
    }

    public static Optional<R01NourishmentMeal> activeMeal(
            ServerPlayer player
    ) {
        return state(player).activeMeal(
                PlayerActiveWorldTimeService.state(player)
                        .activeTicks()
        );
    }

    public static double maxHealthBonus(ServerPlayer player) {
        return activeMagnitude(
                player,
                R01NourishmentMeal.Effect.MAX_HP
        );
    }

    public static double staminaRecoveryBonus(ServerPlayer player) {
        return activeMagnitude(
                player,
                R01NourishmentMeal.Effect.STAMINA_RECOVERY
        );
    }

    public static double manaRecoveryBonus(ServerPlayer player) {
        return activeMagnitude(
                player,
                R01NourishmentMeal.Effect.MANA_RECOVERY
        );
    }

    public static StartResult tryStart(
            ServerPlayer player,
            R01NourishmentMeal meal
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(meal, "meal");
        UUID playerId = player.getUUID();
        long nowTick = player.level().getGameTime();

        MealUseAction existing = ACTIVE_USES.get(playerId);
        if (existing != null) {
            return new StartResult(
                    StartStatus.ALREADY_ACTIVE,
                    Optional.of(existing)
            );
        }
        if (!player.isAlive()) {
            return new StartResult(
                    StartStatus.INVALID_STATE,
                    Optional.empty()
            );
        }
        if (CombatStateServices.states()
                .getOrCreate(playerId, nowTick)
                .isCombatActive(nowTick)) {
            return new StartResult(
                    StartStatus.IN_COMBAT,
                    Optional.empty()
            );
        }
        boolean carried = PlayerInventoryService.state(player)
                .backpack()
                .occupied()
                .stream()
                .anyMatch(entry ->
                        entry.item().itemId().equals(meal.itemId())
                                && entry.item().equipmentProjection().isEmpty()
                );
        if (!carried) {
            return new StartResult(
                    StartStatus.MISSING_MEAL,
                    Optional.empty()
            );
        }

        var begin = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ACTION_ID,
                        USE_DURATION_TICKS,
                        USE_DURATION_TICKS,
                        1.0
                )
        );
        if (!begin.accepted()) {
            return new StartResult(
                    StartStatus.BUSY,
                    Optional.empty()
            );
        }

        MealUseAction action = new MealUseAction(
                meal,
                nowTick,
                begin.endTick()
        );
        ACTIVE_USES.put(playerId, action);
        return new StartResult(
                StartStatus.STARTED,
                Optional.of(action)
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            MealUseAction action = ACTIVE_USES.get(playerId);
            if (action == null) {
                continue;
            }
            long nowTick = player.level().getGameTime();

            if (!player.isAlive()) {
                ACTIVE_USES.remove(playerId, action);
                continue;
            }

            if (nowTick < action.completeAtTick()) {
                PlayerActionRuntimeState.Snapshot snapshot =
                        ProjectPlayerActionRuntime.snapshot(
                                playerId,
                                nowTick
                        );
                if (!snapshot.active()
                        || snapshot.kind()
                                != PlayerActionRuntimeState.WindowKind.ACTION
                        || !ACTION_ID.equals(snapshot.actionId())) {
                    ACTIVE_USES.remove(playerId, action);
                }
                continue;
            }

            var consume = PlayerInventoryService.consumeBackpackStackable(
                    player,
                    action.meal().itemId(),
                    1
            );
            if (!consume.consumed()) {
                ACTIVE_USES.remove(playerId, action);
                continue;
            }

            double effectStrengthBonus =
                    PlayerEquipmentService.state(player)
                            .aggregatePotionFoodEffectStrengthBonus();
            double healBase = player.getMaxHealth()
                    * MEAL_HEAL_MAX_HP_FRACTION
                    * (1.0 + effectStrengthBonus);
            double receivedBonus =
                    PlayerEquipmentService.state(player)
                            .aggregateHealingReceivedBonus();
            player.heal((float) PlayerHealingAuthority
                    .receivedHealingAmount(
                            healBase,
                            receivedBonus
                    ));

            long activeTick =
                    PlayerActiveWorldTimeService.state(player)
                            .activeTicks();
            replace(
                    player,
                    state(player).apply(
                            action.meal(),
                            activeTick,
                            DURATION_ACTIVE_TICKS
                    )
            );
            PlayerCombatBuildPublisher.refresh(player);
            ACTIVE_USES.remove(playerId, action);
        }

        if (Math.floorMod(server.getTickCount(), 20) != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long activeTick =
                    PlayerActiveWorldTimeService.state(player)
                            .activeTicks();
            R01NourishmentState current = state(player);
            R01NourishmentState next =
                    current.clearExpired(activeTick);
            if (!current.equals(next)) {
                replace(player, next);
                PlayerCombatBuildPublisher.refresh(player);
            }
        }
    }

    public static void reconcile(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long activeTick =
                PlayerActiveWorldTimeService.state(player)
                        .activeTicks();
        R01NourishmentState current = state(player);
        R01NourishmentState next =
                current.clearExpired(activeTick);
        if (!current.equals(next)) {
            replace(player, next);
        }
    }

    public static void disconnect(UUID playerId) {
        ACTIVE_USES.remove(
                Objects.requireNonNull(playerId, "playerId")
        );
    }

    private static double activeMagnitude(
            ServerPlayer player,
            R01NourishmentMeal.Effect effect
    ) {
        Optional<R01NourishmentMeal> meal = activeMeal(player);
        if (meal.isEmpty()
                || meal.orElseThrow().effect() != effect) {
            return 0.0;
        }
        double strengthBonus =
                PlayerEquipmentService.state(player)
                        .aggregatePotionFoodEffectStrengthBonus();
        return meal.orElseThrow().baseMagnitude()
                * (1.0 + strengthBonus);
    }

    private static void replace(
            ServerPlayer player,
            R01NourishmentState next
    ) {
        R01NourishmentState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    R01NourishmentAttachments.STATE,
                    next
            );
        }
    }

    public enum StartStatus {
        STARTED,
        ALREADY_ACTIVE,
        INVALID_STATE,
        IN_COMBAT,
        MISSING_MEAL,
        BUSY
    }

    public record StartResult(
            StartStatus status,
            Optional<MealUseAction> action
    ) {
        public StartResult {
            Objects.requireNonNull(status, "status");
            action = Objects.requireNonNull(action, "action");
            if ((status == StartStatus.STARTED
                    || status == StartStatus.ALREADY_ACTIVE)
                    != action.isPresent()) {
                throw new IllegalArgumentException(
                        "Meal-use start status/action mismatch."
                );
            }
        }
    }

    public record MealUseAction(
            R01NourishmentMeal meal,
            long startedAtTick,
            long completeAtTick
    ) {
        public MealUseAction {
            Objects.requireNonNull(meal, "meal");
            if (startedAtTick < 0L
                    || completeAtTick
                            != startedAtTick + USE_DURATION_TICKS) {
                throw new IllegalArgumentException(
                        "Meal-use timing mismatch."
                );
            }
        }
    }
}
