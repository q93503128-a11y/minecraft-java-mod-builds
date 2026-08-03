package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.ArcaneWorldData;
import kr.moonseungjun.arcanecircle.world.MagicTradition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Converts Minecraft's compact internal health pool into an RPG-scale vitality pool.
 *
 * <p>The ratio is derived from existing circle, affiliation and equipment data, so old worlds need
 * no migration. Damage and healing are converted before vanilla processing; the underlying player
 * remains compatible with vanilla death, absorption and effect systems while the visible pool can
 * grow into the hundreds or tens of thousands.</p>
 */
public final class ArcaneVitalityService {
    /** One vanilla health point equals five visible RPG vitality points. */
    public static final double RPG_POINTS_PER_VANILLA_HEALTH = 5.0;

    private ArcaneVitalityService() {}

    public static int effectiveMaxHealth(ServerPlayer player) {
        MagicPlayerData data = MagicPlayerData.get(((ServerLevel) player.level()).getServer());
        int circle = Math.max(1, Math.min(9, data.state(player).circle()));
        int base = switch (circle) {
            case 1 -> 100;
            case 2 -> 190;
            case 3 -> 360;
            case 4 -> 680;
            case 5 -> 1_300;
            case 6 -> 2_500;
            case 7 -> 4_800;
            case 8 -> 9_200;
            case 9 -> 18_000;
            default -> 100;
        };

        MageGearService.GearStats gear = MageGearService.stats(player);
        double equipmentHealth = (base + gear.healthBonus()) * gear.healthMultiplier();
        MagicTradition tradition = ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).tradition(player);
        double doctrine = switch (tradition) {
            case DIVINE -> 1.18;
            case PRIMAL -> 0.92;
            case OCCULT -> 0.96;
            default -> 1.0;
        };
        return Math.max(100, (int) Math.round(equipmentHealth * doctrine));
    }

    public static int effectiveHealth(ServerPlayer player) {
        double vanillaMax = Math.max(1.0, player.getMaxHealth());
        double ratio = Math.max(0.0, Math.min(1.0, player.getHealth() / vanillaMax));
        return Math.max(0, Math.min(effectiveMaxHealth(player),
                (int) Math.ceil(effectiveMaxHealth(player) * ratio - 0.0001)));
    }

    public static int effectiveAbsorption(ServerPlayer player) {
        double vanillaMax = Math.max(1.0, player.getMaxHealth());
        double scaled = player.getAbsorptionAmount() * effectiveMaxHealth(player) / vanillaMax;
        return Math.max(0, (int) Math.ceil(scaled - 0.0001));
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Void/kill-plane style damage must remain capable of killing the player immediately.
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float converted = convertToVanilla(player, event.getAmount());
        event.setAmount(Math.max(0.0F, converted));
    }

    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setAmount(Math.max(0.0F, convertToVanilla(player, event.getAmount())));
    }

    private static float convertToVanilla(ServerPlayer player, float effectiveAmount) {
        double vanillaMax = Math.max(1.0, player.getMaxHealth());
        double effectiveMax = Math.max(vanillaMax, effectiveMaxHealth(player));
        return (float) (effectiveAmount * RPG_POINTS_PER_VANILLA_HEALTH * vanillaMax / effectiveMax);
    }
}
