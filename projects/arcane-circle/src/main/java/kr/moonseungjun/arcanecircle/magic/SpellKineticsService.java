package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps authoritative combat synchronized with authored presentation. Instant spells still
 * resolve immediately unless their profile has an explicit visible wind-up; projectile and
 * sky-drop damage lands on the same server tick as the visible impact, never afterward.
 */
public final class SpellKineticsService {
    private static final Map<UUID, List<PendingCast>> PENDING = new HashMap<>();
    private static final int MAX_PENDING_PER_PLAYER = 32;

    private SpellKineticsService() {}

    public static boolean launch(ServerPlayer player, MagicPlayerData.CastPreparation cast,
                                 CombatGrowthService.Snapshot snapshot) {
        SpellArchetype.Mode mode = SpellArchetype.mode(cast.spell().id());
        int circle = Math.max(1, Math.min(9, cast.spell().circle()));
        WorldMagicService.release(player, cast);

        int presentationImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
                SpellCastingService.kineticDistance(player, cast.spell(), cast.range()));

        if (mode == SpellArchetype.Mode.INSTANT) {
            if (presentationImpactDelay > 1) {
                enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                        0, 1, cast.power(), false));
                return true;
            }
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }
        if (mode == SpellArchetype.Mode.PROJECTILE) {
            if (presentationImpactDelay <= 1) {
                boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(), cast.range(), cast.power());
                SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
                return executed;
            }
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                    0, 1, cast.power(), false));
            return true;
        }

        int totalPulses;
        int interval;
        double pulsePower;
        switch (mode) {
            case CHANNEL -> {
                totalPulses = Math.min(6, 3 + circle / 3);
                interval = Math.max(2, 4 - circle / 4);
                pulsePower = cast.power() * 1.08 / totalPulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§b[집중 방출] §f" + cast.spell().name() + " §7· " + totalPulses + "회 연속 타격"), 45);
            }
            case FIELD -> {
                totalPulses = Math.min(7, 4 + circle / 2);
                interval = Math.max(7, 12 - circle / 2);
                pulsePower = cast.power() * 0.96 / totalPulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§d[영역 전개] §f" + cast.spell().name() + " §7· " + totalPulses + "회 지속 맥동"), 55);
            }
            default -> throw new IllegalStateException("unhandled spell mode " + mode);
        }

        // Ordinary channels/fields still start on the cast tick. Authored sky rituals and other
        // explicitly telegraphed spells instead begin their first authoritative pulse exactly when
        // the visible payload reaches the impact point; later pulses retain the original cadence.
        if (presentationImpactDelay > 1) {
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                    interval, totalPulses, pulsePower, false));
            return true;
        }

        boolean first = SpellCastingService.executeResolved(player, cast.spell().id(),
                cast.range(), pulsePower);
        int remaining = totalPulses - 1;
        if (remaining <= 0) {
            SpellCastingService.finishKineticCast(player, cast, snapshot, first);
            return first;
        }

        enqueue(player, new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
    }

    private static void enqueue(ServerPlayer player, PendingCast pending) {
        List<PendingCast> queue = PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        while (queue.size() >= MAX_PENDING_PER_PLAYER) {
            PendingCast dropped = queue.removeFirst();
            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.snapshot(), dropped.anyExecuted());
        }
        queue.add(pending);
    }

    public static void tick(ServerPlayer player) {
        List<PendingCast> casts = PENDING.get(player.getUUID());
        if (casts == null || casts.isEmpty()) return;
        if (!player.isAlive() || player.isSpectator()) {
            casts.clear();
            PENDING.remove(player.getUUID());
            return;
        }

        long now = clock(player);
        Iterator<PendingCast> iterator = casts.iterator();
        while (iterator.hasNext()) {
            PendingCast pending = iterator.next();
            if (now < pending.nextTick()) continue;
            boolean executed = SpellCastingService.executeResolved(player, pending.cast().spell().id(),
                    pending.cast().range(), pending.pulsePower());
            int remaining = pending.remainingPulses() - 1;
            boolean any = pending.anyExecuted() || executed;
            if (remaining <= 0) {
                iterator.remove();
                SpellCastingService.finishKineticCast(player, pending.cast(), pending.snapshot(), any);
            } else {
                pending.advance(now + pending.interval(), remaining, any);
            }
        }
        if (casts.isEmpty()) PENDING.remove(player.getUUID());
    }

    public static void clear(UUID playerId) {
        PENDING.remove(playerId);
    }

    public static void clearAll() {
        PENDING.clear();
    }

    private static long clock(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().overworld().getGameTime();
    }

    private static final class PendingCast {
        private final MagicPlayerData.CastPreparation cast;
        private final CombatGrowthService.Snapshot snapshot;
        private long nextTick;
        private final int interval;
        private int remainingPulses;
        private final double pulsePower;
        private boolean anyExecuted;

        private PendingCast(MagicPlayerData.CastPreparation cast,
                            CombatGrowthService.Snapshot snapshot,
                            long nextTick,
                            int interval,
                            int remainingPulses,
                            double pulsePower,
                            boolean anyExecuted) {
            this.cast = cast;
            this.snapshot = snapshot;
            this.nextTick = nextTick;
            this.interval = interval;
            this.remainingPulses = remainingPulses;
            this.pulsePower = pulsePower;
            this.anyExecuted = anyExecuted;
        }

        MagicPlayerData.CastPreparation cast() { return cast; }
        CombatGrowthService.Snapshot snapshot() { return snapshot; }
        long nextTick() { return nextTick; }
        int interval() { return interval; }
        int remainingPulses() { return remainingPulses; }
        double pulsePower() { return pulsePower; }
        boolean anyExecuted() { return anyExecuted; }

        void advance(long next, int remaining, boolean executed) {
            this.nextTick = next;
            this.remainingPulses = remaining;
            this.anyExecuted = executed;
        }
    }
}
