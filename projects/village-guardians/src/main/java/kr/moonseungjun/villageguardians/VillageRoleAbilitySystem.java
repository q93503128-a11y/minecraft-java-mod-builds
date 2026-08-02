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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final String GENERATED_ARROW = "vg_generated_arrow";
    private static final Map<UUID, Long> SPIN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> FORTRESS_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> AEGIS_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> CHARGE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST_AEGIS_DASH = new HashMap<>();
    private static final Map<UUID, SlamState> SLAMS = new HashMap<>();
    private static final List<ScheduledAction> SCHEDULED = new ArrayList<>();
    private static final List<AreaState> AREAS = new ArrayList<>();
    private static final Map<UUID, MovingSkill> MOVING = new LinkedHashMap<>();
    private static final Map<UUID, ShieldBlocks> SHIELDS = new HashMap<>();
    private static boolean spawningGeneratedArrow;
    private static boolean replayingEcho;

    private VillageRoleAbilitySystem() {}

    public static void reset() {
        SPIN_UNTIL.clear();
        RAPID_UNTIL.clear();
        RICOCHET_UNTIL.clear();
        FORTRESS_UNTIL.clear();
        AEGIS_UNTIL.clear();
        CHARGE_UNTIL.clear();
        LAST_AEGIS_DASH.clear();
        SLAMS.clear();
        SCHEDULED.clear();
        AREAS.clear();
        MOVING.clear();
        SHIELDS.clear();
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
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                SPIN_UNTIL.put(player.getUUID(), now + Math.max(38, duration / 2));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(38, duration / 2), 0, false, false, true));
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
                CHARGE_UNTIL.put(player.getUUID(), now + 26);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 28, 2, false, false, true));
                for (int i = 0; i < 6; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 5L + i * 4L, player.getUUID(), skill,
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
                RAPID_UNTIL.put(player.getUUID(), now + Math.max(120, duration));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, Math.max(120, duration), 1, false, false, true));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f);
            }
            case RANGER_PIERCE -> {
                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(160, duration));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 1.0f, 1.2f);
            }
            case RANGER_RICOCHET -> {
                Vec3 center = aimedGround(player, 15.0);
                for (int i = 0; i < 8; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 8L + i * 5L, player.getUUID(), skill,
                            ActionKind.ARROW_RAIN, power, durationMultiplier, specialRank, center, Vec3.ZERO));
                }
                play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
            }
            case RANGER_FIRE_RAIN -> {
                CHARGE_UNTIL.put(player.getUUID(), now + 36);
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 36, 4, false, false, true));
                SCHEDULED.add(new ScheduledAction(now + 36, player.getUUID(), skill,
                        ActionKind.ENERGY_ARROW, power, durationMultiplier, specialRank,
                        player.position(), forward));
                play(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.55f);
            }

            case ARCANIST_FIRE_ORB -> launchMoving(level, player, MovingKind.FIRE_ORB,
                    new ItemStack(Items.FIRE_CHARGE), 1.15, 72, (11.0f + playerLevel * 0.55f) * power,
                    3.2, specialRank, forward);
            case ARCANIST_FROST_RING -> {
                Vec3 center = aimedGround(player, 10.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,
                        now + Math.max(120, duration), 6.5, power, specialRank, 0));
                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);
            }
            case ARCANIST_CHAIN -> {
                Vec3 center = player.position().add(forward.scale(2.0));
                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,
                        now + Math.max(100, duration), 5.0, power, specialRank, 0));
                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);
            }
            case ARCANIST_NOVA -> {
                Vec3 center = aimedGround(player, 13.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,
                        now + Math.max(80, duration / 2), 9.0, power, specialRank, 0));
                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.65f, 1.3f);
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
                FORTRESS_UNTIL.put(player.getUUID(), now + Math.max(100, duration));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(100, duration), 5 + Math.min(3, specialRank), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(100, duration), 3, false, false, true));
                updateShieldBlocks(level, player, true, 2);
                play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.4f, 0.55f);
            }
            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(160, duration * 2L));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(160, duration * 2), 1, false, false, true));
                updateShieldBlocks(level, player, false, 2);
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
                        player.position(), forward));
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
    }

    private static void tickPlayers(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || VillageRespawnSystem.isDowned(player)) continue;
            UUID id = player.getUUID();
            if (SPIN_UNTIL.getOrDefault(id, 0L) >= now) {
                player.setYRot(player.getYRot() + 34.0f);
                player.setYHeadRot(player.getYRot());
                if (now % 3L == 0L) {
                    player.swing(InteractionHand.MAIN_HAND, true);
                    damageRadius(level, player, player.position(), 4.7, 10,
                            2.4f + VillageCouncilState.levelOf(id) * 0.16f,
                            false, 0.32, 0.05);
                    play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 0.8f + (now % 4) * 0.05f);
                }
            }
            SlamState slam = SLAMS.get(id);
            if (slam != null && now > slam.startedAt() + 5L && (player.onGround() || now > slam.startedAt() + 34L)) {
                groundSlam(level, player, slam.power(), slam.specialRank());
                SLAMS.remove(id);
            }
            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6, 16, 0.38, 0.04, 0.0f);
                    updateShieldBlocks(level, player, true, 2);
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                updateShieldBlocks(level, player, false, 2);
                if (now % 3L == 0L) pushFront(level, player, 4.6, 20, 0.7, 0.08, 1.2f);
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
                    for (Mob target : targetsNear(level, owner, area.center(), area.radius(), 30)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 3, false, false, true));
                        if (now % 20L == 0L) hurt(level, target, 2.0f * area.power());
                    }
                    if (now % 20L == 0L) play(level, area.center(), SoundEvents.GLASS_HIT, 0.55f, 0.62f);
                }
                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(0.18));
                    area.moveTo(next);
                    for (Mob target : targetsNear(level, owner, next, area.radius(), 24)) {
                        Vec3 pull = next.subtract(target.position());
                        Vec3 horizontal = new Vec3(pull.x, 0.0, pull.z);
                        if (horizontal.lengthSqr() > 0.01) horizontal = horizontal.normalize().scale(0.19);
                        target.push(horizontal.x, 0.17, horizontal.z);
                        target.hurtMarked = true;
                        if (now % 15L == 0L) hurt(level, target, 1.6f * area.power());
                    }
                    if (now % 15L == 0L) play(level, next, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.7f, 0.85f);
                }
                case LIGHTNING -> {
                    if (now % 10L == 0L) {
                        List<Mob> targets = targetsNear(level, owner, area.center(), area.radius(), 32);
                        Vec3 strike = targets.isEmpty()
                                ? area.center().add((owner.getRandom().nextDouble() - 0.5) * area.radius(), 0.0,
                                (owner.getRandom().nextDouble() - 0.5) * area.radius())
                                : targets.get(owner.getRandom().nextInt(targets.size())).position();
                        spawnVisualLightning(level, strike);
                        for (Mob target : targets) {
                            if (target.position().distanceToSqr(strike) <= 7.0) {
                                hurt(level, target, 5.0f * area.power());
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
            List<Mob> hits = targetsNear(level, owner, position, moving.radius(), 24);
            boolean impact = !hits.isEmpty() || entity == null || moving.age() >= moving.maxAge();
            if (!impact) continue;
            switch (moving.kind()) {
                case FIRE_ORB -> {
                    for (Mob target : targetsNear(level, owner, position, moving.radius(), 24)) {
                        hurt(level, target, moving.damage());
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100 + moving.specialRank() * 30));
                    }
                    play(level, position, SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.15f);
                }
                case BLADE -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 0.5, 0.05);
                        }
                    }
                }
                case ENERGY_ARROW -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 1.15, 0.16);
                        }
                    }
                    if (moving.age() < moving.maxAge() && entity != null) continue;
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
        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        CHARGE_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        Iterator<Map.Entry<UUID, ShieldBlocks>> shieldIterator = SHIELDS.entrySet().iterator();
        while (shieldIterator.hasNext()) {
            Map.Entry<UUID, ShieldBlocks> entry = shieldIterator.next();
            if (FORTRESS_UNTIL.getOrDefault(entry.getKey(), 0L) >= now
                    || AEGIS_UNTIL.getOrDefault(entry.getKey(), 0L) >= now) continue;
            restoreShield(server, entry.getValue());
            shieldIterator.remove();
        }
    }

    public static void handleArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || VillageCouncilState.roleOf(player.getUUID()).orElse(null) != VillageRole.RANGER) return;
        int bonus = 5;
        long now = player.level().getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) bonus += 10;
        event.setCharge(Math.min(20, event.getCharge() + bonus));
    }

    public static void handleEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || VillageCouncilState.roleOf(player.getUUID()).orElse(null) != VillageRole.RANGER) return;
        aimAssist(level, player, arrow);
        if (spawningGeneratedArrow) return;
        long now = level.getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) < now) return;
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
            VillageRole role = VillageCouncilState.roleOf(attacker.getUUID()).orElse(null);
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                attacker.heal(Math.min(2.5f, event.getAmount() * 0.055f));
            }
            if (role == VillageRole.RANGER
                    && event.getSource().getDirectEntity() instanceof AbstractArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_UNTIL.getOrDefault(attacker.getUUID(), 0L) >= attacker.level().getGameTime()
                    && attacker.level() instanceof ServerLevel level) {
                RICOCHET_UNTIL.remove(attacker.getUUID());
                List<Mob> chain = targetsNear(level, attacker, primary.position(), 10.0, 7);
                chain.remove(primary);
                chain.sort(Comparator.comparingDouble(primary::distanceToSqr));
                float damage = Math.max(2.0f, event.getAmount() * 0.68f);
                for (int i = 0; i < Math.min(5, chain.size()); i++) {
                    Mob target = chain.get(i);
                    hurt(level, target, damage * (1.0f - i * 0.10f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
            }
        }
        if (event.getEntity() instanceof ServerPlayer defender
                && VillageCouncilState.roleOf(defender.getUUID()).orElse(null) == VillageRole.WARDEN) {
            event.setAmount(event.getAmount() * 0.82f);
        }
    }

    public static void handleDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || VillageCouncilState.roleOf(killer.getUUID()).orElse(null) != VillageRole.RANGER
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

    public static boolean isRapidFire(ServerPlayer player) {
        return player != null && RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= player.level().getGameTime();
    }

    public static boolean isFortress(ServerPlayer player) {
        return player != null && FORTRESS_UNTIL.getOrDefault(player.getUUID(), 0L) >= player.level().getGameTime();
    }

    private static void bladeWave(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = horizontalLook(player);
        launchMoving(level, player, MovingKind.BLADE, new ItemStack(Items.IRON_SWORD),
                1.45, 18, (5.2f + VillageCouncilState.levelOf(player.getUUID()) * 0.28f) * power,
                1.35, specialRank, direction);
        play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.78f + player.getRandom().nextFloat() * 0.18f);
    }

    private static void groundSlam(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        damageRadius(level, player, player.position(), 8.5, 32,
                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,
                false, 1.05, 0.38);
        BlockPos center = player.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < 2.0 || distance > 4.5 || (dx + dz) % 2 != 0) continue;
                BlockPos floor = center.offset(dx, -1, dz);
                BlockState state = level.getBlockState(floor);
                if (!state.isAir()) level.levelEvent(2001, floor, Block.getId(state));
            }
        }
        play(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.55f);
        play(level, player.position(), SoundEvents.ANVIL_LAND, 1.1f, 0.62f);
    }

    private static void arrowRain(ServerLevel level, ServerPlayer player, Vec3 center, float power, int specialRank) {
        List<Mob> targets = targetsNear(level, player, center, 8.5, 36);
        int count = Math.min(targets.size(), 5 + specialRank);
        for (int i = 0; i < count; i++) {
            Mob target = targets.get(i);
            hurt(level, target, (4.2f + VillageCouncilState.levelOf(player.getUUID()) * 0.22f) * power);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), specialRank >= 3 ? 40 : 0));
            spawnFallingArrow(level, player, target.position().add(0.0, 10.0, 0.0));
        }
        play(level, center, SoundEvents.ARROW_SHOOT, 0.8f, 1.45f);
    }

    private static void launchEnergyArrow(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.stopUsingItem();
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = horizontalLook(player);
        float damage = (28.0f + VillageCouncilState.levelOf(player.getUUID()) * 1.15f) * power;
        launchMoving(level, player, MovingKind.ENERGY_ARROW, new ItemStack(Items.SPECTRAL_ARROW),
                2.1, 35, damage, 3.6, specialRank, direction);
        for (double offset : new double[]{-0.28, 0.28}) {
            Vec3 side = new Vec3(-direction.z, 0.0, direction.x).scale(offset);
            launchMovingAt(level, player, MovingKind.ENERGY_ARROW, new ItemStack(Items.SPECTRAL_ARROW),
                    2.05, 30, damage * 0.34f, 2.2, specialRank,
                    player.getEyePosition().add(side), direction);
        }
        play(level, player.position(), SoundEvents.ENDER_DRAGON_SHOOT, 1.35f, 0.72f);
    }

    private static void shieldCharge(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
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
        for (Mob target : targetsNear(level, player, player.position(), 10.0, 30)) {
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
        if (target.level() instanceof ServerLevel level) play(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.28f);
    }

    private static void cleanseAllies(ServerPlayer player, float heal, int specialRank) {
        for (ServerPlayer ally : allies(player, -1.0)) {
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.SLOWNESS);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            healScaled(ally, heal);
        }
        if (player.level() instanceof ServerLevel level) play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.35f);
    }

    private static void miracle(ServerPlayer player, float heal, int specialRank, int duration) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (ally.level() != player.level()) continue;
            if (VillageRespawnSystem.isDowned(ally)) VillageRespawnSystem.reviveNow(ally, "기적의 대성역");
            healScaled(ally, heal);
            int amplifier = lowHealthAmplifier(ally, 2 + Math.min(3, specialRank));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
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
        projectile.setItem(item);
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(direction.normalize().scale(speed));
        if (!level.addFreshEntity(projectile)) return;
        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), kind, maxAge,
                damage, radius, specialRank, origin));
    }

    private static void spawnFallingArrow(ServerLevel level, ServerPlayer owner, Vec3 position) {
        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), owner.getMainHandItem());
        arrow.setPos(position.x, position.y, position.z);
        arrow.setDeltaMovement(0.0, -2.4, 0.0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        spawningGeneratedArrow = true;
        try { level.addFreshEntity(arrow); }
        finally { spawningGeneratedArrow = false; }
    }

    private static void spawnSideArrow(ServerLevel level, ServerPlayer owner, AbstractArrow source, double degrees) {
        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), owner.getMainHandItem());
        arrow.setPos(source.getX(), source.getY(), source.getZ());
        Vec3 velocity = rotateY(source.getDeltaMovement(), Math.toRadians(degrees));
        arrow.setDeltaMovement(velocity);
        arrow.setBaseDamage(2.0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        spawningGeneratedArrow = true;
        try { level.addFreshEntity(arrow); }
        finally { spawningGeneratedArrow = false; }
    }

    private static void aimAssist(ServerLevel level, ServerPlayer player, AbstractArrow arrow) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) return;
        Vec3 direction = velocity.normalize();
        Mob target = targetsNear(level, player, player.position(), 30.0, 32).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(arrow.position());
                    return to.lengthSqr() > 0.1 && to.normalize().dot(direction) >= 0.78;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) return;
        Vec3 assisted = target.getEyePosition().subtract(arrow.position()).normalize();
        Vec3 blended = direction.scale(0.62).add(assisted.scale(0.38)).normalize();
        arrow.setDeltaMovement(blended.scale(speed));
        arrow.hurtMarked = true;
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.snapTo(position.x, position.y, position.z);
        level.addFreshEntity(bolt);
    }

    private static void updateShieldBlocks(ServerLevel level, ServerPlayer player, boolean fortress, int distance) {
        UUID id = player.getUUID();
        ShieldBlocks previous = SHIELDS.remove(id);
        if (previous != null) restoreShield(level.getServer(), previous);
        Vec3 forward = horizontalLook(player);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 center = player.position().add(forward.scale(distance));
        int halfWidth = fortress ? 2 : 1;
        int height = fortress ? 4 : 3;
        Map<BlockPos, BlockState> replaced = new LinkedHashMap<>();
        for (int horizontal = -halfWidth; horizontal <= halfWidth; horizontal++) {
            for (int vertical = 0; vertical < height; vertical++) {
                Vec3 point = center.add(right.scale(horizontal)).add(0.0, vertical, 0.0);
                BlockPos pos = BlockPos.containing(point);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) continue;
                replaced.put(pos, state);
                level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
            }
        }
        SHIELDS.put(id, new ShieldBlocks(level, replaced));
    }

    private static void restoreShield(MinecraftServer server, ShieldBlocks shield) {
        if (server == null) return;
        ServerLevel level = shield.level();
        for (Map.Entry<BlockPos, BlockState> entry : shield.replaced().entrySet()) {
            if (level.getBlockState(entry.getKey()).is(Blocks.GLASS)) {
                level.setBlock(entry.getKey(), entry.getValue(), 3);
            }
        }
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

    private static Vec3 aimedGround(ServerPlayer player, double distance) {
        Vec3 look = horizontalLook(player);
        return player.position().add(look.scale(distance));
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

    private record SlamState(long startedAt, float power, int specialRank, Vec3 origin) {}
    private record ShieldBlocks(ServerLevel level, Map<BlockPos, BlockState> replaced) {}
}
