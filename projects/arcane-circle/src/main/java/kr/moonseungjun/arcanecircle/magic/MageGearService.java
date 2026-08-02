package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime rules for the three-piece mage outfit.
 * A robe is one item from the player's point of view, but reserves both chest and leg slots.
 */
public final class MageGearService {
    private static final Set<UUID> ROBE_SLOT_WARNED = new HashSet<>();

    private MageGearService() {}

    public static void tick(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        boolean wearingRobe = chest.getItem() == ModItems.MAGE_ROBE.get();
        boolean wearingHem = legs.getItem() == ModItems.MAGE_ROBE_HEM.get();

        if (wearingRobe && legs.isEmpty()) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.MAGE_ROBE_HEM.get()));
            wearingHem = true;
            ROBE_SLOT_WARNED.remove(player.getUUID());
        } else if (!wearingRobe && wearingHem) {
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            ROBE_SLOT_WARNED.remove(player.getUUID());
            wearingHem = false;
        } else if (wearingRobe && !wearingHem && !legs.isEmpty()
                && ROBE_SLOT_WARNED.add(player.getUUID())) {
            ArcaneNoticeService.push(player, Component.literal(
                    "[로브 비활성] 로브는 몸·바지 두 슬롯을 사용합니다. 현재 바지 장비를 먼저 빼세요."), 110);
        } else if (!wearingRobe) {
            ROBE_SLOT_WARNED.remove(player.getUUID());
        }

        GearStats stats = stats(player);
        if (stats.boots()) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 30, 0, true, false));
        }
        if (stats.robe()) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 30, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false));
        }
    }

    public static GearStats stats(Player player) {
        boolean hat = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.MAGE_HAT.get();
        boolean robe = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.MAGE_ROBE.get()
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.MAGE_ROBE_HEM.get();
        boolean boots = player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MAGE_BOOTS.get();

        int mana = (hat ? 90 : 0) + (robe ? 45 : 0) + (boots ? 10 : 0);
        double regen = (hat ? 1.20 : 1.0) * (robe ? 1.08 : 1.0) * (boots ? 1.03 : 1.0);
        double manaCost = (hat ? 0.92 : 1.0) * (robe ? 0.97 : 1.0) * (boots ? 0.99 : 1.0);
        double power = (hat ? 1.03 : 1.0) * (robe ? 1.09 : 1.0) * (boots ? 1.02 : 1.0);
        double range = (hat ? 1.01 : 1.0) * (robe ? 1.03 : 1.0) * (boots ? 1.07 : 1.0);
        double cooldown = (hat ? 0.97 : 1.0) * (robe ? 0.95 : 1.0) * (boots ? 0.94 : 1.0);
        return new GearStats(hat, robe, boots, mana, regen, manaCost, power, range, cooldown);
    }

    public static String hatName(Player player) {
        return stats(player).hat() ? "비전 모자" : "모자 없음";
    }

    public static String robeName(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() != ModItems.MAGE_ROBE.get()) return "로브 없음";
        return stats(player).robe() ? "중층 마도 로브" : "중층 마도 로브 · 바지 슬롯 필요";
    }

    public static String bootsName(Player player) {
        return stats(player).boots() ? "유랑 마도화" : "마도화 없음";
    }

    public static void clear(UUID playerId) {
        ROBE_SLOT_WARNED.remove(playerId);
    }

    public record GearStats(
            boolean hat,
            boolean robe,
            boolean boots,
            int maxManaBonus,
            double regenMultiplier,
            double manaCostMultiplier,
            double powerMultiplier,
            double rangeMultiplier,
            double cooldownMultiplier
    ) {}
}
