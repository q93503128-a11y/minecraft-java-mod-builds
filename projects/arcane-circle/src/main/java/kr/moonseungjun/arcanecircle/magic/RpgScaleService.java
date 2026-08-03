package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Keeps every non-player health pool on the same 100:20 scale used by player vitality. */
public final class RpgScaleService {
    public static final String SCALED_TAG = "arcanecircle_rpg_scaled";
    private static final double BASE_SCALE = ArcaneVitalityService.RPG_POINTS_PER_VANILLA_HEALTH;

    private RpgScaleService() {}

    public static void ensureBaseline(LivingEntity entity) {
        if (entity == null || entity instanceof Player) return;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || !entity.addTag(SCALED_TAG)) return;
        double oldMaximum = Math.max(1.0, entity.getMaxHealth());
        double ratio = Math.max(0.0, Math.min(1.0, entity.getHealth() / oldMaximum));
        health.setBaseValue(Math.min(2_000_000.0, Math.max(1.0, health.getBaseValue()) * BASE_SCALE));
        entity.setHealth((float) Math.max(1.0, entity.getMaxHealth() * ratio));
    }

    public static void applyExtraHealth(LivingEntity entity, String key, double multiplier) {
        ensureBaseline(entity);
        if (entity == null || multiplier <= 1.0001) return;
        String tag = "arcanecircle_health_" + key;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || !entity.addTag(tag)) return;
        double oldMaximum = Math.max(1.0, entity.getMaxHealth());
        double ratio = Math.max(0.0, Math.min(1.0, entity.getHealth() / oldMaximum));
        health.setBaseValue(Math.min(2_000_000.0, health.getBaseValue() * multiplier));
        entity.setHealth((float) Math.max(1.0, entity.getMaxHealth() * ratio));
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof Player) return;
        event.setAmount((float) Math.min(1_000_000.0,
                event.getAmount() * ArcaneVitalityService.RPG_POINTS_PER_VANILLA_HEALTH));
    }
}
