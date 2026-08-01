
package kr.moonseungjun.arcanecircle.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameType;

public final class MagicWorldService {
    private MagicWorldService() {}

    public static void onLogin(ServerPlayer player, boolean firstAwakening) {
        ServerLevel level = (ServerLevel) player.level();
        ArcaneWorldData data = ArcaneWorldData.get(level.getServer());
        if (!data.academyBuilt()) {
            BlockPos origin = ArcaneAcademyBuilder.build(level, player.blockPosition());
            data.setAcademy(origin);
        }
        if (data.claimFirstArrival(player)) {
            BlockPos origin = data.academyOrigin();
            BlockPos arrival = origin.offset(0, 1, -10);
            player.teleportTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
            player.setGameMode(GameType.ADVENTURE);
            data.addMarks(player, firstAwakening ? 120L : 40L);
            player.sendSystemMessage(Component.literal("§5[천구 마법학원] §f학원 중앙 회로에 도착했습니다."
                    + " 생존 욕구와 사망 손실은 비활성화되며 모든 거래는 §d아르카나§f로 통일됩니다."));
        }
    }

    public static void onRespawn(ServerPlayer player) {
        ArcaneWorldData data = ArcaneWorldData.get(((ServerLevel) player.level()).getServer());
        BlockPos origin = data.academyOrigin();
        BlockPos arrival = origin.offset(0, 1, -10);
        player.teleportTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
    }

    public static void tick(ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        if (player.tickCount % 80 == 0) awakenNearbyEnemies(player);
    }

    public static BlockPos academy(ServerPlayer player) {
        return ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).academyOrigin();
    }

    public static void teleportToAcademy(ServerPlayer player) {
        BlockPos origin = academy(player);
        BlockPos arrival = origin.offset(0, 1, -10);
        player.teleportTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
    }

    private static void awakenNearbyEnemies(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(42.0),
                value -> value instanceof Enemy && value.isAlive())) {
            if (mob.getCustomName() != null) continue;
            int tier = Math.max(1, Math.min(5, 1 + (int) (level.getGameTime() / 24000L / 3L)));
            if (level.getRandom().nextInt(100) >= 8 + tier * 2) continue;
            mob.setCustomName(Component.literal("§5마력 변이체 " + tier + "환"));
            mob.setCustomNameVisible(false);
            mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, tier));
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, Math.max(0, tier - 1)));
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, Math.max(0, tier / 2 - 1)));
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, Math.max(0, tier / 2)));
            mob.setHealth(mob.getMaxHealth());
        }
    }
}
