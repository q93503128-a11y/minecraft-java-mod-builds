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
 * Separates visual travel from authoritative combat. Projectiles resolve immediately so
 * networking/render duration can never add a hidden one-second damage delay.
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

        if (mode == SpellArchetype.Mode.INSTANT || mode == SpellArchetype.Mode.PROJECTILE) {
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
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

        // The first pulse resolves on the cast tick; only subsequent pulses are queued.
        boolean first = SpellCastingService.executeResolved(player, cast.spell().id(),
                cast.range(), pulsePower);
        int remaining = totalPulses - 1;
        if (remaining <= 0) {
            SpellCastingService.finishKineticCast(player, cast, snapshot, first);
            return first;
        }

        List<PendingCast> queue = PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        while (queue.size() >= MAX_PENDING_PER_PLAYER) {
            PendingCast dropped = queue.removeFirst();
            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.snapshot(), dropped.anyExecuted());
        }
        queue.add(new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
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
