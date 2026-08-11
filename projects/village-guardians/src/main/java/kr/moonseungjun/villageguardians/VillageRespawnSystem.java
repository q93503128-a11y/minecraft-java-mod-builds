package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VillageRespawnSystem {
    public static final int RESPAWN_DELAY_TICKS = 20 * 20;
    private static final Map<UUID, Long> RESPAWN_AT = new HashMap<>();

    private VillageRespawnSystem() {}

    public static void reset() {
        RESPAWN_AT.clear();
    }

    public static boolean isDowned(ServerPlayer player) {
        return RESPAWN_AT.containsKey(player.getUUID());
    }

    public static void onLogin(ServerPlayer player) {
        if (isDowned(player)) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    public static boolean handleFinalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        if (isDowned(player)) {
            event.setNewDamage(0.0f);
            return true;
        }
        float effectiveHealth = player.getHealth() + Math.max(0.0f, player.getAbsorptionAmount());
        if (event.getNewDamage() < effectiveHealth) return false;

        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        event.setNewDamage(0.0f);
        player.setAbsorptionAmount(0.0f);
        player.setHealth(1.0f);
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(Vec3.ZERO);
        player.setGameMode(GameType.SPECTATOR);
        int delay = VillageProgressionSystem.respawnDelayTicks();
        RESPAWN_AT.put(player.getUUID(), server.overworld().getGameTime() + delay);
        player.sendSystemMessage(Component.literal(
                "§c[전투 불능] §f" + (delay / 20)
                        + "초 후 마을 광장에서 부활합니다. 적은 시설 공격을 계속합니다."));
        return true;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> iterator = RESPAWN_AT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            if (now < entry.getValue()) {
                if (!player.isSpectator()) {
                    player.setGameMode(GameType.SPECTATOR);
                }
                continue;
            }

            teleportToVillage(player, server);
            player.setGameMode(GameType.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.setAbsorptionAmount(0.0f);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
            player.setRemainingFireTicks(0);
            player.setDeltaMovement(Vec3.ZERO);
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            VillageRpgSystem.refreshPlayerPassive(player);
            player.sendSystemMessage(Component.literal(
                    "§a[부활] §f마을 광장에서 복귀했습니다. 5초 동안 강한 피해 저항을 얻습니다."));
            iterator.remove();
        }
    }

    public static boolean reviveNow(ServerPlayer player, String source) {
        if (player == null || !isDowned(player)) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        RESPAWN_AT.remove(player.getUUID());
        teleportToVillage(player, server);
        player.setGameMode(GameType.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(8.0f);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(Vec3.ZERO);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 4));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
        VillageRpgSystem.refreshPlayerPassive(player);
        player.sendSystemMessage(Component.literal("§e[즉시 부활] §f" + source + "의 힘으로 전장에 복귀했습니다."));
        return true;
    }

    private static void teleportToVillage(ServerPlayer player, MinecraftServer server) {
        ServerLevel destination = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(player.blockPosition());
        BlockPos target = null;
        for (int z = 12; z <= 22 && target == null; z++) {
            for (int x = -5; x <= 5; x++) {
                BlockPos candidate = center.offset(x, 0, z);
                if (destination.getBlockState(candidate).isAir()
                        && destination.getBlockState(candidate.above()).isAir()
                        && !destination.getBlockState(candidate.below()).isAir()) {
                    target = candidate;
                    break;
                }
            }
        }
        if (target == null) {
            target = center.above();
        }
        player.teleportTo(destination, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                Set.of(), 180.0f, 0.0f, true);
    }

    public static int remainingSeconds(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Long readyAt = RESPAWN_AT.get(player.getUUID());
        if (server == null || readyAt == null) {
            return 0;
        }
        long remaining = Math.max(0L, readyAt - server.overworld().getGameTime());
        return Math.max(1, (int) ((remaining + 19L) / 20L));
    }

    public static String hudText(ServerPlayer player) {
        return "§c전투 불능 §8│ §f부활까지 §e" + remainingSeconds(player) + "초"
                + " §8│ §7적은 시설 공격을 계속합니다";
    }
}
