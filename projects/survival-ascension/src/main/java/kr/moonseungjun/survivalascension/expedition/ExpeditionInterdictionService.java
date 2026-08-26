package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Turns long-form expedition operations into contested field work instead of a pure checklist.
 *
 * Two bounded interdiction waves are attached to each operation. The current operation complication
 * changes when the second wave arrives and how large it is, so the existing complication catalog now
 * has a visible combat consequence instead of being only movement/timer bookkeeping. One audited
 * content-pack monster may replace a vanilla slot without increasing the wave headcount.
 */
public final class ExpeditionInterdictionService {
    private static final String SESSION_KEY = "survivalascension_operation_interdiction_session";
    private static final String STAGE_KEY = "survivalascension_operation_interdiction_stage";

    private ExpeditionInterdictionService() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % 10 != 0 || player.isCreative() || player.isSpectator() || !player.isAlive()) return;

        ExpeditionOperationData data = ExpeditionOperationData.get(player);
        ExpeditionOperationData.ActiveOperation active = data.active(player);
        CompoundTag persistent = player.getPersistentData();
        if (active == null) {
            persistent.remove(SESSION_KEY);
            persistent.remove(STAGE_KEY);
            return;
        }

        long session = sessionKey(active);
        if (persistent.getLongOr(SESSION_KEY, Long.MIN_VALUE) != session) {
            persistent.putLong(SESSION_KEY, session);
            persistent.putInt(STAGE_KEY, 0);
        }

        int stage = persistent.getIntOr(STAGE_KEY, 0);
        ExpeditionOperation operation = ExpeditionOperation.forRegion(active.region());
        if (stage == 0 && active.rangeReached()) {
            WaveResult result = spawnWave(player, level, active, 1);
            if (result.spawned() > 0) {
                persistent.putInt(STAGE_KEY, 1);
                announce(player, active, 1, result);
            }
            return;
        }

        if (stage == 1 && secondWaveReady(active, operation)) {
            WaveResult result = spawnWave(player, level, active, 2);
            if (result.spawned() > 0) {
                persistent.putInt(STAGE_KEY, 2);
                announce(player, active, 2, result);
            }
        }
    }

    private static boolean secondWaveReady(ExpeditionOperationData.ActiveOperation active, ExpeditionOperation operation) {
        boolean firstObjectiveDone = active.progressA() >= operation.tasks().get(0).target();
        return switch (active.complication()) {
            case FORWARD_SHIFT -> firstObjectiveDone && active.complicationState() < 0;
            case HOT_EXTRACTION -> firstObjectiveDone
                    && active.progressB() >= operation.tasks().get(1).target()
                    && active.complicationState() == 1;
            default -> firstObjectiveDone;
        };
    }

    private static long sessionKey(ExpeditionOperationData.ActiveOperation active) {
        long anchor = ((long) active.anchor().getX() * 341873128712L)
                ^ ((long) active.anchor().getY() * 132897987541L)
                ^ ((long) active.anchor().getZ() * 42317861L);
        return active.deadline() ^ anchor ^ ((long) active.region().ordinal() << 56);
    }

    private static WaveResult spawnWave(
            ServerPlayer player,
            ServerLevel level,
            ExpeditionOperationData.ActiveOperation active,
            int wave) {
        ExpeditionRegion region = active.region();
        int worldTier = Math.max(0, Math.min(2, region.requiredWorldStage()));
        int targetCount = wave == 1 ? 2 + worldTier : 3 + worldTier;
        if (active.complication() == ExpeditionComplication.DEEP_FRONT && wave == 1) targetCount++;
        if (active.complication() == ExpeditionComplication.HOT_EXTRACTION && wave == 2) targetCount++;
        List<String> vanilla = fallbackIds(region);

        String contentId = region == ExpeditionRegion.OCEAN
                ? null
                : ContentPackCompatibility.randomIncidentReinforcementId(level.getRandom(), worldTier);
        int contentSlot = contentId == null ? -1 : level.getRandom().nextInt(targetCount);
        int spawned = 0;
        int contentSpawned = 0;

        for (int i = 0; i < targetCount; i++) {
            String vanillaId = vanilla.get(i % vanilla.size());
            boolean contentAttempt = i == contentSlot;
            String typeId = contentAttempt ? contentId : vanillaId;
            Mob mob = spawnOne(level, player.blockPosition(), region == ExpeditionRegion.OCEAN,
                    typeId, i, targetCount);
            if (mob == null && contentAttempt) {
                mob = spawnOne(level, player.blockPosition(), false, vanillaId, i, targetCount);
                contentAttempt = false;
            }
            if (mob == null) continue;
            mob.setTarget(player);
            spawned++;
            if (contentAttempt) contentSpawned++;
            level.sendParticles(
                    contentAttempt ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.55D, mob.getZ(),
                    contentAttempt ? 18 : 8,
                    0.35D, 0.45D, 0.35D, 0.02D);
        }
        return new WaveResult(spawned, contentSpawned);
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, boolean water, String typeId, int index, int count) {
        Identifier id = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) return null;

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.31D) / Math.max(1, count);
            int radius = 8 + level.getRandom().nextInt(6);
            BlockPos base = center.offset(
                    (int) Math.round(Math.cos(angle) * radius),
                    0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = water ? findWaterSpawn(level, base) : findOpenSpawn(level, base);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 5; dy >= -6; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
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

    private static List<String> fallbackIds(ExpeditionRegion region) {
        return switch (region) {
            case WOODLAND -> List.of("minecraft:zombie", "minecraft:spider");
            case ARID -> List.of("minecraft:husk", "minecraft:pillager");
            case WETLAND -> List.of("minecraft:witch", "minecraft:spider", "minecraft:slime");
            case HIGHLANDS -> List.of("minecraft:pillager", "minecraft:skeleton");
            case OCEAN -> List.of("minecraft:drowned");
            case DEEP -> List.of("minecraft:cave_spider", "minecraft:silverfish", "minecraft:zombie");
            case FROZEN -> List.of("minecraft:stray", "minecraft:skeleton");
            case NETHER -> List.of("minecraft:blaze", "minecraft:wither_skeleton", "minecraft:magma_cube");
            case END -> List.of("minecraft:endermite", "minecraft:shulker");
        };
    }

    private static void announce(
            ServerPlayer player,
            ExpeditionOperationData.ActiveOperation active,
            int wave,
            WaveResult result) {
        String phase = switch (active.complication()) {
            case FORWARD_SHIFT when wave == 2 -> "재전개선 차단";
            case HOT_EXTRACTION when wave == 2 -> "긴급 철수선 차단";
            case DEEP_FRONT when wave == 1 -> "고착 전선 돌파";
            default -> wave == 1 ? "전진선 차단" : "회수선 차단";
        };
        player.sendSystemMessage(Component.literal("§c[작전 저지대] §f" + active.region().koreanName() + " · §e" + phase
                + " §7· 적 " + result.spawned() + "체가 현장에 투입되었습니다."
                + (result.contentSpawned() > 0 ? " §b· 외부 이변 개체 1체 포함" : "")
                + " §7· 변수: " + active.complication().koreanName()));
    }

    private record WaveResult(int spawned, int contentSpawned) {}
}
