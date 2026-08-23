package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Authoritative deep runtime for the ten direct 1st-circle spells.
 *
 * The first circle is intentionally simple, not fake: projectiles obey the captured release point,
 * Magic Missile keeps its lock-on identity, Sleep only incapacitates ordinary-weight targets and
 * wakes on damage, Grease is a real maintained slipping field, while Shield/Mage Armor delegate to
 * the charge/plate runtime used by incoming-damage resolution.
 */
public final class FirstCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "magic_missile", "fire_bolt", "ray_of_frost", "shield", "feather_fall",
            "light", "grease", "sleep", "thunderwave", "mage_armor");
    public static final int SLEEP_TICKS = 140;
    public static final int GREASE_TICKS = 160;
    public static final int SHIELD_TICKS = 170;
    public static final int MAGE_ARMOR_TICKS = 720;
    private static final int MAGE_ARMOR_RECHARGE_TICKS = 90;
    private static final int GREASE_PULSE = 4;

    private static final List<GreaseZone> GREASE = new ArrayList<>();
    private static final Map<UUID, SleepState> SLEEP = new HashMap<>();
    private static final Map<UUID, NpcWardState> NPC_SHIELD = new HashMap<>();
    private static final Map<UUID, NpcWardState> NPC_MAGE_ARMOR = new HashMap<>();
    private static final Map<UUID, NpcTimedState> NPC_FEATHER_FALL = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private FirstCircleSpellService() {}

    public static boolean handles(String spellId) {
        return HANDLED.contains(spellId);
    }

    public static boolean execute(ServerPlayer player, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.validFor(player)) return false;
        return switch (spellId) {
            case "magic_missile" -> magicMissile(player, power, snapshot);
            case "fire_bolt" -> fireBolt(player, power, snapshot);
            case "ray_of_frost" -> rayOfFrost(player, power, snapshot);
            case "shield" -> ArcaneBuffRuntime.apply(player, "shield", power, range);
            case "feather_fall" -> featherFall(player);
            case "light" -> light(player);
            case "grease" -> grease(player, range, snapshot);
            case "sleep" -> sleep(player, range, power, snapshot);
            case "thunderwave" -> thunderwave(player, range, power, snapshot);
            case "mage_armor" -> ArcaneBuffRuntime.apply(player, "mage_armor", power, range);
            default -> false;
        };
    }

    /** NPC mages use the same first-circle identities instead of generic direct-damage aliases. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "magic_missile" -> damage(level, caster, designatedTarget, power);
            case "fire_bolt" -> {
                LivingEntity target = impactTarget(level, caster, snapshot.target(), 1.35);
                if (target == null) yield false;
                boolean hit = damage(level, caster, target, power);
                if (hit) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 120));
                yield hit;
            }
            case "ray_of_frost" -> {
                LivingEntity target = designatedTarget;
                if (!enemy(caster, target)) yield false;
                boolean hit = damage(level, caster, target, power);
                if (hit) frost(target, 100, 120);
                yield hit;
            }
            case "shield" -> npcShield(level, caster, power);
            case "feather_fall" -> {
                caster.fallDistance = 0.0F;
                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));
                NPC_FEATHER_FALL.put(caster.getUUID(), new NpcTimedState(level, caster.getUUID(), level.getGameTime() + 120));
                yield true;
            }
            case "light" -> {
                caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0, true, false));
                ArcaneLightService.illuminate(caster, 1800);
                yield true;
            }
            case "grease" -> {
                addGrease(level, caster.getUUID(), snapshot.target(), SpellMetrics.effectRadius("grease", range, 1));
                yield true;
            }
            case "sleep" -> sleepNpc(level, caster, range, power, snapshot.target());
            case "thunderwave" -> thunderwave(level, caster, range, power, snapshot);
            case "mage_armor" -> npcMageArmor(level, caster, power);
            default -> false;
        };
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickGrease(level, now);
        tickSleep(level, now);
        tickNpcWards(level, now);
        tickNpcFeatherFall(level, now);
        ArcaneLightService.tickNpc(level);
    }

    /** Resolve 1C NPC wards late, then wake Sleep only when damage still survives interception. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event == null || event.getAmount() <= 0.0F || event.isCanceled()) return;
        LivingEntity target = event.getEntity();
        if (!(target instanceof ServerPlayer) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            resolveNpcWard(target, event);
            if (event.isCanceled() || event.getAmount() <= 0.0F) return;
        }
        SleepState state = SLEEP.remove(target.getUUID());
        if (state != null) {
            restoreSleep(state);
            cancelSleepReleaseIfIdle(state.level, state.ownerId);
        }
    }

    /** Third-circle Dispel clears every maintained 1C authority owned by or attached to the subject. */
    public static boolean dispel(LivingEntity subject) {
        return clearMaintained(subject);
    }

    public static void clear(LivingEntity subject) {
        clearMaintained(subject);
    }

    private static boolean clearMaintained(LivingEntity subject) {
        if (subject == null) return false;
        UUID id = subject.getUUID();
        boolean changed = false;
        Set<String> cancel = new java.util.HashSet<>();

        if (GREASE.removeIf(zone -> zone.ownerId.equals(id))) { changed = true; cancel.add("grease"); }

        Iterator<Map.Entry<UUID, SleepState>> ownedSleep = SLEEP.entrySet().iterator();
        while (ownedSleep.hasNext()) {
            SleepState state = ownedSleep.next().getValue();
            if (!state.ownerId.equals(id)) continue;
            restoreSleep(state);
            ownedSleep.remove();
            changed = true;
            cancel.add("sleep");
        }
        SleepState received = SLEEP.remove(id);
        if (received != null) {
            restoreSleep(received);
            cancelSleepReleaseIfIdle(received.level, received.ownerId);
            changed = true;
        }

        if (NPC_SHIELD.remove(id) != null) { changed = true; cancel.add("shield"); }
        if (NPC_MAGE_ARMOR.remove(id) != null) { changed = true; cancel.add("mage_armor"); }
        if (NPC_FEATHER_FALL.remove(id) != null) {
            subject.removeEffect(MobEffects.SLOW_FALLING);
            changed = true;
            cancel.add("feather_fall");
        }

        if (ArcaneBuffRuntime.clearSpell(subject, "shield")) changed = true;
        if (ArcaneBuffRuntime.clearSpell(subject, "mage_armor")) changed = true;
        if (subject instanceof ServerPlayer player && MageGearService.clearStableDescent(id)) {
            player.removeEffect(MobEffects.SLOW_FALLING);
            WorldMagicService.cancelRelease(player, "feather_fall");
            changed = true;
        }
        if (ArcaneLightService.clear(subject)) { cancel.add("light"); changed = true; }

        for (String spellId : cancel) WorldMagicService.cancelRelease(subject, spellId);
        return changed;
    }

    public static void clear(UUID ownerId) {
        if (ownerId == null) return;
        GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));
        NPC_SHIELD.remove(ownerId);
        NPC_MAGE_ARMOR.remove(ownerId);
        NPC_FEATHER_FALL.remove(ownerId);
        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();
        while (iterator.hasNext()) {
            SleepState state = iterator.next().getValue();
            if (!state.ownerId.equals(ownerId) && !state.target.getUUID().equals(ownerId)) continue;
            restoreSleep(state);
            iterator.remove();
        }
    }

    public static void clearAll() {
        for (SleepState state : SLEEP.values()) restoreSleep(state);
        SLEEP.clear();
        GREASE.clear();
        NPC_SHIELD.clear();
        NPC_MAGE_ARMOR.clear();
        NPC_FEATHER_FALL.clear();
        LAST_TICK.clear();
    }

    private static boolean magicMissile(ServerPlayer player, double power, CastTargetSnapshot snapshot) {
        LivingEntity target = snapshot.targetEntity(player).orElse(null);
        if (!enemy(player, target)) return false;
        return ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) power);
    }

    private static boolean fireBolt(ServerPlayer player, double power, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = impactTarget(level, player, snapshot.target(), 1.35);
        if (target == null) return false;
        boolean hit = ArcaneDamage.hurt(level, player, target, (float) power);
        if (hit) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 120));
        return hit;
    }

    private static boolean rayOfFrost(ServerPlayer player, double power, CastTargetSnapshot snapshot) {
        LivingEntity target = snapshot.targetEntity(player).orElse(null);
        if (!enemy(player, target)) return false;
        boolean hit = ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) power);
        if (hit) frost(target, 100, 120);
        return hit;
    }

    private static boolean featherFall(ServerPlayer player) {
        player.fallDistance = 0.0F;
        MageGearService.grantStableDescent(player, 120);
        return true;
    }

    private static boolean light(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0, true, false));
        ArcaneLightService.illuminate(player, 1800);
        return true;
    }

    public static double greaseRadius(double range) {
        return Math.max(2.5, Math.min(8.0, SpellMetrics.effectRadius("grease", range, 1)));
    }

    public static double sleepRadius(double range) {
        return Math.max(3.5, SpellMetrics.effectRadius("sleep", range, 1));
    }

    private static boolean grease(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        addGrease((ServerLevel) player.level(), player.getUUID(), snapshot.target(), greaseRadius(range));
        return true;
    }

    private static boolean sleep(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) {
        return sleepArea((ServerLevel) player.level(), player, range, power, snapshot.target());
    }

    private static boolean sleepNpc(ServerLevel level, Mob caster, double range, double power, Vec3 center) {
        return sleepArea(level, caster, range, power, center);
    }

    private static boolean sleepArea(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = sleepRadius(range);
        int affected = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius, Math.max(3.5, radius * .65), radius), value -> enemy(caster, value))) {
            double allowed = radius + target.getBbWidth() * .5;
            double dx = target.getX() - center.x, dz = target.getZ() - center.z;
            if (dx * dx + dz * dz > allowed * allowed || !sleepEligible(target, power)) continue;
            putToSleep(level, caster.getUUID(), target, power);
            affected++;
        }
        return affected > 0;
    }

    private static boolean thunderwave(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) {
        boolean hit = thunderwave((ServerLevel) player.level(), player, range, power, snapshot);
        DestructiveMagicService.applyPhysicalAftermath(player, "thunderwave", snapshot, range, power);
        return hit || snapshot.validFor(player);
    }

    private static boolean thunderwave(ServerLevel level, LivingEntity caster, double range, double power,
                                       CastTargetSnapshot snapshot) {
        Vec3 origin = snapshot.launchOrigin();
        Vec3 direction = horizontal(snapshot.launchDirection());
        double length = SpellMetrics.waveLength(range);
        double endRadius = SpellMetrics.waveEndRadius("thunderwave", range, 1);
        AABB box = new AABB(origin, origin.add(direction.scale(length))).inflate(endRadius + 1.5, 4.0, endRadius + 1.5);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            if (!insideWave(origin, direction, target, length, endRadius)) continue;
            if (damage(level, caster, target, power)) hit = true;
            Vec3 away = horizontal(target.position().subtract(origin));
            target.push(away.x * 1.55, .34, away.z * 1.55);
        }
        return hit;
    }

    private static boolean npcShield(ServerLevel level, Mob caster, double power) {
        NPC_SHIELD.put(caster.getUUID(), new NpcWardState(level, caster.getUUID(), level.getGameTime() + SHIELD_TICKS, power, 2, 0L));
        return true;
    }

    private static boolean npcMageArmor(ServerLevel level, Mob caster, double power) {
        long now = level.getGameTime();
        NPC_MAGE_ARMOR.put(caster.getUUID(), new NpcWardState(level, caster.getUUID(), now + MAGE_ARMOR_TICKS, power, 4, now + MAGE_ARMOR_RECHARGE_TICKS));
        return true;
    }

    private static void resolveNpcWard(LivingEntity target, LivingIncomingDamageEvent event) {
        if (!(target.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        float amount = event.getAmount();
        NpcWardState shield = NPC_SHIELD.get(target.getUUID());
        if (shield != null && shield.level == level && shield.expiresAt > now && shield.charges > 0) {
            shield.charges--;
            amount -= (float) Math.max(3.0, 2.0 + shield.power * .18);
            if (shield.charges <= 0) {
                NPC_SHIELD.remove(target.getUUID());
                WorldMagicService.cancelRelease(target, "shield");
            }
        }
        NpcWardState armor = NPC_MAGE_ARMOR.get(target.getUUID());
        if (armor != null && armor.level == level && armor.expiresAt > now) {
            amount = (float) Math.max(0.0, amount * (armor.charges > 0 ? .68 : .84) - .8);
            if (armor.charges > 0) {
                armor.charges--;
                if (armor.nextChargeAt <= now) armor.nextChargeAt = now + MAGE_ARMOR_RECHARGE_TICKS;
            }
        }
        if (amount <= .05F) event.setCanceled(true); else event.setAmount(amount);
    }

    private static void tickNpcWards(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcWardState>> shields = NPC_SHIELD.entrySet().iterator();
        while (shields.hasNext()) {
            NpcWardState state = shields.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.entityId);
            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {
                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "shield");
                shields.remove();
            }
        }
        Iterator<Map.Entry<UUID, NpcWardState>> armor = NPC_MAGE_ARMOR.entrySet().iterator();
        while (armor.hasNext()) {
            NpcWardState state = armor.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.entityId);
            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {
                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "mage_armor");
                armor.remove();
                continue;
            }
            if (state.charges < 4 && now >= state.nextChargeAt) {
                state.charges++;
                state.nextChargeAt = now + MAGE_ARMOR_RECHARGE_TICKS;
            }
        }
    }

    private static void tickNpcFeatherFall(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcTimedState>> iterator = NPC_FEATHER_FALL.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcTimedState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.entityId);
            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {
                iterator.remove();
            }
        }
    }

    private static void cancelSleepReleaseIfIdle(ServerLevel level, UUID ownerId) {
        if (SLEEP.values().stream().anyMatch(state -> state.ownerId.equals(ownerId))) return;
        Entity raw = level.getEntity(ownerId);
        if (raw instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "sleep");
    }

    private static void frost(LivingEntity target, int slowTicks, int freezeBonus) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowTicks, 2, true, false));
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + freezeBonus));
    }

    private static boolean damage(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        return enemy(caster, target) && ArcaneDamage.hurt(level, caster, target, (float) power);
    }

    private static LivingEntity impactTarget(ServerLevel level, LivingEntity caster, Vec3 impact, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius), value -> enemy(caster, value)).stream()
                .min(java.util.Comparator.comparingDouble(value -> value.getEyePosition().distanceToSqr(impact))).orElse(null);
    }

    private static void addGrease(ServerLevel level, UUID ownerId, Vec3 center, double radius) {
        long now = level.getGameTime();
        GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));
        GREASE.add(new GreaseZone(level, ownerId, center, Math.max(2.5, Math.min(8.0, radius)), now + GREASE_TICKS, now));
    }

    private static void tickGrease(ServerLevel level, long now) {
        Iterator<GreaseZone> iterator = GREASE.iterator();
        while (iterator.hasNext()) {
            GreaseZone zone = iterator.next();
            if (zone.level != level) continue;
            Entity ownerRaw = level.getEntity(zone.ownerId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive() || now >= zone.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < zone.nextPulse) continue;
            zone.nextPulse = now + GREASE_PULSE;
            AABB box = new AABB(zone.center, zone.center).inflate(zone.radius, 3.2, zone.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) {
                double allowed = zone.radius + target.getBbWidth() * .5;
                double dx = target.getX() - zone.center.x, dz = target.getZ() - zone.center.z;
                if (dx * dx + dz * dz > allowed * allowed) continue;
                double angle = Math.toRadians(Math.floorMod(target.getUUID().hashCode() + (int) now * 23, 360));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 1, true, false));
                target.push(Math.cos(angle) * .10, 0.0, Math.sin(angle) * .10);
            }
        }
    }

    private static boolean sleepEligible(LivingEntity target, double power) {
        double maxHealth = Math.min(40.0, Math.max(24.0, 20.0 + Math.max(0.0, power) * 2.0));
        return target.getMaxHealth() <= maxHealth;
    }

    private static void putToSleep(ServerLevel level, UUID ownerId, LivingEntity target, double power) {
        long now = level.getGameTime();
        SleepState previous = SLEEP.get(target.getUUID());
        boolean wasNoAi = previous != null ? previous.wasNoAi : target instanceof Mob mob && mob.isNoAi();
        long expires = Math.max(previous == null ? 0L : previous.expiresAt, now + SLEEP_TICKS);
        SleepState state = new SleepState(level, ownerId, target, wasNoAi, expires,
                Math.min(40.0, Math.max(24.0, 20.0 + Math.max(0.0, power) * 2.0)));
        SLEEP.put(target.getUUID(), state);
        enforceSleep(target);
        if (previous != null && !previous.ownerId.equals(ownerId))
            cancelSleepReleaseIfIdle(previous.level, previous.ownerId);
    }

    private static void tickSleep(ServerLevel level, long now) {
        Set<UUID> finishedOwners = new java.util.HashSet<>();
        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();
        while (iterator.hasNext()) {
            SleepState state = iterator.next().getValue();
            if (state.level != level) continue;
            LivingEntity target = state.target;
            Entity ownerRaw = level.getEntity(state.ownerId);
            if (!(ownerRaw instanceof LivingEntity owner) || !owner.isAlive()
                    || !target.isAlive() || target.isRemoved() || target.level() != level || now >= state.expiresAt) {
                restoreSleep(state);
                iterator.remove();
                finishedOwners.add(state.ownerId);
                continue;
            }
            enforceSleep(target);
        }
        for (UUID ownerId : finishedOwners) cancelSleepReleaseIfIdle(level, ownerId);
    }

    private static void enforceSleep(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 255, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 5, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 8, 0, true, false));
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            WorldMagicService.stop(mob);
        } else if (target instanceof ServerPlayer player) {
            if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
            if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
            SpellKineticsService.cancel(player);
        }
    }

    private static void restoreSleep(SleepState state) {
        LivingEntity target = state.target;
        if (target instanceof Mob mob) mob.setNoAi(state.wasNoAi);
    }

    private static boolean insideWave(Vec3 origin, Vec3 direction, LivingEntity target, double length, double endRadius) {
        Vec3 point = target.position().add(0.0, target.getBbHeight() * .45, 0.0);
        Vec3 relative = point.subtract(origin);
        double projection = relative.x * direction.x + relative.z * direction.z;
        if (projection < 0.0 || projection > length || Math.abs(relative.y) > 4.0) return false;
        double lateralX = relative.x - direction.x * projection;
        double lateralZ = relative.z - direction.z * projection;
        double allowed = Math.max(.85, endRadius * projection / Math.max(1.0, length)) + target.getBbWidth() * .5;
        return lateralX * lateralX + lateralZ * lateralZ <= allowed * allowed;
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static final class NpcWardState {
        final ServerLevel level; final UUID entityId; final long expiresAt; final double power; int charges; long nextChargeAt;
        NpcWardState(ServerLevel level, UUID entityId, long expiresAt, double power, int charges, long nextChargeAt) {
            this.level = level; this.entityId = entityId; this.expiresAt = expiresAt; this.power = power;
            this.charges = charges; this.nextChargeAt = nextChargeAt;
        }
    }

    private static final class NpcTimedState {
        final ServerLevel level; final UUID entityId; final long expiresAt;
        NpcTimedState(ServerLevel level, UUID entityId, long expiresAt) {
            this.level = level; this.entityId = entityId; this.expiresAt = expiresAt;
        }
    }

    private static final class GreaseZone {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final double radius; final long expiresAt; long nextPulse;
        GreaseZone(ServerLevel level, UUID ownerId, Vec3 center, double radius, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.radius = radius;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }

    private static final class SleepState {
        final ServerLevel level; final UUID ownerId; final LivingEntity target; final boolean wasNoAi; final long expiresAt; final double healthLimit;
        SleepState(ServerLevel level, UUID ownerId, LivingEntity target, boolean wasNoAi, long expiresAt, double healthLimit) {
            this.level = level; this.ownerId = ownerId; this.target = target; this.wasNoAi = wasNoAi;
            this.expiresAt = expiresAt; this.healthLimit = healthLimit;
        }
    }
}
