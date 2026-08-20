package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

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
 * Server-authoritative deep runtime for the ten direct 4th-circle spells.
 *
 * Fourth circle is the first strategic tier: walls and storms persist in world space, Greater
 * Invisibility remains combat-capable, Resilient Sphere is true two-way isolation, Dimension Door
 * can carry one willing nearby ally, Stoneskin only resists attacker-caused physical damage,
 * Confusion changes decisions instead of merely slowing, Blight suppresses healing while draining,
 * Freedom continuously rejects movement control, and Phantasmal Killer forces real retreat.
 */
public final class FourthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "wall_of_fire", "ice_storm", "greater_invisibility", "resilient_sphere",
            "dimension_door", "stoneskin", "confusion", "blight",
            "freedom_of_movement", "phantasmal_killer");

    public static final int WALL_TICKS = 240;
    public static final int ICE_STORM_PULSES = 5;
    public static final int GREATER_INVISIBILITY_TICKS = 780;
    public static final int SPHERE_TICKS = 400;
    public static final int STONESKIN_TICKS = 760;
    public static final int CONFUSION_TICKS = 240;
    public static final int BLIGHT_TICKS = 160;
    public static final int FREEDOM_TICKS = 520;
    public static final int PHANTASM_TICKS = 220;

    private static final List<FireWall> FIRE_WALLS = new ArrayList<>();
    private static final List<IceStorm> ICE_STORMS = new ArrayList<>();
    private static final Map<UUID, TimedState> GREATER_INVISIBILITY = new HashMap<>();
    private static final Map<UUID, TimedState> SPHERES = new HashMap<>();
    private static final Map<UUID, TimedState> STONESKIN = new HashMap<>();
    private static final Map<UUID, ConfusionState> CONFUSION = new HashMap<>();
    private static final Map<UUID, BlightState> BLIGHT = new HashMap<>();
    private static final Map<UUID, TimedState> FREEDOM = new HashMap<>();
    private static final Map<UUID, FearState> FEAR = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private FourthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "wall_of_fire" -> wallOfFire(level, caster, range, power, snapshot);
            case "ice_storm" -> iceStorm(level, caster, range, power, snapshot.target());
            case "greater_invisibility" -> greaterInvisibility(level, caster);
            case "resilient_sphere" -> resilientSphere(level, caster);
            case "dimension_door" -> dimensionDoor(caster, range, snapshot);
            case "stoneskin" -> stoneskin(level, caster);
            case "confusion" -> confusion(level, caster, range, snapshot.target());
            case "blight" -> blight(level, caster, snapshot.targetEntity(caster).orElse(null), power);
            case "freedom_of_movement" -> freedom(level, caster);
            case "phantasmal_killer" -> phantasmalKiller(level, caster,
                    snapshot.targetEntity(caster).orElse(null), power);
            default -> false;
        };
    }

    /** NPC mages resolve the same fourth-circle semantic roles before generic direct damage. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "wall_of_fire" -> wallOfFire(level, caster, range, power, snapshot);
            case "ice_storm" -> iceStorm(level, caster, range, power, snapshot.target());
            case "greater_invisibility" -> greaterInvisibility(level, caster);
            case "resilient_sphere" -> resilientSphere(level, caster);
            case "dimension_door" -> dimensionDoor(level, caster, designatedTarget, range);
            case "stoneskin" -> stoneskin(level, caster);
            case "confusion" -> confusion(level, caster, range, snapshot.target());
            case "blight" -> blight(level, caster, designatedTarget, power);
            case "freedom_of_movement" -> freedom(level, caster);
            case "phantasmal_killer" -> phantasmalKiller(level, caster, designatedTarget, power);
            default -> false;
        };
    }

    /** A resilient sphere isolates its occupant; confusion intermittently scrambles Arcane casting. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        long now = ((ServerLevel) caster.level()).getGameTime();
        TimedState sphere = SPHERES.get(caster.getUUID());
        if (sphere != null && sphere.level == caster.level() && sphere.expiresAt > now) return true;
        ConfusionState confusion = CONFUSION.get(caster.getUUID());
        return confusion != null && confusion.level == caster.level()
                && confusion.expiresAt > now && confusion.castBlockedUntil > now;
    }

    /** Freedom does not negate antimagic/time stop/high-circle control, only movement-control tiers. */
    public static boolean hasFreedom(LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        TimedState state = FREEDOM.get(target.getUUID());
        return state != null && state.level == target.level()
                && state.expiresAt > ((ServerLevel) target.level()).getGameTime();
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event == null || event.isCanceled() || event.getAmount() <= 0.0F) return;
        LivingEntity target = event.getEntity();
        long now = target.level() instanceof ServerLevel level ? level.getGameTime() : target.tickCount;

        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            TimedState sphere = SPHERES.get(target.getUUID());
            if (sphere != null && sphere.level == target.level() && sphere.expiresAt > now) {
                event.setCanceled(true);
                sphere.level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                        target instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .55F, 1.20F);
                return;
            }
            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker) {
                TimedState attackerSphere = SPHERES.get(attacker.getUUID());
                if (attackerSphere != null && attackerSphere.level == attacker.level()
                        && attackerSphere.expiresAt > now) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        LivingEntity hostile = hostileAttacker(target, event);
        TimedState veil = GREATER_INVISIBILITY.get(target.getUUID());
        if (hostile != null && veil != null && veil.level == target.level() && veil.expiresAt > now
                && target.getRandom().nextFloat() < .45F) {
            event.setCanceled(true);
            veil.level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    target instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .55F, 1.58F);
            return;
        }

        TimedState stone = STONESKIN.get(target.getUUID());
        if (stone != null && stone.level == target.level() && stone.expiresAt > now && physicalAttack(event)) {
            event.setAmount(Math.max(.05F, event.getAmount() * .50F));
        }
    }

    public static void onHeal(LivingHealEvent event) {
        if (event == null || event.getAmount() <= 0.0F) return;
        LivingEntity target = event.getEntity();
        BlightState state = BLIGHT.get(target.getUUID());
        if (state == null || state.level != target.level() || !state.active()) return;
        event.setAmount(event.getAmount() * .20F);
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickFireWalls(level, now);
        tickIceStorms(level, now);
        tickGreaterInvisibility(level, now);
        tickTimed(level, now);
        tickConfusion(level, now);
        tickBlight(level, now);
        tickFreedom(level, now);
        tickFear(level, now);
    }

    public static void clear(LivingEntity subject) {
        if (subject != null) clear(subject.getUUID());
    }

    public static void clear(UUID id) {
        if (id == null) return;
        FIRE_WALLS.removeIf(state -> state.ownerId.equals(id));
        ICE_STORMS.removeIf(state -> state.ownerId.equals(id));
        GREATER_INVISIBILITY.remove(id);
        SPHERES.remove(id);
        STONESKIN.remove(id);
        FREEDOM.remove(id);
        Iterator<Map.Entry<UUID, ConfusionState>> confused = CONFUSION.entrySet().iterator();
        while (confused.hasNext()) {
            ConfusionState state = confused.next().getValue();
            if (state.ownerId.equals(id) || state.targetId.equals(id)) confused.remove();
        }
        Iterator<Map.Entry<UUID, BlightState>> blights = BLIGHT.entrySet().iterator();
        while (blights.hasNext()) {
            BlightState state = blights.next().getValue();
            if (state.ownerId.equals(id) || state.targetId.equals(id)) blights.remove();
        }
        Iterator<Map.Entry<UUID, FearState>> fears = FEAR.entrySet().iterator();
        while (fears.hasNext()) {
            FearState state = fears.next().getValue();
            if (state.ownerId.equals(id) || state.targetId.equals(id)) fears.remove();
        }
    }

    public static void clearAll() {
        FIRE_WALLS.clear();
        ICE_STORMS.clear();
        GREATER_INVISIBILITY.clear();
        SPHERES.clear();
        STONESKIN.clear();
        CONFUSION.clear();
        BLIGHT.clear();
        FREEDOM.clear();
        FEAR.clear();
        LAST_TICK.clear();
    }

    private static boolean wallOfFire(ServerLevel level, LivingEntity caster, double range, double power,
                                      CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target();
        Vec3 forward = horizontal(snapshot.launchDirection());
        double halfWidth = Math.max(5.5, Math.min(11.0,
                SpellMetrics.wallWidth("wall_of_fire", range, 4) * .50));
        FIRE_WALLS.removeIf(state -> state.ownerId.equals(caster.getUUID()));
        FIRE_WALLS.add(new FireWall(level, caster.getUUID(), center, forward, halfWidth, power,
                level.getGameTime() + WALL_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§6[화염벽] §f12초 · 폭 약 "
                    + one(halfWidth * 2.0) + "m · 벽을 스치거나 통과하는 적을 지속 연소시킵니다."), 70);
        }
        return true;
    }

    private static void tickFireWalls(ServerLevel level, long now) {
        Iterator<FireWall> iterator = FIRE_WALLS.iterator();
        while (iterator.hasNext()) {
            FireWall state = iterator.next();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse) continue;
            state.nextPulse = now + 10L;
            Vec3 right = new Vec3(-state.forward.z, 0.0, state.forward.x);
            AABB box = new AABB(state.center, state.center).inflate(state.halfWidth + 2.0, 5.0, state.halfWidth + 2.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) {
                Vec3 delta = target.position().subtract(state.center);
                double lateral = Math.abs(delta.dot(right));
                double depth = Math.abs(delta.dot(state.forward));
                if (lateral > state.halfWidth + target.getBbWidth() || depth > 1.35 + target.getBbWidth()) continue;
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .055));
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 70));
            }
        }
    }

    private static boolean iceStorm(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(6.0, Math.min(10.0, SpellMetrics.effectRadius("ice_storm", range, 4)));
        ICE_STORMS.removeIf(state -> state.ownerId.equals(caster.getUUID()));
        ICE_STORMS.add(new IceStorm(level, caster.getUUID(), center, radius, power,
                level.getGameTime(), ICE_STORM_PULSES));
        level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .85F, .72F);
        return true;
    }

    private static void tickIceStorms(ServerLevel level, long now) {
        Iterator<IceStorm> iterator = ICE_STORMS.iterator();
        while (iterator.hasNext()) {
            IceStorm state = iterator.next();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive() || state.remaining <= 0) {
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse) continue;
            AABB box = new AABB(state.center, state.center).inflate(state.radius, Math.max(6.0, state.radius * .75), state.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value) && state.center.distanceToSqr(value.position()) <= state.radius * state.radius)) {
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.6, state.power * .24));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 45));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, true, false));
                target.push(0.0, -.12, 0.0);
            }
            state.remaining--;
            state.nextPulse = now + 8L;
            level.playSound(null, BlockPos.containing(state.center), SoundEvents.GLASS_BREAK,
                    owner instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                    .62F, .72F + state.remaining * .06F);
            if (state.remaining <= 0) iterator.remove();
        }
    }

    private static boolean greaterInvisibility(ServerLevel level, LivingEntity caster) {
        GREATER_INVISIBILITY.put(caster.getUUID(), new TimedState(level,
                level.getGameTime() + GREATER_INVISIBILITY_TICKS));
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                GREATER_INVISIBILITY_TICKS, 0, true, true));
        clearAggro(level, caster, 56.0);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§5[상급 투명화] §f39초 · 공격해도 풀리지 않으며 적대 추적을 계속 끊고 직접 공격은 45% 확률로 빗나갑니다."), 80);
        }
        return true;
    }

    private static void tickGreaterInvisibility(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, TimedState>> iterator = GREATER_INVISIBILITY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TimedState> entry = iterator.next();
            TimedState state = entry.getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity hidden) || !hidden.isAlive() || hidden.isRemoved() || now >= state.expiresAt) {
                if (raw instanceof LivingEntity living) living.removeEffect(MobEffects.INVISIBILITY);
                iterator.remove();
                continue;
            }
            hidden.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, true, true));
            if (now % 10L == 0L) clearAggro(level, hidden, 56.0);
        }
    }

    private static void clearAggro(ServerLevel level, LivingEntity hidden, double radius) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, hidden.getBoundingBox().inflate(radius),
                value -> value.isAlive() && value.getTarget() == hidden)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static boolean resilientSphere(ServerLevel level, LivingEntity caster) {
        SPHERES.put(caster.getUUID(), new TimedState(level, level.getGameTime() + SPHERE_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§b[탄성 구체] §f20초 · 안팎의 피해를 모두 차단하는 완전 격리막입니다. §7구체 안에서는 Arcane 시전도 할 수 없습니다."), 85);
        }
        return true;
    }

    private static boolean dimensionDoor(ServerPlayer caster, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) caster.level();
        double maxDistance = Math.max(24.0, Math.min(36.0, range * 1.18));
        Vec3 desired = clampDestination(caster.position(), snapshot.target(), maxDistance);
        Optional<BlockPos> destination = findSafe(level, desired, 10);
        if (destination.isEmpty()) return false;

        ServerPlayer companion = level.getEntitiesOfClass(ServerPlayer.class,
                        caster.getBoundingBox().inflate(3.0), value -> value != caster && value.isAlive()
                                && !value.isSpectator() && value.isCrouching())
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        Vec3 relative = companion == null ? Vec3.ZERO : companion.position().subtract(caster.position());
        BlockPos p = destination.get();
        caster.stopRiding();
        boolean moved = caster.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5,
                Set.<Relative>of(), caster.getYRot(), caster.getXRot(), true);
        if (!moved) return false;
        caster.fallDistance = 0.0F;

        if (companion != null) {
            Vec3 companionDesired = new Vec3(p.getX() + .5 + Math.max(-2.0, Math.min(2.0, relative.x)),
                    p.getY(), p.getZ() + .5 + Math.max(-2.0, Math.min(2.0, relative.z)));
            Optional<BlockPos> companionSafe = findSafe(level, companionDesired, 5);
            if (companionSafe.isPresent()) {
                BlockPos cp = companionSafe.get();
                companion.stopRiding();
                companion.teleportTo(level, cp.getX() + .5, cp.getY(), cp.getZ() + .5,
                        Set.<Relative>of(), companion.getYRot(), companion.getXRot(), true);
                companion.fallDistance = 0.0F;
            }
        }
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, .72F);
        ArcaneNoticeService.push(caster, Component.literal("§5[차원문] §f장거리 공간 접힘 완료"
                + (companion == null ? "" : " · 웅크린 동행자 1명 이동")), 60);
        return true;
    }

    private static boolean dimensionDoor(ServerLevel level, Mob caster, LivingEntity target, double range) {
        if (target == null || !target.isAlive()) return false;
        Vec3 delta = target.position().subtract(caster.position());
        if (delta.lengthSqr() < 1.0E-6) return false;
        double distance = Math.min(Math.max(14.0, range), Math.max(8.0, delta.length() - 4.0));
        Optional<BlockPos> safe = findSafe(level, caster.position().add(delta.normalize().scale(distance)), 10);
        if (safe.isEmpty()) return false;
        BlockPos p = safe.get();
        caster.getNavigation().stop();
        caster.snapTo(p.getX() + .5, p.getY(), p.getZ() + .5, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .92F, .70F);
        return true;
    }

    private static boolean stoneskin(ServerLevel level, LivingEntity caster) {
        STONESKIN.put(caster.getUUID(), new TimedState(level, level.getGameTime() + STONESKIN_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§7[스톤스킨] §f38초 · 적이 가하는 비마법 물리 공격만 50% 경감합니다. §7화염·Arcane·환경 피해는 그대로 받습니다."), 78);
        }
        return true;
    }

    private static boolean confusion(ServerLevel level, LivingEntity caster, double range, Vec3 center) {
        double radius = Math.max(5.5, Math.min(9.5, SpellMetrics.effectRadius("confusion", range, 4)));
        int affected = 0;
        AABB box = new AABB(center, center).inflate(radius, Math.max(5.0, radius * .72), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            long now = level.getGameTime();
            CONFUSION.put(target.getUUID(), new ConfusionState(level, caster.getUUID(), target.getUUID(),
                    now + CONFUSION_TICKS, now, now));
            affected++;
        }
        return affected > 0;
    }

    private static void tickConfusion(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, ConfusionState>> iterator = CONFUSION.entrySet().iterator();
        while (iterator.hasNext()) {
            ConfusionState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            Entity targetRaw = level.getEntity(state.targetId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive()
                    || !(targetRaw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 28, 0, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 28, 1, true, false));
            if (now < state.nextDecision) continue;
            state.nextDecision = now + 20L;
            int decision = target.getRandom().nextInt(4);
            state.castBlockedUntil = decision == 0 || decision == 3 ? now + 12L : now;
            if (target instanceof Mob mob) {
                switch (decision) {
                    case 0 -> { mob.setTarget(null); mob.getNavigation().stop(); }
                    case 1 -> mob.getNavigation().moveTo(target.getX() + target.getRandom().nextInt(13) - 6,
                            target.getY(), target.getZ() + target.getRandom().nextInt(13) - 6, 1.15);
                    case 2 -> randomConfusionTarget(level, mob).ifPresent(mob::setTarget);
                    default -> {
                        Vec3 impulse = new Vec3(target.getRandom().nextDouble() - .5, 0.0,
                                target.getRandom().nextDouble() - .5);
                        if (impulse.lengthSqr() > 1.0E-6) impulse = impulse.normalize().scale(.55);
                        target.push(impulse.x, .08, impulse.z);
                    }
                }
                if (state.castBlockedUntil > now) WorldMagicService.stop(mob);
            } else if (target instanceof ServerPlayer player) {
                if (decision == 1 || decision == 3) {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
                    player.push(Math.cos(angle) * .18, 0.0, Math.sin(angle) * .18);
                }
                if (state.castBlockedUntil > now) {
                    if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
                    if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
                    SpellKineticsService.cancel(player);
                }
            }
        }
    }

    private static Optional<LivingEntity> randomConfusionTarget(ServerLevel level, Mob mob) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(8.0), value -> value != mob && value.isAlive() && !value.isRemoved());
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(mob.getRandom().nextInt(candidates.size())));
    }

    private static boolean blight(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, power * .75));
        long now = level.getGameTime();
        BLIGHT.put(target.getUUID(), new BlightState(level, caster.getUUID(), target.getUUID(), power,
                now + BLIGHT_TICKS, now + 20L, 4));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BLIGHT_TICKS, 2, true, false));
        return true;
    }

    private static void tickBlight(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, BlightState>> iterator = BLIGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            BlightState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            Entity targetRaw = level.getEntity(state.targetId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive()
                    || !(targetRaw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if (state.remainingPulses <= 0 || now < state.nextPulse) continue;
            ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .18));
            state.remainingPulses--;
            state.nextPulse = now + 20L;
        }
    }

    private static boolean freedom(ServerLevel level, LivingEntity caster) {
        FREEDOM.put(caster.getUUID(), new TimedState(level, level.getGameTime() + FREEDOM_TICKS));
        cleanseMovement(caster);
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, FREEDOM_TICKS, 1, true, false));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§a[이동의 자유] §f26초 · 둔화·속박·동결·강제부양을 계속 씻어내고 하위 이동 제어의 시전 봉쇄를 무시합니다."), 82);
        }
        return true;
    }

    private static void tickFreedom(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, TimedState>> iterator = FREEDOM.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TimedState> entry = iterator.next();
            TimedState state = entry.getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            cleanseMovement(target);
            if (target instanceof Mob mob && mob.getTarget() != null && mob.getNavigation().isDone()) {
                LivingEntity targetEntity = mob.getTarget();
                mob.getNavigation().moveTo(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), 1.15);
            }
        }
    }

    private static void cleanseMovement(LivingEntity target) {
        target.removeEffect(MobEffects.SLOWNESS);
        target.removeEffect(MobEffects.MINING_FATIGUE);
        target.removeEffect(MobEffects.LEVITATION);
        target.setTicksFrozen(0);
        target.fallDistance = 0.0F;
    }

    private static boolean phantasmalKiller(ServerLevel level, LivingEntity caster,
                                            LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, power * .55));
        long now = level.getGameTime();
        FEAR.put(target.getUUID(), new FearState(level, caster.getUUID(), target.getUUID(), power,
                now + PHANTASM_TICKS, now + 40L));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, PHANTASM_TICKS, 0, true, false));
        return true;
    }

    private static void tickFear(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, FearState>> iterator = FEAR.entrySet().iterator();
        while (iterator.hasNext()) {
            FearState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity ownerRaw = level.getEntity(state.ownerId);
            Entity targetRaw = level.getEntity(state.targetId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive()
                    || !(targetRaw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            Vec3 away = horizontal(target.position().subtract(owner.position()));
            if (target instanceof Mob mob) {
                Vec3 destination = target.position().add(away.scale(9.0));
                mob.setTarget(null);
                mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35);
            } else {
                target.push(away.x * .09, .01, away.z * .09);
            }
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 1, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 8, 0, true, false));
            if (now >= state.nextPulse) {
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .10));
                state.nextPulse = now + 40L;
            }
        }
    }

    private static void tickTimed(ServerLevel level, long now) {
        cleanupTimedMap(level, now, SPHERES);
        cleanupTimedMap(level, now, STONESKIN);
    }

    private static void cleanupTimedMap(ServerLevel level, long now, Map<UUID, TimedState> states) {
        Iterator<Map.Entry<UUID, TimedState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TimedState> entry = iterator.next();
            TimedState state = entry.getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved() || now >= state.expiresAt) {
                iterator.remove();
            }
        }
    }

    private static boolean physicalAttack(LivingIncomingDamageEvent event) {
        if (ArcaneDamage.isResolving() || event.getSource().is(DamageTypeTags.IS_FIRE)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return event.getSource().getEntity() instanceof LivingEntity;
    }

    private static LivingEntity hostileAttacker(LivingEntity target, LivingIncomingDamageEvent event) {
        Entity raw = event.getSource().getEntity();
        if (!(raw instanceof LivingEntity attacker) || attacker == target || !attacker.isAlive()) return null;
        return target.isAlliedTo(attacker) ? null : attacker;
    }

    private static Optional<BlockPos> findSafe(ServerLevel level, Vec3 desired, int verticalSearch) {
        Optional<BlockPos> direct = findSafeVertical(level, desired, verticalSearch);
        if (direct.isPresent()) return direct;
        int x = (int) Math.floor(desired.x), z = (int) Math.floor(desired.z);
        int y = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int radius = 1; radius <= 5; radius++) {
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

    private static Vec3 clampDestination(Vec3 start, Vec3 desired, double maxDistance) {
        Vec3 delta = desired.subtract(start);
        return delta.lengthSqr() <= maxDistance * maxDistance ? desired : start.add(delta.normalize().scale(maxDistance));
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static final class FireWall {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final Vec3 forward;
        final double halfWidth; final double power; final long expiresAt; long nextPulse;
        FireWall(ServerLevel level, UUID ownerId, Vec3 center, Vec3 forward, double halfWidth,
                 double power, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.forward = forward;
            this.halfWidth = halfWidth; this.power = power; this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }

    private static final class IceStorm {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final double radius; final double power;
        long nextPulse; int remaining;
        IceStorm(ServerLevel level, UUID ownerId, Vec3 center, double radius, double power, long nextPulse, int remaining) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.radius = radius;
            this.power = power; this.nextPulse = nextPulse; this.remaining = remaining;
        }
    }

    private record TimedState(ServerLevel level, long expiresAt) {}

    private static final class ConfusionState {
        final ServerLevel level; final UUID ownerId; final UUID targetId; final long expiresAt;
        long nextDecision; long castBlockedUntil;
        ConfusionState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt,
                       long nextDecision, long castBlockedUntil) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId; this.expiresAt = expiresAt;
            this.nextDecision = nextDecision; this.castBlockedUntil = castBlockedUntil;
        }
    }

    private static final class BlightState {
        final ServerLevel level; final UUID ownerId; final UUID targetId; final double power; final long expiresAt;
        long nextPulse; int remainingPulses;
        BlightState(ServerLevel level, UUID ownerId, UUID targetId, double power,
                    long expiresAt, long nextPulse, int remainingPulses) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId; this.power = power;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse; this.remainingPulses = remainingPulses;
        }
        boolean active() { return level.getGameTime() < expiresAt; }
    }

    private static final class FearState {
        final ServerLevel level; final UUID ownerId; final UUID targetId; final double power; final long expiresAt;
        long nextPulse;
        FearState(ServerLevel level, UUID ownerId, UUID targetId, double power, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId; this.power = power;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }
}
