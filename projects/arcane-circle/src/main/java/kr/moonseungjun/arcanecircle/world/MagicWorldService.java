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

/** Magic-world rules without the deprecated generated test academy. */
public final class MagicWorldService {
    private MagicWorldService() {}

    public static void onLogin(ServerPlayer player, boolean firstAwakening) {
        ArcaneWorldData data = ArcaneWorldData.get(((ServerLevel) player.level()).getServer());
        if (data.claimFirstArrival(player)) {
            if (!player.isCreative() && !player.isSpectator()) player.setGameMode(GameType.SURVIVAL);
            data.addMarks(player, firstAwakening ? 120L : 40L);
            player.sendSystemMessage(Component.literal("§5[마력핵 각성] §f주문과 마도서를 사용할 수 있습니다."));
        }
    }

    public static void onRespawn(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) player.setGameMode(GameType.SURVIVAL);
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 80 == 0) awakenNearbyEnemies(player);
    }

    public static BlockPos academy(ServerPlayer player) {
        return player.blockPosition();
    }

    public static void teleportToAcademy(ServerPlayer player) {
        player.sendOverlayMessage(Component.literal("§7물리 학원 귀환은 비활성화되어 있습니다."));
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
