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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Authoritative, spell-specific runtime for the ten direct 2nd-circle spells. */
public final class SecondCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "scorching_ray", "misty_step", "web", "mirror_image", "invisibility",
            "gust_of_wind", "hold_person", "shatter", "blur", "levitate");

    public static final int WEB_TICKS = 220;
    public static final int MIRROR_TICKS = 260;
    public static final int INVISIBILITY_TICKS = 420;
    public static final int HOLD_PERSON_TICKS = 180;
    public static final int BLUR_TICKS = 360;
    private static final int LEVITATE_RISE_TICKS = 60;
    private static final int LEVITATE_TOTAL_TICKS = 140;
    private static final int WEB_PULSE = 4;
    private static final int RAY_GAP = 10;

    private static final List<RaySalvo> SALVOS = new ArrayList<>();
    private static final List<WebZone> WEBS = new ArrayList<>();
    private static final Map<UUID, MirrorState> MIRRORS = new HashMap<>();
    private static final Map<UUID, TimedState> INVISIBILITY = new HashMap<>();
    private static final Map<UUID, TimedState> BLUR = new HashMap<>();
    private static final Map<UUID, HoldState> HOLDS = new HashMap<>();
    private static final Map<UUID, LevitateState> LEVITATION = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private SecondCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        return switch (spellId) {
            case "scorching_ray" -> scorchingRay((ServerLevel) caster.level(), caster,
                    snapshot.targetEntity(caster).orElse(null), power);
            case "misty_step" -> mistyStep(caster, range, snapshot);
            case "web" -> web((ServerLevel) caster.level(), caster, range, snapshot.target());
            case "mirror_image" -> mirrorImage(caster);
            case "invisibility" -> invisibility(caster);
            case "gust_of_wind" -> gustOfWind((ServerLevel) caster.level(), caster, range,
                    snapshot.launchOrigin(), snapshot.launchDirection());
            case "hold_person" -> holdPerson((ServerLevel) caster.level(), caster,
                    snapshot.targetEntity(caster).orElse(null));
            case "shatter" -> shatter((ServerLevel) caster.level(), caster, range, power, snapshot.target());
            case "blur" -> blur(caster);
            case "levitate" -> levitate((ServerLevel) caster.level(), caster,
                    snapshot.targetEntity(caster).orElse(caster));
            default -> false;
        };
    }

    /** NPC mages use the same second-circle identities instead of generic direct-damage aliases. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "scorching_ray" -> scorchingRay(level, caster, designatedTarget, power);
            case "misty_step" -> mistyStep(level, caster, designatedTarget, range);
            case "web" -> web(level, caster, range, snapshot.target());
            case "mirror_image" -> mirrorImage(caster);
            case "invisibility" -> invisibility(caster);
            case "gust_of_wind" -> gustOfWind(level, caster, range,
                    snapshot.launchOrigin(), snapshot.launchDirection());
            case "hold_person" -> holdPerson(level, caster, designatedTarget);
            case "shatter" -> shatter(level, caster, range, power, snapshot.target());
            case "blur" -> blur(caster);
            case "levitate" -> levitate(level, caster, designatedTarget);
            default -> false;
        };
    }

    /** Hold Person suppresses Arcane casting; the other second-circle roles do not. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        HoldState state = HOLDS.get(caster.getUUID());
        return state != null && state.level == caster.level() && state.active();
    }

    /** Illusions only intercept hostile attack trajectories, never fall/fire/drowning/void damage. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event == null || !(event.getEntity() instanceof LivingEntity target) || event.getAmount() <= 0.0F) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        LivingEntity attacker = hostileAttacker(target, event);
        if (attacker == null) return;
        long now = target.level() instanceof ServerLevel level ? level.getGameTime() : target.tickCount;
        UUID id = target.getUUID();

        MirrorState mirror = MIRRORS.get(id);
        if (mirror != null && mirror.expiresAt > now && mirror.charges > 0) {
            int remaining = mirror.charges - 1;
            if (remaining <= 0) {
                MIRRORS.remove(id);
                WorldMagicService.cancelRelease(target, "mirror_image");
            } else MIRRORS.put(id, new MirrorState(mirror.level, remaining, mirror.expiresAt));
            event.setCanceled(true);
            mirror.level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, .80F, 1.70F);
            if (target instanceof ServerPlayer player) {
                ArcaneNoticeService.push(player, Component.literal(
                        "§b[미러 이미지] §f직접 공격을 환영이 대신 받았습니다. §7남은 환영 " + remaining), 35);
            }
            return;
        }

        TimedState hidden = INVISIBILITY.get(id);
        if (hidden != null && hidden.level == target.level() && hidden.expiresAt > now) {
            INVISIBILITY.remove(id);
            target.removeEffect(MobEffects.INVISIBILITY);
            WorldMagicService.cancelRelease(target, "invisibility");
            event.setCanceled(true);
            hidden.level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, .72F, 1.36F);
            if (target instanceof ServerPlayer player) {
                ArcaneNoticeService.push(player, Component.literal(
                        "§7[투명화] §f첫 직접 공격 궤적을 흘리고 은신이 해제되었습니다."), 45);
            }
            return;
        }

        TimedState blur = BLUR.get(id);
        if (blur != null && blur.level == target.level() && blur.expiresAt > now
                && target.getRandom().nextFloat() < .35F) {
            event.setCanceled(true);
            blur.level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, .55F, 1.48F);
            if (target instanceof ServerPlayer player) {
                ArcaneNoticeService.push(player, Component.literal(
                        "§b[블러] §f흐릿한 윤곽 때문에 직접 공격이 빗나갔습니다."), 28);
            }
        }
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickSalvos(level, now);
        tickWebs(level, now);
        tickHold(level, now);
        tickLevitation(level, now);
        cleanupTimed(level, now);
    }

    public static void clear(LivingEntity subject) { if (subject != null) clear(subject.getUUID()); }

    public static void clear(UUID id) {
        if (id == null) return;
        SALVOS.removeIf(salvo -> salvo.ownerId.equals(id) || salvo.targetId.equals(id));
        WEBS.removeIf(zone -> zone.ownerId.equals(id));
        MIRRORS.remove(id);
        TimedState hidden = INVISIBILITY.remove(id);
        if (hidden != null) {
            Entity raw = hidden.level.getEntity(id);
            if (raw instanceof LivingEntity living) living.removeEffect(MobEffects.INVISIBILITY);
        }
        BLUR.remove(id);
        Iterator<Map.Entry<UUID, HoldState>> holds = HOLDS.entrySet().iterator();
        while (holds.hasNext()) {
            HoldState state = holds.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreHold(state);
            holds.remove();
        }
        Iterator<Map.Entry<UUID, LevitateState>> levitates = LEVITATION.entrySet().iterator();
        while (levitates.hasNext()) {
            LevitateState state = levitates.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            finishLevitation(state, false);
            levitates.remove();
        }
    }

    public static void clearAll() {
        for (HoldState state : HOLDS.values()) restoreHold(state);
        for (LevitateState state : LEVITATION.values()) finishLevitation(state, false);
        SALVOS.clear();
        WEBS.clear();
        MIRRORS.clear();
        for (Map.Entry<UUID, TimedState> entry : INVISIBILITY.entrySet()) {
            Entity raw = entry.getValue().level.getEntity(entry.getKey());
            if (raw instanceof LivingEntity living) living.removeEffect(MobEffects.INVISIBILITY);
        }
        INVISIBILITY.clear();
        BLUR.clear();
        HOLDS.clear();
        LEVITATION.clear();
        LAST_TICK.clear();
    }

    private static boolean scorchingRay(ServerLevel level, LivingEntity caster,
                                        LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        double perRay = Math.max(1.0, power * .42);
        if (!rayHit(level, caster, target, perRay)) return false;
        SALVOS.add(new RaySalvo(level, caster.getUUID(), target.getUUID(), perRay,
                level.getGameTime() + RAY_GAP, 2));
        return true;
    }

    private static boolean rayHit(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        boolean hit = ArcaneDamage.hurt(level, caster, target, (float) power);
        if (hit) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT,
                    caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .52F, 1.35F);
        }
        return hit;
    }

    private static void tickSalvos(ServerLevel level, long now) {
        Iterator<RaySalvo> iterator = SALVOS.iterator();
        while (iterator.hasNext()) {
            RaySalvo salvo = iterator.next();
            if (salvo.level != level) continue;
            Entity rawOwner = level.getEntity(salvo.ownerId);
            Entity rawTarget = level.getEntity(salvo.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || !enemy(owner, target)) {
                iterator.remove();
                continue;
            }
            if (now < salvo.nextTick) continue;
            rayHit(level, owner, target, salvo.power);
            salvo.remaining--;
            if (salvo.remaining <= 0) iterator.remove();
            else salvo.nextTick = now + RAY_GAP;
        }
    }

    private static boolean mistyStep(ServerPlayer caster, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) caster.level();
        double maxDistance = Math.max(5.0, Math.min(12.0, range * .45));
        Vec3 desired = clampDestination(caster.position(), snapshot.target(), maxDistance);
        Optional<BlockPos> safe = findSafe(level, desired, 6);
        if (safe.isEmpty()) {
            ArcaneNoticeService.push(caster, Component.literal(
                    "§c[미스티 스텝] §f가까운 안전 착지점을 찾지 못했습니다."), 45);
            return false;
        }
        BlockPos p = safe.get();
        caster.stopRiding();
        boolean moved = caster.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5,
                Set.<Relative>of(), caster.getYRot(), caster.getXRot(), true);
        if (moved) {
            caster.fallDistance = 0.0F;
            level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .75F, 1.48F);
        }
        return moved;
    }

    private static boolean mistyStep(ServerLevel level, Mob caster, LivingEntity target, double range) {
        if (target == null || !target.isAlive()) return false;
        Vec3 toward = target.position().subtract(caster.position());
        if (toward.lengthSqr() < 1.0E-6) return false;
        double maxDistance = Math.max(4.0, Math.min(10.0, range * .42));
        double distance = Math.min(maxDistance, Math.max(3.0, toward.length() - 4.0));
        Vec3 desired = caster.position().add(toward.normalize().scale(distance));
        Optional<BlockPos> safe = findSafe(level, desired, 6);
        if (safe.isEmpty()) return false;
        BlockPos p = safe.get();
        caster.getNavigation().stop();
        caster.snapTo(p.getX() + .5, p.getY(), p.getZ() + .5, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .72F, 1.42F);
        return true;
    }

    private static boolean web(ServerLevel level, LivingEntity caster, double range, Vec3 center) {
        double radius = Math.max(4.2, Math.min(7.5, SpellMetrics.effectRadius("web", range, 2)));
        WEBS.removeIf(zone -> zone.ownerId.equals(caster.getUUID()));
        WEBS.add(new WebZone(level, caster.getUUID(), center, radius,
                level.getGameTime() + WEB_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§f[웹] §f11초 동안 반경 "
                    + one(radius) + "의 이동 억제장을 유지합니다."), 55);
        }
        return true;
    }

    private static void tickWebs(ServerLevel level, long now) {
        Iterator<WebZone> iterator = WEBS.iterator();
        while (iterator.hasNext()) {
            WebZone zone = iterator.next();
            if (zone.level != level) continue;
            Entity rawOwner = level.getEntity(zone.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= zone.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < zone.nextPulse) continue;
            zone.nextPulse = now + WEB_PULSE;
            AABB box = new AABB(zone.center, zone.center).inflate(zone.radius,
                    Math.max(4.0, zone.radius * .7), zone.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value)
                            && zone.center.distanceToSqr(value.position()) <= zone.radius * zone.radius)) {
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * .18, Math.min(motion.y, .10), motion.z * .18);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 4, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 1, true, false));
            }
        }
    }

    private static boolean mirrorImage(LivingEntity caster) {
        ServerLevel level = (ServerLevel) caster.level();
        MIRRORS.put(caster.getUUID(), new MirrorState(level, 3, level.getGameTime() + MIRROR_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§b[미러 이미지] §f13초 내 직접 공격 3회를 환영이 대신 받습니다. §7환경 피해에는 반응하지 않습니다."), 65);
        }
        return true;
    }

    private static boolean invisibility(LivingEntity caster) {
        ServerLevel level = (ServerLevel) caster.level();
        long expires = level.getGameTime() + INVISIBILITY_TICKS;
        INVISIBILITY.put(caster.getUUID(), new TimedState(level, expires));
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, true, true));
        clearAggro(level, caster, 48.0);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§7[투명화] §f적대 추적을 끊었습니다. §7첫 직접 공격 궤적 1회를 회피하면 은신이 해제됩니다."), 70);
        }
        return true;
    }

    private static void clearAggro(ServerLevel level, LivingEntity hidden, double radius) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, hidden.getBoundingBox().inflate(radius),
                value -> value.isAlive() && value.getTarget() == hidden)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static boolean gustOfWind(ServerLevel level, LivingEntity caster, double range,
                                      Vec3 origin, Vec3 launchDirection) {
        Vec3 direction = horizontal(launchDirection);
        double length = Math.max(8.0, Math.min(18.0, range * .65));
        AABB box = new AABB(origin, origin.add(direction.scale(length))).inflate(2.2, 3.0, 2.2);
        boolean moved = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            Vec3 relative = target.position().add(0, target.getBbHeight() * .45, 0).subtract(origin);
            double projection = relative.dot(direction);
            if (projection < 0.0 || projection > length) continue;
            double width = 1.25 + target.getBbWidth() * .55;
            if (relative.subtract(direction.scale(projection)).lengthSqr() > width * width) continue;
            target.push(direction.x * 1.75, .22, direction.z * 1.75);
            moved = true;
        }
        boolean terrain = stripFragileWindBlocks(level, origin, direction, length);
        level.playSound(null, BlockPos.containing(origin.add(direction.scale(length * .55))),
                SoundEvents.ENDER_DRAGON_FLAP, caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                .70F, 1.35F);
        return moved || terrain || length > 0.0;
    }

    private static boolean stripFragileWindBlocks(ServerLevel level, Vec3 origin, Vec3 direction, double length) {
        boolean changed = false;
        int steps = Math.max(1, (int) Math.ceil(length * 2.0));
        for (int step = 1; step <= steps; step++) {
            BlockPos center = BlockPos.containing(origin.add(direction.scale(step * .5)));
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, dy, dz);
                BlockState state = level.getBlockState(pos);
                if (!windFragile(state)) continue;
                boolean flame = state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
                if (flame) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                else level.destroyBlock(pos, true);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean windFragile(BlockState state) {
        return state.is(Blocks.COBWEB) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    private static boolean holdPerson(ServerLevel level, LivingEntity caster, LivingEntity target) {
        if (!enemy(caster, target) || !holdEligible(target)) {
            if (caster instanceof ServerPlayer player) {
                ArcaneNoticeService.push(player, Component.literal(
                        "§c[홀드 퍼슨] §f보스급·대형 생명체는 이 2써클 속박으로 고정할 수 없습니다."), 55);
            }
            return false;
        }
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        HOLDS.put(target.getUUID(), new HoldState(level, caster.getUUID(), target.getUUID(), oldTarget,
                level.getGameTime() + HOLD_PERSON_TICKS));
        enforceHold(target);
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[홀드 퍼슨] §f" + target.getName().getString()
                    + "을 9초간 속박했습니다. §7이동·공격·Arcane 시전이 멈춥니다."), 65);
        }
        return true;
    }

    private static boolean holdEligible(LivingEntity target) {
        return target.getBbWidth() <= 1.60F && target.getBbHeight() <= 3.20F && target.getMaxHealth() <= 80.0F;
    }

    private static void enforceHold(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 255, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 5, true, false));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            WorldMagicService.stop(mob);
        } else if (target instanceof ServerPlayer player) {
            if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
            if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
            SpellKineticsService.cancel(player);
        }
    }

    private static void tickHold(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, HoldState>> iterator = HOLDS.entrySet().iterator();
        while (iterator.hasNext()) {
            HoldState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || now >= state.expiresAt) {
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

    private static boolean shatter(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(4.0, Math.min(6.5, SpellMetrics.effectRadius("shatter", range, 2)));
        boolean hit = false;
        AABB box = new AABB(center, center).inflate(radius, Math.max(4.0, radius * .72), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            if (ArcaneDamage.hurt(level, caster, target, (float) power)) hit = true;
        }
        boolean terrain = shatterBrittle(level, center, radius);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.0F, 1.28F);
        return hit || terrain || center != null;
    }

    private static boolean shatterBrittle(ServerLevel level, Vec3 center, double radius) {
        int r = Math.max(2, Math.min(6, (int) Math.ceil(radius)));
        BlockPos origin = BlockPos.containing(center);
        boolean changed = false;
        double r2 = radius * radius;
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            BlockPos pos = origin.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            if (!shatterBrittle(state)) continue;
            level.destroyBlock(pos, Math.floorMod(pos.hashCode(), 4) == 0);
            changed = true;
        }
        return changed;
    }

    private static boolean shatterBrittle(BlockState state) {
        return state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.is(Blocks.TINTED_GLASS)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.AMETHYST_BLOCK);
    }

    private static boolean blur(LivingEntity caster) {
        ServerLevel level = (ServerLevel) caster.level();
        BLUR.put(caster.getUUID(), new TimedState(level, level.getGameTime() + BLUR_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§b[블러] §f18초 동안 직접 적대 공격이 35% 확률로 빗나갑니다. §7환경 피해는 그대로 받습니다."), 65);
        }
        return true;
    }

    private static boolean levitate(ServerLevel level, LivingEntity caster, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) return false;
        long now = level.getGameTime();
        LEVITATION.put(target.getUUID(), new LevitateState(level, caster.getUUID(), target.getUUID(),
                now + LEVITATE_RISE_TICKS, now + LEVITATE_TOTAL_TICKS));
        target.fallDistance = 0.0F;
        return true;
    }

    private static void tickLevitation(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, LevitateState>> iterator = LEVITATION.entrySet().iterator();
        while (iterator.hasNext()) {
            LevitateState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawTarget = level.getEntity(state.targetId);
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()
                    || !(rawOwner instanceof LivingEntity owner) || !owner.isAlive()) {
                finishLevitation(state, false);
                iterator.remove();
                continue;
            }
            target.fallDistance = 0.0F;
            if (now < state.riseUntil) {
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * .72, Math.max(.24, motion.y), motion.z * .72);
            } else if (now < state.expiresAt) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, true, false));
            } else {
                finishLevitation(state, true);
                iterator.remove();
            }
        }
    }

    private static void finishLevitation(LevitateState state, boolean grantDescent) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) return;
        target.fallDistance = 0.0F;
        if (grantDescent) target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
    }

    private static void cleanupTimed(ServerLevel level, long now) {
        MIRRORS.entrySet().removeIf(entry -> entry.getValue().level == level
                && (entry.getValue().expiresAt <= now || entry.getValue().charges <= 0));
        Iterator<Map.Entry<UUID, TimedState>> hidden = INVISIBILITY.entrySet().iterator();
        while (hidden.hasNext()) {
            Map.Entry<UUID, TimedState> entry = hidden.next();
            TimedState state = entry.getValue();
            if (state.level != level || state.expiresAt > now) continue;
            Entity raw = level.getEntity(entry.getKey());
            if (raw instanceof LivingEntity living) living.removeEffect(MobEffects.INVISIBILITY);
            hidden.remove();
        }
        BLUR.entrySet().removeIf(entry -> entry.getValue().level == level && entry.getValue().expiresAt <= now);
    }

    private static LivingEntity hostileAttacker(LivingEntity target, LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (!(source instanceof LivingEntity attacker) || attacker == target || !attacker.isAlive()) return null;
        if (direct == null || target.isAlliedTo(attacker)) return null;
        return attacker;
    }

    private static boolean enemy(LivingEntity caster, LivingEntity target) {
        return target != null && target != caster && target.isAlive() && !target.isRemoved() && !caster.isAlliedTo(target);
    }

    private static Vec3 clampDestination(Vec3 start, Vec3 desired, double maxDistance) {
        Vec3 delta = desired.subtract(start);
        return delta.lengthSqr() <= maxDistance * maxDistance ? desired
                : start.add(delta.normalize().scale(maxDistance));
    }

    private static Optional<BlockPos> findSafe(ServerLevel level, Vec3 desired, int verticalSearch) {
        Optional<BlockPos> direct = findSafeVertical(level, desired, verticalSearch);
        if (direct.isPresent()) return direct;
        int x = (int) Math.floor(desired.x), z = (int) Math.floor(desired.z);
        int y = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int radius = 1; radius <= 4; radius++) {
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
                        && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                    return Optional.of(feet);
                }
            }
        }
        return Optional.empty();
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static final class RaySalvo {
        private final ServerLevel level;
        private final UUID ownerId;
        private final UUID targetId;
        private final double power;
        private long nextTick;
        private int remaining;
        private RaySalvo(ServerLevel level, UUID ownerId, UUID targetId, double power, long nextTick, int remaining) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId; this.power = power;
            this.nextTick = nextTick; this.remaining = remaining;
        }
    }

    private static final class WebZone {
        private final ServerLevel level;
        private final UUID ownerId;
        private final Vec3 center;
        private final double radius;
        private final long expiresAt;
        private long nextPulse;
        private WebZone(ServerLevel level, UUID ownerId, Vec3 center, double radius, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.radius = radius;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }

    private static final class MirrorState {
        private final ServerLevel level;
        private final int charges;
        private final long expiresAt;
        private MirrorState(ServerLevel level, int charges, long expiresAt) {
            this.level = level; this.charges = charges; this.expiresAt = expiresAt;
        }
    }

    private static final class TimedState {
        private final ServerLevel level;
        private final long expiresAt;
        private TimedState(ServerLevel level, long expiresAt) { this.level = level; this.expiresAt = expiresAt; }
    }

    private static final class HoldState {
        private final ServerLevel level;
        private final UUID ownerId;
        private final UUID targetId;
        private final UUID oldTargetId;
        private final long expiresAt;
        private HoldState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId, long expiresAt) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId;
            this.oldTargetId = oldTargetId; this.expiresAt = expiresAt;
        }
        private boolean active() { return level.getGameTime() < expiresAt; }
    }

    private static final class LevitateState {
        private final ServerLevel level;
        private final UUID ownerId;
        private final UUID targetId;
        private final long riseUntil;
        private final long expiresAt;
        private LevitateState(ServerLevel level, UUID ownerId, UUID targetId, long riseUntil, long expiresAt) {
            this.level = level; this.ownerId = ownerId; this.targetId = targetId;
            this.riseUntil = riseUntil; this.expiresAt = expiresAt;
        }
    }
}
