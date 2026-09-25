package dev.moonseungjun.openworldrpg.recovery;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Applies canonical Recovery Belt effects only after the server-owned resolution point. */
public final class RecoveryEffectRuntime {
    private static final ConcurrentHashMap<UUID, FocusTail> FOCUS_TAILS =
            new ConcurrentHashMap<>();

    private RecoveryEffectRuntime() {
    }

    public static Application apply(
            ServerPlayer player,
            RecoveryConsumable consumable,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(consumable, "consumable");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }

        var resources = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        RecoveryEffectAuthority.Effect effect = RecoveryEffectAuthority.resolve(
                consumable,
                player.getMaxHealth(),
                resources.maxMana()
        );

        float healthBefore = player.getHealth();
        if (effect.hpRestore() > 0.0) {
            player.heal((float) effect.hpRestore());
        }
        double hpRestored = player.getHealth() - healthBefore;

        double manaBefore = resources.mana(nowTick);
        if (effect.immediateManaRestore() > 0.0) {
            resources.restoreMana(effect.immediateManaRestore(), nowTick);
        }
        double immediateManaRestored = resources.mana(nowTick) - manaBefore;

        if (effect.manaTailTicks() > 0 && effect.manaRestorePerTick() > 0.0) {
            FOCUS_TAILS.put(
                    player.getUUID(),
                    new FocusTail(
                            effect.manaRestorePerTick(),
                            effect.manaTailTicks()
                    )
            );
        }

        int cleansed = 0;
        if (effect.cleanseMinorDispellable()) {
            var statuses = CombatStateServices.negativeStatusStates()
                    .getOrCreate(player.getUUID());
            cleansed = statuses.cleanseTagged(
                    RecoveryEffectAuthority.MINOR_DISPELLABLE_TAG,
                    nowTick
            );
            statuses.applyNegativeBuildupResistance(
                    effect.negativeBuildupResistanceTicks(),
                    nowTick
            );
        }

        return new Application(
                consumable,
                hpRestored,
                immediateManaRestored,
                effect.manaTailTicks(),
                cleansed,
                effect.negativeBuildupResistanceTicks()
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            FocusTail tail = FOCUS_TAILS.get(playerId);
            if (tail == null) {
                continue;
            }

            long nowTick = player.level().getGameTime();
            var resources = CombatStateServices.states()
                    .getOrCreate(playerId, nowTick);
            resources.restoreMana(tail.manaPerTick(), nowTick);

            FocusTail next = tail.afterTick();
            if (next.remainingTicks() <= 0) {
                FOCUS_TAILS.remove(playerId, tail);
            } else {
                FOCUS_TAILS.replace(playerId, tail, next);
            }
        }
    }

    public static void disconnect(UUID playerId) {
        FOCUS_TAILS.remove(playerId);
    }

    public static int activeFocusTailCount() {
        return FOCUS_TAILS.size();
    }

    public record FocusTail(double manaPerTick, int remainingTicks) {
        public FocusTail {
            if (!Double.isFinite(manaPerTick) || manaPerTick <= 0.0) {
                throw new IllegalArgumentException(
                        "Focus tail manaPerTick must be finite and positive."
                );
            }
            if (remainingTicks < 0) {
                throw new IllegalArgumentException(
                        "Focus tail remainingTicks must be non-negative."
                );
            }
        }

        public FocusTail afterTick() {
            return new FocusTail(manaPerTick, Math.max(0, remainingTicks - 1));
        }
    }

    public record Application(
            RecoveryConsumable consumable,
            double hpRestored,
            double immediateManaRestored,
            int focusTailTicks,
            int cleansedStatuses,
            int negativeBuildupResistanceTicks
    ) {
        public Application {
            Objects.requireNonNull(consumable, "consumable");
            if (hpRestored < 0.0
                    || immediateManaRestored < 0.0
                    || focusTailTicks < 0
                    || cleansedStatuses < 0
                    || negativeBuildupResistanceTicks < 0) {
                throw new IllegalArgumentException(
                        "Recovery application values must be non-negative."
                );
            }
        }
    }
}
