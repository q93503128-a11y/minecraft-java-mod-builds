package kr.moonseungjun.survivalascension.elite;

/*
 * Rank-driven permanent attribute construction is adapted from Mob Champions.
 * Copyright (c) 2024 Wendall Cada, MIT License.
 * Survival Ascension uses its own rank probabilities, traits, progression coupling, reactions and rewards.
 */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.endgame.FinalAscensionBossSystem;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.network.MythicTargetPayload;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillType;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EliteMobSystem {
    private static final String RANK_KEY = "survivalascension_elite_rank";
    private static final String TRAIT_KEY = "survivalascension_elite_trait";
    private static final String REACTION_READY_KEY = "survivalascension_elite_reaction_ready";
    private static final String MYTHIC_PHASE_KEY = "survivalascension_mythic_phase";
    private static final double MYTHIC_ALERT_RADIUS = 192.0D;
    private static final double MYTHIC_BOSSBAR_RADIUS = 128.0D;
    private static final double MYTHIC_REWARD_RADIUS = 48.0D;

    private static final Identifier HEALTH_ID = id("elite_health");
    private static final Identifier ARMOR_ID = id("elite_armor");
    private static final Identifier SPEED_ID = id("elite_speed");
    private static final Identifier ATTACK_ID = id("elite_attack");
    private static final Identifier KNOCKBACK_ID = id("elite_knockback");
    private static final Identifier TRAIT_SPEED_ID = id("elite_trait_speed");
    private static final Identifier TRAIT_ARMOR_ID = id("elite_trait_armor");
    private static final Identifier TRAIT_ATTACK_ID = id("elite_trait_attack");
    private static final Identifier TRAIT_KNOCKBACK_ID = id("elite_trait_knockback");
    private static final Identifier MYTHIC_COOP_HEALTH_ID = id("mythic_coop_health");
    private static final Map<UUID, MythicRuntime> MYTHICS = new HashMap<>();
    private static int mythicTicker;

    private EliteMobSystem() {}

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (FinalAscensionBossSystem.isInternalSpawn()) return;
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob instanceof EnderDragon || mob instanceof WitherBoss || mob.isBaby()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (isElite(mob)) return;
        if (event.getSpawnType().name().contains("SPAWNER")) return;

        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(96.0D),
                player -> !player.isSpectator());
        if (nearby.isEmpty()) return;

        double power = nearby.stream().mapToDouble(EliteMobSystem::averageSkillLevel).average().orElse(0.0D);
        int worldStage = WorldAscensionData.get(level.getServer()).stage();
        RandomSource random = level.getRandom();
        double eliteChance = Math.min(0.28D, 0.025D + power * 0.00135D + worldStage * 0.04D);
        if (random.nextDouble() >= eliteChance) return;

        Rank rank = chooseRank(random, power, worldStage);
        Trait trait = Trait.values()[random.nextInt(Trait.values().length)];
        applyElite(mob, rank, trait, nearby.size());

        if (rank == Rank.MYTHIC_III) {
            MythicRuntime runtime = ensureMythicRuntime(mob);
            for (ServerPlayer viewer : playersNear(level, mob, MYTHIC_ALERT_RADIUS)) {
                runtime.contributors.add(viewer.getUUID());
                viewer.sendSystemMessage(Component.literal("§6§l[신화 III 출현] §r§f" + mob.getName().getString()
                        + " §7· §e" + directionLabel(viewer, mob) + " " + (int)Math.round(Math.sqrt(viewer.distanceToSqr(mob))) + "m"
                        + " §7· §6상단 방향 화살표로 추적됩니다."));
            }
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel && rank(mob) == Rank.MYTHIC_III) {
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            ensureMythicRuntime(mob);
        }
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++mythicTicker < 10) return;
        mythicTicker = 0;
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, MythicRuntime> entry : new ArrayList<>(MYTHICS.entrySet())) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level.getServer() != event.getServer()) {
                closeMythicBar(runtime);
                remove.add(entry.getKey());
                continue;
            }
            Entity entity = runtime.level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || rank(mob) != Rank.MYTHIC_III) {
                closeMythicBar(runtime);
                remove.add(entry.getKey());
                continue;
            }
            mob.setGlowingTag(true);
            mob.setPersistenceRequired();
            syncMythicBossBar(runtime, mob);
            int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
            if (phase >= 1) mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, phase >= 2 ? 1 : 0, true, false));
            if (phase >= 2) {
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 1, true, false));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false));
            }
            if (runtime.level.getGameTime() % 20L == 0L) {
                runtime.level.sendParticles(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.TOTEM_OF_UNDYING,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(), phase >= 2 ? 22 : 12,
                        0.55D, 0.8D, 0.55D, 0.02D);
            }
        }
        for (UUID id : remove) MYTHICS.remove(id);
        if (event.getServer().getTickCount() % 20 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) syncMythicTracker(player);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        for (MythicRuntime runtime : new ArrayList<>(MYTHICS.values())) {
            if (runtime.level.getServer() == event.getServer()) closeMythicBar(runtime);
        }
        MYTHICS.entrySet().removeIf(entry -> entry.getValue().level.getServer() == event.getServer());
        mythicTicker = 0;
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !isElite(attacker)) return;
        if (trait(attacker) != Trait.BERSERKER || attacker.getHealth() > attacker.getMaxHealth() * 0.5F) return;
        float multiplier = switch (rank(attacker)) {
            case ELITE_I -> 1.25F;
            case ASCENDED_II -> 1.40F;
            case MYTHIC_III -> 1.60F;
            default -> 1.0F;
        };
        event.setNewDamage(event.getNewDamage() * multiplier);
    }

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Mob attacker
                && event.getEntity() instanceof ServerPlayer
                && isElite(attacker)
                && trait(attacker) == Trait.VAMPIRIC
                && event.getHealthDamage() > 0.0F
                && attacker.isAlive()) {
            float fraction = switch (rank(attacker)) {
                case ELITE_I -> 0.18F;
                case ASCENDED_II -> 0.28F;
                case MYTHIC_III -> 0.48F;
                default -> 0.0F;
            };
            float heal = Math.min(attacker.getMaxHealth() * 0.10F, event.getHealthDamage() * fraction);
            if (heal > 0.0F) attacker.heal(heal);
        }

        if (event.getEntity() instanceof Mob defender
                && defender.level() instanceof ServerLevel level
                && isElite(defender)
                && defender.isAlive()
                && event.getHealthDamage() > 0.0F) {
            ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                    ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
            if (player != null) {
                reactToPlayerHit(defender, player);
                if (rank(defender) == Rank.MYTHIC_III) {
                    ensureMythicRuntime(defender).contributors.add(player.getUUID());
                    updateMythicPhase(defender);
                }
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isElite(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        Rank rank = rank(event.getEntity());

        if (rank == Rank.MYTHIC_III && event.getEntity() instanceof Mob mob) {
            MythicRuntime runtime = MYTHICS.remove(mob.getUUID());
            Set<UUID> recipients = new HashSet<>();
            if (runtime != null) recipients.addAll(runtime.contributors);
            if (killer != null) recipients.add(killer.getUUID());
            for (ServerPlayer player : playersNear(level, mob, MYTHIC_REWARD_RADIUS)) recipients.add(player.getUUID());
            if (runtime != null) closeMythicBar(runtime);
            dropRankReward(level, mob, rank);
            for (UUID id : recipients) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player == null || player.level() != level) continue;
                player.giveExperiencePoints(90);
                giveOrDrop(player, new ItemStack(Items.DIAMOND, 1));
                giveOrDrop(player, new ItemStack(Items.EMERALD, 2 + level.getRandom().nextInt(3)));
                giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 1));
                player.sendSystemMessage(Component.literal("§6[신화 공동 격파] §f경험치 §e+90 §7· 다이아1 · 에메랄드2~4 · 메아리1"));
            }
            return;
        }

        if (killer == null) return;
        int vanillaXp = switch (rank) {
            case ELITE_I -> 8;
            case ASCENDED_II -> 24;
            case MYTHIC_III -> 60;
            default -> 0;
        };
        if (vanillaXp > 0) killer.giveExperiencePoints(vanillaXp);
        if (event.getEntity() instanceof Mob mob) dropRankReward(level, mob, rank);
    }

    public static boolean isElite(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0) > 0;
    }

    public static int rankId(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0);
    }

    public static int spawnTestMythic(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie"));
        if (type == null) return 0;
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 10);
        Entity entity = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            player.sendSystemMessage(Component.literal("§c[신화 테스트] §f좀비 생성에 실패했습니다."));
            return 0;
        }
        applyElite(mob, Rank.MYTHIC_III, Trait.SWIFT, 1);
        syncMythicTracker(player);
        player.sendSystemMessage(Component.literal("§6[신화 테스트] §f정면 약 10블록에 신화 III 좀비를 소환했습니다."));
        return 1;
    }

    private static void reactToPlayerHit(Mob defender, ServerPlayer player) {
        if (!(defender.level() instanceof ServerLevel level)) return;
        CompoundTag data = defender.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLongOr(REACTION_READY_KEY, 0L)) return;

        Rank rank = rank(defender);
        Trait trait = trait(defender);
        int cooldown = switch (rank) {
            case ELITE_I -> 60;
            case ASCENDED_II -> 45;
            case MYTHIC_III -> 30;
            default -> 80;
        };
        boolean reacted = false;
        Vec3 horizontal = defender.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > 1.0E-5D) horizontal = horizontal.normalize();

        switch (trait) {
            case SWIFT -> {
                if (horizontal.lengthSqr() > 0.0D) {
                    double sideSign = level.getRandom().nextBoolean() ? 1.0D : -1.0D;
                    Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x).scale(sideSign);
                    double sidePower = switch (rank) { case ELITE_I -> 0.42D; case ASCENDED_II -> 0.55D; case MYTHIC_III -> 0.70D; default -> 0.0D; };
                    Vec3 impulse = horizontal.scale(0.20D).add(side.scale(sidePower));
                    defender.setDeltaMovement(impulse.x, Math.max(0.08D, defender.getDeltaMovement().y), impulse.z);
                    defender.hurtMarked = true;
                    reacted = true;
                }
            }
            case BULWARK -> {
                Vec3 push = player.position().subtract(defender.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-5D) {
                    push = push.normalize();
                    double power = switch (rank) { case ELITE_I -> 0.65D; case ASCENDED_II -> 0.90D; case MYTHIC_III -> 1.15D; default -> 0.0D; };
                    player.setDeltaMovement(player.getDeltaMovement().add(push.x * power, 0.24D, push.z * power));
                    player.hurtMarked = true;
                    reacted = true;
                }
            }
            case BERSERKER -> {
                if (defender.getHealth() <= defender.getMaxHealth() * 0.5F && horizontal.lengthSqr() > 0.0D) {
                    Vec3 toward = horizontal.scale(-1.0D);
                    double power = switch (rank) { case ELITE_I -> 0.50D; case ASCENDED_II -> 0.68D; case MYTHIC_III -> 0.85D; default -> 0.0D; };
                    defender.setDeltaMovement(toward.x * power, Math.max(0.08D, defender.getDeltaMovement().y), toward.z * power);
                    defender.hurtMarked = true;
                    reacted = true;
                }
            }
            case VAMPIRIC -> { }
        }
        if (reacted) data.putLong(REACTION_READY_KEY, now + cooldown);
    }

    private static void dropRankReward(ServerLevel level, Mob mob, Rank rank) {
        RandomSource random = level.getRandom();
        switch (rank) {
            case ELITE_I -> spawnItem(level, mob, new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(3)));
            case ASCENDED_II -> spawnItem(level, mob, new ItemStack(Items.EMERALD, 1 + random.nextInt(2)));
            case MYTHIC_III -> {
                spawnItem(level, mob, new ItemStack(Items.DIAMOND, 1));
                spawnItem(level, mob, new ItemStack(Items.EMERALD, 2 + random.nextInt(3)));
            }
            default -> { }
        }
    }

    private static void spawnItem(ServerLevel level, Mob mob, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), stack));
    }

    private static double averageSkillLevel(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        int total = 0;
        for (SkillType skill : SkillType.values()) total += data.level(player, skill);
        return total / (double) SkillType.values().length;
    }

    private static Rank chooseRank(RandomSource random, double power, int worldStage) {
        double roll = random.nextDouble();
        double mythicChance = Math.min(0.08D, 0.004D + power * 0.00035D + worldStage * 0.012D);
        double ascendedChance = Math.min(0.52D, 0.11D + power * 0.0032D + worldStage * 0.05D);
        if (roll < mythicChance) return Rank.MYTHIC_III;
        if (roll < mythicChance + ascendedChance) return Rank.ASCENDED_II;
        return Rank.ELITE_I;
    }

    private static void applyElite(Mob mob, Rank rank, Trait trait, int nearbyPlayers) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(RANK_KEY, rank.id);
        data.putString(TRAIT_KEY, trait.id);

        addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, rank.healthBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ARMOR), ARMOR_ID, rank.armorBonus, AttributeModifier.Operation.ADD_VALUE);
        addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, rank.speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, rank.attackBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, rank.knockbackBonus, AttributeModifier.Operation.ADD_VALUE);
        if (rank == Rank.MYTHIC_III) {
            int extraPlayers = Math.min(4, Math.max(0, nearbyPlayers - 1));
            addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), MYTHIC_COOP_HEALTH_ID, extraPlayers * 0.70D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
        }

        switch (trait) {
            case SWIFT -> addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), TRAIT_SPEED_ID,
                    switch (rank) { case ELITE_I -> 0.12D; case ASCENDED_II -> 0.18D; case MYTHIC_III -> 0.34D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BULWARK -> {
                addPermanent(mob.getAttribute(Attributes.ARMOR), TRAIT_ARMOR_ID,
                        switch (rank) { case ELITE_I -> 2.0D; case ASCENDED_II -> 4.0D; case MYTHIC_III -> 12.0D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
                addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TRAIT_KNOCKBACK_ID,
                        switch (rank) { case ELITE_I -> 0.10D; case ASCENDED_II -> 0.20D; case MYTHIC_III -> 0.55D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
            }
            case BERSERKER -> addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), TRAIT_ATTACK_ID,
                    switch (rank) { case ELITE_I -> 0.08D; case ASCENDED_II -> 0.15D; case MYTHIC_III -> 0.45D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case VAMPIRIC -> { }
        }

        mob.setHealth(mob.getMaxHealth());
        if (mob.getCustomName() == null) {
            String prefix = switch (rank) {
                case ELITE_I -> "§b[정예 I] ";
                case ASCENDED_II -> "§d[승천 II] ";
                case MYTHIC_III -> "§6§l[신화 III] §r";
                default -> "";
            };
            mob.setCustomName(Component.literal(prefix + trait.koreanName + " " + mob.getName().getString()));
            mob.setCustomNameVisible(rank == Rank.MYTHIC_III);
        }
        if (rank == Rank.MYTHIC_III) ensureMythicRuntime(mob);
    }

    private static MythicRuntime ensureMythicRuntime(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) throw new IllegalStateException("mythic mob must be server-side");
        UUID id = mob.getUUID();
        MythicRuntime current = MYTHICS.get(id);
        if (current != null && current.level == level) return current;

        // Entity UUIDs survive portal/dimension transfer. A computeIfAbsent here would keep the
        // old ServerLevel forever, so replace that runtime atomically and carry only real credit.
        MythicRuntime replacement = new MythicRuntime(level, mob);
        if (current != null) {
            replacement.contributors.addAll(current.contributors);
            closeMythicBar(current);
        }
        MYTHICS.put(id, replacement);
        return replacement;
    }

    private static void updateMythicPhase(Mob mob) {
        int oldPhase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
        float ratio = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        int newPhase = ratio <= 0.33F ? 2 : ratio <= 0.66F ? 1 : 0;
        if (newPhase <= oldPhase || !(mob.level() instanceof ServerLevel level)) return;
        mob.getPersistentData().putInt(MYTHIC_PHASE_KEY, newPhase);
        level.sendParticles(newPhase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.TOTEM_OF_UNDYING,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(), newPhase >= 2 ? 70 : 45,
                1.2D, 1.0D, 1.2D, 0.08D);
        for (ServerPlayer player : playersNear(level, mob, MYTHIC_BOSSBAR_RADIUS)) {
            player.sendSystemMessage(Component.literal(newPhase >= 2
                    ? "§c§l[신화 폭주] §r§f최종 단계 돌입 · 공격/저항 강화"
                    : "§6[신화 격변] §f2단계 돌입 · 기동 강화"), true);
            if (newPhase >= 2 && player.distanceToSqr(mob) <= 36.0D) {
                Vec3 push = player.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-5D) {
                    push = push.normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(push.x * 0.85D, 0.28D, push.z * 0.85D));
                    player.hurtMarked = true;
                }
            }
        }
    }

    private static List<ServerPlayer> playersNear(ServerLevel level, Entity entity, double radius) {
        double radiusSqr = radius * radius;
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.isAlive() && !player.isSpectator() && player.distanceToSqr(entity) <= radiusSqr) players.add(player);
        }
        return players;
    }

    private static void syncMythicTracker(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            SkillNetwork.sendMythicTarget(player, MythicTargetPayload.clear());
            return;
        }
        double maxDistanceSqr = MYTHIC_ALERT_RADIUS * MYTHIC_ALERT_RADIUS;
        Mob nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (Map.Entry<UUID, MythicRuntime> entry : MYTHICS.entrySet()) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level != level) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || rank(mob) != Rank.MYTHIC_III) continue;
            double distanceSqr = player.distanceToSqr(mob);
            if (distanceSqr > maxDistanceSqr) continue;
            if (nearest == null || distanceSqr < nearestDistanceSqr
                    || (distanceSqr == nearestDistanceSqr && mob.getUUID().compareTo(nearest.getUUID()) < 0)) {
                nearest = mob;
                nearestDistanceSqr = distanceSqr;
            }
        }
        SkillNetwork.sendMythicTarget(player, nearest == null
                ? MythicTargetPayload.clear()
                : MythicTargetPayload.target(nearest.getUUID(), nearest.getX(), nearest.getZ()));
    }

    private static String directionLabel(ServerPlayer player, Entity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        if (Math.abs(dx) > Math.abs(dz) * 2.0D) return dx >= 0 ? "동쪽" : "서쪽";
        if (Math.abs(dz) > Math.abs(dx) * 2.0D) return dz >= 0 ? "남쪽" : "북쪽";
        if (dx >= 0 && dz >= 0) return "남동쪽";
        if (dx >= 0) return "북동쪽";
        if (dz >= 0) return "남서쪽";
        return "북서쪽";
    }

    private static void syncMythicBossBar(MythicRuntime runtime, Mob mob) {
        Set<ServerPlayer> shouldSee = new HashSet<>(playersNear(runtime.level, mob, MYTHIC_BOSSBAR_RADIUS));
        for (ServerPlayer player : shouldSee) {
            runtime.contributors.add(player.getUUID());
            if (!runtime.bossBar.getPlayers().contains(player)) runtime.bossBar.addPlayer(player);
        }
        for (ServerPlayer viewer : List.copyOf(runtime.bossBar.getPlayers())) if (!shouldSee.contains(viewer)) runtime.bossBar.removePlayer(viewer);
        int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0) + 1;
        runtime.bossBar.setName(Component.literal("§6신화 III §7· §f" + trait(mob).koreanName + " §7· 단계 " + phase + "/3"));
        runtime.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()))));
        runtime.bossBar.setVisible(true);
    }

    private static void closeMythicBar(MythicRuntime runtime) {
        runtime.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(runtime.bossBar.getPlayers())) runtime.bossBar.removePlayer(viewer);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static final class MythicRuntime {
        final ServerLevel level;
        final ServerBossEvent bossBar;
        final Set<UUID> contributors = new HashSet<>();

        MythicRuntime(ServerLevel level, Mob mob) {
            this.level = level;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("신화 III"),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
            this.bossBar.setVisible(true);
        }
    }

    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null || amount == 0.0D || attribute.hasModifier(id)) return;
        attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Rank rank(net.minecraft.world.entity.LivingEntity entity) {
        return Rank.fromId(entity.getPersistentData().getIntOr(RANK_KEY, 0));
    }

    private static Trait trait(net.minecraft.world.entity.LivingEntity entity) {
        return Trait.fromId(entity.getPersistentData().getStringOr(TRAIT_KEY, ""));
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path); }

    private enum Rank {
        COMMON(0, 0, 0, 0, 0, 0),
        ELITE_I(1, 0.35D, 2.0D, 0.05D, 0.15D, 0.05D),
        ASCENDED_II(2, 0.85D, 5.0D, 0.10D, 0.35D, 0.15D),
        MYTHIC_III(3, 3.50D, 14.0D, 0.22D, 1.10D, 0.65D);

        final int id;
        final double healthBonus, armorBonus, speedBonus, attackBonus, knockbackBonus;
        Rank(int id, double healthBonus, double armorBonus, double speedBonus, double attackBonus, double knockbackBonus) {
            this.id = id;
            this.healthBonus = healthBonus;
            this.armorBonus = armorBonus;
            this.speedBonus = speedBonus;
            this.attackBonus = attackBonus;
            this.knockbackBonus = knockbackBonus;
        }
        static Rank fromId(int id) {
            for (Rank rank : values()) if (rank.id == id) return rank;
            return COMMON;
        }
    }

    private enum Trait {
        SWIFT("swift", "신속"), BULWARK("bulwark", "철벽"), VAMPIRIC("vampiric", "흡혈"), BERSERKER("berserker", "광전사");
        final String id, koreanName;
        Trait(String id, String koreanName) { this.id = id; this.koreanName = koreanName; }
        static Trait fromId(String id) {
            for (Trait trait : values()) if (trait.id.equals(id)) return trait;
            return SWIFT;
        }
    }
}
