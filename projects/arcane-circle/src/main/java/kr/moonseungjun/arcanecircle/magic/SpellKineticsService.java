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

public final class SpellKineticsService {
    private static final Map<UUID, List<PendingCast>> PENDING = new HashMap<>();
    private static final SpellArchetype.Mode[] KINETIC_MODES = {
            SpellArchetype.Mode.PROJECTILE,
            SpellArchetype.Mode.CHANNEL,
            SpellArchetype.Mode.FIELD
    };

    private SpellKineticsService() {}

    public static boolean launch(ServerPlayer player, MagicPlayerData.CastPreparation cast,
                                 CombatGrowthService.Snapshot snapshot) {
        SpellArchetype.Mode mode = SpellArchetype.mode(cast.spell().id());
        int circle = Math.max(1, Math.min(9, cast.spell().circle()));
        long now = clock(player);
        WorldMagicService.release(player, cast);

        if (mode == SpellArchetype.Mode.INSTANT) {
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }

        int pulses;
        int interval;
        int delay;
        double pulsePower;
        switch (mode) {
            case PROJECTILE -> {
                pulses = 1;
                interval = 1;
                delay = Math.max(4, Math.min(24,
                        (int) Math.ceil(Math.min(48.0, cast.range()) / (2.4 + circle * 0.22))));
                pulsePower = cast.power();
                ArcaneNoticeService.push(player, Component.literal(
                        "§7[투사체] §f" + cast.spell().name() + "§7이 목표 방향으로 비행합니다."), 35);
            }
            case CHANNEL -> {
                pulses = Math.min(6, 3 + circle / 3);
                interval = Math.max(2, 4 - circle / 4);
                delay = 1;
                pulsePower = cast.power() * 1.08 / pulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§b[집중 방출] §f" + cast.spell().name() + " §7· " + pulses + "회 연속 타격"), 45);
            }
            case FIELD -> {
                pulses = Math.min(7, 4 + circle / 2);
                interval = Math.max(7, 12 - circle / 2);
                delay = 1;
                pulsePower = cast.power() * 0.96 / pulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§d[영역 전개] §f" + cast.spell().name() + " §7· " + pulses + "회 지속 맥동"), 55);
            }
            default -> throw new IllegalStateException("unhandled spell mode " + mode);
        }

        PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
                .add(new PendingCast(cast, snapshot, mode, now + delay, interval,
                        pulses, pulsePower, false));
        return true;
    }

    public static void tick(ServerPlayer player) {
        List<PendingCast> casts = PENDING.get(player.getUUID());
        if (casts == null || casts.isEmpty()) return;
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
        private final SpellArchetype.Mode mode;
        private long nextTick;
        private final int interval;
        private int remainingPulses;
        private final double pulsePower;
        private boolean anyExecuted;

        private PendingCast(MagicPlayerData.CastPreparation cast,
                            CombatGrowthService.Snapshot snapshot,
                            SpellArchetype.Mode mode,
                            long nextTick,
                            int interval,
                            int remainingPulses,
                            double pulsePower,
                            boolean anyExecuted) {
            this.cast = cast;
            this.snapshot = snapshot;
            this.mode = mode;
            this.nextTick = nextTick;
            this.interval = interval;
            this.remainingPulses = remainingPulses;
            this.pulsePower = pulsePower;
            this.anyExecuted = anyExecuted;
        }

        MagicPlayerData.CastPreparation cast() { return cast; }
        CombatGrowthService.Snapshot snapshot() { return snapshot; }
        SpellArchetype.Mode mode() { return mode; }
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
