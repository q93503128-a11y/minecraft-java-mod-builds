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
import net.minecraft.world.item.BowItem;
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
    private static final Map<UUID, SkillScale> SPIN_SCALE = new HashMap<>();
    private static final Map<UUID, TimedScale> RALLY_SCALE = new HashMap<>();
    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> RAPID_SCALE = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> RAPID_ARROWS = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> RICOCHET_SCALE = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> RICOCHET_ARROWS = new HashMap<>();
    private static final Map<UUID, TrackingArrowState> TRACKING_ARROWS = new HashMap<>();
    private static final List<RicochetHop> RICOCHET_HOPS = new ArrayList<>();
    private static final Set<RicochetDamageKey> PRE_SCALED_RICOCHET_DAMAGE = new HashSet<>();
    private static final Map<UUID, EmpoweredArrowState> ARROW_RAIN_READY = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> MEGA_ARROW_READY = new HashMap<>();
    private static final Map<UUID, Long> FORTRESS_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> FORTRESS_SCALE = new HashMap<>();
    private static final Map<UUID, Long> AEGIS_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> AEGIS_SCALE = new HashMap<>();
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
        SPIN_SCALE.clear();
        RALLY_SCALE.clear();
        RAPID_UNTIL.clear();
        RAPID_SCALE.clear();
        RAPID_ARROWS.clear();
        RAPID_DRAW_TICKS.clear();
        RICOCHET_UNTIL.clear();
        RICOCHET_SCALE.clear();
        RICOCHET_ARROWS.clear();
        TRACKING_ARROWS.clear();
        RICOCHET_HOPS.clear();
        PRE_SCALED_RICOCHET_DAMAGE.clear();
        ARROW_RAIN_READY.clear();
        MEGA_ARROW_READY.clear();
        FORTRESS_UNTIL.clear();
        FORTRESS_SCALE.clear();
        AEGIS_UNTIL.clear();
        AEGIS_SCALE.clear();
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
                SPIN_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", spinDuration + 8);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, spinDuration, 0, false, false, true));
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.72f);
            }
            case VANGUARD_BREAKER -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                RALLY_SCALE.put(player.getUUID(), new TimedScale(now + duration, power, specialRank));
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
                int waveCount = 6 + Math.min(4, Math.max(0,
                        Math.round((durationMultiplier - 1.0f) * 5.0f)));
                for (int i = 0; i < waveCount; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 4L + i * 4L, player.getUUID(), skill,
                            ActionKind.BLADE_WAVE, power, durationMultiplier, specialRank, player.position(), forward));
                }
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.72f);
            }
            case VANGUARD_STORM -> {
                SLAMS.put(player.getUUID(), new SlamState(
                        now, power, durationMultiplier, specialRank, player.position()));
                player.setDeltaMovement(forward.scale(0.52).add(0.0, 1.05, 0.0));
                player.hurtMarked = true;
                play(level, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.35f);
            }

            case RANGER_VOLLEY -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(240L, duration * 2L);
                RAPID_UNTIL.put(player.getUUID(), until);
                RAPID_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f);
            }
            case RANGER_PIERCE -> {
                clearRangerReadies(player.getUUID());
                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240L, duration * 2L));
                RICOCHET_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
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

            case ARCANIST_FIRE_ORB -> launchFireOrb(level, player,
                    1.35, Math.max(80, Math.round(112 * durationMultiplier)),
                    (12.0f + playerLevel * 0.65f) * power,
                    areaRadius(4.8, specialRank), specialRank, sight);
            case ARCANIST_FROST_RING -> {
                double radius = areaRadius(7.5, specialRank);
                Vec3 center = aimedGround(level, player, maximumRange(28.0, specialRank));
                int until = Math.max(140, duration);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,
                        now + until, radius, power, specialRank, 0));
                VillageSkillEffectSystem.frostField(level, player, center, until, radius, specialRank);
                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);
            }
            case ARCANIST_CHAIN -> {
                double radius = areaRadius(8.5, specialRank);
                Vec3 center = aimedGround(level, player, maximumRange(30.0, specialRank));
                int until = Math.max(120, duration);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,
                        now + until, radius, power, specialRank, 0));
                VillageSkillEffectSystem.tornadoField(level, player, center, forward, until, radius, specialRank);
                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);
            }
            case ARCANIST_NOVA -> {
                double radius = areaRadius(18.0, specialRank);
                Vec3 center = aimedGround(level, player, maximumRange(36.0, specialRank));
                int until = Math.max(100, duration / 2);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,
                        now + until, radius, power, specialRank, 0));
                VillageSkillEffectSystem.lightningField(level, player, center, until, radius, specialRank);
                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 1.15f);
            }

            case LUMINAR_HEAL -> healLowestAlly(player,
                    (10.0f + playerLevel * 0.7f) * power, duration, specialRank, false);
            case LUMINAR_CLEANSE -> cleanseAllies(player,
                    (3.0f + playerLevel * 0.22f) * power, duration, specialRank);
            case LUMINAR_VEIL -> {
                double radius = areaRadius(7.5, specialRank);
                int until = Math.max(160, duration * 2);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.HEALING, player.position(),
                        now + until, radius, power, specialRank, 0));
                VillageSkillEffectSystem.healingField(level, player, player.position(), until, radius, specialRank);
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
                FORTRESS_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                int shieldAmplifier = 5 + Math.min(3, specialRank)
                        + Math.min(3, Math.max(0, Math.round((power - 1.0f) * 4.0f)));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(120, duration), shieldAmplifier, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(120, duration), 3, false, false, true));
                play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.4f, 0.55f);
            }
            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(180, duration * 2L));
                AEGIS_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                        Math.max(180, duration * 2), 1 + Math.min(1, specialRank / 4), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(180, duration * 2),
                        Math.min(5, 1 + Math.max(0, Math.round((power - 1.0f) * 4.0f))), false, false, true));
                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 0.7f);
            }
        }

        if (skill.role() == VillageRole.ARCANIST && !replayingEcho) {
            int echoes = 0;
            float firstEchoChance = Math.min(0.58f, 0.30f + specialRank * 0.045f);
            float secondEchoChance = Math.min(0.32f, 0.12f + specialRank * 0.025f);
            if (player.getRandom().nextFloat() < firstEchoChance) echoes++;
            if (player.getRandom().nextFloat() < secondEchoChance) echoes++;
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
        tickTrackingArrows(server, now);
        tickRicochetHops(server, now);
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
            tickRapidBow(player, id, now);
            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);
            if (spinUntil >= now) {
                SkillScale spin = SPIN_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                player.setYBodyRot((float) ((now * 38.0) % 360.0));
                if (now % 6L == 0L) {
                    VillageNetwork.sendSkillMotion(level, player, "vanguard_spin",
                            (int) Math.max(8L, spinUntil - now + 8L));
                }
                if (now % 3L == 0L) {
                    damageRadius(level, player, player.position(),
                            areaRadius(4.7, spin.specialRank()), 10 + spin.specialRank() * 2,
                            (2.4f + VillageCouncilState.levelOf(id) * 0.16f) * spin.power(),
                            false, 0.32 + spin.specialRank() * 0.03, 0.05);
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
                groundSlam(level, player, slam.power(), slam.durationMultiplier(), slam.specialRank());
                SLAMS.remove(id);
            }
            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                SkillScale fortress = FORTRESS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6 + fortress.specialRank() * 0.35,
                            16 + fortress.specialRank() * 3,
                            0.38 + fortress.specialRank() * 0.025, 0.04,
                            0.55f * fortress.power());
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                SkillScale aegis = AEGIS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                if (now % 3L == 0L) pushFront(level, player,
                        7.0 + aegis.specialRank() * 0.65, 30 + aegis.specialRank() * 4,
                        0.7 + aegis.specialRank() * 0.035, 0.08,
                        1.2f * aegis.power());
                if (player.isSprinting() && now - LAST_AEGIS_DASH.getOrDefault(id, -100L) >= 14L) {
                    LAST_AEGIS_DASH.put(id, now);
                    Vec3 forward = horizontalLook(player);
                    player.setDeltaMovement(forward.scale(0.78 + Math.min(0.25, (aegis.power() - 1.0f) * 0.22))
                            .add(0.0, 0.05, 0.0));
                    player.hurtMarked = true;
                    play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.15f);
                }
            }
            if (activeRole(player) == VillageRole.WARDEN && now % 40L == 0L) {
                int passiveRank = VillageRoleSkillSystem.specialRank(player, VillageRole.WARDEN);
                player.heal(0.8f + passiveRank * 0.16f);
            }
        }
    }

    private static void tickRapidBow(ServerPlayer player, UUID id, long now) {
        if (RAPID_UNTIL.getOrDefault(id, 0L) < now
                || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof BowItem)) {
            RAPID_DRAW_TICKS.remove(id);
            return;
        }
        int usedTicks = RAPID_DRAW_TICKS.merge(id, 1, Integer::sum);
        int specialRank = VillageRoleSkillSystem.specialRank(player, VillageRole.RANGER);
        int completeAt = Math.max(5, 9 - Math.min(4, specialRank));
        if (usedTicks < completeAt) return;

        // ArrowLooseEvent turns this prepared shot into a full-charge shot.
        // Releasing the real use action also resets the client draw animation,
        // unlike only changing a duration value inside an item-use tick event.
        player.releaseUsingItem();
        RAPID_DRAW_TICKS.remove(id);
    }

    private static void tickTrackingArrows(MinecraftServer server, long now) {
        ServerLevel level = server.overworld();
        Iterator<Map.Entry<UUID, TrackingArrowState>> iterator = TRACKING_ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackingArrowState> entry = iterator.next();
            TrackingArrowState state = entry.getValue();
            Entity arrowEntity = level.getEntity(entry.getKey());
            if (now > state.until()
                    || !(arrowEntity instanceof AbstractArrow arrow)
                    || !arrow.isAlive()
                    || !(arrow.getOwner() instanceof ServerPlayer owner)
                    || !isRangerContext(owner)) {
                iterator.remove();
                continue;
            }

            Entity lockedEntity = level.getEntity(state.target());
            Mob target = lockedEntity instanceof Mob locked && locked.isAlive() ? locked : null;
            if (target == null) {
                target = bestFlightTarget(level, owner, arrow, 52.0);
                if (target == null) {
                    arrow.setNoGravity(false);
                    iterator.remove();
                    continue;
                }
                entry.setValue(new TrackingArrowState(target.getUUID(), state.until()));
            }

            Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
            Vec3 delta = body.subtract(arrow.position());
            if (delta.lengthSqr() < 0.05) continue;
            double currentSpeed = arrow.getDeltaMovement().length();
            double speed = Math.max(2.0, Math.min(3.6, currentSpeed));
            double leadTicks = Math.min(7.0, Math.sqrt(delta.lengthSqr()) / Math.max(0.1, speed));
            Vec3 predicted = body.add(target.getDeltaMovement().scale(leadTicks * 0.58));
            Vec3 guided = predicted.subtract(arrow.position());
            if (guided.lengthSqr() < 1.0E-5) continue;

            EmpoweredArrowState empowered = RICOCHET_ARROWS.get(entry.getKey());
            int specialRank = empowered == null ? 0 : empowered.specialRank();
            double turnStrength = Math.min(0.76, 0.46 + specialRank * 0.05);
            Vec3 current = arrow.getDeltaMovement().lengthSqr() < 1.0E-5
                    ? guided.normalize() : arrow.getDeltaMovement().normalize();
            Vec3 blended = current.scale(1.0 - turnStrength)
                    .add(guided.normalize().scale(turnStrength));
            if (blended.lengthSqr() < 1.0E-5) blended = guided.normalize();

            arrow.setNoGravity(true);
            arrow.setDeltaMovement(blended.normalize().scale(speed));
            arrow.hurtMarked = true;
            if (now % 3L == 0L) {
                VillageSkillEffectSystem.trackingReticle(
                        level, owner, body, body.subtract(arrow.position()));
            }
        }
    }

    private static void tickRicochetHops(MinecraftServer server, long now) {
        Iterator<RicochetHop> iterator = RICOCHET_HOPS.iterator();
        while (iterator.hasNext()) {
            RicochetHop hop = iterator.next();
            if (hop.executeAt() > now) continue;
            iterator.remove();
            ServerPlayer owner = server.getPlayerList().getPlayer(hop.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level)) continue;
            Entity entity = level.getEntity(hop.target());
            if (!(entity instanceof Mob target) || !target.isAlive()) continue;
            hurtByPlayer(level, owner, target, hop.damage());
            play(level, target.position(), SoundEvents.ARROW_HIT, 0.62f,
                    1.18f + Math.min(0.34f, hop.hopIndex() * 0.055f));
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
                case SHIELD_CHARGE -> shieldCharge(level, player, action.power(),
                        action.durationMultiplier(), action.specialRank());
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

            // Exactly three strike pulses per ten ticks: 1.5x the former two pulses.
            if (area.kind() == AreaKind.LIGHTNING) {
                int cycle = (int) Math.floorMod(now + area.phase(), 10L);
                if (cycle != 0 && cycle != 3 && cycle != 6) continue;
                float damage = (7.0f + VillageCouncilState.levelOf(owner.getUUID()) * 0.42f)
                        * area.power();
                double strikeRadius = areaRadius(4.8, area.specialRank());
                List<Mob> fieldTargets = targetsNear(level, owner, area.center(), area.radius(), 80);
                for (int strikeIndex = 0; strikeIndex < 2; strikeIndex++) {
                    Vec3 strike;
                    if (!fieldTargets.isEmpty() && owner.getRandom().nextFloat() < 0.90f) {
                        Mob preferred = fieldTargets.get(owner.getRandom().nextInt(fieldTargets.size()));
                        strike = preferred.position().add(
                                (owner.getRandom().nextDouble() - 0.5) * 1.4,
                                0.0,
                                (owner.getRandom().nextDouble() - 0.5) * 1.4);
                    } else {
                        strike = randomPointInCircle(level, area.center(), area.radius());
                    }
                    spawnVisualLightning(level, strike);
                    for (Mob target : targetsNear(level, owner, strike, strikeRadius, 28)) {
                        hurt(level, target, damage);
                        target.addEffect(new MobEffectInstance(
                                MobEffects.SLOWNESS, 30, 1, false, false, true));
                    }
                }
                continue;
            }

            if (now % 5L != area.phase() % 5L) continue;
            switch (area.kind()) {
                case FROST -> {
                    for (Mob target : targetsNear(level, owner, area.center(), area.radius(), 48)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 3, false, false, true));
                        if (now % 20L == 0L) hurt(level, target, 2.2f * area.power());
                    }
                    if (now % 20L == 0L) play(level, area.center(), SoundEvents.GLASS_HIT, 0.55f, 0.62f);
                }
                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(1.20));
                    area.moveTo(next);
                    for (Mob target : targetsNear(level, owner, next, area.radius(), 48)) {
                        Vec3 pull = next.subtract(target.position());
                        Vec3 horizontal = new Vec3(pull.x, 0.0, pull.z);
                        if (horizontal.lengthSqr() > 0.01) horizontal = horizontal.normalize().scale(0.24);
                        target.push(horizontal.x, 0.20, horizontal.z);
                        target.hurtMarked = true;
                        if (now % 15L == 0L) hurt(level, target, 1.8f * area.power());
                    }
                    if (now % 15L == 0L) play(level, next, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 0.78f);
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
                case LIGHTNING -> { /* handled above */ }
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
            Vec3 previous = moving.lastPosition();
            Vec3 position = entity == null ? previous : entity.position();
            boolean blocked = false;
            if (entity != null && previous.distanceToSqr(position) > 1.0E-6) {
                var hit = level.clip(new net.minecraft.world.level.ClipContext(
                        previous, position,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        owner));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    blocked = true;
                    position = hit.getLocation();
                }
            }
            moving.lastPosition(position);
            moving.age(moving.age() + 1);
            List<Mob> hits = moving.kind() == MovingKind.FIRE_ORB
                    ? fireOrbContacts(level, owner, position, moving.specialRank(), 40)
                    : targetsNear(level, owner, position, moving.radius(), 40);
            boolean expired = entity == null || !entity.isAlive() || blocked || moving.age() >= moving.maxAge();
            switch (moving.kind()) {
                case FIRE_ORB -> {
                    if (hits.isEmpty() && !expired) continue;
                    for (Mob target : targetsNear(level, owner, position, moving.radius(), 40)) {
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
            if (moving.effectId() != null) {
                Entity visual = level.getEntity(moving.effectId());
                if (visual != null) visual.discard();
            }
            iterator.remove();
        }
    }

    private static void cleanupExpired(MinecraftServer server, long now) {
        SPIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        SPIN_SCALE.keySet().removeIf(id -> SPIN_UNTIL.getOrDefault(id, 0L) < now);
        RALLY_SCALE.entrySet().removeIf(entry -> entry.getValue().until() < now);
        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RAPID_SCALE.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
        RAPID_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        RAPID_DRAW_TICKS.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
        RICOCHET_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_SCALE.keySet().removeIf(id -> RICOCHET_UNTIL.getOrDefault(id, 0L) < now);
        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        TRACKING_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        ARROW_RAIN_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);
        MEGA_ARROW_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);
        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        FORTRESS_SCALE.keySet().removeIf(id -> FORTRESS_UNTIL.getOrDefault(id, 0L) < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_SCALE.keySet().removeIf(id -> AEGIS_UNTIL.getOrDefault(id, 0L) < now);
        CHARGE_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    public static void handleArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isRangerContext(player)) return;
        long now = player.level().getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
            event.setCharge(20);
            return;
        }
        event.setCharge(Math.min(20, event.getCharge() + 5));
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
        SkillScale trackingScale = RICOCHET_SCALE.remove(id);
        boolean tracking = trackingUntil != null && trackingUntil >= now;
        if (tracking) {
            SkillScale scale = trackingScale == null ? SkillScale.DEFAULT : trackingScale;
            RICOCHET_ARROWS.put(arrow.getUUID(),
                    new EmpoweredArrowState(now + 240L, scale.power(), scale.specialRank()));
            Mob locked = lockArrowOnTarget(level, player, arrow);
            if (locked != null) {
                TRACKING_ARROWS.put(arrow.getUUID(),
                        new TrackingArrowState(locked.getUUID(), now + 240L));
            }
            play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 0.85f, 1.3f);
        } else {
            int passiveRank = VillageRoleSkillSystem.specialRank(player, VillageRole.RANGER);
            aimAssist(level, player, arrow, 0.24 + passiveRank * 0.025);
        }

        Long rapidUntil = RAPID_UNTIL.remove(id);
        SkillScale rapidScale = RAPID_SCALE.remove(id);
        if (rapidUntil == null || rapidUntil < now) return;
        SkillScale scale = rapidScale == null ? SkillScale.DEFAULT : rapidScale;
        RAPID_ARROWS.put(arrow.getUUID(),
                new EmpoweredArrowState(now + 240L, scale.power(), scale.specialRank()));
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0, scale.power());
            spawnSideArrow(level, player, arrow, 8.0, scale.power());
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, scale.power() * 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, scale.power() * 0.82f);
            }
        } finally {
            spawningGeneratedArrow = false;
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            VillageRole role = activeRole(attacker);
            if (event.getSource().getDirectEntity() instanceof AbstractArrow directArrow) {
                EmpoweredArrowState rapid = RAPID_ARROWS.remove(directArrow.getUUID());
                if (rapid != null) event.setAmount(event.getAmount() * rapid.power());
            }
            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());
            if (rally != null && rally.until() >= attacker.level().getGameTime()) {
                event.setAmount(event.getAmount() * (1.0f + Math.max(0.0f, rally.power() - 1.0f) * 0.80f));
            }
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                int passiveRank = VillageRoleSkillSystem.specialRank(attacker, VillageRole.VANGUARD);
                float ratio = 0.055f + passiveRank * 0.008f
                        + VillageRelicSystem.vanguardLifeStealBonus(attacker);
                attacker.heal(Math.min(4.5f, event.getAmount() * ratio));
            }
            if (role == VillageRole.RANGER
                    && event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_ARROWS.containsKey(directArrow.getUUID())
                    && attacker.level() instanceof ServerLevel level) {
                EmpoweredArrowState ricochet = RICOCHET_ARROWS.remove(directArrow.getUUID());
                TRACKING_ARROWS.remove(directArrow.getUUID());
                if (ricochet != null) event.setAmount(event.getAmount() * ricochet.power());
                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                queueRicochet(level, attacker, primary, damage, ricochetRank);
            }
        }
        if (event.getEntity() instanceof ServerPlayer defender && activeRole(defender) == VillageRole.WARDEN) {
            int passiveRank = VillageRoleSkillSystem.specialRank(defender, VillageRole.WARDEN);
            event.setAmount(event.getAmount() * Math.max(0.68f, 0.82f - passiveRank * 0.018f));
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
                1.75 + specialRank * 0.035, 24 + specialRank * 2,
                (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45 + specialRank * 0.12, specialRank, origin, direction);
        play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f,
                0.78f + player.getRandom().nextFloat() * 0.18f);
    }

    private static void groundSlam(ServerLevel level, ServerPlayer player,
                                   float power, float durationMultiplier, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        double radius = areaRadius(8.5, specialRank);
        damageRadius(level, player, player.position(), radius, 40 + specialRank * 4,
                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,
                false, 1.05 + specialRank * 0.05, 0.38);
        int fractureDuration = Math.max(40, Math.round(55 * durationMultiplier));
        for (Mob target : targetsNear(level, player, player.position(), radius, 48)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    fractureDuration, Math.min(2, specialRank / 2), false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    fractureDuration, Math.min(3, 1 + specialRank / 2), false, false, true));
        }
        VillageSkillEffectSystem.slamImpact(level, player, radius, specialRank);
        play(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.55f);
        play(level, player.position(), SoundEvents.ANVIL_LAND, 1.1f, 0.62f);
    }

    private static void arrowRain(ServerLevel level, ServerPlayer player, Vec3 center, float power, int specialRank) {
        double radius = areaRadius(8.5, specialRank);
        VillageSkillEffectSystem.arrowRainImpact(level, player, center, radius, specialRank);
        float damage = (3.3f + VillageCouncilState.levelOf(player.getUUID()) * 0.18f) * power;
        for (Mob target : targetsNear(level, player, center, radius, 48)) {
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

    private static void shieldCharge(ServerLevel level, ServerPlayer player,
                                     float power, float durationMultiplier, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        VillageSkillEffectSystem.shieldCharge(level, player, horizontalLook(player));
        int steps = 5 + Math.min(4, Math.max(0, Math.round((durationMultiplier - 1.0f) * 4.0f)));
        double contactRadius = 2.3 + specialRank * 0.22;
        for (int i = 0; i < steps; i++) {
            Vec3 center = player.position().add(horizontalLook(player).scale(1.0 + i * 1.2));
            for (Mob target : targetsNear(level, player, center, contactRadius, 12 + specialRank * 3)) {
                hurt(level, target, (5.5f + VillageCouncilState.levelOf(player.getUUID()) * 0.3f) * power);
                knockFrom(player.position(), target, 1.15 + specialRank * 0.07, 0.12);
            }
        }
        play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.15f, 0.82f);
    }

    private static void tauntShout(ServerLevel level, ServerPlayer player,
                                   float damage, int duration, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        double radius = 20.0 + specialRank * 2.0;
        for (Mob target : targetsNear(level, player, player.position(), radius, 60 + specialRank * 5)) {
            hurt(level, target, damage);
            target.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    Math.min(180, duration), 1 + Math.min(2, specialRank / 2), false, false, true));
        }
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                Math.min(180, duration), 1 + Math.min(1, specialRank / 4), false, false, true));
        play(level, player.position(), SoundEvents.RAVAGER_ROAR, 1.1f, 0.68f);
    }

    private static void healLowestAlly(ServerPlayer player, float amount,
                                       int duration, int specialRank, boolean barrier) {
        ServerPlayer target = allies(player, 24.0).stream()
                .min(Comparator.comparingDouble(ally -> ally.getHealth() / Math.max(1.0f, ally.getMaxHealth())))
                .orElse(player);
        healScaled(target, amount);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                Math.max(60, duration / 2), Math.min(2, specialRank / 2), false, false, true));
        if (barrier || specialRank >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    Math.max(120, duration), 1 + Math.min(3, specialRank), false, false, true));
        }
        if (target.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.healLink(level, player, target);
            play(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.28f);
        }
    }

    private static void cleanseAllies(ServerPlayer player, float heal, int duration, int specialRank) {
        List<ServerPlayer> affected = allies(player, -1.0);
        for (ServerPlayer ally : affected) {
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.SLOWNESS);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            ally.removeEffect(MobEffects.NAUSEA);
            ally.removeEffect(MobEffects.MINING_FATIGUE);
            healScaled(ally, heal);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    Math.max(50, duration / 2), Math.min(1, specialRank / 3), false, false, true));
            if (specialRank >= 2) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(80, duration), Math.min(4, specialRank), false, false, true));
            }
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


    private static void launchFireOrb(
            ServerLevel level, ServerPlayer player, double speed, int maxAge,
            float damage, double radius, int specialRank, Vec3 direction) {
        Vec3 normalized = direction.normalize();
        Vec3 origin = player.getEyePosition().add(normalized.scale(0.8));
        var projectile = EntityTypes.SNOWBALL.create(level, EntitySpawnReason.EVENT);
        if (projectile == null) return;
        projectile.setOwner(player);
        projectile.setItem(ItemStack.EMPTY);
        projectile.setInvisible(true);
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(normalized.scale(speed));
        if (!level.addFreshEntity(projectile)) return;
        VillageSkillEffectEntity visual = VillageSkillEffectSystem.fireOrb(
                level, player, origin, normalized, maxAge, (float) speed, specialRank);
        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), MovingKind.FIRE_ORB,
                maxAge, damage, radius, specialRank, origin,
                visual == null ? null : visual.getUUID()));
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
                damage, radius, specialRank, origin, null));
    }

    private static void spawnSideArrow(ServerLevel level, ServerPlayer owner,
                                       AbstractArrow source, double degrees, float power) {
        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.setPos(source.getX(), source.getY(), source.getZ());
        Vec3 velocity = rotateY(source.getDeltaMovement(), Math.toRadians(degrees));
        arrow.setDeltaMovement(velocity);
        arrow.setBaseDamage(2.0);
        RAPID_ARROWS.put(arrow.getUUID(), new EmpoweredArrowState(
                level.getGameTime() + 160L, power, 0));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        spawningGeneratedArrow = true;
        try { level.addFreshEntity(arrow); }
        finally { spawningGeneratedArrow = false; }
    }

    private static Mob lockArrowOnTarget(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow) {
        Mob target = bestAimTarget(level, player, arrow.position(), 64.0);
        if (target == null) return null;
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        Vec3 delta = body.subtract(arrow.position());
        if (delta.lengthSqr() < 1.0E-5) return null;
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = Math.max(2.0, Math.min(3.6, velocity.length()));
        Vec3 current = velocity.lengthSqr() < 1.0E-5
                ? lookDirection(player) : velocity.normalize();
        Vec3 desired = delta.normalize();
        Vec3 blended = current.scale(0.38).add(desired.scale(0.62));
        if (blended.lengthSqr() < 1.0E-5) blended = desired;
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(blended.normalize().scale(speed));
        arrow.hurtMarked = true;
        return target;
    }

    private static void queueRicochet(
            ServerLevel level, ServerPlayer owner, Mob primary, float baseDamage, int specialRank) {
        double bounceRadius = areaRadius(10.5, specialRank);
        int maximumChain = 4 + Math.min(4, Math.max(0, specialRank));
        List<Mob> chain = buildRicochetChain(
                level, owner, primary, bounceRadius, maximumChain);
        if (chain.isEmpty()) return;

        VillageSkillEffectSystem.ricochet(level, owner, primary, chain);
        long now = level.getGameTime();
        for (int i = 0; i < chain.size(); i++) {
            Mob target = chain.get(i);
            float falloff = (float) Math.pow(0.86, i);
            RICOCHET_HOPS.add(new RicochetHop(
                    now + 2L + i * 2L,
                    owner.getUUID(), target.getUUID(),
                    Math.max(1.5f, baseDamage * falloff), i));
        }
    }

    private static List<Mob> buildRicochetChain(
            ServerLevel level, ServerPlayer owner, Mob primary,
            double bounceRadius, int maximumChain) {
        List<Mob> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(primary.getUUID());
        Mob cursor = primary;
        for (int hop = 0; hop < maximumChain; hop++) {
            Mob from = cursor;
            Mob next = targetsNear(level, owner, from.position(), bounceRadius, 36).stream()
                    .filter(target -> !visited.contains(target.getUUID()))
                    .filter(from::hasLineOfSight)
                    .min(Comparator.comparingDouble(from::distanceToSqr))
                    .orElse(null);
            if (next == null) break;
            result.add(next);
            visited.add(next.getUUID());
            cursor = next;
        }
        return result;
    }

    private static Mob bestFlightTarget(
            ServerLevel level, ServerPlayer owner, AbstractArrow arrow, double range) {
        Vec3 velocity = arrow.getDeltaMovement();
        Vec3 forward = velocity.lengthSqr() < 1.0E-5
                ? lookDirection(owner) : velocity.normalize();
        Vec3 origin = arrow.position();
        return targetsNear(level, owner, origin, range, 64).stream()
                .filter(target -> hasClearFlightPath(level, owner, origin, target))
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(forward) >= 0.30;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    double alignment = to.normalize().dot(forward);
                    return to.lengthSqr() * (1.15 - Math.max(0.0, alignment));
                }))
                .orElse(null);
    }

    private static boolean hasClearFlightPath(
            ServerLevel level, ServerPlayer owner, Vec3 origin, Mob target) {
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                origin, body,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                owner));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private static void aimAssist(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow, double strength) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) return;
        Mob target = bestAimTarget(level, player, arrow.position(), 48.0);
        if (target == null) return;
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        Vec3 assisted = body.subtract(arrow.position()).normalize();
        double safe = Math.max(0.0, Math.min(0.45, strength));
        Vec3 blended = velocity.normalize().scale(1.0 - safe).add(assisted.scale(safe)).normalize();
        arrow.setDeltaMovement(blended.scale(speed));
        arrow.hurtMarked = true;
    }

    private static Mob bestAimTarget(
            ServerLevel level, ServerPlayer player, Vec3 origin, double range) {
        Vec3 look = lookDirection(player);
        return targetsNear(level, player, player.position(), range, 80).stream()
                .filter(player::hasLineOfSight)
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(look) >= 0.62;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    double forward = Math.max(0.0, to.dot(look));
                    Vec3 closest = origin.add(look.scale(forward));
                    double miss = body.distanceToSqr(closest);
                    return miss * 6.5 + to.lengthSqr() * 0.010;
                }))
                .orElse(null);
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

    public static boolean isPreScaledRicochetDamage(ServerPlayer owner, Entity target) {
        return owner != null && target != null
                && PRE_SCALED_RICOCHET_DAMAGE.contains(
                        new RicochetDamageKey(owner.getUUID(), target.getUUID()));
    }

    private static void hurtByPlayer(
            ServerLevel level, ServerPlayer owner, Mob target, float damage) {
        RicochetDamageKey key = new RicochetDamageKey(owner.getUUID(), target.getUUID());
        PRE_SCALED_RICOCHET_DAMAGE.add(key);
        try {
            target.hurtServer(level, level.damageSources().playerAttack(owner), Math.max(0.1f, damage));
        } finally {
            PRE_SCALED_RICOCHET_DAMAGE.remove(key);
        }
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
        double radius = areaRadius(8.5, specialRank);
        Vec3 center = aimedGround(level, player, maximumRange(30.0, specialRank));
        int fieldDuration = 42;
        VillageSkillEffectSystem.arrowRainField(
                level, player, center, fieldDuration, radius, specialRank);
        for (int i = 0; i < 8; i++) {
            SCHEDULED.add(new ScheduledAction(now + 2L + i * 4L, player.getUUID(),
                    VillageRoleSkillSystem.ActiveSkill.RANGER_RICOCHET,
                    ActionKind.ARROW_RAIN, power, 1.0f, specialRank, center, Vec3.ZERO));
        }
        play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
    }

    private static void clearRangerReadies(UUID id) {
        RAPID_UNTIL.remove(id);
        RAPID_SCALE.remove(id);
        RICOCHET_UNTIL.remove(id);
        RICOCHET_SCALE.remove(id);
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

    private static List<Mob> fireOrbContacts(
            ServerLevel level, ServerPlayer owner, Vec3 position, int specialRank, int limit) {
        double padding = fireOrbContactPadding(specialRank);
        List<Mob> candidates = new ArrayList<>(targetsNear(
                level, owner, position, padding + 3.5, Math.max(40, limit)));
        candidates.removeIf(target -> !target.getBoundingBox().inflate(padding).contains(position));
        if (candidates.size() > Math.max(0, limit)) {
            return new ArrayList<>(candidates.subList(0, Math.max(0, limit)));
        }
        return candidates;
    }

    private static double fireOrbContactPadding(int specialRank) {
        return Math.min(1.95, 1.55 + Math.min(5, Math.max(0, specialRank)) * 0.08);
    }

    private static double areaRadius(double base, int specialRank) {
        return base * (1.0 + Math.min(5, Math.max(0, specialRank)) * 0.08);
    }

    private static double maximumRange(double base, int specialRank) {
        return base + Math.min(5, Math.max(0, specialRank)) * 3.0;
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
        private final UUID effectId;
        private Vec3 lastPosition;
        private int age;

        private MovingSkill(UUID owner, MovingKind kind, int maxAge, float damage,
                            double radius, int specialRank, Vec3 lastPosition, UUID effectId) {
            this.owner = owner;
            this.kind = kind;
            this.maxAge = maxAge;
            this.damage = damage;
            this.radius = radius;
            this.specialRank = specialRank;
            this.lastPosition = lastPosition;
            this.effectId = effectId;
        }

        UUID owner() { return owner; }
        MovingKind kind() { return kind; }
        int maxAge() { return maxAge; }
        float damage() { return damage; }
        double radius() { return radius; }
        int specialRank() { return specialRank; }
        Set<UUID> hit() { return hit; }
        UUID effectId() { return effectId; }
        Vec3 lastPosition() { return lastPosition; }
        void lastPosition(Vec3 value) { lastPosition = value; }
        int age() { return age; }
        void age(int value) { age = value; }
    }

    private record RicochetDamageKey(UUID owner, UUID target) {}

    private record RicochetHop(long executeAt, UUID owner, UUID target, float damage, int hopIndex) {}

    private record TrackingArrowState(UUID target, long until) {}

    private record SkillScale(float power, float durationMultiplier, int specialRank) {
        private static final SkillScale DEFAULT = new SkillScale(1.0f, 1.0f, 0);
    }

    private record TimedScale(long until, float power, int specialRank) {}

    private record EmpoweredArrowState(long until, float power, int specialRank) {}

    private record SlamState(long startedAt, float power, float durationMultiplier,
                             int specialRank, Vec3 origin) {}
}
