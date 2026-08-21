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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative ninth-circle runtime.
 *
 * Already-strong player meanings are preserved by delegation: Shapechange/Foresight stay in
 * ArcaneBuffRuntime, Time Stop/Wish stay in ArcaneFieldService and True Polymorph stays in
 * HighUtilitySpellService. The remaining ninth-circle spells receive explicit locked/maintained
 * behavior here, and NPC mages use role-equivalent authorities instead of generic damage.
 */
public final class NinthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "meteor_swarm", "power_word_kill", "prismatic_wall", "shapechange", "time_stop",
            "true_polymorph", "weird", "wish", "gate", "foresight");

    public static final int PRISMATIC_WALL_TICKS = 400;
    public static final int NPC_SHAPECHANGE_TICKS = 1800;
    public static final int NPC_TIME_STOP_TICKS = ArcaneFieldService.TIME_STOP_TICKS;
    public static final int NPC_TRUE_POLYMORPH_TICKS = 480;
    public static final int WEIRD_TICKS = 300;
    public static final int GATE_TICKS = 600;
    public static final int NPC_FORESIGHT_TICKS = 2400;

    private static final List<PrismaticWallField> PRISMATIC_WALLS = new ArrayList<>();
    private static final Map<UUID, NpcShapeState> NPC_SHAPECHANGE = new HashMap<>();
    private static final Map<UUID, NpcTimeField> NPC_TIME_FIELDS = new HashMap<>();
    private static final Map<UUID, FrozenMob> NPC_FROZEN_MOBS = new HashMap<>();
    private static final Map<UUID, FrozenEntity> NPC_FROZEN_ENTITIES = new HashMap<>();
    private static final Map<UUID, NpcPolymorphState> NPC_POLYMORPHS = new HashMap<>();
    private static final Map<UUID, NpcPlayerPolymorphState> NPC_PLAYER_POLYMORPHS = new HashMap<>();
    private static final Map<UUID, WeirdState> WEIRD = new HashMap<>();
    private static final List<GateField> GATES = new ArrayList<>();
    private static final Map<UUID, Long> GATE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, NpcForesightState> NPC_FORESIGHT = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private NinthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            // The normal player Meteor Swarm path is scheduled strike-by-strike in SpellKineticsService.
            case "meteor_swarm" -> meteorImpact(caster, snapshot.target(), power, 0, snapshot.barrageSeed());
            case "power_word_kill" -> powerWordKill(level, caster,
                    snapshot.targetEntity(caster).orElse(null), power);
            case "prismatic_wall" -> prismaticWall(level, caster, snapshot, range, power);
            case "shapechange", "foresight" -> ArcaneBuffRuntime.apply(caster, spellId, power, range);
            case "time_stop", "wish" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);
            case "true_polymorph" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);
            case "weird" -> weird(level, caster, snapshot.target(), range, power);
            case "gate" -> gate(level, caster, snapshot, range);
            default -> false;
        };
    }

    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity fallback,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        LivingEntity target = targetEntity(level, fallback, snapshot);
        return switch (spell.id()) {
            // NpcMeteorBarrageService normally schedules all seeded impacts.
            case "meteor_swarm" -> resolveNpcMeteorImpact(level, caster, snapshot.target(), power,
                    0, snapshot.barrageSeed());
            case "power_word_kill" -> powerWordKill(level, caster, target, power);
            case "prismatic_wall" -> prismaticWall(level, caster, snapshot, range, power);
            case "shapechange" -> npcShapechange(level, caster, power);
            case "time_stop" -> npcTimeStop(level, caster, snapshot, range);
            case "true_polymorph" -> npcTruePolymorph(level, caster, target);
            case "weird" -> weird(level, caster, snapshot.target(), range, power);
            case "wish" -> npcWish(caster);
            case "gate" -> gate(level, caster, snapshot, range);
            case "foresight" -> npcForesight(level, caster);
            default -> false;
        };
    }

    /** NPC time stop, Weird and NPC-player True Polymorph suppress Arcane casting directly. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        long now = caster.level() instanceof ServerLevel level ? level.getGameTime() : Long.MAX_VALUE;
        WeirdState weird = WEIRD.get(caster.getUUID());
        if (weird != null && weird.level == caster.level() && now < weird.expiresAt) return true;
        NpcPlayerPolymorphState poly = NPC_PLAYER_POLYMORPHS.get(caster.getUUID());
        if (poly != null && poly.level == caster.level() && now < poly.expiresAt) return true;
        for (NpcTimeField field : NPC_TIME_FIELDS.values()) {
            if (field.level != caster.level() || now >= field.expiresAt) continue;
            Entity rawOwner = field.level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) continue;
            if (owner == caster || owner.isAlliedTo(caster)) continue;
            if (field.center.distanceToSqr(caster.position()) <= field.radius * field.radius) return true;
        }
        return false;
    }

    /** Prismatic Wall blocks hostile Arcane trajectories crossing the wall plane. */
    public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        long now = caster.level() instanceof ServerLevel level ? level.getGameTime() : Long.MAX_VALUE;
        for (PrismaticWallField wall : PRISMATIC_WALLS) {
            if (wall.level != caster.level() || now >= wall.expiresAt) continue;
            Entity rawOwner = wall.level.getEntity(wall.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) continue;
            if (owner == caster || owner.isAlliedTo(caster)) continue;
            if (segmentCrossesWall(snapshot.launchOrigin(), snapshot.target(), wall)) return true;
        }
        return false;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event == null || event.isCanceled() || event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        NpcForesightState foresight = NPC_FORESIGHT.get(mob.getUUID());
        if (foresight != null && foresight.level == level && now < foresight.expiresAt) {
            if (now >= foresight.nextDodgeAt) {
                foresight.nextDodgeAt = now + 40L;
                event.setCanceled(true);
                return;
            }
            event.setAmount(Math.max(.05F, event.getAmount() * .75F));
        }
        NpcShapeState shape = NPC_SHAPECHANGE.get(mob.getUUID());
        if (!event.isCanceled() && shape != null && shape.level == level && now < shape.expiresAt) {
            event.setAmount(Math.max(.05F, event.getAmount() * .50F));
        }
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickPrismaticWalls(level, now);
        tickNpcShapechange(level, now);
        tickNpcPolymorphs(level, now);
        tickNpcPlayerPolymorphs(level, now);
        tickWeird(level, now);
        tickGates(level, now);
        tickNpcForesight(level, now);
        // NPC Time Stop is applied last inside this authority and restores all frozen state later.
        tickNpcTimeStop(level, now);
    }

    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();

        boolean ownedWall = PRISMATIC_WALLS.removeIf(wall -> wall.ownerId.equals(id));
        boolean ownedShape = NPC_SHAPECHANGE.remove(id) != null;
        NpcTimeField removedTime = NPC_TIME_FIELDS.remove(id);
        if (removedTime != null) restoreReleasedTimeStop(removedTime.level);
        boolean ownedForesight = NPC_FORESIGHT.remove(id) != null;
        GATE_COOLDOWNS.remove(id);
        boolean ownedGate = GATES.removeIf(gate -> gate.ownerId.equals(id));

        NpcPolymorphState victimPoly = NPC_POLYMORPHS.remove(id);
        if (victimPoly != null) restoreNpcPolymorph(victimPoly, false);
        boolean ownedPolymorph = false;
        Iterator<Map.Entry<UUID, NpcPolymorphState>> poly = NPC_POLYMORPHS.entrySet().iterator();
        while (poly.hasNext()) {
            NpcPolymorphState state = poly.next().getValue();
            if (!state.ownerId.equals(id)) continue;
            restoreNpcPolymorph(state, false);
            poly.remove();
            ownedPolymorph = true;
        }
        boolean ownedPlayerPolymorph = NPC_PLAYER_POLYMORPHS.values().stream()
                .anyMatch(state -> state.ownerId.equals(id));
        NPC_PLAYER_POLYMORPHS.entrySet().removeIf(entry ->
                entry.getKey().equals(id) || entry.getValue().ownerId.equals(id));

        WeirdState victimWeird = WEIRD.remove(id);
        if (victimWeird != null) restoreWeird(victimWeird);
        boolean ownedWeird = false;
        Iterator<Map.Entry<UUID, WeirdState>> weird = WEIRD.entrySet().iterator();
        while (weird.hasNext()) {
            WeirdState state = weird.next().getValue();
            if (!state.ownerId.equals(id)) continue;
            restoreWeird(state);
            weird.remove();
            ownedWeird = true;
        }

        if (ownedWall) WorldMagicService.cancelRelease(subject, "prismatic_wall");
        if (ownedShape) WorldMagicService.cancelRelease(subject, "shapechange");
        if (removedTime != null) WorldMagicService.cancelRelease(subject, "time_stop");
        if (ownedPolymorph || ownedPlayerPolymorph) WorldMagicService.cancelRelease(subject, "true_polymorph");
        if (ownedWeird) WorldMagicService.cancelRelease(subject, "weird");
        if (ownedGate) WorldMagicService.cancelRelease(subject, "gate");
        if (ownedForesight) WorldMagicService.cancelRelease(subject, "foresight");
    }

    public static void clearAll() {
        for (FrozenMob frozen : new ArrayList<>(NPC_FROZEN_MOBS.values())) restoreFrozenMob(frozen);
        for (FrozenEntity frozen : new ArrayList<>(NPC_FROZEN_ENTITIES.values())) restoreFrozenEntity(frozen);
        for (NpcPolymorphState state : new ArrayList<>(NPC_POLYMORPHS.values())) restoreNpcPolymorph(state, false);
        for (WeirdState state : new ArrayList<>(WEIRD.values())) restoreWeird(state);
        PRISMATIC_WALLS.clear();
        NPC_SHAPECHANGE.clear();
        NPC_TIME_FIELDS.clear();
        NPC_FROZEN_MOBS.clear();
        NPC_FROZEN_ENTITIES.clear();
        NPC_POLYMORPHS.clear();
        NPC_PLAYER_POLYMORPHS.clear();
        WEIRD.clear();
        GATES.clear();
        GATE_COOLDOWNS.clear();
        NPC_FORESIGHT.clear();
        LAST_TICK.clear();
    }

    // Meteor Swarm ----------------------------------------------------------------------------

    public static boolean meteorImpact(ServerPlayer caster, Vec3 barrageCenter, double power,
                                       int index, long seed) {
        if (caster == null || !(caster.level() instanceof ServerLevel level)) return false;
        MeteorBarragePattern.Strike strike = MeteorBarragePattern.strike(seed, index);
        Vec3 impact = MeteorBarragePattern.position(barrageCenter, strike);
        double radius = 3.0 + strike.scale() * 1.65;
        double strikePower = power * (.19 + .075 * strike.scale());
        resolveMeteorEntities(level, caster, impact, radius, strikePower);
        DestructiveMagicService.meteorCrater(caster, impact, radius, strikePower);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, Math.min(1.6F, .82F + (float) strike.scale() * .38F),
                .62F + (index % 4) * .055F);
        return true;
    }

    public static boolean resolveNpcMeteorImpact(ServerLevel level, Mob caster, Vec3 barrageCenter,
                                                 double power, int index, long seed) {
        if (level == null || caster == null || !caster.isAlive()) return false;
        MeteorBarragePattern.Strike strike = MeteorBarragePattern.strike(seed, index);
        Vec3 impact = MeteorBarragePattern.position(barrageCenter, strike);
        double radius = 3.0 + strike.scale() * 1.65;
        double strikePower = power * (.19 + .075 * strike.scale());
        resolveMeteorEntities(level, caster, impact, radius, strikePower);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, Math.min(1.6F, .82F + (float) strike.scale() * .38F),
                .62F + (index % 4) * .055F);
        return true;
    }

    private static void resolveMeteorEntities(ServerLevel level, LivingEntity caster, Vec3 impact,
                                              double radius, double power) {
        AABB box = new AABB(impact, impact).inflate(radius, Math.max(4.0, radius * .78), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value))) {
            double distance = Math.sqrt(impact.distanceToSqr(target.position()));
            if (distance > radius + target.getBbWidth()) continue;
            double falloff = Math.max(.48, 1.0 - distance / Math.max(1.0, radius) * .52);
            ArcaneDamage.hurt(level, caster, target, (float) (power * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 160));
            Vec3 away = horizontal(target.position().subtract(impact));
            target.push(away.x * (.58 + .45 * falloff), .32 + .36 * falloff,
                    away.z * (.58 + .45 * falloff));
        }
    }

    // Power Word Kill --------------------------------------------------------------------------

    private static boolean powerWordKill(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        double threshold = Math.max(100.0, power * 1.05);
        double pool = target.getHealth() + target.getAbsorptionAmount();
        if (pool <= threshold) {
            // Use the normal attacker-bearing Arcane damage path so death/retaliation events still fire.
            ArcaneDamage.hurt(level, caster, target,
                    Math.max(2048.0F, target.getHealth() + target.getAbsorptionAmount() + target.getMaxHealth() * 8.0F));
            level.playSound(null, target.blockPosition(), SoundEvents.WITHER_DEATH,
                    caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .9F, .58F);
            return true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 3, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, true, false));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[죽음의 권능어] §f대상의 생명력이 처형 역치 "
                    + whole(threshold) + "을 초과해 즉사 명령을 버텼습니다."), 65);
        }
        return true;
    }

    // Prismatic Wall ---------------------------------------------------------------------------

    private static boolean prismaticWall(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                         double range, double power) {
        Vec3 forward = horizontal(snapshot.launchDirection());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 center = snapshot.target();
        double halfWidth = Math.max(13.0, Math.min(26.0, range * .32));
        double depth = 2.8;
        PRISMATIC_WALLS.removeIf(wall -> wall.ownerId.equals(caster.getUUID()));
        PRISMATIC_WALLS.add(new PrismaticWallField(level, caster.getUUID(), center, forward, right,
                halfWidth, depth, power, level.getGameTime() + PRISMATIC_WALL_TICKS));
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_POWER_SELECT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.2F, 1.35F);
        return true;
    }

    private static void tickPrismaticWalls(ServerLevel level, long now) {
        Iterator<PrismaticWallField> iterator = PRISMATIC_WALLS.iterator();
        while (iterator.hasNext()) {
            PrismaticWallField wall = iterator.next();
            if (wall.level != level) continue;
            Entity rawOwner = level.getEntity(wall.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || now >= wall.expiresAt) {
                if (rawOwner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "prismatic_wall");
                iterator.remove();
                continue;
            }
            AABB box = new AABB(wall.center, wall.center)
                    .inflate(wall.halfWidth + 2.0, 7.0, wall.halfWidth + 2.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value))) {
                Vec3 delta = target.position().subtract(wall.center);
                double lateral = delta.dot(wall.right);
                double normal = delta.dot(wall.forward);
                if (Math.abs(lateral) > wall.halfWidth + target.getBbWidth()
                        || Math.abs(normal) > wall.depth + target.getBbWidth()) continue;
                long ready = wall.nextHit.getOrDefault(target.getUUID(), 0L);
                if (now >= ready) {
                    wall.nextHit.put(target.getUUID(), now + 14L);
                    int layer = Math.floorMod(target.getUUID().hashCode() + (int) (now / 14L), 7);
                    applyPrismaticLayer(level, owner, target, wall.power, layer);
                }
                // Physical body collision: keep the entity on the side it entered from.
                Vec3 motion = target.getDeltaMovement();
                double into = motion.dot(wall.forward);
                if (normal * into < 0.0) target.setDeltaMovement(motion.subtract(wall.forward.scale(into)));
                double push = normal >= 0.0 ? .18 : -.18;
                target.push(wall.forward.x * push, .03, wall.forward.z * push);
            }
        }
    }

    private static void applyPrismaticLayer(ServerLevel level, LivingEntity owner, LivingEntity target,
                                            double power, int layer) {
        switch (layer) {
            case 0 -> {
                ArcaneDamage.hurt(level, owner, target, (float) (power * .20));
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 180));
            }
            case 1 -> {
                ArcaneDamage.hurt(level, owner, target, (float) (power * .18));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 260));
            }
            case 2 -> {
                ArcaneDamage.hurt(level, owner, target, (float) (power * .22));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, true, false));
            }
            case 3 -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, true, false));
            }
            case 4 -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 2, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 1, true, false));
            }
            case 5 -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 5, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 4, true, false));
            }
            default -> {
                ArcaneDamage.hurt(level, owner, target, (float) (power * .28));
                Vec3 away = horizontal(target.position().subtract(owner.position()));
                target.push(away.x * .75, .24, away.z * .75);
            }
        }
    }

    private static boolean segmentCrossesWall(Vec3 start, Vec3 end, PrismaticWallField wall) {
        double a = start.subtract(wall.center).dot(wall.forward);
        double b = end.subtract(wall.center).dot(wall.forward);
        if (a == 0.0 || b == 0.0 || a * b > 0.0) return false;
        double denominator = a - b;
        if (Math.abs(denominator) < 1.0E-8) return false;
        double t = a / denominator;
        if (t < 0.0 || t > 1.0) return false;
        Vec3 hit = start.add(end.subtract(start).scale(t));
        Vec3 delta = hit.subtract(wall.center);
        return Math.abs(delta.dot(wall.right)) <= wall.halfWidth && Math.abs(delta.y) <= 7.0;
    }

    // NPC Shapechange --------------------------------------------------------------------------

    private static boolean npcShapechange(ServerLevel level, Mob caster, double power) {
        NPC_SHAPECHANGE.put(caster.getUUID(), new NpcShapeState(level, caster.getUUID(), power,
                level.getGameTime() + NPC_SHAPECHANGE_TICKS));
        caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, NPC_SHAPECHANGE_TICKS, 5, true, false));
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, NPC_SHAPECHANGE_TICKS, 3, true, false));
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, NPC_SHAPECHANGE_TICKS, 3, true, false));
        return true;
    }

    private static void tickNpcShapechange(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcShapeState>> iterator = NPC_SHAPECHANGE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcShapeState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.ownerId);
            if (!(raw instanceof Mob caster) || !caster.isAlive() || caster.isRemoved() || now >= state.expiresAt) {
                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "shapechange");
                iterator.remove();
                continue;
            }
            if (now % 20L == 0L) caster.heal((float) Math.min(6.0, Math.max(1.0, .8 + state.power * .006)));
        }
    }

    // NPC Time Stop ----------------------------------------------------------------------------

    private static boolean npcTimeStop(ServerLevel level, Mob caster, CastTargetSnapshot snapshot, double range) {
        Vec3 center = snapshot.target();
        double radius = Math.max(20.0, Math.min(48.0, range * .75));
        NPC_TIME_FIELDS.put(caster.getUUID(), new NpcTimeField(level, caster.getUUID(), center, radius,
                level.getGameTime() + NPC_TIME_STOP_TICKS));
        applyNpcTimeStop(level, level.getGameTime());
        return true;
    }

    private static void tickNpcTimeStop(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcTimeField>> fields = NPC_TIME_FIELDS.entrySet().iterator();
        while (fields.hasNext()) {
            NpcTimeField field = fields.next().getValue();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof Mob owner) || !owner.isAlive() || owner.isRemoved() || now >= field.expiresAt) {
                if (rawOwner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "time_stop");
                fields.remove();
            }
        }
        applyNpcTimeStop(level, now);
    }

    private static void applyNpcTimeStop(ServerLevel level, long now) {
        Set<UUID> frozenMobs = new HashSet<>();
        Set<UUID> frozenEntities = new HashSet<>();
        for (NpcTimeField field : NPC_TIME_FIELDS.values()) {
            if (field.level != level || now >= field.expiresAt) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof Mob owner) || !owner.isAlive()) continue;
            AABB box = new AABB(field.center, field.center).inflate(field.radius, field.radius * .75, field.radius);
            for (Mob target : level.getEntitiesOfClass(Mob.class, box,
                    value -> enemy(owner, value) && field.center.distanceToSqr(value.position()) <= field.radius * field.radius)) {
                frozenMobs.add(target.getUUID());
                NPC_FROZEN_MOBS.computeIfAbsent(target.getUUID(), ignored ->
                        new FrozenMob(level, target.getUUID(), target.isNoAi()));
                target.setNoAi(true);
                target.setDeltaMovement(Vec3.ZERO);
                target.getNavigation().stop();
                WorldMagicService.stop(target);
            }
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                    value -> enemy(owner, value) && field.center.distanceToSqr(value.position()) <= field.radius * field.radius)) {
                player.setDeltaMovement(Vec3.ZERO);
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 255, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 255, true, false));
                suppressPlayerCasting(player);
            }
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
                    value -> !value.isRemoved() && !(value instanceof LivingEntity)
                            && field.center.distanceToSqr(value.position()) <= field.radius * field.radius)) {
                frozenEntities.add(entity.getUUID());
                NPC_FROZEN_ENTITIES.computeIfAbsent(entity.getUUID(), ignored ->
                        new FrozenEntity(level, entity.getUUID(), entity.getDeltaMovement(), entity.isNoGravity()));
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setNoGravity(true);
            }
        }

        Iterator<Map.Entry<UUID, FrozenMob>> mobIterator = NPC_FROZEN_MOBS.entrySet().iterator();
        while (mobIterator.hasNext()) {
            FrozenMob frozen = mobIterator.next().getValue();
            if (frozen.level != level || frozenMobs.contains(frozen.entityId)) continue;
            restoreFrozenMob(frozen);
            mobIterator.remove();
        }
        Iterator<Map.Entry<UUID, FrozenEntity>> entityIterator = NPC_FROZEN_ENTITIES.entrySet().iterator();
        while (entityIterator.hasNext()) {
            FrozenEntity frozen = entityIterator.next().getValue();
            if (frozen.level != level || frozenEntities.contains(frozen.entityId)) continue;
            restoreFrozenEntity(frozen);
            entityIterator.remove();
        }
    }

    private static void restoreReleasedTimeStop(ServerLevel level) {
        applyNpcTimeStop(level, level.getGameTime());
    }

    private static void restoreFrozenMob(FrozenMob frozen) {
        Entity raw = frozen.level.getEntity(frozen.entityId);
        if (raw instanceof Mob mob && !mob.isRemoved()) mob.setNoAi(frozen.wasNoAi);
    }

    private static void restoreFrozenEntity(FrozenEntity frozen) {
        Entity raw = frozen.level.getEntity(frozen.entityId);
        if (raw == null || raw.isRemoved()) return;
        raw.setNoGravity(frozen.wasNoGravity);
        raw.setDeltaMovement(frozen.velocity);
    }

    // NPC True Polymorph -----------------------------------------------------------------------

    private static boolean npcTruePolymorph(ServerLevel level, Mob caster, LivingEntity target) {
        if (!enemy(caster, target)) return false;
        if (target instanceof ServerPlayer player) {
            NPC_PLAYER_POLYMORPHS.put(player.getUUID(), new NpcPlayerPolymorphState(level, caster.getUUID(),
                    player.getUUID(), level.getGameTime() + NPC_TRUE_POLYMORPH_TICKS));
            applyNpcPlayerPolymorph(player);
            return true;
        }
        if (!(target instanceof Mob original)) return false;
        NpcPolymorphState existing = NPC_POLYMORPHS.remove(original.getUUID());
        if (existing != null) restoreNpcPolymorph(existing, false);

        Mob proxy = switch (Math.floorMod(original.getUUID().hashCode(), 4)) {
            case 0 -> EntityTypes.RABBIT.create(level, EntitySpawnReason.EVENT);
            case 1 -> EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
            case 2 -> EntityTypes.PIG.create(level, EntitySpawnReason.EVENT);
            default -> EntityTypes.SHEEP.create(level, EntitySpawnReason.EVENT);
        };
        if (proxy == null) return false;
        Vec3 anchor = original.position();
        proxy.snapTo(anchor.x, anchor.y, anchor.z, original.getYRot(), original.getXRot());
        proxy.finalizeSpawn(level, level.getCurrentDifficultyAt(original.blockPosition()), EntitySpawnReason.EVENT, null);
        proxy.setCustomName(Component.literal("§d[NPC 진정한 변신] §f" + original.getName().getString()));
        proxy.setCustomNameVisible(true);
        proxy.setPersistenceRequired();
        proxy.addTag("arcanecircle_npc_true_polymorph_proxy");
        level.addFreshEntityWithPassengers(proxy);

        UUID oldTarget = original.getTarget() == null ? null : original.getTarget().getUUID();
        NpcPolymorphState state = new NpcPolymorphState(level, caster.getUUID(), original.getUUID(), proxy.getUUID(),
                level.getGameTime() + NPC_TRUE_POLYMORPH_TICKS, anchor, original.getYRot(), original.getXRot(),
                original.getHealth(), original.isInvisible(), original.isInvulnerable(), original.isNoGravity(),
                original.isSilent(), original.isNoAi(), original.noPhysics, oldTarget);
        NPC_POLYMORPHS.put(original.getUUID(), state);
        stashMob(original);
        return true;
    }

    private static void tickNpcPolymorphs(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcPolymorphState>> iterator = NPC_POLYMORPHS.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcPolymorphState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawProxy = level.getEntity(state.proxyId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || now >= state.expiresAt) {
                restoreNpcPolymorph(state, false);
                iterator.remove();
                continue;
            }
            if (rawProxy instanceof Mob proxy && proxy.isAlive() && !proxy.isRemoved()) {
                state.lastPosition = proxy.position();
                state.lastYaw = proxy.getYRot();
                state.lastPitch = proxy.getXRot();
                continue;
            }
            restoreNpcPolymorph(state, true);
            iterator.remove();
        }
    }

    private static void tickNpcPlayerPolymorphs(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcPlayerPolymorphState>> iterator = NPC_PLAYER_POLYMORPHS.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcPlayerPolymorphState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawTarget instanceof ServerPlayer player) || !player.isAlive() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            applyNpcPlayerPolymorph(player);
        }
    }

    private static void applyNpcPlayerPolymorph(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12, 6, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 3, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 12, 5, true, false));
        suppressPlayerCasting(player);
    }

    private static void stashMob(Mob original) {
        original.setTarget(null);
        original.getNavigation().stop();
        original.setInvisible(true);
        original.setInvulnerable(true);
        original.setNoGravity(true);
        original.setSilent(true);
        original.setNoAi(true);
        original.noPhysics = true;
        original.setDeltaMovement(Vec3.ZERO);
    }

    private static void restoreNpcPolymorph(NpcPolymorphState state, boolean proxyBroken) {
        Entity rawOriginal = state.level.getEntity(state.originalId);
        if (!(rawOriginal instanceof Mob original) || original.isRemoved()) return;
        Entity rawProxy = state.level.getEntity(state.proxyId);
        if (rawProxy instanceof Mob proxy && !proxy.isRemoved()) {
            state.lastPosition = proxy.position();
            state.lastYaw = proxy.getYRot();
            state.lastPitch = proxy.getXRot();
            proxy.discard();
        }
        original.setInvisible(state.oldInvisible);
        original.setInvulnerable(state.oldInvulnerable);
        original.setNoGravity(state.oldNoGravity);
        original.setSilent(state.oldSilent);
        original.setNoAi(state.oldNoAi);
        original.noPhysics = state.oldNoPhysics;
        Vec3 restore = state.lastPosition == null ? state.anchor : state.lastPosition;
        original.snapTo(restore.x, restore.y, restore.z, state.lastYaw, state.lastPitch);
        float health = proxyBroken ? Math.max(1.0F, state.oldHealth * .35F) : state.oldHealth;
        original.setHealth(Math.min(original.getMaxHealth(), Math.max(1.0F, health)));
        original.setTarget(living(state.level, state.oldTargetId));
        original.fallDistance = 0.0F;
    }

    // Weird ------------------------------------------------------------------------------------

    private static boolean weird(ServerLevel level, LivingEntity caster, Vec3 center, double range, double power) {
        double radius = Math.max(16.0, Math.min(28.0, range * .42));
        List<LivingEntity> targets = enemies(level, caster, center, radius, Math.max(10.0, radius * .66));
        int applied = 0;
        for (LivingEntity target : targets) {
            if (applied >= 16) break;
            WeirdState old = WEIRD.remove(target.getUUID());
            if (old != null) restoreWeird(old);
            UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
            WEIRD.put(target.getUUID(), new WeirdState(level, caster.getUUID(), target.getUUID(), power,
                    level.getGameTime() + WEIRD_TICKS, level.getGameTime(), oldTarget));
            ArcaneDamage.hurt(level, caster, target, (float) (power * .42));
            applyWeird(caster, target);
            applied++;
        }
        return applied > 0 || caster.isAlive();
    }

    private static void tickWeird(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, WeirdState>> iterator = WEIRD.entrySet().iterator();
        while (iterator.hasNext()) {
            WeirdState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreWeird(state);
                iterator.remove();
                continue;
            }
            if (now >= state.nextPulse) {
                state.nextPulse = now + 10L;
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(1.0, state.power * .075));
            }
            applyWeird(owner, target);
        }
    }

    private static void applyWeird(LivingEntity owner, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 14, 4, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 14, 1, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 14, 1, true, false));
        if (target instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 14, 2, true, false));
            suppressPlayerCasting(player);
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            Vec3 away = horizontal(target.position().subtract(owner.position()));
            Vec3 destination = target.position().add(away.scale(20.0));
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.28);
            WorldMagicService.stop(mob);
        }
    }

    private static void restoreWeird(WeirdState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (raw instanceof Mob mob && !mob.isRemoved()) mob.setTarget(living(state.level, state.oldTargetId));
    }

    // Wish -------------------------------------------------------------------------------------

    /** NPCs have no player mana/cooldown profile; their Wish role is full combat recovery/cleanse. */
    private static boolean npcWish(Mob caster) {
        caster.setHealth(caster.getMaxHealth());
        caster.setRemainingFireTicks(0);
        caster.setTicksFrozen(0);
        caster.removeEffect(MobEffects.SLOWNESS);
        caster.removeEffect(MobEffects.WEAKNESS);
        caster.removeEffect(MobEffects.BLINDNESS);
        caster.removeEffect(MobEffects.NAUSEA);
        caster.removeEffect(MobEffects.WITHER);
        caster.removeEffect(MobEffects.POISON);
        caster.removeEffect(MobEffects.MINING_FATIGUE);
        caster.removeEffect(MobEffects.LEVITATION);
        caster.removeEffect(MobEffects.DARKNESS);
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 5, true, false));
        caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 2, true, false));
        return true;
    }

    // Gate -------------------------------------------------------------------------------------

    private static boolean gate(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot, double range) {
        Vec3 forward = horizontal(snapshot.launchDirection());
        Optional<BlockPos> sourceSafe = findSafe(level, caster.position().add(forward.scale(2.4)), 5);
        Optional<BlockPos> targetSafe = findSafe(level, snapshot.target(), 12);
        if (sourceSafe.isEmpty() || targetSafe.isEmpty()) {
            if (caster instanceof ServerPlayer player)
                ArcaneNoticeService.push(player, Component.literal("§c[월드 게이트] §f연결할 두 끝점 중 안전한 공간을 찾지 못했습니다."), 55);
            return false;
        }
        Vec3 source = standing(sourceSafe.get());
        Vec3 target = standing(targetSafe.get());
        if (source.distanceToSqr(target) < 100.0) {
            if (caster instanceof ServerPlayer player)
                ArcaneNoticeService.push(player, Component.literal("§7[월드 게이트] §f두 문이 너무 가까워 공간 회로를 고정할 수 없습니다."), 50);
            return false;
        }
        Vec3 targetArrival = findSafe(level, target.add(forward.scale(3.6)), 6)
                .map(NinthCircleSpellService::standing).orElse(target);
        Vec3 sourceArrival = findSafe(level, source.subtract(forward.scale(3.6)), 6)
                .map(NinthCircleSpellService::standing).orElse(source);
        GATES.removeIf(field -> field.ownerId.equals(caster.getUUID()));
        GATES.add(new GateField(level, caster.getUUID(), source, target, sourceArrival, targetArrival,
                Math.max(2.4, Math.min(3.4, range * .045)), level.getGameTime() + GATE_TICKS));
        if (caster instanceof ServerPlayer player)
            ArcaneNoticeService.push(player, Component.literal("§5[월드 게이트] §f30초 동안 두 지점을 잇는 실제 양방향 문을 열었습니다."), 85);
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
        AABB box = new AABB(entrance, entrance).inflate(gate.radius, 3.4, gate.radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value.isAlive() && !value.isRemoved())) {
            if (GATE_COOLDOWNS.getOrDefault(entity.getUUID(), 0L) > now) continue;
            Vec3 delta = entity.position().subtract(entrance);
            if (delta.x * delta.x + delta.z * delta.z > gate.radius * gate.radius || Math.abs(delta.y) > 3.4) continue;
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

    // NPC Foresight ----------------------------------------------------------------------------

    private static boolean npcForesight(ServerLevel level, Mob caster) {
        long now = level.getGameTime();
        NPC_FORESIGHT.put(caster.getUUID(), new NpcForesightState(level, caster.getUUID(),
                now + NPC_FORESIGHT_TICKS, now));
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, NPC_FORESIGHT_TICKS, 3, true, false));
        caster.addEffect(new MobEffectInstance(MobEffects.GLOWING, NPC_FORESIGHT_TICKS, 0, true, false));
        return true;
    }

    private static void tickNpcForesight(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcForesightState>> iterator = NPC_FORESIGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcForesightState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.ownerId);
            if (!(raw instanceof Mob caster) || !caster.isAlive() || caster.isRemoved() || now >= state.expiresAt) {
                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "foresight");
                iterator.remove();
            }
        }
    }

    // Helpers ----------------------------------------------------------------------------------

    private static LivingEntity targetEntity(ServerLevel level, LivingEntity fallback, CastTargetSnapshot snapshot) {
        if (snapshot.targetEntityId() != null) {
            Entity raw = level.getEntity(snapshot.targetEntityId());
            if (raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) return living;
            return null;
        }
        return fallback != null && fallback.isAlive() && !fallback.isRemoved() ? fallback : null;
    }

    private static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center,
                                              double horizontalRadius, double verticalRadius) {
        AABB box = new AABB(center, center).inflate(horizontalRadius, verticalRadius, horizontalRadius);
        List<LivingEntity> result = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value)
                        && center.distanceToSqr(value.position()) <= horizontalRadius * horizontalRadius));
        result.sort(Comparator.comparingDouble(value -> value.distanceToSqr(caster)));
        return result;
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static void suppressPlayerCasting(ServerPlayer player) {
        if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
        if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
        SpellKineticsService.cancel(player);
    }

    private static LivingEntity living(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity raw = level.getEntity(id);
        return raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static Optional<BlockPos> findSafe(ServerLevel level, Vec3 desired, int verticalSearch) {
        Optional<BlockPos> direct = findSafeVertical(level, desired, verticalSearch);
        if (direct.isPresent()) return direct;
        int x = (int) Math.floor(desired.x), z = (int) Math.floor(desired.z);
        int y = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int radius = 1; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                Optional<BlockPos> safe = findSafeVertical(level, new Vec3(x + dx, y, z + dz), verticalSearch);
                if (safe.isPresent()) return safe;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findSafeVertical(ServerLevel level, Vec3 desired, int verticalSearch) {
        int x = (int) Math.floor(desired.x), z = (int) Math.floor(desired.z);
        int startY = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int d = 0; d <= verticalSearch; d++) {
            int[] ys = d == 0 ? new int[]{startY} : new int[]{startY + d, startY - d};
            for (int y : ys) {
                if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 2) continue;
                BlockPos feet = new BlockPos(x, y, z);
                if (level.getBlockState(feet.below()).blocksMotion()
                        && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static Vec3 standing(BlockPos pos) { return new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5); }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static String whole(double value) { return Long.toString(Math.round(value)); }

    private static final class PrismaticWallField {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final Vec3 forward; final Vec3 right;
        final double halfWidth; final double depth; final double power; final long expiresAt;
        final Map<UUID, Long> nextHit = new HashMap<>();
        PrismaticWallField(ServerLevel level, UUID ownerId, Vec3 center, Vec3 forward, Vec3 right,
                           double halfWidth, double depth, double power, long expiresAt) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.forward = forward; this.right = right;
            this.halfWidth = halfWidth; this.depth = depth; this.power = power; this.expiresAt = expiresAt;
        }
    }

    private record NpcShapeState(ServerLevel level, UUID ownerId, double power, long expiresAt) {}
    private record NpcTimeField(ServerLevel level, UUID ownerId, Vec3 center, double radius, long expiresAt) {}
    private record FrozenMob(ServerLevel level, UUID entityId, boolean wasNoAi) {}
    private record FrozenEntity(ServerLevel level, UUID entityId, Vec3 velocity, boolean wasNoGravity) {}

    private static final class NpcPolymorphState {
        final ServerLevel level; final UUID ownerId; final UUID originalId; final UUID proxyId; final long expiresAt;
        final Vec3 anchor; final float oldHealth; final boolean oldInvisible; final boolean oldInvulnerable;
        final boolean oldNoGravity; final boolean oldSilent; final boolean oldNoAi; final boolean oldNoPhysics;
        final UUID oldTargetId; Vec3 lastPosition; float lastYaw; float lastPitch;
        NpcPolymorphState(ServerLevel level, UUID ownerId, UUID originalId, UUID proxyId, long expiresAt,
                          Vec3 anchor, float yaw, float pitch, float oldHealth, boolean oldInvisible,
                          boolean oldInvulnerable, boolean oldNoGravity, boolean oldSilent, boolean oldNoAi,
                          boolean oldNoPhysics, UUID oldTargetId) {
            this.level = level; this.ownerId = ownerId; this.originalId = originalId; this.proxyId = proxyId;
            this.expiresAt = expiresAt; this.anchor = anchor; this.lastPosition = anchor; this.lastYaw = yaw;
            this.lastPitch = pitch; this.oldHealth = oldHealth; this.oldInvisible = oldInvisible;
            this.oldInvulnerable = oldInvulnerable; this.oldNoGravity = oldNoGravity; this.oldSilent = oldSilent;
            this.oldNoAi = oldNoAi; this.oldNoPhysics = oldNoPhysics; this.oldTargetId = oldTargetId;
        }
    }

    private record NpcPlayerPolymorphState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt) {}

    private static final class WeirdState {
        final ServerLevel level; final UUID ownerId; final UUID targetId; final double power; final long expiresAt;
        final UUID oldTargetId; long nextPulse;
        WeirdState(ServerLevel level, UUID ownerId, UUID targetId, double power, long expiresAt,
                   long nextPulse, UUID oldTargetId) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId; this.power = power;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse; this.oldTargetId = oldTargetId;
        }
    }

    private record GateField(ServerLevel level, UUID ownerId, Vec3 source, Vec3 target,
                             Vec3 sourceArrival, Vec3 targetArrival, double radius, long expiresAt) {}

    private static final class NpcForesightState {
        final ServerLevel level; final UUID ownerId; final long expiresAt; long nextDodgeAt;
        NpcForesightState(ServerLevel level, UUID ownerId, long expiresAt, long nextDodgeAt) {
            this.level = level; this.ownerId = ownerId; this.expiresAt = expiresAt; this.nextDodgeAt = nextDodgeAt;
        }
    }
}
