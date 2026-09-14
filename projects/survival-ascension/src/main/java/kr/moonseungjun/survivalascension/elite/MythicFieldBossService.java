package kr.moonseungjun.survivalascension.elite;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Optional content-pack seam for true field bosses.
 *
 * The external mod keeps ownership of models, textures, animation, sounds and native combat AI.
 * Survival Ascension only selects entity types from its data tag, spawns the original entity and
 * attaches bounded tracking/reward/contribution and encounter-defense rules. No external
 * implementation class is linked.
 */
public final class MythicFieldBossService {
    static final String FIELD_BOSS_KEY = "survivalascension_external_field_boss";
    private static final String FIELD_BOSS_PHASE_KEY = "survivalascension_external_field_boss_phase";
    private static final String FIELD_ESCORT_KEY = "survivalascension_field_boss_escort";
    private static final String FIELD_ESCORT_OWNER_KEY = "survivalascension_field_boss_escort_owner";

    private static final TagKey<EntityType<?>> FIELD_BOSS_TYPES = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );

    private static final int TICK_INTERVAL = 10;
    private static final int WARD_MAX_TICKS = 300;
    private static final double NOTIFY_RADIUS = 96.0D;
    private static final double ESCORT_TARGET_RADIUS = 72.0D;
    private static final float BASE_DAMAGE_MULTIPLIER = 0.82F;
    private static final float WARD_DAMAGE_MULTIPLIER = 0.42F;
    private static final float MAX_SINGLE_HIT_SHARE = 0.14F;

    private static final Map<UUID, EncounterRuntime> ACTIVE = new HashMap<>();
    private static boolean internalSpawn;
    private static int ticker;

    private MythicFieldBossService() {}

    public static boolean isInternalSpawn() {
        return internalSpawn;
    }

    public static boolean isMajorTarget(LivingEntity entity) {
        return entity != null && entity.getType().builtInRegistryHolder().is(FIELD_BOSS_TYPES);
    }

    public static boolean isExternalFieldBoss(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBooleanOr(FIELD_BOSS_KEY, false);
    }

    static void markExternalFieldBoss(LivingEntity entity) {
        entity.getPersistentData().putBoolean(FIELD_BOSS_KEY, true);
        if (entity instanceof Mob mob && mob.level() instanceof ServerLevel level) ensureRuntime(level, mob);
    }

    /**
     * Replaces a would-be vanilla Mythic III promotion with an original external boss. The ordinary
     * hostile is discarded only after the replacement has actually spawned and been admitted by the
     * shared Mythic population cap, so missing optional content fails closed without creating a fake boss.
     */
    public static boolean tryReplacePromotion(ServerLevel level, Mob original, int nearbyPlayers) {
        if (level == null || original == null || original.isRemoved()) return false;
        Mob boss = spawnTaggedBoss(level, original.blockPosition(), EntitySpawnReason.TRIGGERED, nearbyPlayers);
        if (boss == null) return false;
        original.discard();
        return true;
    }

    public static int spawnTestFieldBoss(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        List<EntityType<?>> pool = fieldBossTypes();
        if (pool.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "§e[필드보스] §f현재 로드된 콘텐츠 팩에 등록된 필드보스가 없습니다."));
            return 0;
        }

        BlockPos pos = player.blockPosition().relative(player.getDirection(), 10);
        Mob boss = spawnTaggedBoss(level, pos, EntitySpawnReason.COMMAND, 1);
        if (boss == null) {
            player.sendSystemMessage(Component.literal(
                    "§e[필드보스] §f소환 공간 또는 동시 출현 제한 때문에 필드보스를 만들지 못했습니다."));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
                "§4[필드보스] §f정면 약 10블록에 §e" + boss.getName().getString() + "§f을 소환했습니다."));
        return 1;
    }

    /**
     * Field bosses are tougher than legacy Mythic III through bounded mitigation rather than absurd
     * attack values. Burst damage is softened and a single hit cannot erase a large fraction of the
     * encounter. Native armor/resistance from the source mod still applies afterwards.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob boss) || !isExternalFieldBoss(boss) || event.getAmount() <= 0.0F) return;
        if (!(boss.level() instanceof ServerLevel level)) return;

        EncounterRuntime runtime = ensureRuntime(level, boss);
        cleanupEscortRefs(runtime);
        float adjusted = event.getAmount() * BASE_DAMAGE_MULTIPLIER;
        float cap = Math.max(8.0F, boss.getMaxHealth() * MAX_SINGLE_HIT_SHARE);
        adjusted = Math.min(adjusted, cap);
        if (wardActive(runtime, level.getGameTime())) adjusted *= WARD_DAMAGE_MULTIPLIER;
        event.setAmount(Math.max(0.0F, adjusted));
    }

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob boss) || !isExternalFieldBoss(boss)
                || !boss.isAlive() || event.getHealthDamage() <= 0.0F) return;
        if (!(boss.level() instanceof ServerLevel level)) return;

        EncounterRuntime runtime = ensureRuntime(level, boss);
        int oldPhase = boss.getPersistentData().getIntOr(FIELD_BOSS_PHASE_KEY, 0);
        float ratio = boss.getHealth() / Math.max(1.0F, boss.getMaxHealth());
        int targetPhase = ratio <= 0.33F ? 2 : ratio <= 0.66F ? 1 : 0;
        if (targetPhase <= oldPhase) return;

        for (int phase = oldPhase + 1; phase <= targetPhase; phase++) beginWardPhase(runtime, boss, phase);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
        if (isExternalFieldBoss(mob)) {
            EncounterRuntime runtime = ACTIVE.remove(mob.getUUID());
            if (runtime != null) clearEscorts(runtime);
            return;
        }
        if (!mob.getPersistentData().getBooleanOr(FIELD_ESCORT_KEY, false)) return;

        String rawOwner = mob.getPersistentData().getStringOr(FIELD_ESCORT_OWNER_KEY, "");
        try {
            EncounterRuntime runtime = ACTIVE.get(UUID.fromString(rawOwner));
            if (runtime == null) return;
            runtime.escorts.remove(mob.getUUID());
            if (runtime.wardActive && runtime.escorts.isEmpty()) collapseWard(runtime, "호위가 무너지며 방어 공명이 끊겼습니다.");
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        if (isExternalFieldBoss(mob)) {
            ensureRuntime(level, mob);
            return;
        }
        if (!mob.getPersistentData().getBooleanOr(FIELD_ESCORT_KEY, false)) return;
        String rawOwner = mob.getPersistentData().getStringOr(FIELD_ESCORT_OWNER_KEY, "");
        try {
            EncounterRuntime runtime = ACTIVE.get(UUID.fromString(rawOwner));
            if (runtime != null && runtime.level == level) runtime.escorts.add(mob.getUUID());
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++ticker < TICK_INTERVAL) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        MinecraftServer server = event.getServer();
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, EncounterRuntime> entry : new ArrayList<>(ACTIVE.entrySet())) {
            EncounterRuntime runtime = entry.getValue();
            if (runtime.level.getServer() != server) continue;
            Entity entity = runtime.level.getEntity(entry.getKey());
            if (!(entity instanceof Mob boss) || !boss.isAlive() || !isExternalFieldBoss(boss)) {
                clearEscorts(runtime);
                remove.add(entry.getKey());
                continue;
            }

            cleanupEscortRefs(runtime);
            long now = runtime.level.getGameTime();
            if (runtime.wardActive && (runtime.escorts.isEmpty() || now >= runtime.wardUntil)) {
                collapseWard(runtime, runtime.escorts.isEmpty()
                        ? "호위가 무너지며 방어 공명이 끊겼습니다."
                        : "방어 공명이 약해져 본체에 다시 큰 피해를 줄 수 있습니다.");
            }

            if (runtime.wardActive && now % 20L < TICK_INTERVAL) {
                runtime.level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        boss.getX(), boss.getY() + boss.getBbHeight() * 0.55D, boss.getZ(),
                        14, 0.85D, 0.8D, 0.85D, 0.02D);
            }
        }
        for (UUID id : remove) ACTIVE.remove(id);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, EncounterRuntime> entry : ACTIVE.entrySet()) {
            if (entry.getValue().level.getServer() != event.getServer()) continue;
            clearEscorts(entry.getValue());
            remove.add(entry.getKey());
        }
        for (UUID id : remove) ACTIVE.remove(id);
        ticker = 0;
    }

    private static void beginWardPhase(EncounterRuntime runtime, Mob boss, int phase) {
        boss.getPersistentData().putInt(FIELD_BOSS_PHASE_KEY, phase);
        clearEscorts(runtime);

        int nearbyPlayers = nearbyPlayers(runtime.level, boss, NOTIFY_RADIUS).size();
        int summonTarget = phase == 1 ? 2 : 3;
        if (nearbyPlayers >= 3) summonTarget++;
        summonTarget = Math.min(4, summonTarget);

        int worldStage = WorldAscensionData.get(runtime.level.getServer()).stage();
        ServerPlayer target = nearestPlayer(runtime.level, boss, ESCORT_TARGET_RADIUS);
        for (int i = 0; i < summonTarget; i++) {
            String typeId = ContentPackCompatibility.randomIncidentReinforcementId(runtime.level.getRandom(), worldStage);
            if (typeId == null) break;
            Mob escort = spawnEscort(runtime.level, boss, typeId, i, summonTarget, target);
            if (escort != null) runtime.escorts.add(escort.getUUID());
        }

        runtime.wardActive = !runtime.escorts.isEmpty();
        runtime.wardUntil = runtime.level.getGameTime() + WARD_MAX_TICKS;
        runtime.level.sendParticles(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL,
                boss.getX(), boss.getY() + boss.getBbHeight() * 0.55D, boss.getZ(),
                phase >= 2 ? 60 : 42, 1.3D, 1.0D, 1.3D, 0.05D);

        if (runtime.wardActive) {
            notifyNearby(runtime.level, boss, Component.literal(phase >= 2
                    ? "§4§l[필드보스 격변] §r§f마지막 호위 " + runtime.escorts.size()
                    + "체가 결집했습니다. §c호위를 끊어 방어 공명을 무너뜨리세요."
                    : "§6§l[필드보스 격변] §r§f호위 " + runtime.escorts.size()
                    + "체가 소환되었습니다. §e호위가 살아 있는 동안 본체 피해가 크게 줄어듭니다."));
        } else {
            notifyNearby(runtime.level, boss, Component.literal(
                    "§6[필드보스 격변] §f공명이 일었지만 호위가 정착하지 못해 본체의 방어만 강화됩니다."));
        }
    }

    private static Mob spawnEscort(ServerLevel level, Mob boss, String typeId, int index, int count, ServerPlayer target) {
        Identifier identifier = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null || type.builtInRegistryHolder().is(FIELD_BOSS_TYPES)) return null;

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.31D) / Math.max(1, count);
            int radius = 6 + level.getRandom().nextInt(6);
            BlockPos base = boss.blockPosition().offset(
                    (int) Math.round(Math.cos(angle) * radius), 0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = findOpenSpawn(level, base);
            if (pos == null) continue;

            Entity entity;
            internalSpawn = true;
            try {
                entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            } finally {
                internalSpawn = false;
            }
            if (!(entity instanceof Mob escort)) {
                if (entity != null) entity.discard();
                continue;
            }

            escort.getPersistentData().putBoolean(FIELD_ESCORT_KEY, true);
            escort.getPersistentData().putString(FIELD_ESCORT_OWNER_KEY, boss.getUUID().toString());
            escort.setPersistenceRequired();
            escort.setGlowingTag(true);
            if (target != null) escort.setTarget(target);
            return escort;
        }
        return null;
    }

    private static Mob spawnTaggedBoss(ServerLevel level, BlockPos pos, EntitySpawnReason reason, int nearbyPlayers) {
        List<EntityType<?>> pool = fieldBossTypes();
        if (pool.isEmpty()) return null;

        int start = level.getRandom().nextInt(pool.size());
        for (int offset = 0; offset < pool.size(); offset++) {
            EntityType<?> type = pool.get((start + offset) % pool.size());
            Entity entity;
            internalSpawn = true;
            try {
                entity = type.spawn(level, pos, reason);
            } finally {
                internalSpawn = false;
            }
            if (!(entity instanceof Mob boss)) {
                if (entity != null) entity.discard();
                continue;
            }
            if (!EliteMobSystem.adoptExternalFieldBoss(boss, nearbyPlayers)) {
                boss.discard();
                continue;
            }
            ensureRuntime(level, boss);
            return boss;
        }
        return null;
    }

    private static EncounterRuntime ensureRuntime(ServerLevel level, Mob boss) {
        EncounterRuntime existing = ACTIVE.get(boss.getUUID());
        if (existing != null && existing.level == level) return existing;
        if (existing != null) clearEscorts(existing);
        EncounterRuntime created = new EncounterRuntime(level, boss.getUUID());
        ACTIVE.put(boss.getUUID(), created);
        return created;
    }

    private static void cleanupEscortRefs(EncounterRuntime runtime) {
        runtime.escorts.removeIf(id -> {
            Entity entity = runtime.level.getEntity(id);
            return !(entity instanceof Mob mob) || !mob.isAlive()
                    || !mob.getPersistentData().getBooleanOr(FIELD_ESCORT_KEY, false);
        });
    }

    private static boolean wardActive(EncounterRuntime runtime, long now) {
        return runtime.wardActive && now < runtime.wardUntil && !runtime.escorts.isEmpty();
    }

    private static void collapseWard(EncounterRuntime runtime, String message) {
        if (!runtime.wardActive) return;
        runtime.wardActive = false;
        Entity entity = runtime.level.getEntity(runtime.bossId);
        if (entity instanceof Mob boss && boss.isAlive()) {
            runtime.level.sendParticles(ParticleTypes.ENCHANT,
                    boss.getX(), boss.getY() + boss.getBbHeight() * 0.55D, boss.getZ(),
                    28, 1.0D, 0.8D, 1.0D, 0.08D);
            notifyNearby(runtime.level, boss, Component.literal("§a[방어 공명 붕괴] §f" + message));
        }
    }

    private static void clearEscorts(EncounterRuntime runtime) {
        for (UUID id : new HashSet<>(runtime.escorts)) {
            Entity entity = runtime.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        runtime.escorts.clear();
        runtime.wardActive = false;
    }

    private static List<EntityType<?>> fieldBossTypes() {
        List<EntityType<?>> result = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null && type.builtInRegistryHolder().is(FIELD_BOSS_TYPES)) result.add(type);
        }
        result.sort((left, right) -> BuiltInRegistries.ENTITY_TYPE.getKey(left).toString()
                .compareTo(BuiltInRegistries.ENTITY_TYPE.getKey(right).toString()));
        return result;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 4; dy >= -5; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, Entity origin, double radius) {
        ServerPlayer nearest = null;
        double best = radius * radius;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || player.isSpectator()) continue;
            double distance = player.distanceToSqr(origin);
            if (distance > best) continue;
            nearest = player;
            best = distance;
        }
        return nearest;
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, Entity origin, double radius) {
        List<ServerPlayer> players = new ArrayList<>();
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.isAlive() && !player.isSpectator()
                    && player.distanceToSqr(origin) <= radiusSqr) players.add(player);
        }
        return players;
    }

    private static void notifyNearby(ServerLevel level, Entity origin, Component message) {
        for (ServerPlayer player : nearbyPlayers(level, origin, NOTIFY_RADIUS)) player.sendSystemMessage(message, true);
    }

    private static final class EncounterRuntime {
        final ServerLevel level;
        final UUID bossId;
        final Set<UUID> escorts = new HashSet<>();
        long wardUntil;
        boolean wardActive;

        EncounterRuntime(ServerLevel level, UUID bossId) {
            this.level = level;
            this.bossId = bossId;
        }
    }
}
