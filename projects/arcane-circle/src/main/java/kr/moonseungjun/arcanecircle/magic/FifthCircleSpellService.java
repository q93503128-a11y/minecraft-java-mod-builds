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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative deep runtime for the ten direct 5th-circle spells.
 *
 * Fifth circle is battlefield-command magic: Cone of Cold owns a real widening cone, Wall of
 * Force blocks hostile bodies and spell trajectories, Cloudkill is a drifting poison front,
 * Telekinesis is a sustained grab and throw, Flame Strike is a vertical impact, Hold Monster is
 * hard control that even large creatures only partially resist, Mass Cure is true allied healing,
 * Passwall opens and later restores a physical tunnel, Dominate Person turns a person-scale foe
 * into a temporary combat proxy, and Insect Plague is a fixed concentration-breaking swarm field.
 */
public final class FifthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "cone_of_cold", "wall_of_force", "cloudkill", "telekinesis", "flame_strike",
            "hold_monster", "mass_cure_wounds", "passwall", "dominate_person", "insect_plague");

    public static final int FORCE_WALL_TICKS = 240;
    public static final int CLOUDKILL_TICKS = 220;
    public static final int TELEKINESIS_TICKS = 100;
    public static final int HOLD_MONSTER_TICKS = 300;
    public static final int PASSWALL_TICKS = 240;
    public static final int DOMINATE_PERSON_TICKS = 260;
    public static final int INSECT_PLAGUE_TICKS = 220;

    private static final List<ForceWall> FORCE_WALLS = new ArrayList<>();
    private static final List<CloudkillZone> CLOUDS = new ArrayList<>();
    private static final List<InsectZone> INSECTS = new ArrayList<>();
    private static final Map<UUID, TelekinesisState> TELEKINESIS = new HashMap<>();
    private static final Map<UUID, HoldState> HOLDS = new HashMap<>();
    private static final Map<UUID, DominateState> DOMINATED = new HashMap<>();
    private static final Map<UUID, JamState> SWARM_JAM = new HashMap<>();
    private static final List<PasswallState> PASSWALLS = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private FifthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "cone_of_cold" -> coneOfCold(level, caster, range, power, snapshot);
            case "wall_of_force" -> wallOfForce(level, caster, range, snapshot);
            case "cloudkill" -> cloudkill(level, caster, range, power, snapshot);
            case "telekinesis" -> telekinesis(level, caster, snapshot.targetEntity(caster).orElse(null));
            case "flame_strike" -> flameStrike(level, caster, range, power, snapshot.target());
            case "hold_monster" -> holdMonster(level, caster, snapshot.targetEntity(caster).orElse(null));
            case "mass_cure_wounds" -> massCure(level, caster, range, power);
            case "passwall" -> passwall(level, caster, range, snapshot);
            case "dominate_person" -> dominatePerson(level, caster, snapshot.targetEntity(caster).orElse(null));
            case "insect_plague" -> insectPlague(level, caster, range, power, snapshot.target());
            default -> false;
        };
    }

    /** NPC mages use the same fifth-circle semantic contracts before generic damage fallback. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "cone_of_cold" -> coneOfCold(level, caster, range, power, snapshot);
            case "wall_of_force" -> wallOfForce(level, caster, range, snapshot);
            case "cloudkill" -> cloudkill(level, caster, range, power, snapshot);
            case "telekinesis" -> telekinesis(level, caster, designatedTarget);
            case "flame_strike" -> flameStrike(level, caster, range, power, snapshot.target());
            case "hold_monster" -> holdMonster(level, caster, designatedTarget);
            case "mass_cure_wounds" -> massCure(level, caster, range, power);
            case "passwall" -> passwall(level, caster, range, snapshot);
            case "dominate_person" -> dominatePerson(level, caster, designatedTarget);
            case "insect_plague" -> insectPlague(level, caster, range, power, snapshot.target());
            default -> false;
        };
    }

    /** Hold/Domination and swarm concentration breaks prevent Arcane casting. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        long now = ((ServerLevel) caster.level()).getGameTime();
        HoldState hold = HOLDS.get(caster.getUUID());
        if (hold != null && hold.level == caster.level() && hold.expiresAt > now) return true;
        DominateState dominated = DOMINATED.get(caster.getUUID());
        if (dominated != null && dominated.level == caster.level() && dominated.expiresAt > now) return true;
        JamState jam = SWARM_JAM.get(caster.getUUID());
        return jam != null && jam.level == caster.level() && jam.expiresAt > now;
    }

    /** Hostile Wall of Force segments stop spell trajectories that cross the wall plane. */
    public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        for (ForceWall wall : FORCE_WALLS) {
            if (wall.level != caster.level() || !wall.active()) continue;
            Entity rawOwner = wall.level.getEntity(wall.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isAlliedTo(caster)) continue;
            if (crossesWall(snapshot.launchOrigin(), snapshot.target(), wall)) return true;
        }
        return false;
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickForceWalls(level, now);
        tickClouds(level, now);
        tickInsects(level, now);
        tickTelekinesis(level, now);
        tickHolds(level, now);
        tickDomination(level, now);
        tickPasswalls(level, now);
        cleanupJams(level, now);
    }

    public static void clear(LivingEntity subject) { if (subject != null) clear(subject.getUUID()); }

    public static void clear(UUID id) {
        if (id == null) return;
        FORCE_WALLS.removeIf(state -> state.ownerId.equals(id));
        CLOUDS.removeIf(state -> state.ownerId.equals(id));
        INSECTS.removeIf(state -> state.ownerId.equals(id));
        SWARM_JAM.remove(id);

        Iterator<Map.Entry<UUID, TelekinesisState>> telekinesis = TELEKINESIS.entrySet().iterator();
        while (telekinesis.hasNext()) {
            TelekinesisState state = telekinesis.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreTelekinesis(state, false);
            telekinesis.remove();
        }
        Iterator<Map.Entry<UUID, HoldState>> holds = HOLDS.entrySet().iterator();
        while (holds.hasNext()) {
            HoldState state = holds.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreHold(state);
            holds.remove();
        }
        Iterator<Map.Entry<UUID, DominateState>> dominated = DOMINATED.entrySet().iterator();
        while (dominated.hasNext()) {
            DominateState state = dominated.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreDomination(state);
            dominated.remove();
        }
        Iterator<PasswallState> passwalls = PASSWALLS.iterator();
        while (passwalls.hasNext()) {
            PasswallState state = passwalls.next();
            if (!state.ownerId.equals(id)) continue;
            restorePasswall(state);
            passwalls.remove();
        }
    }

    public static void clearAll() {
        for (TelekinesisState state : TELEKINESIS.values()) restoreTelekinesis(state, false);
        for (HoldState state : HOLDS.values()) restoreHold(state);
        for (DominateState state : DOMINATED.values()) restoreDomination(state);
        for (PasswallState state : PASSWALLS) restorePasswall(state);
        FORCE_WALLS.clear();
        CLOUDS.clear();
        INSECTS.clear();
        TELEKINESIS.clear();
        HOLDS.clear();
        DOMINATED.clear();
        SWARM_JAM.clear();
        PASSWALLS.clear();
        LAST_TICK.clear();
    }

    private static boolean coneOfCold(ServerLevel level, LivingEntity caster, double range, double power,
                                      CastTargetSnapshot snapshot) {
        Vec3 origin = snapshot.launchOrigin();
        Vec3 direction = snapshot.launchDirection().normalize();
        double length = Math.max(10.0, Math.min(24.0, SpellMetrics.waveLength(range)));
        double endRadius = Math.max(5.0, Math.min(11.0, SpellMetrics.waveEndRadius("cone_of_cold", range, 5)));
        AABB box = new AABB(origin, origin.add(direction.scale(length))).inflate(endRadius + 1.0);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            Vec3 relative = target.getEyePosition().subtract(origin);
            double forward = relative.dot(direction);
            if (forward < 0.0 || forward > length) continue;
            double allowed = endRadius * (.18 + .82 * forward / length) + target.getBbWidth() * .50;
            if (relative.subtract(direction.scale(forward)).lengthSqr() > allowed * allowed) continue;
            double falloff = .82 + .18 * (1.0 - forward / length);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * falloff))) hit = true;
            target.setRemainingFireTicks(0);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 320));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3, true, false));
            target.push(direction.x * .35, .06, direction.z * .35);
        }
        level.playSound(null, BlockPos.containing(origin.add(direction.scale(length * .55))), SoundEvents.GLASS_BREAK,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .90F, .62F);
        return hit || length > 0.0;
    }

    private static boolean wallOfForce(ServerLevel level, LivingEntity caster, double range,
                                       CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target();
        Vec3 forward = horizontal(snapshot.launchDirection());
        double halfWidth = Math.max(6.0, Math.min(12.0, SpellMetrics.wallWidth("wall_of_force", range, 5) * .50));
        FORCE_WALLS.removeIf(state -> state.ownerId.equals(caster.getUUID()));
        FORCE_WALLS.add(new ForceWall(level, caster.getUUID(), center, forward, halfWidth,
                level.getGameTime() + FORCE_WALL_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§b[역장벽] §f12초 · 폭 " + one(halfWidth * 2.0)
                    + "m · 적대 생명체와 적대 주문 궤적을 실제로 차단합니다."), 75);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_ACTIVATE,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .75F, 1.12F);
        return true;
    }

    private static void tickForceWalls(ServerLevel level, long now) {
        Iterator<ForceWall> iterator = FORCE_WALLS.iterator();
        while (iterator.hasNext()) {
            ForceWall wall = iterator.next();
            if (wall.level != level) continue;
            Entity rawOwner = level.getEntity(wall.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= wall.expiresAt) {
                iterator.remove();
                continue;
            }
            Vec3 right = wall.right();
            AABB box = new AABB(wall.center, wall.center).inflate(wall.halfWidth + 2.0, 5.5, wall.halfWidth + 2.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) {
                Vec3 delta = target.position().subtract(wall.center);
                double lateral = Math.abs(delta.dot(right));
                double depth = delta.dot(wall.forward);
                if (lateral > wall.halfWidth + target.getBbWidth() || Math.abs(depth) > 1.0 + target.getBbWidth()) continue;
                double side = Math.abs(depth) < .05 ? Math.signum(target.position().subtract(owner.position()).dot(wall.forward)) : Math.signum(depth);
                if (side == 0.0) side = 1.0;
                target.push(wall.forward.x * side * .48, .03, wall.forward.z * side * .48);
                Vec3 motion = target.getDeltaMovement();
                double normal = motion.dot(wall.forward);
                if (normal * side < 0.0) target.setDeltaMovement(motion.subtract(wall.forward.scale(normal)));
            }
        }
    }

    private static boolean crossesWall(Vec3 start, Vec3 end, ForceWall wall) {
        Vec3 delta = end.subtract(start);
        double startSide = start.subtract(wall.center).dot(wall.forward);
        double endSide = end.subtract(wall.center).dot(wall.forward);
        if (startSide * endSide > 0.0 || Math.abs(startSide - endSide) < 1.0E-7) return false;
        double t = startSide / (startSide - endSide);
        if (t < 0.0 || t > 1.0) return false;
        Vec3 hit = start.add(delta.scale(t));
        Vec3 offset = hit.subtract(wall.center);
        double lateral = Math.abs(offset.dot(wall.right()));
        double vertical = hit.y - wall.center.y;
        return lateral <= wall.halfWidth + .8 && vertical >= -.5 && vertical <= 5.5;
    }

    private static boolean cloudkill(ServerLevel level, LivingEntity caster, double range, double power,
                                     CastTargetSnapshot snapshot) {
        double radius = Math.max(7.0, Math.min(11.0, SpellMetrics.effectRadius("cloudkill", range, 5)));
        Vec3 drift = horizontal(snapshot.launchDirection());
        CLOUDS.removeIf(state -> state.ownerId.equals(caster.getUUID()));
        CLOUDS.add(new CloudkillZone(level, caster.getUUID(), snapshot.target(), drift, radius, power,
                level.getGameTime() + CLOUDKILL_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§2[독구름] §f11초 · 반경 " + one(radius)
                    + "m · 시전 방향으로 천천히 이동하며 약해진 적에게 더 치명적인 독성 전선을 형성합니다."), 78);
        }
        return true;
    }

    private static void tickClouds(ServerLevel level, long now) {
        Iterator<CloudkillZone> iterator = CLOUDS.iterator();
        while (iterator.hasNext()) {
            CloudkillZone state = iterator.next();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse) continue;
            state.nextPulse = now + 10L;
            state.center = state.center.add(state.drift.scale(.45));
            AABB box = new AABB(state.center, state.center).inflate(state.radius, Math.max(5.0, state.radius * .65), state.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value) && state.center.distanceToSqr(value.position()) <= state.radius * state.radius)) {
                double ratio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
                double executePressure = ratio <= .35 ? 1.45 : 1.0;
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.6, state.power * .065 * executePressure));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 28, ratio <= .35 ? 2 : 1, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 18, 0, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 1, true, false));
            }
        }
    }

    private static boolean telekinesis(ServerLevel level, LivingEntity caster, LivingEntity target) {
        if (!enemy(caster, target) || !telekinesisEligible(target)) return false;
        TelekinesisState previous = TELEKINESIS.remove(target.getUUID());
        if (previous != null) restoreTelekinesis(previous, false);
        TELEKINESIS.put(target.getUUID(), new TelekinesisState(level, caster.getUUID(), target.getUUID(),
                level.getGameTime() + TELEKINESIS_TICKS, target.isNoGravity()));
        target.setNoGravity(true);
        target.fallDistance = 0.0F;
        if (target instanceof Mob mob) mob.getNavigation().stop();
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§d[염동력] §f5초 동안 대상을 시선 앞에 붙잡습니다. §7종료 순간 현재 시선 방향으로 강하게 내던집니다."), 72);
        }
        return true;
    }

    private static boolean telekinesisEligible(LivingEntity target) {
        return target.getBbWidth() <= 3.5F && target.getBbHeight() <= 5.5F && target.getMaxHealth() <= 320.0F;
    }

    private static void tickTelekinesis(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, TelekinesisState>> iterator = TELEKINESIS.entrySet().iterator();
        while (iterator.hasNext()) {
            TelekinesisState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
                restoreTelekinesis(state, false);
                iterator.remove();
                continue;
            }
            if (now >= state.expiresAt) {
                restoreTelekinesis(state, true);
                iterator.remove();
                continue;
            }
            target.setNoGravity(true);
            target.fallDistance = 0.0F;
            if (target instanceof Mob mob) mob.getNavigation().stop();
            Vec3 look = owner.getLookAngle().normalize();
            Vec3 desired = owner.getEyePosition().add(look.scale(4.5)).add(0.0, -.65, 0.0);
            Vec3 delta = desired.subtract(target.position());
            double distance = delta.length();
            Vec3 pull = distance < .15 ? Vec3.ZERO : delta.scale(Math.min(.42, .18 + distance * .055) / Math.max(.001, distance));
            target.setDeltaMovement(target.getDeltaMovement().scale(.30).add(pull));
        }
    }

    private static void restoreTelekinesis(TelekinesisState state, boolean fling) {
        Entity rawTarget = state.level.getEntity(state.targetId);
        if (!(rawTarget instanceof LivingEntity target) || target.isRemoved()) return;
        target.setNoGravity(state.wasNoGravity);
        target.fallDistance = 0.0F;
        if (!fling) return;
        Entity rawOwner = state.level.getEntity(state.ownerId);
        if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()) return;
        Vec3 look = owner.getLookAngle().normalize();
        target.setDeltaMovement(look.scale(2.25).add(0.0, .72, 0.0));
    }

    private static boolean flameStrike(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(5.5, Math.min(8.0, SpellMetrics.effectRadius("flame_strike", range, 5)));
        AABB box = new AABB(center, center).inflate(radius, 9.0, radius);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            double horizontal = new Vec3(target.getX() - center.x, 0.0, target.getZ() - center.z).length();
            if (horizontal > radius + target.getBbWidth()) continue;
            double falloff = Math.max(.65, 1.0 - horizontal / Math.max(1.0, radius) * .35);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * 1.08 * falloff))) hit = true;
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 220));
            target.push(0.0, .28 * falloff, 0.0);
        }
        if (caster instanceof ServerPlayer player) DestructiveMagicService.impact(player, "flame_strike", center, radius, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.15F, .76F);
        return hit || center != null;
    }

    private static boolean holdMonster(ServerLevel level, LivingEntity caster, LivingEntity target) {
        if (!enemy(caster, target)) return false;
        long duration = holdDuration(target);
        HoldState previous = HOLDS.remove(target.getUUID());
        if (previous != null) restoreHold(previous);
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        HOLDS.put(target.getUUID(), new HoldState(level, caster.getUUID(), target.getUUID(), oldTarget,
                level.getGameTime() + duration));
        enforceHold(target);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[괴물 속박] §f" + target.getName().getString() + "을 "
                    + one(duration / 20.0) + "초 봉쇄했습니다. §7초대형/보스급은 지속시간으로 저항하지만 완전 면역은 아닙니다."), 78);
        }
        return true;
    }

    private static long holdDuration(LivingEntity target) {
        return target.getMaxHealth() > 220.0F || target.getBbWidth() > 3.2F || target.getBbHeight() > 5.0F
                ? 140L : HOLD_MONSTER_TICKS;
    }

    private static void enforceHold(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 255, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 6, true, false));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            WorldMagicService.stop(mob);
        } else if (target instanceof ServerPlayer player) {
            interruptPlayer(player);
        }
    }

    private static void tickHolds(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, HoldState>> iterator = HOLDS.entrySet().iterator();
        while (iterator.hasNext()) {
            HoldState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            Entity targetRaw = level.getEntity(state.targetId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive()
                    || !(targetRaw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreHold(state);
                iterator.remove();
                continue;
            }
            enforceHold(target);
        }
    }

    private static void restoreHold(HoldState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) return;
        LivingEntity oldTarget = null;
        if (state.oldTargetId != null) {
            Entity candidate = state.level.getEntity(state.oldTargetId);
            if (candidate instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) oldTarget = living;
        }
        mob.setTarget(oldTarget);
    }

    private static boolean massCure(ServerLevel level, LivingEntity caster, double range, double power) {
        double radius = Math.max(8.0, Math.min(14.0, range));
        AABB box = caster.getBoundingBox().inflate(radius, 6.0, radius);
        int healed = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> ally(caster, value))) {
            float before = target.getHealth();
            float amount = (float) Math.max(2.0, power * (target instanceof ServerPlayer ? 1.0 : .45));
            target.heal(amount);
            if (target.getHealth() > before) healed++;
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .62F, 1.42F);
        return healed > 0;
    }

    private static boolean passwall(ServerLevel level, LivingEntity caster, double range, CastTargetSnapshot snapshot) {
        Vec3 direction = snapshot.launchDirection().normalize();
        Vec3 right = horizontal(new Vec3(-direction.z, 0.0, direction.x));
        int maxDepth = Math.max(6, Math.min(16, (int) Math.round(range * .55)));
        List<ChangedBlock> changed = new ArrayList<>();
        boolean enteredWall = false;
        int openSlices = 0;

        for (int step = 2; step <= maxDepth; step++) {
            BlockPos center = BlockPos.containing(snapshot.launchOrigin().add(direction.scale(step)));
            boolean solidSlice = false;
            List<BlockPos> slice = new ArrayList<>();
            for (int side = -1; side <= 1; side++) {
                int ox = (int) Math.round(right.x * side);
                int oz = (int) Math.round(right.z * side);
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = center.offset(ox, dy, oz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.blocksMotion()) continue;
                    solidSlice = true;
                    if (protectedPasswallBlock(level, pos, state)) {
                        restoreBlocksImmediately(level, changed);
                        if (caster instanceof ServerPlayer player) {
                            ArcaneNoticeService.push(player, Component.literal("§c[통과문] §f보호된 구조물 때문에 공간 통로를 열 수 없습니다."), 55);
                        }
                        return false;
                    }
                    if (slice.stream().noneMatch(pos::equals)) slice.add(pos);
                }
            }
            if (solidSlice) {
                enteredWall = true;
                openSlices = 0;
                for (BlockPos pos : slice) {
                    BlockState original = level.getBlockState(pos);
                    if (!original.blocksMotion()) continue;
                    changed.add(new ChangedBlock(pos.immutable(), original));
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            } else if (enteredWall) {
                openSlices++;
                if (openSlices >= 2) break;
            }
        }

        if (changed.isEmpty()) return false;
        PASSWALLS.removeIf(state -> {
            if (!state.ownerId.equals(caster.getUUID())) return false;
            restorePasswall(state);
            return true;
        });
        PASSWALLS.add(new PasswallState(level, caster.getUUID(), changed, level.getGameTime() + PASSWALL_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[통과문] §f실제 벽을 관통하는 임시 통로를 열었습니다. §7약 12초 뒤 통로가 비어 있으면 원래 블록으로 복원됩니다."), 85);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .82F, .72F);
        return true;
    }

    private static boolean protectedPasswallBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) return true;
        return state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER) || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.REINFORCED_DEEPSLATE) || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.JIGSAW);
    }

    private static void tickPasswalls(ServerLevel level, long now) {
        Iterator<PasswallState> iterator = PASSWALLS.iterator();
        while (iterator.hasNext()) {
            PasswallState state = iterator.next();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()) {
                restorePasswall(state);
                iterator.remove();
                continue;
            }
            if (now < state.restoreAt) continue;
            if (passageOccupied(state)) {
                state.restoreAt = now + 20L;
                continue;
            }
            restorePasswall(state);
            iterator.remove();
        }
    }

    private static boolean passageOccupied(PasswallState state) {
        for (ChangedBlock changed : state.blocks) {
            BlockPos pos = changed.pos;
            AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
            if (!state.level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> value.isAlive() && !value.isRemoved()).isEmpty()) return true;
        }
        return false;
    }

    private static void restorePasswall(PasswallState state) {
        restoreBlocksImmediately(state.level, state.blocks);
    }

    private static void restoreBlocksImmediately(ServerLevel level, List<ChangedBlock> changed) {
        for (ChangedBlock block : changed) {
            if (level.getBlockState(block.pos).isAir()) level.setBlockAndUpdate(block.pos, block.original);
        }
    }

    private static boolean dominatePerson(ServerLevel level, LivingEntity caster, LivingEntity target) {
        if (!(target instanceof Mob mob) || !enemy(caster, mob) || !personScale(mob)) return false;
        DominateState previous = DOMINATED.remove(mob.getUUID());
        if (previous != null) restoreDomination(previous);
        UUID oldTarget = mob.getTarget() == null ? null : mob.getTarget().getUUID();
        DOMINATED.put(mob.getUUID(), new DominateState(level, caster.getUUID(), mob.getUUID(), oldTarget,
                level.getGameTime() + DOMINATE_PERSON_TICKS));
        mob.setTarget(null);
        mob.getNavigation().stop();
        WorldMagicService.stop(mob);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§d[인간형 지배] §f" + mob.getName().getString()
                    + "의 의지를 13초간 장악했습니다. §7시전자를 공격하지 않고 주변 위협과 싸우며 비전투 시 따라옵니다."), 88);
        }
        return true;
    }

    private static boolean personScale(Mob target) {
        return target.getBbWidth() <= 1.8F && target.getBbHeight() <= 3.4F && target.getMaxHealth() <= 140.0F;
    }

    private static void tickDomination(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, DominateState>> iterator = DOMINATED.entrySet().iterator();
        while (iterator.hasNext()) {
            DominateState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof Mob target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreDomination(state);
                iterator.remove();
                continue;
            }
            WorldMagicService.stop(target);
            LivingEntity current = target.getTarget();
            if (current == owner || (current != null && owner.isAlliedTo(current))) target.setTarget(null);
            Mob threat = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(14.0),
                            candidate -> candidate != target && enemy(owner, candidate)
                                    && !DOMINATED.containsKey(candidate.getUUID()))
                    .stream().min(Comparator.comparingDouble(target::distanceToSqr)).orElse(null);
            if (threat != null) {
                target.setTarget(threat);
            } else if (target.distanceToSqr(owner) > 16.0) {
                target.setTarget(null);
                target.getNavigation().moveTo(owner, 1.10);
            }
        }
    }

    private static void restoreDomination(DominateState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob target) || !target.isAlive() || target.isRemoved()) return;
        target.getNavigation().stop();
        LivingEntity oldTarget = null;
        if (state.oldTargetId != null) {
            Entity candidate = state.level.getEntity(state.oldTargetId);
            if (candidate instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) oldTarget = living;
        }
        target.setTarget(oldTarget);
    }

    private static boolean insectPlague(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(7.0, Math.min(11.0, SpellMetrics.effectRadius("insect_plague", range, 5)));
        INSECTS.removeIf(state -> state.ownerId.equals(caster.getUUID()));
        INSECTS.add(new InsectZone(level, caster.getUUID(), center, radius, power,
                level.getGameTime() + INSECT_PLAGUE_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§6[곤충 떼] §f11초 고정 영역 · 반복 피해·행동 방해 + 내부 Arcane 집중을 간헐적으로 끊습니다."), 78);
        }
        return true;
    }

    private static void tickInsects(ServerLevel level, long now) {
        Iterator<InsectZone> iterator = INSECTS.iterator();
        while (iterator.hasNext()) {
            InsectZone state = iterator.next();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse) continue;
            state.nextPulse = now + 10L;
            AABB box = new AABB(state.center, state.center).inflate(state.radius, Math.max(5.0, state.radius * .70), state.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value) && state.center.distanceToSqr(value.position()) <= state.radius * state.radius)) {
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .05));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 18, 1, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 18, 1, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 18, 2, true, false));
                if (target.getRandom().nextFloat() < .45F) {
                    SWARM_JAM.put(target.getUUID(), new JamState(level, now + 12L));
                    interruptCasting(target);
                }
            }
        }
    }

    private static void cleanupJams(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, JamState>> iterator = SWARM_JAM.entrySet().iterator();
        while (iterator.hasNext()) {
            JamState state = iterator.next().getValue();
            if (state.level == level && now >= state.expiresAt) iterator.remove();
        }
    }

    private static void interruptCasting(LivingEntity target) {
        if (target instanceof ServerPlayer player) interruptPlayer(player);
        else if (target instanceof Mob mob) WorldMagicService.stop(mob);
    }

    private static void interruptPlayer(ServerPlayer player) {
        if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
        if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
        SpellKineticsService.cancel(player);
    }

    private static boolean ally(LivingEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || owner.level() != target.level()) return false;
        if (target == owner || owner.isAlliedTo(target)) return true;
        if (target instanceof TamableAnimal tame && tame.isTame() && owner instanceof ServerPlayer player && tame.isOwnedBy(player)) return true;
        return false;
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static String one(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static final class ForceWall {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final Vec3 forward;
        final double halfWidth; final long expiresAt;
        ForceWall(ServerLevel level, UUID ownerId, Vec3 center, Vec3 forward, double halfWidth, long expiresAt) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.forward = forward;
            this.halfWidth = halfWidth; this.expiresAt = expiresAt;
        }
        Vec3 right() { return new Vec3(-forward.z, 0.0, forward.x); }
        boolean active() { return level.getGameTime() < expiresAt; }
    }

    private static final class CloudkillZone {
        final ServerLevel level; final UUID ownerId; Vec3 center; final Vec3 drift;
        final double radius; final double power; final long expiresAt; long nextPulse;
        CloudkillZone(ServerLevel level, UUID ownerId, Vec3 center, Vec3 drift, double radius,
                      double power, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.drift = drift;
            this.radius = radius; this.power = power; this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }

    private record TelekinesisState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt,
                                    boolean wasNoGravity) {}
    private record HoldState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId, long expiresAt) {}
    private record DominateState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId, long expiresAt) {}
    private record JamState(ServerLevel level, long expiresAt) {}
    private record ChangedBlock(BlockPos pos, BlockState original) {}

    private static final class PasswallState {
        final ServerLevel level; final UUID ownerId; final List<ChangedBlock> blocks; long restoreAt;
        PasswallState(ServerLevel level, UUID ownerId, List<ChangedBlock> blocks, long restoreAt) {
            this.level = level; this.ownerId = ownerId; this.blocks = List.copyOf(blocks); this.restoreAt = restoreAt;
        }
    }

    private static final class InsectZone {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final double radius; final double power;
        final long expiresAt; long nextPulse;
        InsectZone(ServerLevel level, UUID ownerId, Vec3 center, double radius, double power,
                   long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.radius = radius;
            this.power = power; this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }
}
