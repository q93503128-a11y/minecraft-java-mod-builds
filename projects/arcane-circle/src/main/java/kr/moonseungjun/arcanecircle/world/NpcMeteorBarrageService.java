package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.CastTargetSnapshot;
import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Staggered NPC Meteor Swarm scheduler using the same seeded pattern as player/server/client paths. */
public final class NpcMeteorBarrageService {
    private static final int MAX_ACTIVE_BARRAGES = 24;
    private static final List<Barrage> ACTIVE = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private NpcMeteorBarrageService() {}

    public static boolean schedule(ServerLevel level, Mob caster, CastTargetSnapshot targetSnapshot,
                                   double range, double power) {
        if (ArcaneFieldService.blocksCasting(caster)
                || targetSnapshot == null || !targetSnapshot.validFor(caster)) return false;
        while (ACTIVE.size() >= MAX_ACTIVE_BARRAGES) ACTIVE.removeFirst();
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
                    || !barrage.targetSnapshot().validFor(caster)) {
                iterator.remove();
                continue;
            }

            long elapsed = now - barrage.startedAt();
            int next = barrage.nextStrike();
            while (next < MeteorBarragePattern.count()) {
                MeteorBarragePattern.Strike strike =
                        MeteorBarragePattern.strike(barrage.targetSnapshot().barrageSeed(), next);
                if (elapsed < strike.impactTick()) break;
                resolveStrike(level, caster, barrage, strike, next);
                next++;
            }
            if (next >= MeteorBarragePattern.count()) iterator.remove();
            else barrage.nextStrike(next);
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
        LAST_TICK.clear();
    }

    private static void resolveStrike(ServerLevel level, Mob caster, Barrage barrage,
                                      MeteorBarragePattern.Strike strike, int index) {
        Vec3 impact = MeteorBarragePattern.position(barrage.targetSnapshot().target(), strike);
        double radius = 3.0 + strike.scale() * 1.65;
        double strikePower = barrage.power() * (.19 + .075 * strike.scale());
        AABB box = new AABB(impact, impact).inflate(radius, Math.max(3.0, radius * .72), radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value != caster && value.isAlive() && !value.isRemoved() && !caster.isAlliedTo(value))) {
            ArcaneDamage.hurt(level, caster, entity, (float) strikePower);
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 120));
        }
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, Math.min(1.6F, .82F + (float) strike.scale() * .38F),
                .62F + (index % 4) * .055F);
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
            this.level = level;
            this.casterId = casterId;
            this.targetSnapshot = targetSnapshot;
            this.range = range;
            this.power = power;
            this.startedAt = startedAt;
            this.nextStrike = nextStrike;
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
