package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * TURNBOUND uses the Minecraft player as an explorer/camera/controller shell, not as a survival combatant.
 * Party CombatantState HP is the real battle health system.
 */
public final class PlayerShellRules {
    private PlayerShellRules() {
    }

    public static void maintain(ServerPlayer player) {
        if (player.getHealth() < player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        if (player.getFoodData().getFoodLevel() < 20) player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer) event.setCanceled(true);
    }
}
