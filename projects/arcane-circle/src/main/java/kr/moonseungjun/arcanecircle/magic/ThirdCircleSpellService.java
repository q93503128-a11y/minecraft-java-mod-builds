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

/**
 * Server-authoritative deep runtime for the ten direct 3rd-circle spells.
 * Fireball is a falloff blast, Lightning Bolt a penetrating line, Fly real temporary flight,
 * Haste changes Arcane tempo, Dispel removes maintained magic, Vampiric Touch heals from damage
 * actually dealt, Slow/Sleet are distinct fields, Energy Protection only reacts to energy-like
 * damage, and Blink is a longer endpoint-safe spatial jump.
 */
public final class ThirdCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "fireball", "lightning_bolt", "fly", "haste", "dispel_magic",
            "vampiric_touch", "slow", "protection_from_energy", "sleet_storm", "blink");
    public static final int FLY_TICKS = 600;
    public static final int HASTE_TICKS = 600;
    public static final int SLOW_TICKS = 180;
    public static final int ENERGY_TICKS = 600;
    public static final int SLEET_TICKS = 180;
    private static final int ENERGY_MAX_CHARGES = 5;
    private static final int ENERGY_RECHARGE_TICKS = 70;
    private static final int SLOW_PULSE = 4;
    private static final int SLEET_PULSE = 10;

    private static final Map<UUID, PlayerFlight> PLAYER_FLIGHT = new HashMap<>();
    private static final Map<UUID, MobFlight> MOB_FLIGHT = new HashMap<>();
    private static final Map<UUID, EnergyWard> ENERGY = new HashMap<>();
    private static final List<SlowZone> SLOW_ZONES = new ArrayList<>();
    private static final List<SleetZone> SLEET_ZONES = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private ThirdCircleSpellService() {}
    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        return switch (spellId) {
            case "fireball" -> fireball((ServerLevel) caster.level(), caster, range, power, snapshot);
            case "lightning_bolt" -> lightningBolt((ServerLevel) caster.level(), caster, range, power, snapshot);
            case "fly" -> fly(caster);
            case "haste" -> ArcaneBuffRuntime.apply(caster, "haste", power, range);
            case "dispel_magic" -> dispelMagic(caster, snapshot);
            case "vampiric_touch" -> vampiricTouch((ServerLevel) caster.level(), caster,
                    snapshot.targetEntity(caster).orElse(null), power);
            case "slow" -> slow((ServerLevel) caster.level(), caster, range, snapshot.target());
            case "protection_from_energy" -> protectionFromEnergy(caster);
            case "sleet_storm" -> sleetStorm((ServerLevel) caster.level(), caster, range, power, snapshot.target());
            case "blink" -> blink(caster, range, snapshot);
            default -> false;
        };
    }

    /** NPC mages use the same third-circle role contracts before generic resolution. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "fireball" -> fireball(level, caster, range, power, snapshot);
            case "lightning_bolt" -> lightningBolt(level, caster, range, power, snapshot);
            case "fly" -> fly(level, caster, designatedTarget);
            case "haste" -> {
                caster.addEffect(new MobEffectInstance(MobEffects.SPEED, HASTE_TICKS, 2, true, false));
                yield true;
            }
            case "dispel_magic" -> dispelMagic(caster, designatedTarget);
            case "vampiric_touch" -> vampiricTouch(level, caster, designatedTarget, power);
            case "slow" -> slow(level, caster, range, snapshot.target());
            case "protection_from_energy" -> protectionFromEnergy(caster);
            case "sleet_storm" -> sleetStorm(level, caster, range, power, snapshot.target());
            case "blink" -> blink(level, caster, designatedTarget, range);
            default -> false;
        };
    }

    /** Sleet Storm is area denial: hostile Arcane casting inside the storm is disrupted. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        for (SleetZone zone : SLEET_ZONES) {
            if (zone.level != caster.level() || !zone.active()) continue;
            Entity rawOwner = zone.level.getEntity(zone.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isAlliedTo(caster)) continue;
            if (zone.center.distanceToSqr(caster.position()) <= zone.radius * zone.radius) return true;
        }
        return false;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event == null || event.isCanceled() || event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof LivingEntity target)) return;
        EnergyWard ward = ENERGY.get(target.getUUID());
        if (ward == null || ward.level != target.level() || !ward.active() || ward.charges <= 0) return;
        if (!isEnergyDamage(event)) return;
        ward.charges--;
        long now = ward.level.getGameTime();
        if (ward.nextChargeAt <= now) ward.nextChargeAt = now + ENERGY_RECHARGE_TICKS;
        event.setAmount((float) Math.max(0.0, event.getAmount() * .55));
        ward.level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                target instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                .58F, 1.18F + ward.charges * .05F);
        if (target instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§d[에너지 보호] §f에너지 충격 45% 경감 · 남은 공명막 "
                    + ward.charges + "/" + ENERGY_MAX_CHARGES), 35);
        }
    }

    private static boolean isEnergyDamage(LivingIncomingDamageEvent event) {
        if (ArcaneDamage.isResolving()) return true;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) return true;
        Entity direct = event.getSource().getDirectEntity();
        Entity source = event.getSource().getEntity();
        return direct != null && direct != source;
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickPlayerFlight(level, now);
        tickMobFlight(level, now);
        tickEnergy(level, now);
        tickSlow(level, now);
        tickSleet(level, now);
    }

    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        Set<String> owned = ownedSpellIds(id);
        clear(id);
        for (String spellId : owned) WorldMagicService.cancelRelease(subject, spellId);
    }

    public static void clear(UUID id) {
        if (id == null) return;
        PlayerFlight playerFlight = PLAYER_FLIGHT.remove(id);
        if (playerFlight != null) restorePlayerFlight(playerFlight);
        MobFlight mobFlight = MOB_FLIGHT.remove(id);
        if (mobFlight != null) restoreMobFlight(mobFlight);
        ENERGY.remove(id);
        SLOW_ZONES.removeIf(zone -> zone.ownerId.equals(id));
        SLEET_ZONES.removeIf(zone -> zone.ownerId.equals(id));
    }

    public static void clearAll() {
        for (PlayerFlight state : PLAYER_FLIGHT.values()) restorePlayerFlight(state);
        for (MobFlight state : MOB_FLIGHT.values()) restoreMobFlight(state);
        PLAYER_FLIGHT.clear();
        MOB_FLIGHT.clear();
        ENERGY.clear();
        SLOW_ZONES.clear();
        SLEET_ZONES.clear();
        LAST_TICK.clear();
    }

    private static Set<String> ownedSpellIds(UUID id) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        if (PLAYER_FLIGHT.containsKey(id) || MOB_FLIGHT.containsKey(id)) result.add("fly");
        if (ENERGY.containsKey(id)) result.add("protection_from_energy");
        for (SlowZone zone : SLOW_ZONES) if (zone.ownerId.equals(id)) result.add("slow");
        for (SleetZone zone : SLEET_ZONES) if (zone.ownerId.equals(id)) result.add("sleet_storm");
        return result;
    }

    private static boolean fireball(ServerLevel level, LivingEntity caster, double range, double power,
                                    CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target();
        double radius = Math.max(4.8, Math.min(7.2, SpellMetrics.effectRadius("fireball", range, 3)));
        boolean hit = false;
        AABB box = new AABB(center, center).inflate(radius, Math.max(5.0, radius * .72), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            if (distance > radius + target.getBbWidth()) continue;
            double falloff = Math.max(.55, 1.0 - Math.max(0.0, distance - 1.0) / Math.max(1.0, radius) * .45);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * falloff))) hit = true;
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 180));
            Vec3 away = horizontal(target.position().subtract(center));
            target.push(away.x * .55 * falloff, .16 * falloff, away.z * .55 * falloff);
        }
        if (caster instanceof ServerPlayer player) DestructiveMagicService.impact(player, "fireball", center, radius, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.0F, .84F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean lightningBolt(ServerLevel level, LivingEntity caster, double range, double power,
                                         CastTargetSnapshot snapshot) {
        Vec3 start = snapshot.launchOrigin();
        Vec3 end = snapshot.target();
        Vec3 delta = end.subtract(start);
        double length = Math.max(.001, delta.length());
        Vec3 unit = delta.scale(1.0 / length);
        boolean hit = false;
        AABB box = new AABB(start, end).inflate(1.45, 1.65, 1.45);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            Vec3 relative = target.getEyePosition().subtract(start);
            double projection = relative.dot(unit);
            if (projection < 0.0 || projection > length) continue;
            double width = .92 + target.getBbWidth() * .50;
            if (relative.subtract(unit.scale(projection)).lengthSqr() > width * width) continue;
            if (ArcaneDamage.hurt(level, caster, target, (float) power)) hit = true;
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false));
        }
        if (caster instanceof ServerPlayer player) {
            DestructiveMagicService.applyPhysicalAftermath(player, "lightning_bolt", snapshot, range, power);
        }
        level.playSound(null, BlockPos.containing(end), SoundEvents.LIGHTNING_BOLT_THUNDER,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .82F, 1.24F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean fly(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        PlayerFlight previous = PLAYER_FLIGHT.get(player.getUUID());
        boolean wasMayfly = previous == null ? player.getAbilities().mayfly : previous.wasMayfly;
        boolean wasFlying = previous == null ? player.getAbilities().flying : previous.wasFlying;
        PLAYER_FLIGHT.put(player.getUUID(), new PlayerFlight(level, player.getUUID(), now + FLY_TICKS, wasMayfly, wasFlying));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        ArcaneNoticeService.push(player, Component.literal(
                "§b[플라이] §f30초 동안 실제 자유 비행 권한을 얻습니다. §7종료 시 기존 비행 권한을 복원하고 안전 낙하합니다."), 75);
        return true;
    }

    private static boolean fly(ServerLevel level, Mob caster, LivingEntity target) {
        MobFlight previous = MOB_FLIGHT.get(caster.getUUID());
        boolean wasNoGravity = previous == null ? caster.isNoGravity() : previous.wasNoGravity;
        MOB_FLIGHT.put(caster.getUUID(), new MobFlight(level, caster.getUUID(),
                target == null ? null : target.getUUID(), level.getGameTime() + FLY_TICKS, wasNoGravity));
        caster.setNoGravity(true);
        caster.getNavigation().stop();
        return true;
    }

    private static void tickPlayerFlight(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, PlayerFlight>> iterator = PLAYER_FLIGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerFlight state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.playerId);
            if (!(raw instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator() || now >= state.expiresAt) {
                restorePlayerFlight(state);
                iterator.remove();
                continue;
            }
            player.getAbilities().mayfly = true;
            player.fallDistance = 0.0F;
        }
    }

    private static void restorePlayerFlight(PlayerFlight state) {
        Entity raw = state.level.getEntity(state.playerId);
        if (!(raw instanceof ServerPlayer player)) return;
        player.getAbilities().mayfly = state.wasMayfly;
        player.getAbilities().flying = state.wasFlying && state.wasMayfly;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
    }

    private static void tickMobFlight(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, MobFlight>> iterator = MOB_FLIGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            MobFlight state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.mobId);
            if (!(raw instanceof Mob mob) || !mob.isAlive() || mob.isRemoved() || now >= state.expiresAt) {
                restoreMobFlight(state);
                iterator.remove();
                continue;
            }
            mob.setNoGravity(true);
            mob.getNavigation().stop();
            LivingEntity target = null;
            if (state.targetId != null) {
                Entity candidate = level.getEntity(state.targetId);
                if (candidate instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) target = living;
            }
            if (target != null) {
                Vec3 delta = target.getEyePosition().add(0.0, 2.2, 0.0).subtract(mob.position());
                if (delta.lengthSqr() > 1.0) mob.setDeltaMovement(mob.getDeltaMovement().scale(.45).add(delta.normalize().scale(.18)));
            } else {
                Vec3 motion = mob.getDeltaMovement();
                mob.setDeltaMovement(motion.x * .8, motion.y * .25, motion.z * .8);
            }
            mob.fallDistance = 0.0F;
        }
    }

    private static void restoreMobFlight(MobFlight state) {
        Entity raw = state.level.getEntity(state.mobId);
        if (!(raw instanceof Mob mob) || mob.isRemoved()) return;
        mob.setNoGravity(state.wasNoGravity);
        mob.fallDistance = 0.0F;
        mob.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
    }

    private static boolean dispelMagic(ServerPlayer caster, CastTargetSnapshot snapshot) {
        LivingEntity target = snapshot.targetEntity(caster).orElse(null);
        if (target == null) {
            cleanseHarmful(caster);
            ArcaneNoticeService.push(caster, Component.literal("§b[디스펠] §f자신에게 걸린 해로운 상태 마법을 해제했습니다."), 55);
            return true;
        }
        dispelTarget(target);
        ArcaneNoticeService.push(caster, Component.literal("§b[디스펠] §f" + target.getName().getString()
                + "의 유지형 강화·제어 마법을 해제했습니다."), 60);
        return true;
    }

    private static boolean dispelMagic(Mob caster, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        dispelTarget(target);
        return true;
    }

    private static void dispelTarget(LivingEntity target) {
        FirstCircleSpellService.dispel(target);
        SecondCircleSpellService.clear(target);
        ThirdCircleSpellService.clear(target);
        FourthCircleSpellService.clear(target);
        FifthCircleSpellService.clear(target);
        SixthCircleSpellService.clear(target);
        SeventhCircleSpellService.clear(target);
        EighthCircleSpellService.clear(target);
        SpellGameplayService.clear(target);
        HighWardSpellService.clear(target);
        HighControlSpellService.clear(target);
        if (target instanceof ServerPlayer player) {
            HighUtilitySpellService.clear(player);
            SimulacrumService.clear(player);
            ArcaneLightService.clear(player);
        }
        removeBeneficialMagic(target);
        WorldMagicService.stop(target);
    }

    private static void removeBeneficialMagic(LivingEntity target) {
        target.removeEffect(MobEffects.ABSORPTION);
        target.removeEffect(MobEffects.RESISTANCE);
        target.removeEffect(MobEffects.REGENERATION);
        target.removeEffect(MobEffects.SPEED);
        target.removeEffect(MobEffects.STRENGTH);
        target.removeEffect(MobEffects.INVISIBILITY);
        target.removeEffect(MobEffects.FIRE_RESISTANCE);
        target.removeEffect(MobEffects.NIGHT_VISION);
        target.removeEffect(MobEffects.LUCK);
        target.removeEffect(MobEffects.JUMP_BOOST);
        target.removeEffect(MobEffects.SLOW_FALLING);
    }

    private static void cleanseHarmful(LivingEntity target) {
        target.removeEffect(MobEffects.SLOWNESS);
        target.removeEffect(MobEffects.WEAKNESS);
        target.removeEffect(MobEffects.BLINDNESS);
        target.removeEffect(MobEffects.NAUSEA);
        target.removeEffect(MobEffects.WITHER);
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.MINING_FATIGUE);
        target.removeEffect(MobEffects.LEVITATION);
        target.removeEffect(MobEffects.DARKNESS);
        target.removeEffect(MobEffects.HUNGER);
        target.setTicksFrozen(0);
    }

    private static boolean vampiricTouch(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        if (!enemy(caster, target) || caster.distanceToSqr(target) > 10.0 * 10.0) return false;
        float before = target.getHealth() + target.getAbsorptionAmount();
        if (!ArcaneDamage.hurt(level, caster, target, (float) power)) return false;
        float after = target.getHealth() + target.getAbsorptionAmount();
        float actual = Math.max(0.0F, before - after);
        caster.heal(Math.max(.5F, actual * .60F));
        level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .60F, .72F);
        return true;
    }

    private static boolean slow(ServerLevel level, LivingEntity caster, double range, Vec3 center) {
        double radius = Math.max(5.0, Math.min(9.0, SpellMetrics.effectRadius("slow", range, 3)));
        SLOW_ZONES.removeIf(zone -> zone.ownerId.equals(caster.getUUID()));
        SLOW_ZONES.add(new SlowZone(level, caster.getUUID(), center, radius,
                level.getGameTime() + SLOW_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[슬로우] §f9초 동안 반경 " + one(radius)
                    + "에서 이동·공격 행동을 반복적으로 둔화합니다."), 60);
        }
        return true;
    }

    private static void tickSlow(ServerLevel level, long now) {
        Iterator<SlowZone> iterator = SLOW_ZONES.iterator();
        while (iterator.hasNext()) {
            SlowZone zone = iterator.next();
            if (zone.level != level) continue;
            Entity rawOwner = level.getEntity(zone.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= zone.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < zone.nextPulse) continue;
            zone.nextPulse = now + SLOW_PULSE;
            AABB box = new AABB(zone.center, zone.center).inflate(zone.radius, Math.max(4.5, zone.radius * .72), zone.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value) && zone.center.distanceToSqr(value.position()) <= zone.radius * zone.radius)) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 3, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 2, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 10, 2, true, false));
            }
        }
    }

    private static boolean protectionFromEnergy(LivingEntity caster) {
        ServerLevel level = (ServerLevel) caster.level();
        long now = level.getGameTime();
        ENERGY.put(caster.getUUID(), new EnergyWard(level, caster.getUUID(), now + ENERGY_TICKS,
                ENERGY_MAX_CHARGES, now + ENERGY_RECHARGE_TICKS));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§d[에너지 보호] §f30초 · 5중 공명막 · Arcane/화염/투사체성 충격만 45% 경감하며 소모된 막은 3.5초마다 재충전됩니다."), 80);
        }
        return true;
    }

    private static void tickEnergy(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, EnergyWard>> iterator = ENERGY.entrySet().iterator();
        while (iterator.hasNext()) {
            EnergyWard ward = iterator.next().getValue();
            if (ward.level != level) continue;
            Entity raw = level.getEntity(ward.ownerId);
            if (!(raw instanceof LivingEntity living) || !living.isAlive() || now >= ward.expiresAt) {
                iterator.remove();
                continue;
            }
            if (ward.charges < ENERGY_MAX_CHARGES && now >= ward.nextChargeAt) {
                ward.charges++;
                ward.nextChargeAt = now + ENERGY_RECHARGE_TICKS;
            }
        }
    }

    private static boolean sleetStorm(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(6.5, Math.min(10.5, SpellMetrics.effectRadius("sleet_storm", range, 3)));
        SLEET_ZONES.removeIf(zone -> zone.ownerId.equals(caster.getUUID()));
        SLEET_ZONES.add(new SleetZone(level, caster.getUUID(), center, radius, power,
                level.getGameTime() + SLEET_TICKS, level.getGameTime()));
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§b[진눈깨비 폭풍] §f9초 · 반경 " + one(radius)
                    + " · 냉기·시야·미끄럼 압박 + 내부 적대 Arcane 시전 방해"), 75);
        }
        return true;
    }

    private static void tickSleet(ServerLevel level, long now) {
        Iterator<SleetZone> iterator = SLEET_ZONES.iterator();
        while (iterator.hasNext()) {
            SleetZone zone = iterator.next();
            if (zone.level != level) continue;
            Entity rawOwner = level.getEntity(zone.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || now >= zone.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now < zone.nextPulse) continue;
            zone.nextPulse = now + SLEET_PULSE;
            AABB box = new AABB(zone.center, zone.center).inflate(zone.radius, Math.max(5.0, zone.radius * .76), zone.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> enemy(owner, value) && zone.center.distanceToSqr(value.position()) <= zone.radius * zone.radius)) {
                ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, zone.power * .055));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 16, 3, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 16, 0, true, false));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 24));
                target.setRemainingFireTicks(0);
                double angle = Math.toRadians(Math.floorMod(target.getUUID().hashCode() + (int) now * 17, 360));
                target.push(Math.cos(angle) * .08, 0.0, Math.sin(angle) * .08);
            }
        }
    }

    private static boolean blink(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        double maxDistance = Math.max(12.0, Math.min(20.0, range));
        Vec3 desired = clampDestination(player.position(), snapshot.target(), maxDistance);
        Optional<BlockPos> safe = findSafe(level, desired, 8);
        if (safe.isEmpty()) {
            ArcaneNoticeService.push(player, Component.literal("§c[점멸] §f도착 지점 주변에 안전한 공간이 없습니다."), 45);
            return false;
        }
        BlockPos p = safe.get();
        player.stopRiding();
        boolean moved = player.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        if (!moved) return false;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .88F, .92F);
        ArcaneNoticeService.push(player, Component.literal(
                "§5[점멸] §f장거리 공간 도약 완료 · §7착지 직후 2초간 위상 잔류막으로 충격을 완화합니다."), 55);
        return true;
    }

    private static boolean blink(ServerLevel level, Mob caster, LivingEntity target, double range) {
        if (target == null || !target.isAlive()) return false;
        Vec3 delta = target.position().subtract(caster.position());
        if (delta.lengthSqr() < 1.0E-6) return false;
        double distance = Math.min(Math.max(8.0, range), Math.max(5.0, delta.length() - 3.0));
        Vec3 desired = caster.position().add(delta.normalize().scale(distance));
        Optional<BlockPos> safe = findSafe(level, desired, 8);
        if (safe.isEmpty()) return false;
        BlockPos p = safe.get();
        caster.getNavigation().stop();
        caster.snapTo(p.getX() + .5, p.getY(), p.getZ() + .5, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .84F, .90F);
        return true;
    }

    private static Vec3 clampDestination(Vec3 start, Vec3 desired, double maxDistance) {
        Vec3 delta = desired.subtract(start);
        return delta.lengthSqr() <= maxDistance * maxDistance ? desired : start.add(delta.normalize().scale(maxDistance));
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

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static String one(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private record PlayerFlight(ServerLevel level, UUID playerId, long expiresAt, boolean wasMayfly, boolean wasFlying) {}
    private record MobFlight(ServerLevel level, UUID mobId, UUID targetId, long expiresAt, boolean wasNoGravity) {}

    private static final class EnergyWard {
        final ServerLevel level; final UUID ownerId; final long expiresAt; int charges; long nextChargeAt;
        EnergyWard(ServerLevel level, UUID ownerId, long expiresAt, int charges, long nextChargeAt) {
            this.level = level; this.ownerId = ownerId; this.expiresAt = expiresAt;
            this.charges = charges; this.nextChargeAt = nextChargeAt;
        }
        boolean active() { return level.getGameTime() < expiresAt; }
    }

    private static class SlowZone {
        final ServerLevel level; final UUID ownerId; final Vec3 center; final double radius; final long expiresAt; long nextPulse;
        SlowZone(ServerLevel level, UUID ownerId, Vec3 center, double radius, long expiresAt, long nextPulse) {
            this.level = level; this.ownerId = ownerId; this.center = center; this.radius = radius;
            this.expiresAt = expiresAt; this.nextPulse = nextPulse;
        }
    }

    private static final class SleetZone extends SlowZone {
        final double power;
        SleetZone(ServerLevel level, UUID ownerId, Vec3 center, double radius, double power, long expiresAt, long nextPulse) {
            super(level, ownerId, center, radius, expiresAt, nextPulse); this.power = power;
        }
        boolean active() { return level.getGameTime() < expiresAt; }
    }
}
