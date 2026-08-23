package kr.moonseungjun.survivalascension.apex;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionIncidentSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ApexHuntSystem {
    public static final int ECHO_SHARD_COST = 8;
    public static final int AMETHYST_COST = 32;
    public static final int GOLD_COST = 32;

    private static final String READY_TICK_KEY = "survivalascension_apex_hunt_ready";
    private static final String INCIDENT_READY_TICK_KEY = "survivalascension_expedition_incident_ready";
    private static final String TRIAL_READY_TICK_KEY = "survivalascension_ascension_trial_ready";
    private static final String APEX_OWNER_KEY = "survivalascension_apex_owner";
    private static final String APEX_TYPE_KEY = "survivalascension_apex_type";

    private static final int HUNT_TIMEOUT_TICKS = 1800;
    private static final int START_COOLDOWN_TICKS = 2400;
    private static final int OWNER_GRACE_TICKS = 200;
    private static final double PLAYER_RADIUS = 64.0D;
    private static final double RECALL_RADIUS = 48.0D;
    private static final double EXCLUSION_RADIUS = 96.0D;

    private static final Identifier HEALTH_ID = id("apex_health");
    private static final Identifier ARMOR_ID = id("apex_armor");
    private static final Identifier ATTACK_ID = id("apex_attack");

    private static final Map<UUID, Hunt> ACTIVE = new HashMap<>();
    private static int ticker;

    private ApexHuntSystem() {}

    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    public static void tryStart(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f크리에이티브/관전자 상태에서는 시작할 수 없습니다."));
            return;
        }
        MinecraftServer server = level.getServer();
        removeStaleServerHunts(server);
        int worldStage = WorldAscensionData.get(server).stage();
        if (worldStage < 1) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f월드 승천 §d1단계§f가 필요합니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.APEX_TRACKING_POST)) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f먼저 §e정점 추적소§f를 완공해야 합니다."));
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f이미 진행 중인 사냥이 있습니다."));
            return;
        }
        if (ExpeditionIncidentSystem.isActive(player)) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f진행 중인 현장 사건을 먼저 끝내세요."));
            return;
        }

        long now = level.getGameTime();
        CompoundTag persistent = player.getPersistentData();
        long trialReady = persistent.getLongOr(TRIAL_READY_TICK_KEY, 0L);
        if (trialReady > now) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f승천 시련 재개방 시간 동안에는 정점 사냥을 열 수 없습니다."));
            return;
        }
        long ready = persistent.getLongOr(READY_TICK_KEY, 0L);
        if (now < ready) {
            long seconds = Math.max(1L, (ready - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f재추적까지 §e" + seconds + "초§f 남았습니다."));
            return;
        }

        ExpeditionRegion region = ExpeditionProgression.currentRegion(player);
        if (region == null || !ExpeditionData.get(player).isComplete(player, region)) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f이미 §a완수한 원정권§f 안에서 추적소를 다시 선택해야 합니다."));
            return;
        }
        ApexArchetype archetype = ApexArchetype.forRegion(region);
        if (archetype == null) return;

        for (Hunt active : ACTIVE.values()) {
            if (active.level == level && active.center.distSqr(player.blockPosition()) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) {
                player.sendSystemMessage(Component.literal("§4[정점 사냥] §f근처에서 다른 정점 사냥이 진행 중입니다. §7(96블록 간격 필요)"));
                return;
            }
        }
        if (!hasCost(player)) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f추적 재료가 부족합니다. §b메아리8 §7· §d자수정32 §7· §6금32"));
            return;
        }

        Hunt hunt = spawnHunt(player, level, archetype);
        if (hunt == null) {
            player.sendSystemMessage(Component.literal("§4[정점 사냥] §f정점 개체를 안전하게 배치할 공간이 부족합니다. 더 열린 지형에서 다시 시도하세요."));
            return;
        }

        consume(player, Items.ECHO_SHARD, ECHO_SHARD_COST);
        consume(player, Items.AMETHYST_SHARD, AMETHYST_COST);
        consume(player, Items.GOLD_INGOT, GOLD_COST);
        persistent.putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        persistent.putLong(INCIDENT_READY_TICK_KEY, Math.max(
                persistent.getLongOr(INCIDENT_READY_TICK_KEY, 0L), now + HUNT_TIMEOUT_TICKS + 200L));

        ACTIVE.put(player.getUUID(), hunt);
        hunt.bossBar.addPlayer(player);
        hunt.bossBar.setVisible(true);
        player.sendSystemMessage(Component.literal("§4[정점 사냥] §f" + region.koreanName() + " · §e" + archetype.koreanName()
                + "§f 출현. §7보스의 행동 전조와 호위 조합을 읽고 90초 안에 격파하세요."));
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        removeStaleServerHunts(event.getServer());
        if (++ticker < 5) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Hunt> entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (tickHunt(event.getServer(), entry.getValue())) finished.add(entry.getKey());
        }
        for (UUID owner : finished) ACTIVE.remove(owner);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Hunt hunt = ACTIVE.remove(event.getEntity().getUUID());
        if (hunt != null) {
            cleanupMobs(hunt);
            closeBossBar(hunt);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        String ownerText = mob.getPersistentData().getStringOr(APEX_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;
        UUID owner;
        try {
            owner = UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
            return;
        }
        Hunt hunt = ACTIVE.get(owner);
        if (hunt == null || hunt.level != level || !hunt.mobIds.contains(mob.getUUID())) event.setCanceled(true);
    }

    private static Hunt spawnHunt(ServerPlayer owner, ServerLevel level, ApexArchetype archetype) {
        BlockPos center = owner.blockPosition();
        Hunt hunt = new Hunt(owner.getUUID(), level, center, archetype, level.getGameTime() + HUNT_TIMEOUT_TICKS);

        Mob boss = spawnOne(level, center, archetype.aquatic(), archetype == ApexArchetype.END_HARBINGER,
                archetype.bossTypeId(), 0, Math.max(1, archetype.escortCount() + 1));
        if (boss == null) return null;
        markMob(boss, hunt);
        hunt.bossId = boss.getUUID();
        hunt.mobIds.add(boss.getUUID());
        applyBossStats(boss, archetype);
        boss.setTarget(owner);

        int added = 0;
        for (int i = 0; i < archetype.escortCount(); i++) {
            String typeId = archetype.escortTypeIds().get(i % archetype.escortTypeIds().size());
            Mob escort = spawnOne(level, center, archetype.aquatic(), false, typeId, i + 1, archetype.escortCount() + 1);
            if (escort == null) continue;
            markMob(escort, hunt);
            escort.setTarget(owner);
            hunt.mobIds.add(escort.getUUID());
            added++;
        }
        if (added < Math.max(2, archetype.escortCount() / 2)) {
            cleanupMobs(hunt);
            return null;
        }
        hunt.initialEscortCount = added;
        return hunt;
    }

    private static boolean tickHunt(MinecraftServer server, Hunt hunt) {
        long now = hunt.level.getGameTime();
        ServerPlayer owner = server.getPlayerList().getPlayer(hunt.owner);
        boolean ownerValid = owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == hunt.level && distanceToCenterSqr(owner, hunt.center) <= PLAYER_RADIUS * PLAYER_RADIUS
                && ExpeditionProgression.currentRegion(owner) == hunt.archetype.region();
        if (ownerValid) hunt.ownerAbsentTicks = 0;
        else hunt.ownerAbsentTicks += 5;
        syncBossBarPlayers(server, hunt);

        if (hunt.ownerAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(hunt, owner, "소유자가 사냥권에서 이탈하거나 사망했습니다.");
            return true;
        }
        if (now >= hunt.deadline) {
            fail(hunt, owner, "90초 제한시간을 초과했습니다.");
            return true;
        }

        Entity bossEntity = hunt.level.getEntity(hunt.bossId);
        if (!(bossEntity instanceof Mob boss) || !boss.isAlive()) {
            complete(hunt, owner);
            return true;
        }
        pruneAndRecall(hunt, owner, boss);
        if (owner != null) runPattern(hunt, boss, owner, now);

        int seconds = Math.max(0, (int) ((hunt.deadline - now + 19L) / 20L));
        hunt.bossBar.setName(Component.literal("§4정점 사냥 §7[" + hunt.archetype.koreanName() + "] §f"
                + Math.max(0, (int) Math.ceil(boss.getHealth())) + "/" + (int) Math.ceil(boss.getMaxHealth())
                + " §7· 호위 " + Math.max(0, hunt.mobIds.size() - 1) + " · " + seconds + "초"));
        hunt.bossBar.setProgress(Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F));
        return false;
    }

    private static void runPattern(Hunt hunt, Mob boss, ServerPlayer owner, long now) {
        double distance = boss.distanceToSqr(owner);

        if (hunt.chargeExecuteTick > 0L && now >= hunt.chargeExecuteTick) {
            hunt.chargeExecuteTick = 0L;
            Vec3 toward = horizontalToward(boss, owner);
            if (toward.lengthSqr() > 0.0D) {
                boss.setDeltaMovement(toward.x * 1.15D, Math.max(0.12D, boss.getDeltaMovement().y), toward.z * 1.15D);
                boss.hurtMarked = true;
                hunt.chargeImpactUntil = now + 20L;
            }
        }
        if (hunt.chargeImpactUntil > now && distance <= 16.0D) {
            Vec3 away = owner.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-5D) {
                away = away.normalize();
                owner.setDeltaMovement(owner.getDeltaMovement().add(away.x * 1.15D, 0.35D, away.z * 1.15D));
                owner.hurtMarked = true;
            }
            hunt.chargeImpactUntil = 0L;
        }
        if (now < hunt.patternReadyTick) return;

        switch (hunt.archetype.pattern()) {
            case CHARGE -> {
                if (distance >= 25.0D && distance <= 324.0D) {
                    hunt.chargeExecuteTick = now + 20L;
                    hunt.patternReadyTick = now + 90L;
                    owner.sendSystemMessage(Component.literal("§c[정점 전조] §f수림 파쇄자가 정면 돌진을 준비합니다."), true);
                }
            }
            case REINFORCE -> {
                double ratio = boss.getHealth() / Math.max(1.0F, boss.getMaxHealth());
                if (!hunt.phaseOneTriggered && ratio <= 0.70D) {
                    hunt.phaseOneTriggered = true;
                    addReinforcements(hunt, owner, 2);
                    owner.sendSystemMessage(Component.literal("§6[정점 전조] §f황야 지휘관이 1차 증원을 호출했습니다."), true);
                } else if (!hunt.phaseTwoTriggered && ratio <= 0.35D) {
                    hunt.phaseTwoTriggered = true;
                    addReinforcements(hunt, owner, 2);
                    owner.sendSystemMessage(Component.literal("§6[정점 전조] §f황야 지휘관이 최후 증원을 호출했습니다."), true);
                }
                boss.getNavigation().moveTo(owner, 1.20D);
                hunt.patternReadyTick = now + 25L;
            }
            case PLAGUE -> {
                if (distance <= 64.0D) {
                    owner.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    boss.heal(Math.max(2.0F, boss.getMaxHealth() * 0.04F));
                    owner.sendSystemMessage(Component.literal("§2[정점 전조] §f역병핵의 독기 안에서 보스가 체력을 흡수합니다."), true);
                }
                hunt.patternReadyTick = now + 100L;
            }
            case SKIRMISH -> {
                Vec3 away = boss.position().subtract(owner.position()).multiply(1.0D, 0.0D, 1.0D);
                if (away.lengthSqr() > 1.0E-5D) {
                    away = away.normalize();
                    double sign = hunt.level.getRandom().nextBoolean() ? 1.0D : -1.0D;
                    Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(sign * 0.50D);
                    Vec3 correction = distance < 36.0D ? away.scale(0.35D) : (distance > 196.0D ? away.scale(-0.30D) : Vec3.ZERO);
                    Vec3 impulse = side.add(correction);
                    boss.setDeltaMovement(boss.getDeltaMovement().add(impulse.x, 0.05D, impulse.z));
                    boss.hurtMarked = true;
                }
                hunt.patternReadyTick = now + 55L;
            }
            case PULL -> {
                if (distance >= 25.0D && distance <= 256.0D) {
                    Vec3 toward = boss.position().subtract(owner.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (toward.lengthSqr() > 1.0E-5D) {
                        toward = toward.normalize();
                        owner.setDeltaMovement(owner.getDeltaMovement().add(toward.x * 0.42D, 0.06D, toward.z * 0.42D));
                        owner.hurtMarked = true;
                        owner.sendSystemMessage(Component.literal("§3[정점 전조] §f심해 압제자가 사냥감을 사거리 안으로 끌어당깁니다."), true);
                    }
                }
                hunt.patternReadyTick = now + 80L;
            }
            case LEAP -> {
                if (distance >= 16.0D && distance <= 196.0D) {
                    Vec3 toward = horizontalToward(boss, owner);
                    boss.setDeltaMovement(toward.x * 0.85D, 0.42D, toward.z * 0.85D);
                    boss.hurtMarked = true;
                    owner.sendSystemMessage(Component.literal("§8[정점 전조] §f심층 추적자가 도약합니다."), true);
                }
                hunt.patternReadyTick = now + 60L;
            }
            case FROST -> {
                if (distance <= 100.0D) {
                    owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 1));
                    owner.sendSystemMessage(Component.literal("§b[정점 전조] §f빙설 감시자의 냉기장이 기동을 묶습니다."), true);
                }
                hunt.patternReadyTick = now + 100L;
            }
            case WITHER -> {
                if (distance <= 81.0D) {
                    owner.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0));
                    Vec3 away = owner.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-5D) {
                        away = away.normalize();
                        owner.setDeltaMovement(owner.getDeltaMovement().add(away.x * 0.35D, 0.14D, away.z * 0.35D));
                        owner.hurtMarked = true;
                    }
                    owner.sendSystemMessage(Component.literal("§5[정점 전조] §f네더 약탈자가 쇠약 파동을 방출합니다."), true);
                }
                hunt.patternReadyTick = now + 120L;
            }
            case VOID -> {
                if (distance <= 100.0D) {
                    owner.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 0));
                    boss.getNavigation().moveTo(owner, 1.45D);
                    owner.sendSystemMessage(Component.literal("§5[정점 전조] §f공허 전조자가 발밑의 중력을 끊습니다."), true);
                }
                hunt.patternReadyTick = now + 120L;
            }
        }
    }

    private static void addReinforcements(Hunt hunt, ServerPlayer owner, int requested) {
        for (int i = 0; i < requested; i++) {
            List<String> types = hunt.archetype.escortTypeIds();
            String typeId = types.get(hunt.level.getRandom().nextInt(types.size()));
            Mob mob = spawnOne(hunt.level, hunt.center, hunt.archetype.aquatic(), false,
                    typeId, 20 + i + hunt.mobIds.size(), Math.max(4, requested + hunt.mobIds.size()));
            if (mob == null) continue;
            markMob(mob, hunt);
            mob.setTarget(owner);
            hunt.mobIds.add(mob.getUUID());
        }
    }

    private static void pruneAndRecall(Hunt hunt, ServerPlayer owner, Mob boss) {
        Set<UUID> alive = new HashSet<>();
        for (UUID id : hunt.mobIds) {
            Entity entity = hunt.level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            alive.add(id);
            if (owner != null && mob.getTarget() == null) mob.setTarget(owner);
            if (mob != boss && distanceToCenterSqr(mob, hunt.center) > RECALL_RADIUS * RECALL_RADIUS) {
                mob.getNavigation().moveTo(hunt.center.getX() + 0.5D, hunt.center.getY(), hunt.center.getZ() + 0.5D, 1.20D);
            }
        }
        hunt.mobIds.clear();
        hunt.mobIds.addAll(alive);
    }

    private static void complete(Hunt hunt, ServerPlayer owner) {
        if (owner != null) {
            int stage = WorldAscensionData.get(hunt.level.getServer()).stage();
            boolean mythic = stage >= 2 && hunt.level.getRandom().nextDouble() < 0.20D;
            giveOrDrop(owner, AscensionAffixes.createEliteDrop(hunt.level.getRandom(), mythic ? 3 : 2));
            if (stage >= 2) {
                giveOrDrop(owner, new ItemStack(Items.DIAMOND, 3));
                giveOrDrop(owner, new ItemStack(Items.ECHO_SHARD, 6));
                giveOrDrop(owner, new ItemStack(Items.NETHERITE_SCRAP, 1));
                owner.giveExperiencePoints(180);
            } else {
                giveOrDrop(owner, new ItemStack(Items.DIAMOND, 2));
                giveOrDrop(owner, new ItemStack(Items.ECHO_SHARD, 4));
                owner.giveExperiencePoints(120);
            }

            ApexHuntData data = ApexHuntData.get(owner);
            boolean first = data.recordVictory(owner, hunt.archetype);
            owner.sendSystemMessage(Component.literal("§a[정점 격파] §f" + hunt.archetype.koreanName()
                    + " §7· 승천 II 장비 이상 1개 · 정점 도감 " + data.uniqueDefeated(owner) + "/9 · 총 " + data.victories(owner) + "승"
                    + (first ? " §e· 최초 격파" : "")));

            if (data.claimMasteryReward(owner)) {
                giveOrDrop(owner, AscensionAffixes.createEliteDrop(hunt.level.getRandom(), 3));
                giveOrDrop(owner, new ItemStack(Items.NETHERITE_SCRAP, 4));
                giveOrDrop(owner, new ItemStack(Items.ECHO_SHARD, 32));
                giveOrDrop(owner, new ItemStack(Items.DRAGON_BREATH, 16));
                owner.giveExperiencePoints(500);
                owner.sendSystemMessage(Component.literal("§6[정점 사냥 완주] §f9개 원정권의 정점 강적 최초 격파 완료"
                        + " §7· 신화 III 1개 · 네더라이트 파편4 · 메아리32 · 드래곤의 숨결16 · 경험치500"));
            }
        }
        for (ServerPlayer player : hunt.level.getServer().getPlayerList().getPlayers()) {
            if (player == owner || player.level() != hunt.level || !player.isAlive() || player.isSpectator()) continue;
            if (distanceToCenterSqr(player, hunt.center) <= 48.0D * 48.0D) {
                player.giveExperiencePoints(50);
                player.sendSystemMessage(Component.literal("§4[정점 사냥] §f협동 격파 보상 경험치 §e+50"));
            }
        }
        cleanupMobs(hunt);
        closeBossBar(hunt);
    }

    private static void fail(Hunt hunt, ServerPlayer owner, String reason) {
        cleanupMobs(hunt);
        closeBossBar(hunt);
        if (owner != null) owner.sendSystemMessage(Component.literal("§c[정점 사냥 실패] §f" + hunt.archetype.koreanName()
                + " · " + reason + " §7· 추적 재료는 반환되지 않습니다."));
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, boolean water, boolean tall,
                                String typeId, int index, int count) {
        Identifier identifier = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) return null;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.31D) / Math.max(1, count);
            int radius = 8 + level.getRandom().nextInt(7);
            BlockPos base = center.offset((int) Math.round(Math.cos(angle) * radius), 0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = water ? findWaterSpawn(level, base) : findOpenSpawn(level, base, tall);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base, boolean tall) {
        for (int dy = 5; dy >= -6; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (tall && !level.getBlockState(pos.above(2)).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static BlockPos findWaterSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 4; dy >= -10; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (level.getFluidState(pos).isEmpty() || level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static void markMob(Mob mob, Hunt hunt) {
        mob.setPersistenceRequired();
        mob.getPersistentData().putString(APEX_OWNER_KEY, hunt.owner.toString());
        mob.getPersistentData().putString(APEX_TYPE_KEY, hunt.archetype.name());
    }

    private static void applyBossStats(Mob boss, ApexArchetype archetype) {
        addPermanent(boss.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, archetype.healthBonus(), AttributeModifier.Operation.ADD_VALUE);
        addPermanent(boss.getAttribute(Attributes.ARMOR), ARMOR_ID, archetype.armorBonus(), AttributeModifier.Operation.ADD_VALUE);
        addPermanent(boss.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, archetype.attackBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        boss.setHealth(boss.getMaxHealth());
        boss.setCustomName(Component.literal("§4[정점] §e" + archetype.koreanName()));
        boss.setCustomNameVisible(true);
    }

    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null || amount == 0.0D || attribute.hasModifier(id)) return;
        attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static void syncBossBarPlayers(MinecraftServer server, Hunt hunt) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == hunt.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, hunt.center) <= PLAYER_RADIUS * PLAYER_RADIUS) {
                shouldSee.add(player);
                if (!hunt.bossBar.getPlayers().contains(player)) hunt.bossBar.addPlayer(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(hunt.bossBar.getPlayers())) {
            if (!shouldSee.contains(viewer)) hunt.bossBar.removePlayer(viewer);
        }
    }

    private static void removeStaleServerHunts(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Hunt> entry : ACTIVE.entrySet()) {
            Hunt hunt = entry.getValue();
            if (hunt.level.getServer() == server) continue;
            cleanupMobs(hunt);
            closeBossBar(hunt);
            stale.add(entry.getKey());
        }
        for (UUID owner : stale) ACTIVE.remove(owner);
    }

    private static void cleanupMobs(Hunt hunt) {
        for (UUID id : hunt.mobIds) {
            Entity entity = hunt.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        hunt.mobIds.clear();
    }

    private static void closeBossBar(Hunt hunt) {
        hunt.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(hunt.bossBar.getPlayers())) hunt.bossBar.removePlayer(viewer);
    }

    private static boolean hasCost(ServerPlayer player) {
        return count(player, Items.ECHO_SHARD) >= ECHO_SHARD_COST
                && count(player, Items.AMETHYST_SHARD) >= AMETHYST_COST
                && count(player, Items.GOLD_INGOT) >= GOLD_COST;
    }

    private static int count(ServerPlayer player, Item item) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) found += stack.getCount();
        }
        return found;
    }

    private static void consume(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static Vec3 horizontalToward(Entity from, Entity to) {
        Vec3 toward = to.position().subtract(from.position()).multiply(1.0D, 0.0D, 1.0D);
        return toward.lengthSqr() <= 1.0E-5D ? Vec3.ZERO : toward.normalize();
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dy = entity.getY() - (center.getY() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path); }

    private static final class Hunt {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ApexArchetype archetype;
        final long deadline;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        UUID bossId;
        int initialEscortCount;
        int ownerAbsentTicks;
        long patternReadyTick;
        long chargeExecuteTick;
        long chargeImpactUntil;
        boolean phaseOneTriggered;
        boolean phaseTwoTriggered;

        Hunt(UUID owner, ServerLevel level, BlockPos center, ApexArchetype archetype, long deadline) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.archetype = archetype;
            this.deadline = deadline;
            this.patternReadyTick = level.getGameTime() + 40L;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("§4정점 사냥 §7[" + archetype.koreanName() + "]"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
        }
    }
}
