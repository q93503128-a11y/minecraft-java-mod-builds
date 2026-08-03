package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real role ability motion/state system.  The visuals are driven by player
 * movement, arm swings, real projectiles, lightning, temporary shield blocks,
 * enemy displacement and spatial sounds instead of particle geometry.
 */
public final class VillageRoleAbilitySystem {
    private static final Map<UUID, Long> SPIN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> ARROW_RAIN_READY = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> MEGA_ARROW_READY = new HashMap<>();
    private static final Map<UUID, Long> FORTRESS_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> AEGIS_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> CHARGE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST_AEGIS_DASH = new HashMap<>();
    private static final Map<UUID, SlamState> SLAMS = new HashMap<>();
    private static final List<ScheduledAction> SCHEDULED = new ArrayList<>();
    private static final List<AreaState> AREAS = new ArrayList<>();
    private static final Map<UUID, MovingSkill> MOVING = new LinkedHashMap<>();
    private static boolean spawningGeneratedArrow;
    private static boolean replayingEcho;

    private VillageRoleAbilitySystem() {}

    public static void reset() {
        SPIN_UNTIL.clear();
        RAPID_UNTIL.clear();
        RICOCHET_UNTIL.clear();
        RICOCHET_ARROWS.clear();
        ARROW_RAIN_READY.clear();
        MEGA_ARROW_READY.clear();
        FORTRESS_UNTIL.clear();
        AEGIS_UNTIL.clear();
        CHARGE_UNTIL.clear();
        LAST_AEGIS_DASH.clear();
        SLAMS.clear();
        SCHEDULED.clear();
        AREAS.clear();
        MOVING.clear();
        VillageSkillEffectSystem.reset();
        spawningGeneratedArrow = false;
        replayingEcho = false;
    }

    public static void cast(
            ServerLevel level,
            ServerPlayer player,
            VillageRoleSkillSystem.ActiveSkill skill,
            float power,
            float durationMultiplier,
            int specialRank) {
        long now = level.getGameTime();
        int playerLevel = VillageCouncilState.levelOf(player.getUUID());
        int duration = Math.max(40, Math.round((120 + playerLevel * 3) * durationMultiplier));
        Vec3 forward = horizontalLook(player);
        Vec3 sight = lookDirection(player);
        Vec3 visualDirection = skill == VillageRoleSkillSystem.ActiveSkill.ARCANIST_FIRE_ORB
                ? sight : forward;
        VillageSkillEffectSystem.startCast(level, player, skill, duration, visualDirection);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                int spinDuration = Math.max(48, duration / 2);
                SPIN_UNTIL.put(player.getUUID(), now + spinDuration);
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", spinDuration + 8);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, spinDuration, 0, false, false, true));
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.72f);
            }
            case VANGUARD_BREAKER -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 1 + Math.min(1, specialRank / 3), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1 + Math.min(1, specialRank / 4), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, false, false, true));
                for (ServerPlayer ally : allies(player, 11.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, specialRank >= 3 ? 1 : 0, false, false, true));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, false, true));
                }
                play(level, player.position(), SoundEvents.RAVAGER_ROAR, 0.85f, 1.18f);
            }
            case VANGUARD_CRY -> {
                CHARGE_UNTIL.put(player.getUUID(), now + 22);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 26, 2, false, false, true));
                for (int i = 0; i < 6; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 4L + i * 4L, player.getUUID(), skill,
                            ActionKind.BLADE_WAVE, power, durationMultiplier, specialRank, player.position(), forward));
                }
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.72f);
            }
            case VANGUARD_STORM -> {
                SLAMS.put(player.getUUID(), new SlamState(now, power, specialRank, player.position()));
                player.setDeltaMovement(forward.scale(0.52).add(0.0, 1.05, 0.0));
                player.hurtMarked = true;
                play(level, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.35f);
            }

            case RANGER_VOLLEY -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(240L, duration * 2L);
                RAPID_UNTIL.put(player.getUUID(), until);
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f);
            }
            case RANGER_PIERCE -> {
                clearRangerReadies(player.getUUID());
                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240L, duration * 2L));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 1.0f, 1.2f);
            }
            case RANGER_RICOCHET -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(260L, duration * 2L);
                ARROW_RAIN_READY.put(player.getUUID(),
                        new EmpoweredArrowState(until, power, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 0.92f);
            }
            case RANGER_FIRE_RAIN -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(280L, duration * 2L);
                MEGA_ARROW_READY.put(player.getUUID(), new EmpoweredArrowState(until, power, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.62f);
            }

            case ARCANIST_FIRE_ORB -> launchMoving(level, player, MovingKind.FIRE_ORB,
                    new ItemStack(Items.FIRE_CHARGE), 1.35, 100,
                    (12.0f + playerLevel * 0.65f) * power, 4.8, specialRank, sight);
            case ARCANIST_FROST_RING -> {
                Vec3 center = aimedGround(level, player, 18.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,
                        now + Math.max(140, duration), 7.5, power, specialRank, 0));
                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);
            }
            case ARCANIST_CHAIN -> {
                Vec3 center = player.position().add(forward.scale(3.0));
                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,
                        now + Math.max(120, duration), 8.5, power, specialRank, 0));
                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);
            }
            case ARCANIST_NOVA -> {
                Vec3 center = aimedGround(level, player, 22.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,
                        now + Math.max(100, duration / 2), 18.0, power, specialRank, 0));
                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 1.15f);
            }

            case LUMINAR_HEAL -> healLowestAlly(player,
                    (10.0f + playerLevel * 0.7f) * power, specialRank, false);
            case LUMINAR_CLEANSE -> cleanseAllies(player,
                    (3.0f + playerLevel * 0.22f) * power, specialRank);
            case LUMINAR_VEIL -> {
                AREAS.add(new AreaState(player.getUUID(), AreaKind.HEALING, player.position(),
                        now + Math.max(160, duration * 2L), 7.5, power, specialRank, 0));
                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);
            }
            case LUMINAR_SANCTUARY -> miracle(player,
                    (15.0f + playerLevel * 0.9f) * power, Math.max(2, specialRank), duration * 2);

            case WARDEN_TAUNT -> {
                AEGIS_UNTIL.remove(player.getUUID());
                SCHEDULED.add(new ScheduledAction(now, player.getUUID(), skill,
                        ActionKind.SHIELD_CHARGE, power, durationMultiplier, specialRank,
                        player.position(), forward));
                player.setDeltaMovement(forward.scale(1.05).add(0.0, 0.08, 0.0));
                player.hurtMarked = true;
            }
            case WARDEN_BASH -> tauntShout(level, player,
                    (4.0f + playerLevel * 0.25f) * power, duration, specialRank);
            case WARDEN_FORMATION -> {
                FORTRESS_UNTIL.put(player.getUUID(), now + Math.max(120, duration));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(120, duration), 5 + Math.min(3, specialRank), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(120, duration), 3, false, false, true));
                play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.4f, 0.55f);
            }
            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(180, duration * 2L));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(180, duration * 2), 1, false, false, true));
                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 0.7f);
            }
        }

        if (skill.role() == VillageRole.ARCANIST && !replayingEcho) {
            int echoes = 0;
            if (player.getRandom().nextFloat() < 0.30f) echoes++;
            if (player.getRandom().nextFloat() < 0.12f) echoes++;
            for (int i = 0; i < echoes; i++) {
                SCHEDULED.add(new ScheduledAction(now + 8L + i * 8L, player.getUUID(), skill,
                        ActionKind.ARCANE_ECHO, power * 0.72f, durationMultiplier, specialRank,
                        player.position(), visualDirection));
            }
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        tickPlayers(server, now);
        tickScheduled(server, now);
        tickAreas(server, now);
        tickMoving(server, now);
        cleanupExpired(server, now);
        VillageSkillEffectSystem.tick(server);
    }

    private static void tickPlayers(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || VillageRespawnSystem.isDowned(player)) continue;
            UUID id = player.getUUID();
            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);
            if (spinUntil >= now) {
                player.setYBodyRot((float) ((now * 38.0) % 360.0));
                if (now % 6L == 0L) {
                    VillageNetwork.sendSkillMotion(level, player, "vanguard_spin",
                            (int) Math.max(8L, spinUntil - now + 8L));
                }
                if (now % 3L == 0L) {
                    damageRadius(level, player, player.position(), 4.7, 10,
                            2.4f + VillageCouncilState.levelOf(id) * 0.16f,
                            false, 0.32, 0.05);
                    play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f,
                            0.8f + (now % 4) * 0.05f);
                }
            }
            if (RICOCHET_UNTIL.getOrDefault(id, 0L) >= now && now % 4L == 0L) {
                Vec3 sight = lookDirection(player);
                Vec3 readyPoint = player.getEyePosition().add(sight.scale(1.45));
                VillageSkillEffectSystem.trackingReticle(level, player, readyPoint, sight);
            }
            SlamState slam = SLAMS.get(id);
            if (slam != null && now > slam.startedAt() + 5L
                    && (player.onGround() || now > slam.startedAt() + 34L)) {
                groundSlam(level, player, slam.power(), slam.specialRank());
                SLAMS.remove(id);
            }
            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6, 16, 0.38, 0.04, 0.0f);
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                if (now % 3L == 0L) pushFront(level, player, 7.0, 30, 0.7, 0.08, 1.2f);
                if (player.isSprinting() && now - LAST_AEGIS_DASH.getOrDefault(id, -100L) >= 14L) {
                    LAST_AEGIS_DASH.put(id, now);
                    Vec3 forward = horizontalLook(player);
                    player.setDeltaMovement(forward.scale(0.78).add(0.0, 0.05, 0.0));
                    player.hurtMarked = true;
                    play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.15f);
                }
            }
            if (VillageCouncilState.roleOf(id).orElse(null) == VillageRole.WARDEN && now % 40L == 0L) {
                player.heal(0.8f);
            }
        }
    }

    private static void tickScheduled(MinecraftServer server, long now) {
        Iterator<ScheduledAction> iterator = SCHEDULED.iterator();
        while (iterator.hasNext()) {
            ScheduledAction action = iterator.next();
            if (action.executeAt() > now) continue;
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(action.owner());
            if (player == null || !(player.level() instanceof ServerLevel level)) continue;
            switch (action.kind()) {
                case BLADE_WAVE -> bladeWave(level, player, action.power(), action.specialRank());
                case ARROW_RAIN -> arrowRain(level, player, action.origin(), action.power(), action.specialRank());
                case ENERGY_ARROW -> launchEnergyArrow(level, player, action.power(), action.specialRank());
                case SHIELD_CHARGE -> shieldCharge(level, player, action.power(), action.specialRank());
                case ARCANE_ECHO -> {
                    replayingEcho = true;
                    try {
                        cast(level, player, action.skill(), action.power(),
                                action.durationMultiplier(), action.specialRank());
                    } finally {
                        replayingEcho = false;
                    }
                }
            }
        }
    }

    private static void tickAreas(MinecraftServer server, long now) {
        Iterator<AreaState> iterator = AREAS.iterator();
        while (iterator.hasNext()) {
            AreaState area = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(area.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level) || now > area.until()) {
                iterator.remove();
                continue;
            }
            if (now % 5L != area.phase() % 5L) continue;
            switch (area.kind()) {
                case FROST -> {
                    for (Mob target : targetsNear(level, owner, area.center(), area.radius(), 40)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 3, false, false, true));
                        if (now % 20L == 0L) hurt(level, target, 2.2f * area.power());
                    }
                    if (now % 20L == 0L) play(level, area.center(), SoundEvents.GLASS_HIT, 0.55f, 0.62f);
                }
                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(0.24));
                    area.moveTo(next);
                    for (Mob target : targetsNear(level, owner, next, area.radius(), 36)) {
                        Vec3 pull = next.subtract(target.position());
                        Vec3 horizontal = new Vec3(pull.x, 0.0, pull.z);
                        if (horizontal.lengthSqr() > 0.01) horizontal = horizontal.normalize().scale(0.24);
                        target.push(horizontal.x, 0.20, horizontal.z);
                        target.hurtMarked = true;
                        if (now % 15L == 0L) hurt(level, target, 1.8f * area.power());
                    }
                    if (now % 15L == 0L) play(level, next, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 0.78f);
                }
                case LIGHTNING -> {
                    if (now % 5L == 0L) {
                        float damage = (7.0f + VillageCouncilState.levelOf(owner.getUUID()) * 0.42f)
                                * area.power();
                        for (int strikeIndex = 0; strikeIndex < 2; strikeIndex++) {
                            Vec3 strike = randomPointInCircle(level, area.center(), area.radius());
                            List<Mob> nearby = targetsNear(level, owner, strike, 4.8, 24);
                            if (!nearby.isEmpty() && owner.getRandom().nextFloat() < 0.72f) {
                                strike = nearby.get(owner.getRandom().nextInt(nearby.size())).position();
                            }
                            spawnVisualLightning(level, strike);
                            for (Mob target : targetsNear(level, owner, strike, 4.8, 24)) {
                                hurt(level, target, damage);
                                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, false, true));
                            }
                        }
                    }
                }
                case HEALING -> {
                    if (now % 20L == 0L) {
                        for (ServerPlayer ally : alliesAt(owner, area.center(), area.radius())) {
                            healScaled(ally, (2.6f + area.specialRank() * 0.35f) * area.power());
                            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 35, 0, false, false, true));
                        }
                        play(level, area.center(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.15f);
                    }
                }
            }
        }
    }

    private static void tickMoving(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, MovingSkill>> iterator = MOVING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MovingSkill> entry = iterator.next();
            MovingSkill moving = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(moving.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            Vec3 position = entity == null ? moving.lastPosition() : entity.position();
            moving.lastPosition(position);
            moving.age(moving.age() + 1);
            List<Mob> hits = targetsNear(level, owner, position, moving.radius(), 36);
            boolean expired = entity == null || moving.age() >= moving.maxAge();
            switch (moving.kind()) {
                case FIRE_ORB -> {
                    if (hits.isEmpty() && !expired) continue;
                    for (Mob target : targetsNear(level, owner, position, moving.radius(), 36)) {
                        hurt(level, target, moving.damage());
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),
                                120 + moving.specialRank() * 35));
                    }
                    VillageSkillEffectSystem.fireImpact(level, owner, position, moving.radius());
                    play(level, position, SoundEvents.GENERIC_EXPLODE.value(), 1.05f, 1.08f);
                }
                case BLADE -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 0.5, 0.05);
                        }
                    }
                    if (!expired) continue;
                }
                case ENERGY_ARROW -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 1.35, 0.18);
                        }
                    }
                    if (!expired) continue;
                    play(level, position, SoundEvents.GENERIC_EXPLODE.value(), 1.3f, 0.62f);
                }
            }
            if (entity != null) entity.discard();
            iterator.remove();
        }
    }

    private static void cleanupExpired(MinecraftServer server, long now) {
        SPIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);
        ARROW_RAIN_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);
        MEGA_ARROW_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);
        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        CHARGE_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    public static void handleArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isRangerContext(player)) return;
        int bonus = 5;
        long now = player.level().getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) bonus += 10;
        event.setCharge(Math.min(20, event.getCharge() + bonus));
    }

    public static void handleEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || !isRangerContext(player)) return;
        if (spawningGeneratedArrow) return;
        long now = level.getGameTime();
        UUID id = player.getUUID();

        EmpoweredArrowState mega = MEGA_ARROW_READY.remove(id);
        if (mega != null && mega.until() >= now) {
            event.setCanceled(true);
            launchEnergyArrow(level, player, mega.power(), mega.specialRank());
            return;
        }

        EmpoweredArrowState rain = ARROW_RAIN_READY.remove(id);
        if (rain != null && rain.until() >= now) {
            activateArrowRain(level, player, rain.power(), rain.specialRank());
        }

        Long trackingUntil = RICOCHET_UNTIL.remove(id);
        boolean tracking = trackingUntil != null && trackingUntil >= now;
        if (tracking) {
            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);
            aimAssist(level, player, arrow, 0.68);
            play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 0.85f, 1.3f);
        } else {
            aimAssist(level, player, arrow, 0.24);
        }

        Long rapidUntil = RAPID_UNTIL.remove(id);
        if (rapidUntil == null || rapidUntil < now) return;
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0);
            spawnSideArrow(level, player, arrow, 8.0);
        } finally {
            spawningGeneratedArrow = false;
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            VillageRole role = activeRole(attacker);
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                attacker.heal(Math.min(2.5f, event.getAmount() * 0.055f));
            }
            if (role == VillageRole.RANGER
                    && event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_ARROWS.remove(directArrow.getUUID()) != null
                    && attacker.level() instanceof ServerLevel level) {
                List<Mob> chain = targetsNear(level, attacker, primary.position(), 12.0, 9);
                chain.remove(primary);
                chain.sort(Comparator.comparingDouble(primary::distanceToSqr));
                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                List<Mob> visualChain = new ArrayList<>();
                for (int i = 0; i < Math.min(6, chain.size()); i++) {
                    Mob target = chain.get(i);
                    visualChain.add(target);
                    hurt(level, target, damage * (1.0f - i * 0.09f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
                VillageSkillEffectSystem.ricochet(level, attacker, primary, visualChain);
            }
        }
        if (event.getEntity() instanceof ServerPlayer defender
                && VillageCouncilState.roleOf(defender.getUUID()).orElse(null) == VillageRole.WARDEN) {
            event.setAmount(event.getAmount() * 0.82f);
        }
    }

    public static void handleDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || !isRangerContext(killer)
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow)
                || VillageSkillTestSystem.isTestDummy(event.getEntity())) return;
        killer.getInventory().add(new ItemStack(Items.ARROW));
    }

    public static void handleKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN) {
            event.setCanceled(true);
        }
    }

    public static String activeSkillHud(ServerPlayer player) {
        if (player == null) return "";
        long now = player.level().getGameTime();
        List<String> states = new ArrayList<>();
        appendReady(states, "§b신속 삼연사", RAPID_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        appendReady(states, "§e추적 도탄", RICOCHET_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        EmpoweredArrowState rain = ARROW_RAIN_READY.get(player.getUUID());
        if (rain != null) appendReady(states, "§9천공 화살비", rain.until(), now);
        EmpoweredArrowState mega = MEGA_ARROW_READY.get(player.getUUID());
        if (mega != null) appendReady(states, "§a성멸 대궁", mega.until(), now);
        appendTimed(states, "§9거대 방패 태세", FORTRESS_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        appendTimed(states, "§3대수호 진군", AEGIS_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        return String.join(" §8· ", states);
    }

    private static void appendReady(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f다음 활 · "
                + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }

    private static void appendTimed(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f" + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }

    public static boolean isRapidFire(ServerPlayer player) {
        return player != null && RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= player.level().getGameTime();
    }

    public static boolean isFortress(ServerPlayer player) {
        return player != null && FORTRESS_UNTIL.getOrDefault(player.getUUID(), 0L) >= player.level().getGameTime();
    }

    private static void bladeWave(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = horizontalLook(player);
        VillageSkillEffectSystem.bladeWave(level, player, direction);
        Vec3 origin = player.position().add(0.0, 0.82, 0.0).add(direction.scale(1.0));
        launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,
                1.75, 24, (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45, specialRank, origin, direction);
        play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f,
                0.78f + player.getRandom().nextFloat() * 0.18f);
    }

    private static void groundSlam(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        damageRadius(level, player, player.position(), 8.5, 32,
                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,
                false, 1.05, 0.38);
        VillageSkillEffectSystem.slamImpact(level, player);
        play(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.55f);
        play(level, player.position(), SoundEvents.ANVIL_LAND, 1.1f, 0.62f);
    }

    private static void arrowRain(ServerLevel level, ServerPlayer player, Vec3 center, float power, int specialRank) {
        VillageSkillEffectSystem.arrowRainImpact(level, player, center);
        float damage = (3.3f + VillageCouncilState.levelOf(player.getUUID()) * 0.18f) * power;
        for (Mob target : targetsNear(level, player, center, 8.5, 40)) {
            hurt(level, target, damage);
            if (specialRank >= 3) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
        }
        // Falling arrows are rendered by one short-lived synchronized mesh field.
        // No persistent Arrow entities are created, preventing stuck arrows and weapon validation crashes.
        play(level, center, SoundEvents.ARROW_SHOOT, 0.8f, 1.45f);
    }

    private static void launchEnergyArrow(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.stopUsingItem();
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = lookDirection(player);
        Vec3 origin = player.getEyePosition().add(direction.scale(2.8));
        VillageSkillEffectSystem.energyArrow(level, player, origin, direction);
        float damage = (31.0f + VillageCouncilState.levelOf(player.getUUID()) * 1.35f) * power;
        launchMovingAt(level, player, MovingKind.ENERGY_ARROW, new ItemStack(Items.SPECTRAL_ARROW),
                2.65, 55, damage, 5.0, specialRank, origin, direction);
        play(level, player.position(), SoundEvents.ENDER_DRAGON_SHOOT, 1.45f, 0.78f);
    }

    private static void shieldCharge(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        VillageSkillEffectSystem.shieldCharge(level, player, horizontalLook(player));
        for (int i = 0; i < 5; i++) {
            Vec3 center = player.position().add(horizontalLook(player).scale(1.0 + i * 1.2));
            for (Mob target : targetsNear(level, player, center, 2.3, 12)) {
                hurt(level, target, (5.5f + VillageCouncilState.levelOf(player.getUUID()) * 0.3f) * power);
                knockFrom(player.position(), target, 1.15, 0.12);
            }
        }
        play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.15f, 0.82f);
    }

    private static void tauntShout(ServerLevel level, ServerPlayer player,
                                   float damage, int duration, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        for (Mob target : targetsNear(level, player, player.position(), 20.0, 60)) {
            hurt(level, target, damage);
            target.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(100, duration), 1, false, false, true));
        }
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.min(100, duration), 1, false, false, true));
        play(level, player.position(), SoundEvents.RAVAGER_ROAR, 1.1f, 0.68f);
    }

    private static void healLowestAlly(ServerPlayer player, float amount, int specialRank, boolean barrier) {
        ServerPlayer target = allies(player, 24.0).stream()
                .min(Comparator.comparingDouble(ally -> ally.getHealth() / Math.max(1.0f, ally.getMaxHealth())))
                .orElse(player);
        healScaled(target, amount);
        if (barrier || specialRank >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1 + Math.min(3, specialRank), false, false, true));
        }
        if (target.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.healLink(level, player, target);
            play(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.28f);
        }
    }

    private static void cleanseAllies(ServerPlayer player, float heal, int specialRank) {
        List<ServerPlayer> affected = allies(player, -1.0);
        for (ServerPlayer ally : affected) {
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.SLOWNESS);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            healScaled(ally, heal);
        }
        if (player.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.cleanse(level, player, affected);
            play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.35f);
        }
    }

    private static void miracle(ServerPlayer player, float heal, int specialRank, int duration) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<ServerPlayer> affected = server.getPlayerList().getPlayers().stream()
                .filter(ally -> ally.level() == player.level())
                .toList();
        for (ServerPlayer ally : affected) {
            if (VillageRespawnSystem.isDowned(ally)) VillageRespawnSystem.reviveNow(ally, "기적의 대성역");
            healScaled(ally, heal);
            int amplifier = lowHealthAmplifier(ally, 2 + Math.min(3, specialRank));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.miracle(level, player, affected);
            play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.4f, 0.82f);
            play(level, player.position(), SoundEvents.TOTEM_USE, 1.0f, 1.0f);
        }
    }

    private static void healScaled(ServerPlayer target, float amount) {
        float missing = 1.0f - target.getHealth() / Math.max(1.0f, target.getMaxHealth());
        float multiplier = VillageCouncilState.roleOf(target.getUUID()).orElse(null) == VillageRole.LUMINAR
                ? 1.0f : 1.0f;
        if (missing >= 0.75f) multiplier *= 1.75f;
        else if (missing >= 0.50f) multiplier *= 1.45f;
        else if (missing >= 0.25f) multiplier *= 1.20f;
        target.heal(amount * multiplier);
    }

    private static int lowHealthAmplifier(ServerPlayer target, int base) {
        float ratio = target.getHealth() / Math.max(1.0f, target.getMaxHealth());
        if (ratio <= 0.25f) return base + 2;
        if (ratio <= 0.50f) return base + 1;
        return base;
    }

    private static void launchMoving(
            ServerLevel level, ServerPlayer player, MovingKind kind, ItemStack item,
            double speed, int maxAge, float damage, double radius, int specialRank, Vec3 direction) {
        launchMovingAt(level, player, kind, item, speed, maxAge, damage, radius, specialRank,
                player.getEyePosition().add(direction.scale(0.8)), direction);
    }

    private static void launchMovingAt(
            ServerLevel level, ServerPlayer player, MovingKind kind, ItemStack item,
            double speed, int maxAge, float damage, double radius, int specialRank,
            Vec3 origin, Vec3 direction) {
        var projectile = EntityTypes.SNOWBALL.create(level, EntitySpawnReason.EVENT);
        if (projectile == null) return;
        projectile.setOwner(player);
        projectile.setItem(ItemStack.EMPTY);
        projectile.setInvisible(true);
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(direction.normalize().scale(speed));
        if (!level.addFreshEntity(projectile)) return;
        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), kind, maxAge,
                damage, radius, specialRank, origin));
    }

    private static void spawnSideArrow(ServerLevel level, ServerPlayer owner, AbstractArrow source, double degrees) {
        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.setPos(source.getX(), source.getY(), source.getZ());
        Vec3 velocity = rotateY(source.getDeltaMovement(), Math.toRadians(degrees));
        arrow.setDeltaMovement(velocity);
        arrow.setBaseDamage(2.0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        spawningGeneratedArrow = true;
        try { level.addFreshEntity(arrow); }
        finally { spawningGeneratedArrow = false; }
    }

    private static void aimAssist(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow, double strength) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) return;
        Vec3 direction = velocity.normalize();
        Mob target = targetsNear(level, player, player.position(), 44.0, 48).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(arrow.position());
                    return to.lengthSqr() > 0.1 && to.normalize().dot(direction) >= 0.72;
                })
                .min(Comparator.comparingDouble(mob ->
                        mob.getEyePosition().distanceToSqr(aimPoint(level, player, 44.0))))
                .orElse(null);
        if (target == null) return;
        Vec3 assisted = target.getEyePosition().subtract(arrow.position()).normalize();
        double safe = Math.max(0.0, Math.min(0.82, strength));
        Vec3 blended = direction.scale(1.0 - safe).add(assisted.scale(safe)).normalize();
        arrow.setDeltaMovement(blended.scale(speed));
        arrow.hurtMarked = true;
    }


    private static List<Mob> targetsNear(
            ServerLevel level, ServerPlayer owner, Vec3 center, double radius, int limit) {
        List<Mob> candidates = VillageSkillTestSystem.isEnabled(owner)
                ? VillageSkillTestSystem.targetsNear(level, owner, Math.max(40.0, radius + 8.0), 64)
                : VillageRaidSystem.activeEnemiesNear(level, center, radius, Math.max(limit, 1), null);
        double squared = radius * radius;
        List<Mob> result = candidates.stream()
                .filter(Mob::isAlive)
                .filter(target -> target.position().distanceToSqr(center) <= squared)
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)))
                .toList();
        return result.size() <= limit ? new ArrayList<>(result)
                : new ArrayList<>(result.subList(0, limit));
    }

    private static void damageRadius(
            ServerLevel level, ServerPlayer owner, Vec3 center, double radius, int limit,
            float damage, boolean fire, double horizontalKnockback, double verticalKnockback) {
        for (Mob target : targetsNear(level, owner, center, radius, limit)) {
            hurt(level, target, damage);
            if (fire) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
            if (horizontalKnockback > 0.0 || verticalKnockback > 0.0) {
                knockFrom(center, target, horizontalKnockback, verticalKnockback);
            }
        }
    }

    private static void pushFront(
            ServerLevel level, ServerPlayer player, double range, int limit,
            double horizontal, double vertical, float damage) {
        Vec3 forward = horizontalLook(player);
        for (Mob target : targetsNear(level, player, player.position(), range + 2.0, limit)) {
            Vec3 to = target.position().subtract(player.position());
            if (to.lengthSqr() < 0.01 || to.normalize().dot(forward) < 0.20) continue;
            if (damage > 0.0f) hurt(level, target, damage);
            target.push(forward.x * horizontal, vertical, forward.z * horizontal);
            target.hurtMarked = true;
        }
    }

    private static void knockFrom(Vec3 source, Mob target, double horizontal, double vertical) {
        Vec3 delta = target.position().subtract(source);
        Vec3 direction = new Vec3(delta.x, 0.0, delta.z);
        if (direction.lengthSqr() < 0.001) direction = new Vec3(0.0, 0.0, 1.0);
        direction = direction.normalize().scale(horizontal);
        target.push(direction.x, vertical, direction.z);
        target.hurtMarked = true;
    }

    private static void hurt(ServerLevel level, Mob target, float damage) {
        target.hurtServer(level, level.damageSources().magic(), Math.max(0.1f, damage));
    }

    private static List<ServerPlayer> allies(ServerPlayer player, double radius) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return List.of(player);
        double squared = radius < 0.0 ? Double.MAX_VALUE : radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(ally -> ally.level() == player.level())
                .filter(ally -> ally.distanceToSqr(player) <= squared)
                .toList();
    }

    private static List<ServerPlayer> alliesAt(ServerPlayer player, Vec3 center, double radius) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return List.of(player);
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(ally -> ally.level() == player.level())
                .filter(ally -> ally.position().distanceToSqr(center) <= squared)
                .toList();
    }

    private static Vec3 aimedGround(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 point = aimPoint(level, player, distance);
        Vec3 start = point.add(0.0, 12.0, 0.0);
        Vec3 end = point.add(0.0, -48.0, 0.0);
        var ground = level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        if (ground.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return new Vec3(point.x, player.getY() + 0.02, point.z);
        }
        return ground.getLocation().add(0.0, 0.02, 0.0);
    }

    private static void activateArrowRain(
            ServerLevel level, ServerPlayer player, float power, int specialRank) {
        long now = level.getGameTime();
        Vec3 center = aimedGround(level, player, 24.0);
        int fieldDuration = 42;
        VillageSkillEffectSystem.arrowRainField(level, player, center, fieldDuration, 8.5);
        for (int i = 0; i < 8; i++) {
            SCHEDULED.add(new ScheduledAction(now + 2L + i * 4L, player.getUUID(),
                    VillageRoleSkillSystem.ActiveSkill.RANGER_RICOCHET,
                    ActionKind.ARROW_RAIN, power, 1.0f, specialRank, center, Vec3.ZERO));
        }
        play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
    }

    private static void clearRangerReadies(UUID id) {
        RAPID_UNTIL.remove(id);
        RICOCHET_UNTIL.remove(id);
        ARROW_RAIN_READY.remove(id);
        MEGA_ARROW_READY.remove(id);
    }

    private static VillageRole activeRole(ServerPlayer player) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            return VillageSkillTestSystem.selectedRole(player);
        }
        return VillageCouncilState.roleOf(player.getUUID()).orElse(null);
    }

    private static boolean isRangerContext(ServerPlayer player) {
        return activeRole(player) == VillageRole.RANGER;
    }

    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(lookDirection(player).scale(distance));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static Vec3 lookDirection(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        return look.lengthSqr() < 0.001 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        return horizontal.lengthSqr() < 0.001 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
    }

    private static Vec3 rotateY(Vec3 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y,
                vector.x * sin + vector.z * cos);
    }

    private static Vec3 randomPointInCircle(ServerLevel level, Vec3 center, double radius) {
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(level.getRandom().nextDouble()) * radius;
        return center.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (lightning == null) return;
        lightning.setVisualOnly(true);
        lightning.setPos(position.x, position.y, position.z);
        level.addFreshEntity(lightning);
    }

    private static void play(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, BlockPos.containing(position), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private enum ActionKind { BLADE_WAVE, ARROW_RAIN, ENERGY_ARROW, SHIELD_CHARGE, ARCANE_ECHO }
    private enum AreaKind { FROST, TORNADO, LIGHTNING, HEALING }
    private enum MovingKind { FIRE_ORB, BLADE, ENERGY_ARROW }

    private record ScheduledAction(
            long executeAt, UUID owner, VillageRoleSkillSystem.ActiveSkill skill,
            ActionKind kind, float power, float durationMultiplier, int specialRank,
            Vec3 origin, Vec3 direction) {}

    private static final class AreaState {
        private final UUID owner;
        private final AreaKind kind;
        private Vec3 center;
        private final long until;
        private final double radius;
        private final float power;
        private final int specialRank;
        private final int phase;

        private AreaState(UUID owner, AreaKind kind, Vec3 center, long until,
                          double radius, float power, int specialRank, int phase) {
            this.owner = owner;
            this.kind = kind;
            this.center = center;
            this.until = until;
            this.radius = radius;
            this.power = power;
            this.specialRank = specialRank;
            this.phase = phase;
        }

        UUID owner() { return owner; }
        AreaKind kind() { return kind; }
        Vec3 center() { return center; }
        void moveTo(Vec3 value) { center = value; }
        long until() { return until; }
        double radius() { return radius; }
        float power() { return power; }
        int specialRank() { return specialRank; }
        int phase() { return phase; }
    }

    private static final class MovingSkill {
        private final UUID owner;
        private final MovingKind kind;
        private final int maxAge;
        private final float damage;
        private final double radius;
        private final int specialRank;
        private final Set<UUID> hit = new HashSet<>();
        private Vec3 lastPosition;
        private int age;

        private MovingSkill(UUID owner, MovingKind kind, int maxAge, float damage,
                            double radius, int specialRank, Vec3 lastPosition) {
            this.owner = owner;
            this.kind = kind;
            this.maxAge = maxAge;
            this.damage = damage;
            this.radius = radius;
            this.specialRank = specialRank;
            this.lastPosition = lastPosition;
        }

        UUID owner() { return owner; }
        MovingKind kind() { return kind; }
        int maxAge() { return maxAge; }
        float damage() { return damage; }
        double radius() { return radius; }
        int specialRank() { return specialRank; }
        Set<UUID> hit() { return hit; }
        Vec3 lastPosition() { return lastPosition; }
        void lastPosition(Vec3 value) { lastPosition = value; }
        int age() { return age; }
        void age(int value) { age = value; }
    }

    private record EmpoweredArrowState(long until, float power, int specialRank) {}

    private record SlamState(long startedAt, float power, int specialRank, Vec3 origin) {}
}
