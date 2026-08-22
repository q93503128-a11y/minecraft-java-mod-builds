package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.Alpha65NinthCircleRuntime;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.CastTargetSnapshot;
import kr.moonseungjun.arcanecircle.magic.EighthCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import kr.moonseungjun.arcanecircle.magic.MeteorCataclysmService;
import kr.moonseungjun.arcanecircle.magic.NinthCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.WorldMagicService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Range-scaled NPC Meteor Swarm scheduler using the same seeded cityfall pattern as player/client paths. */
public final class NpcMeteorBarrageService {
    private static final int MAX_ACTIVE_BARRAGES = 24;
    private static final List<Barrage> ACTIVE = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private NpcMeteorBarrageService() {}

    public static boolean schedule(ServerLevel level, Mob caster, CastTargetSnapshot targetSnapshot,
                                   double range, double power) {
        if (ArcaneFieldService.blocksCasting(caster)
                || EighthCircleSpellService.blocksCasting(caster)
                || NinthCircleSpellService.blocksCasting(caster)
                || targetSnapshot == null || !targetSnapshot.validFor(caster)) return false;
        while (ACTIVE.size() >= MAX_ACTIVE_BARRAGES) ACTIVE.removeFirst();
        MeteorBarragePattern.rememberRange(targetSnapshot.barrageSeed(), range);
        ACTIVE.add(new Barrage(level, caster.getUUID(), targetSnapshot, range, power,
                level.getGameTime(), 0));
        return true;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        Iterator<Barrage> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Barrage barrage = iterator.next();
            if (barrage.level() != level) continue;
            Entity rawCaster = level.getEntity(barrage.casterId());
            if (!(rawCaster instanceof Mob caster) || !caster.isAlive()
                    || ArcaneFieldService.blocksCasting(caster)
                    || EighthCircleSpellService.blocksCasting(caster)
                    || NinthCircleSpellService.blocksCasting(caster)
                    || !barrage.targetSnapshot().validFor(caster)) {
                if (rawCaster instanceof LivingEntity livingCaster)
                    WorldMagicService.cancelRelease(livingCaster, "meteor_swarm");
                iterator.remove();
                continue;
            }
            long elapsed = now - barrage.startedAt();
            int next = barrage.nextStrike();
            int count = MeteorBarragePattern.count(barrage.range());
            while (next < count) {
                int strikeIndex = next;
                long seed = barrage.targetSnapshot().barrageSeed();
                MeteorBarragePattern.Strike strike = MeteorBarragePattern.strike(seed, barrage.range(), strikeIndex);
                if (elapsed < strike.impactTick()) break;
                boolean executed = MeteorBarragePattern.withContext(seed, barrage.range(),
                        () -> Alpha65NinthCircleRuntime.meteorImpactNpc(level, caster,
                                barrage.targetSnapshot().target(), barrage.power(), strikeIndex, seed));
                if (executed && MeteorBarragePattern.isCrownStrike(barrage.range(), strikeIndex)) {
                    Vec3 grounded = Alpha65NinthCircleRuntime.groundedBarrageCenter(level,
                            barrage.targetSnapshot().target());
                    MeteorCataclysmService.crownImpactNpc(level, caster,
                            grounded, barrage.range(), barrage.power(), seed);
                }
                next++;
            }
            if (next >= count) iterator.remove();
            else barrage.nextStrike(next);
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
        LAST_TICK.clear();
    }

    private static final class Barrage {
        private final ServerLevel level;
        private final UUID casterId;
        private final CastTargetSnapshot targetSnapshot;
        private final double range;
        private final double power;
        private final long startedAt;
        private int nextStrike;

        private Barrage(ServerLevel level, UUID casterId, CastTargetSnapshot targetSnapshot,
                        double range, double power, long startedAt, int nextStrike) {
            this.level = level; this.casterId = casterId; this.targetSnapshot = targetSnapshot;
            this.range = range; this.power = power; this.startedAt = startedAt; this.nextStrike = nextStrike;
        }
        ServerLevel level() { return level; }
        UUID casterId() { return casterId; }
        CastTargetSnapshot targetSnapshot() { return targetSnapshot; }
        double range() { return range; }
        double power() { return power; }
        long startedAt() { return startedAt; }
        int nextStrike() { return nextStrike; }
        void nextStrike(int value) { nextStrike = value; }
    }
}
