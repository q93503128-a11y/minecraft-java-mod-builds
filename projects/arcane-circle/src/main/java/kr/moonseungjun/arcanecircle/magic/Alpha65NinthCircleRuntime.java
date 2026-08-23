package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Alpha.65 corrective authority for the ninth-circle contracts that failed real play-testing.
 * This class intentionally overrides only the affected spell semantics; unaffected 9C spells
 * continue through NinthCircleSpellService.
 */
public final class Alpha65NinthCircleRuntime {
    public static final int WEIRD_ESCAPE_TICKS = 300;
    public static final int GATE_TICKS = 600;

    private static final Map<UUID, NightmareField> NIGHTMARES = new HashMap<>();
    private static final List<GateField> GATES = new ArrayList<>();
    private static final Map<UUID, Long> GATE_COOLDOWNS = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private Alpha65NinthCircleRuntime() {}

    public static boolean overrides(String spellId) {
        return "gate".equals(spellId) || "weird".equals(spellId);
    }

    public static boolean executeOrDelegate(ServerPlayer caster, String spellId, double range,
                                            double power, CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "gate" -> gate(level, caster, snapshot, range);
            case "weird" -> weird(level, caster, snapshot.target(), range, power);
            default -> NinthCircleSpellService.execute(caster, spellId, range, power, snapshot);
        };
    }

    public static boolean executeNpcOrDelegate(ServerLevel level, Mob caster, LivingEntity fallback,
                                               SpellDefinition spell, double range, double power,
                                               CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null || !snapshot.validFor(caster))
            return false;
        return switch (spell.id()) {
            case "gate" -> gate(level, caster, snapshot, range);
            case "weird" -> weird(level, caster, snapshot.target(), range, power);
            default -> NinthCircleSpellService.executeNpc(level, caster, fallback, spell, range, power, snapshot);
        };
    }

    /** Grounded player Meteor impact: visible X/Z footprint and authoritative hit share one surface. */
    public static boolean meteorImpact(ServerPlayer caster, Vec3 barrageCenter, double power,
                                       int index, long seed) {
        if (caster == null || !(caster.level() instanceof ServerLevel level) || barrageCenter == null) return false;
        MeteorBarragePattern.Strike strike = MeteorBarragePattern.strike(seed, index);
        Vec3 nominal = MeteorBarragePattern.position(barrageCenter, strike);
        Vec3 impact = GroundTargetResolver.surface(level, nominal);
        double radius = 3.0 + strike.scale() * 1.65;
        double strikePower = power * (.19 + .075 * strike.scale());
        resolveMeteorEntities(level, caster, impact, radius, strikePower);
        DestructiveMagicService.meteorCrater(caster, impact, radius, strikePower);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, Math.min(1.8F, .90F + (float) strike.scale() * .40F),
                .58F + (index % 4) * .055F);
        return true;
    }

    /** NPC Meteor shares the same grounded combat point but never mutates terrain. */
    public static boolean meteorImpactNpc(ServerLevel level, Mob caster, Vec3 barrageCenter,
                                          double power, int index, long seed) {
        if (level == null || caster == null || !caster.isAlive() || barrageCenter == null) return false;
        MeteorBarragePattern.Strike strike = MeteorBarragePattern.strike(seed, index);
        Vec3 nominal = MeteorBarragePattern.position(barrageCenter, strike);
        Vec3 impact = GroundTargetResolver.surface(level, nominal);
        double radius = 3.0 + strike.scale() * 1.65;
        double strikePower = power * (.19 + .075 * strike.scale());
        resolveMeteorEntities(level, caster, impact, radius, strikePower);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, Math.min(1.8F, .90F + (float) strike.scale() * .40F),
                .58F + (index % 4) * .055F);
        return true;
    }

    public static Vec3 groundedBarrageCenter(ServerLevel level, Vec3 barrageCenter) {
        return GroundTargetResolver.surface(level, barrageCenter);
    }

    private static void resolveMeteorEntities(ServerLevel level, LivingEntity caster, Vec3 impact,
                                              double radius, double power) {
        AABB box = new AABB(impact, impact).inflate(radius, Math.max(5.0, radius * 1.15), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value))) {
            double dx = target.getX() - impact.x;
            double dz = target.getZ() - impact.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > radius + target.getBbWidth()) continue;
            double falloff = Math.max(.48, 1.0 - horizontal / Math.max(1.0, radius) * .52);
            ArcaneDamage.hurt(level, caster, target, (float) (power * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 180));
            Vec3 away = horizontal(target.position().subtract(impact));
            target.push(away.x * (.62 + .48 * falloff), .38 + .42 * falloff,
                    away.z * (.62 + .48 * falloff));
        }
    }

    // Weird ------------------------------------------------------------------------------------

    /**
     * Weird is no longer a generic DoT. It creates an escape-or-die nightmare domain. Every living
     * entity except the caster can be caught. Leaving the horizontal boundary releases the victim;
     * remaining inside when the 15-second verdict arrives causes a ninth-circle fatal hit.
     */
    private static boolean weird(ServerLevel level, LivingEntity caster, Vec3 requestedCenter,
                                 double range, double power) {
        Vec3 center = GroundTargetResolver.surface(level, requestedCenter);
        double radius = Math.max(18.0, Math.min(34.0, range * .52));
        NightmareField old = NIGHTMARES.remove(caster.getUUID());
        if (old != null) restoreNightmare(old);
        NightmareField field = new NightmareField(level, caster.getUUID(), center, radius, power,
                level.getGameTime() + WEIRD_ESCAPE_TICKS);
        NIGHTMARES.put(caster.getUUID(), field);
        captureNightmareVictims(field, caster);
        applyNightmare(field, caster);
        level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .72F, .45F);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[기괴한 환영] §f15초 악몽영역 전개 · 시전자 외 생명체는 경계를 벗어나지 못하면 종말 판결을 받습니다."), 100);
        }
        return true;
    }

    private static void tickNightmares(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NightmareField>> iterator = NIGHTMARES.entrySet().iterator();
        while (iterator.hasNext()) {
            NightmareField field = iterator.next().getValue();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) {
                restoreNightmare(field);
                iterator.remove();
                continue;
            }

            captureNightmareVictims(field, owner);
            Iterator<Map.Entry<UUID, UUID>> victims = field.oldTargets.entrySet().iterator();
            while (victims.hasNext()) {
                Map.Entry<UUID, UUID> entry = victims.next();
                Entity raw = level.getEntity(entry.getKey());
                if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
                    victims.remove();
                    continue;
                }
                if (!insideNightmare(field, target)) {
                    restoreNightmareVictim(level, target, entry.getValue());
                    victims.remove();
                    continue;
                }
                applyNightmareVictim(field, owner, target);
            }

            if (now < field.expiresAt) continue;
            for (UUID targetId : new ArrayList<>(field.oldTargets.keySet())) {
                Entity raw = level.getEntity(targetId);
                if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) continue;
                if (!insideNightmare(field, target)) continue;
                float fatal = Math.max(4096.0F,
                        target.getHealth() + target.getAbsorptionAmount() + target.getMaxHealth() * 12.0F);
                ArcaneDamage.hurt(level, owner, target, fatal);
            }
            restoreNightmare(field);
            WorldMagicService.cancelRelease(owner, "weird");
            iterator.remove();
        }
    }

    private static void captureNightmareVictims(NightmareField field, LivingEntity owner) {
        double vertical = Math.max(24.0, field.radius * .85);
        AABB box = new AABB(field.center, field.center).inflate(field.radius, vertical, field.radius);
        for (LivingEntity target : field.level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value != owner && value.isAlive() && !value.isRemoved())) {
            if (!insideNightmare(field, target) || field.oldTargets.containsKey(target.getUUID())) continue;
            UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
            field.oldTargets.put(target.getUUID(), oldTarget);
        }
    }

    private static void applyNightmare(NightmareField field, LivingEntity owner) {
        for (UUID id : new ArrayList<>(field.oldTargets.keySet())) {
            Entity raw = field.level.getEntity(id);
            if (raw instanceof LivingEntity target && target.isAlive()) applyNightmareVictim(field, owner, target);
        }
    }

    private static void applyNightmareVictim(NightmareField field, LivingEntity owner, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 14, 2, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 14, 1, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 14, 4, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 14, 1, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 0, true, false));
        if (target instanceof ServerPlayer player) {
            if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
            if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
            SpellKineticsService.cancel(player);
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            Vec3 away = horizontal(target.position().subtract(field.center));
            Vec3 destination = field.center.add(away.scale(field.radius + 7.0));
            mob.getNavigation().moveTo(destination.x, target.getY(), destination.z, 1.34);
            WorldMagicService.stop(mob);
        }
    }

    private static boolean insideNightmare(NightmareField field, LivingEntity target) {
        return GroundTargetResolver.horizontalDistanceSqr(field.center, target.position())
                <= Math.pow(field.radius + target.getBbWidth() * .5, 2.0);
    }

    private static void restoreNightmare(NightmareField field) {
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(field.oldTargets.entrySet())) {
            Entity raw = field.level.getEntity(entry.getKey());
            if (raw instanceof LivingEntity target && !target.isRemoved())
                restoreNightmareVictim(field.level, target, entry.getValue());
        }
        field.oldTargets.clear();
    }

    private static void restoreNightmareVictim(ServerLevel level, LivingEntity target, UUID oldTargetId) {
        if (target instanceof Mob mob) {
            Entity old = oldTargetId == null ? null : level.getEntity(oldTargetId);
            mob.setTarget(old instanceof LivingEntity living && living.isAlive() ? living : null);
        }
    }

    // Gate -------------------------------------------------------------------------------------

    /** Opens from the caster's nearby safe ground to a genuinely distant safe ground target. */
    private static boolean gate(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot, double range) {
        Vec3 forward = horizontal(snapshot.launchDirection());
        Optional<BlockPos> sourceSafe = GroundTargetResolver.safeStanding(level,
                caster.position().add(forward.scale(2.6)), 8);

        Vec3 requested = snapshot.target();
        if (GroundTargetResolver.horizontalDistanceSqr(caster.position(), requested) < 12.0 * 12.0) {
            double travel = Math.max(20.0, Math.min(80.0, range * .82));
            requested = caster.position().add(forward.scale(travel));
        }
        Optional<BlockPos> targetSafe = GroundTargetResolver.safeStanding(level, requested, 18);
        if (targetSafe.isEmpty()) {
            double travel = Math.max(24.0, Math.min(80.0, range * .72));
            targetSafe = GroundTargetResolver.safeStanding(level, caster.position().add(forward.scale(travel)), 24);
        }
        if (sourceSafe.isEmpty() || targetSafe.isEmpty()) {
            if (caster instanceof ServerPlayer player)
                ArcaneNoticeService.push(player, Component.literal("§c[월드 게이트] §f열린 지형을 포함해 넓게 탐색했지만 두 블록 높이의 안전한 착지면을 찾지 못했습니다."), 65);
            return false;
        }

        Vec3 source = GroundTargetResolver.standing(sourceSafe.get());
        Vec3 target = GroundTargetResolver.standing(targetSafe.get());
        if (GroundTargetResolver.horizontalDistanceSqr(source, target) < 12.0 * 12.0) {
            Optional<BlockPos> farther = GroundTargetResolver.safeStanding(level,
                    caster.position().add(forward.scale(Math.max(28.0, Math.min(80.0, range)))), 24);
            if (farther.isPresent()) target = GroundTargetResolver.standing(farther.get());
        }
        if (GroundTargetResolver.horizontalDistanceSqr(source, target) < 12.0 * 12.0) {
            if (caster instanceof ServerPlayer player)
                ArcaneNoticeService.push(player, Component.literal("§7[월드 게이트] §f목표 지점이 너무 가까워 두 공간문을 분리할 수 없습니다."), 55);
            return false;
        }

        Vec3 finalTarget = target;
        Vec3 targetArrival = GroundTargetResolver.safeStanding(level, target.add(forward.scale(3.8)), 10)
                .map(GroundTargetResolver::standing).orElse(finalTarget);
        Vec3 finalSource = source;
        Vec3 sourceArrival = GroundTargetResolver.safeStanding(level, source.subtract(forward.scale(3.8)), 10)
                .map(GroundTargetResolver::standing).orElse(finalSource);
        GATES.removeIf(field -> field.ownerId.equals(caster.getUUID()));
        GATES.add(new GateField(level, caster.getUUID(), source, target, sourceArrival, targetArrival,
                Math.max(2.5, Math.min(3.6, range * .047)), level.getGameTime() + GATE_TICKS));
        if (caster instanceof ServerPlayer player)
            ArcaneNoticeService.push(player, Component.literal("§5[월드 게이트] §f30초 양방향 문 고정 · 생명체가 어느 쪽으로 들어가도 반대편 안전지점으로 이동합니다."), 90);
        return true;
    }

    private static void tickGates(ServerLevel level, long now) {
        GATE_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now - 80L);
        Iterator<GateField> iterator = GATES.iterator();
        while (iterator.hasNext()) {
            GateField gate = iterator.next();
            if (gate.level != level) continue;
            Entity rawOwner = level.getEntity(gate.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || now >= gate.expiresAt) {
                if (rawOwner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "gate");
                iterator.remove();
                continue;
            }
            processGateEndpoint(level, gate, gate.source, gate.targetArrival, now);
            processGateEndpoint(level, gate, gate.target, gate.sourceArrival, now);
        }
    }

    private static void processGateEndpoint(ServerLevel level, GateField gate, Vec3 entrance,
                                            Vec3 destination, long now) {
        AABB box = new AABB(entrance, entrance).inflate(gate.radius, 3.6, gate.radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value.isAlive() && !value.isRemoved())) {
            if (GATE_COOLDOWNS.getOrDefault(entity.getUUID(), 0L) > now) continue;
            Vec3 delta = entity.position().subtract(entrance);
            if (delta.x * delta.x + delta.z * delta.z > gate.radius * gate.radius || Math.abs(delta.y) > 3.6) continue;
            if (!teleportWithinLevel(level, entity, destination)) continue;
            GATE_COOLDOWNS.put(entity.getUUID(), now + 60L);
        }
    }

    private static boolean teleportWithinLevel(ServerLevel level, LivingEntity entity, Vec3 destination) {
        entity.stopRiding();
        if (entity instanceof ServerPlayer player) {
            boolean moved = player.teleportTo(level, destination.x, destination.y, destination.z,
                    Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
            if (!moved) return false;
        } else {
            if (entity instanceof Mob mob) mob.getNavigation().stop();
            entity.snapTo(destination.x, destination.y, destination.z, entity.getYRot(), entity.getXRot());
        }
        entity.fallDistance = 0.0F;
        return true;
    }

    // World Sunder -----------------------------------------------------------------------------

    /** Fusion World Sunder now damages the same horizontal rift that the player sees. */
    public static boolean worldSunder(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.validFor(player)) return false;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = GroundTargetResolver.surface(level, snapshot.target());
        Vec3 forward = horizontal(snapshot.launchDirection());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double length = Math.max(42.0, Math.min(104.0, range * 1.06));
        double half = length * .5;
        double halfWidth = Math.max(5.5, Math.min(11.0, range * .095));
        double vertical = Math.max(20.0, Math.min(42.0, range * .40));
        Vec3 start = center.subtract(forward.scale(half));
        Vec3 end = center.add(forward.scale(half));
        AABB box = new AABB(start.add(right.scale(-halfWidth)), end.add(right.scale(halfWidth)))
                .inflate(2.5, vertical, 2.5);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(player, value))) {
            Vec3 flat = new Vec3(target.getX() - center.x, 0.0, target.getZ() - center.z);
            double along = flat.dot(forward);
            if (Math.abs(along) > half + target.getBbWidth()) continue;
            double lateral = Math.abs(flat.dot(right));
            if (lateral > halfWidth + target.getBbWidth() * .60) continue;
            double core = Math.max(.42, 1.0 - lateral / Math.max(1.0, halfWidth) * .58);
            double longitudinal = Math.max(.58, 1.0 - Math.abs(along) / Math.max(1.0, half) * .25);
            float amount = (float) Math.max(1.0, power * core * longitudinal);
            if (ArcaneDamage.hurt(level, player, target, amount)) hit = true;
            double side = Math.signum(flat.dot(right));
            if (side == 0.0) side = 1.0;
            target.push(right.x * side * (.55 + core * .75), .70 + core * .95,
                    right.z * side * (.55 + core * .75));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3, true, false));
        }

        for (int i = -6; i <= 6; i++) {
            Vec3 node = GroundTargetResolver.surface(level, center.add(forward.scale(i * half / 6.0)));
            double nodeRadius = 7.5 + (6 - Math.abs(i)) * .70;
            DestructiveMagicService.impact(player, "world_sunder", node.add(0, -.7, 0),
                    nodeRadius, power * (i == 0 ? 1.20 : .82));
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.35F, .42F);
        return hit || center != null;
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickNightmares(level, now);
        tickGates(level, now);
    }

    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        NightmareField owned = NIGHTMARES.remove(id);
        if (owned != null) restoreNightmare(owned);
        for (NightmareField field : NIGHTMARES.values()) {
            boolean wasVictim = field.oldTargets.containsKey(id);
            UUID oldTarget = field.oldTargets.remove(id);
            if (wasVictim) restoreNightmareVictim(field.level, subject, oldTarget);
        }
        boolean gate = GATES.removeIf(field -> field.ownerId.equals(id));
        GATE_COOLDOWNS.remove(id);
        if (owned != null) WorldMagicService.cancelRelease(subject, "weird");
        if (gate) WorldMagicService.cancelRelease(subject, "gate");
    }

    public static void clearAll() {
        for (NightmareField field : new ArrayList<>(NIGHTMARES.values())) restoreNightmare(field);
        NIGHTMARES.clear();
        GATES.clear();
        GATE_COOLDOWNS.clear();
        LAST_TICK.clear();
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static final class NightmareField {
        final ServerLevel level;
        final UUID ownerId;
        final Vec3 center;
        final double radius;
        final double power;
        final long expiresAt;
        final Map<UUID, UUID> oldTargets = new HashMap<>();

        NightmareField(ServerLevel level, UUID ownerId, Vec3 center, double radius,
                       double power, long expiresAt) {
            this.level = level;
            this.ownerId = ownerId;
            this.center = center;
            this.radius = radius;
            this.power = power;
            this.expiresAt = expiresAt;
        }
    }

    private static final class GateField {
        final ServerLevel level;
        final UUID ownerId;
        final Vec3 source;
        final Vec3 target;
        final Vec3 sourceArrival;
        final Vec3 targetArrival;
        final double radius;
        final long expiresAt;

        GateField(ServerLevel level, UUID ownerId, Vec3 source, Vec3 target,
                  Vec3 sourceArrival, Vec3 targetArrival, double radius, long expiresAt) {
            this.level = level;
            this.ownerId = ownerId;
            this.source = source;
            this.target = target;
            this.sourceArrival = sourceArrival;
            this.targetArrival = targetArrival;
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }
}
