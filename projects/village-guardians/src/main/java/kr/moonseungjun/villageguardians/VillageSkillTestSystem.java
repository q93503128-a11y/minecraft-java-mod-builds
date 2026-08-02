package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Transient, reward-free skill testing targets owned by individual players. */
public final class VillageSkillTestSystem {
    private static final Set<UUID> ENABLED = new HashSet<>();
    private static final Set<UUID> DUMMIES = new HashSet<>();
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();

    private VillageSkillTestSystem() {}

    public static void initializeServer(MinecraftServer server) {
        ENABLED.clear();
        DUMMIES.clear();
        OWNERS.clear();
    }

    public static boolean recognize(Mob mob) {
        if (mob == null || !OWNERS.containsKey(mob.getUUID())) return false;
        mob.setNoAi(true);
        mob.setCanPickUpLoot(false);
        VillageWorldSystem.markAllowedGameMob(mob);
        DUMMIES.add(mob.getUUID());
        return true;
    }

    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    public static String enable(ServerPlayer player) {
        ENABLED.add(player.getUUID());
        if (player.level() instanceof ServerLevel level
                && targetsNear(level, player, 128.0, 128).isEmpty()) {
            spawnTargets(player);
        }
        return "기술 시험 모드 활성화 · 습득·비용·재사용 대기시간을 무시합니다."
                + "\n시험 표적은 경험치·주화·아이템을 주지 않습니다.";
    }

    public static String disable(ServerPlayer player) {
        String result = clearTargets(player);
        ENABLED.remove(player.getUUID());
        return "기술 시험 모드를 종료했습니다. " + result;
    }

    public static String spawnTargets(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return "현재 월드에서는 표적을 만들 수 없습니다.";
        }
        ENABLED.add(player.getUUID());
        clearTargets(player);

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        if (forward.lengthSqr() < 0.01) forward = new Vec3(0, 0, 1);
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        int spawned = 0;

        for (int i = 0; i < 6; i++) {
            Zombie dummy = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            if (dummy == null) continue;
            int row = i / 3;
            int column = i % 3 - 1;
            Vec3 pos = player.position().add(forward.scale(6 + row * 3)).add(side.scale(column * 2.5));
            dummy.snapTo(pos.x, player.getY(), pos.z);
            dummy.setNoAi(true);
            dummy.setCanPickUpLoot(false);

            var hp = dummy.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(240 + i * 40);
            var knockback = dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) knockback.setBaseValue(1.0);
            dummy.setHealth(dummy.getMaxHealth());
            dummy.setCustomName(Component.literal("기술 시험 표적 " + (i + 1)
                    + " · 체력 " + Math.round(dummy.getMaxHealth())));
            dummy.setCustomNameVisible(true);

            UUID id = dummy.getUUID();
            DUMMIES.add(id);
            OWNERS.put(id, player.getUUID());
            VillageWorldSystem.markAllowedGameMob(dummy);
            if (level.addFreshEntity(dummy)) {
                spawned++;
            } else {
                DUMMIES.remove(id);
                OWNERS.remove(id);
                VillageWorldSystem.unmarkAllowedGameMob(id);
            }
        }
        return "기술 시험 표적 " + spawned + "개를 전방에 배치했습니다.";
    }

    public static String clearTargets(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return "표적을 정리할 수 없습니다.";
        int count = 0;
        UUID owner = player.getUUID();
        for (UUID id : new HashSet<>(DUMMIES)) {
            if (!owner.equals(OWNERS.get(id))) continue;
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
            DUMMIES.remove(id);
            OWNERS.remove(id);
            VillageWorldSystem.unmarkAllowedGameMob(id);
            count++;
        }
        return "시험 표적 " + count + "개 정리";
    }

    public static void clearAll(MinecraftServer server) {
        for (UUID id : new HashSet<>(DUMMIES)) {
            Entity entity = server.overworld().getEntity(id);
            if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(id);
        }
        DUMMIES.clear();
        OWNERS.clear();
        ENABLED.clear();
    }

    public static boolean isTestDummy(Entity entity) {
        return entity != null && DUMMIES.contains(entity.getUUID());
    }

    public static List<Mob> targetsNear(
            ServerLevel level, ServerPlayer player, double radius, int limit) {
        double squared = radius * radius;
        UUID owner = player.getUUID();
        List<Mob> result = new ArrayList<>();
        for (UUID id : new HashSet<>(DUMMIES)) {
            if (!owner.equals(OWNERS.get(id))) continue;
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob && mob.isAlive() && mob.distanceToSqr(player) <= squared) {
                result.add(mob);
            }
        }
        result.sort(Comparator.comparingDouble(player::distanceToSqr));
        int capped = Math.max(0, limit);
        return result.size() <= capped ? result : new ArrayList<>(result.subList(0, capped));
    }
}
