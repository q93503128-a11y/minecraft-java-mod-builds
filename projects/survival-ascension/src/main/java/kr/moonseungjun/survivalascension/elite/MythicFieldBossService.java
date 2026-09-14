package kr.moonseungjun.survivalascension.elite;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional content-pack seam for true field bosses.
 *
 * The external mod keeps ownership of models, textures, animation, sounds and native combat AI.
 * Survival Ascension only selects entity types from its data tag, spawns the original entity and
 * attaches its own tracking/reward/contribution contract. No external implementation class is linked.
 */
public final class MythicFieldBossService {
    static final String FIELD_BOSS_KEY = "survivalascension_external_field_boss";

    private static final TagKey<EntityType<?>> FIELD_BOSS_TYPES = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );

    private static boolean internalSpawn;

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
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§e[필드보스] §f현재 로드된 콘텐츠 팩에 등록된 필드보스가 없습니다."));
            return 0;
        }

        BlockPos pos = player.blockPosition().relative(player.getDirection(), 10);
        Mob boss = spawnTaggedBoss(level, pos, EntitySpawnReason.COMMAND, 1);
        if (boss == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§e[필드보스] §f소환 공간 또는 동시 출현 제한 때문에 필드보스를 만들지 못했습니다."));
            return 0;
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§4[필드보스] §f정면 약 10블록에 §e" + boss.getName().getString() + "§f을 소환했습니다."));
        return 1;
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
            return boss;
        }
        return null;
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
}
