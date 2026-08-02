package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Personal tactical capstones that need transient cooldown or party context. */
final class VillagePersonalCombatSystem {
    private static final Map<UUID, Long> NEXT_BARRIER_AT = new HashMap<>();

    private VillagePersonalCombatSystem() {}

    static void reset() {
        NEXT_BARRIER_AT.clear();
    }

    static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= 0.0f
                || !VillageSkillTreeSystem.emergencyBarrierUnlocked(player)) return;
        float projected = player.getHealth() - event.getAmount();
        if (projected > player.getMaxHealth() * 0.30f) return;
        long now = System.currentTimeMillis();
        long readyAt = NEXT_BARRIER_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) return;

        float reduced = event.getAmount() * 0.65f;
        if (player.getHealth() > 1.0f) reduced = Math.min(reduced, player.getHealth() - 1.0f);
        event.setAmount(Math.max(0.0f, reduced));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8,
                VillageSkillTreeSystem.emergencyBarrierAbsorptionAmplifier(player), false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 4, 1, false, true, true));
        NEXT_BARRIER_AT.put(player.getUUID(), now
                + VillageSkillTreeSystem.emergencyBarrierCooldownSeconds(player) * 1000L);
        player.sendSystemMessage(Component.literal("§b[응급 장막] §f치명적인 충격을 흡수했습니다."));
    }

    static void applyLowHealthPassive(ServerPlayer player) {
        if (VillageSkillTreeSystem.lowHealthRegenerationUnlocked(player)
                && player.getHealth() <= player.getMaxHealth() * 0.40f) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 55, 0, false, false, true));
        }
    }

    static void applyKillMomentum(ServerPlayer player) {
        int seconds = VillageSkillTreeSystem.killMomentumSeconds(player);
        if (seconds > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, seconds * 20, 0, false, true, true));
        }
    }

    static void healNearbyAlliesOnKill(ServerPlayer player) {
        float amount = VillageSkillTreeSystem.teamHealOnKillAmount(player);
        MinecraftServer server = player.level().getServer();
        if (amount <= 0.0f || server == null) return;
        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (ally != player && ally.level() == player.level() && ally.distanceToSqr(player) <= 144.0) {
                ally.heal(amount);
            }
        }
    }
}
