package kr.moonseungjun.survivalascension.equipment;

import kr.moonseungjun.survivalascension.registry.AscensionItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Late-game enchantment completion without another menu or abstract currency.
 *
 * Normal use completes conventional enchantments without deleting the player's specialist protection/damage
 * choice. Sneak-use on Mythic III affix gear is a separate endgame sink: it consumes multiple stones to swap
 * exactly one ascension affix while preserving the rest of the item.
 */
public final class EnchantmentCompletionService {
    private static final Spec PROTECTION = new Spec("protection", "보호");
    private static final Spec SHARPNESS = new Spec("sharpness", "날카로움");

    private static final List<String> PROTECTION_ALTERNATIVES = List.of(
            "fire_protection", "blast_protection", "projectile_protection"
    );
    private static final List<String> DAMAGE_ALTERNATIVES = List.of(
            "smite", "bane_of_arthropods"
    );

    /**
     * Enchants that may be newly added when compatible. Specialist protection/damage variants are not in
     * this list, so the stone never randomly changes the player's chosen damage/protection specialization.
     */
    private static final List<Spec> ADDABLE = List.of(
            PROTECTION,
            new Spec("feather_falling", "가벼운 착지"),
            new Spec("respiration", "호흡"),
            new Spec("aqua_affinity", "친수성"),
            new Spec("thorns", "가시"),
            new Spec("depth_strider", "물갈퀴"),
            new Spec("soul_speed", "영혼 가속"),
            new Spec("swift_sneak", "신속한 잠행"),
            SHARPNESS,
            new Spec("knockback", "밀치기"),
            new Spec("fire_aspect", "발화"),
            new Spec("looting", "약탈"),
            new Spec("sweeping_edge", "휩쓸기"),
            new Spec("efficiency", "효율"),
            new Spec("silk_touch", "섬세한 손길"),
            new Spec("fortune", "행운"),
            new Spec("power", "힘"),
            new Spec("punch", "밀어내기"),
            new Spec("flame", "화염"),
            new Spec("infinity", "무한"),
            new Spec("quick_charge", "빠른 장전"),
            new Spec("piercing", "관통"),
            new Spec("multishot", "다중 발사"),
            new Spec("impaling", "찌르기"),
            new Spec("loyalty", "충성"),
            new Spec("channeling", "집전"),
            new Spec("riptide", "급류"),
            new Spec("density", "밀도"),
            new Spec("breach", "격파"),
            new Spec("wind_burst", "돌풍"),
            new Spec("lunge", "돌진"),
            new Spec("luck_of_the_sea", "바다의 행운"),
            new Spec("lure", "미끼"),
            new Spec("unbreaking", "내구성"),
            new Spec("mending", "수선")
    );

    /** Existing specialist choices are eligible for +1 upgrades, but never added from an empty slot. */
    private static final List<Spec> UPGRADE_ONLY = List.of(
            new Spec("fire_protection", "화염으로부터 보호"),
            new Spec("blast_protection", "폭발로부터 보호"),
            new Spec("projectile_protection", "발사체로부터 보호"),
            new Spec("smite", "강타"),
            new Spec("bane_of_arthropods", "살충")
    );

    private EnchantmentCompletionService() {}

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stone = event.getItemStack();
        if (!stone.is(AscensionItems.ENCHANTMENT_STONE.get())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        InteractionHand targetHand = event.getHand() == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(targetHand);
        if (target.isEmpty() || target.getMaxStackSize() != 1) {
            player.sendSystemMessage(Component.literal(
                    "§d[마력 각인석] §f반대 손에 강화할 단일 장비를 들고 사용하세요."));
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;

        if (player.isShiftKeyDown()) {
            precisionReengrave(player, stone, targetHand, target, level);
            return;
        }

        Result result = improve(target, level);
        if (!result.changed()) {
            String precisionHint = AscensionAffixes.rarity(target) == 3
                    ? " §7신화 장비는 웅크리고 사용하면 승천 옵션 하나를 정밀 재각인할 수 있습니다."
                    : "";
            player.sendSystemMessage(Component.literal(
                    "§d[마력 각인석] §f이 장비에는 현재 각인석으로 더 완성할 수 있는 호환 인챈트가 없습니다. §7재화는 소비하지 않았습니다."
                            + precisionHint));
            return;
        }

        if (!player.isCreative()) stone.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§d[마력 각인] §f" + result.description()));
    }

    private static void precisionReengrave(ServerPlayer player, ItemStack stone, InteractionHand targetHand,
                                           ItemStack target, ServerLevel level) {
        int cost = PrecisionReengravingService.stoneCost(target);
        if (cost <= 0) {
            player.sendSystemMessage(Component.literal(
                    "§d[정밀 재각인] §f승천 옵션이 정상적으로 붙은 신화 III 장비만 정밀 재각인할 수 있습니다. §7재화는 소비하지 않았습니다."));
            return;
        }
        if (!player.isCreative() && stone.getCount() < cost) {
            player.sendSystemMessage(Component.literal(
                    "§d[정밀 재각인] §f마력 각인석이 부족합니다. §7필요 " + cost + "개 · 보유 " + stone.getCount() + "개"));
            return;
        }

        PrecisionReengravingService.Result result = PrecisionReengravingService.rerollOne(target, level.getRandom());
        if (!result.changed()) {
            player.sendSystemMessage(Component.literal(
                    "§d[정밀 재각인] §f유효한 새 각인을 만들지 못했습니다. §7재화는 소비하지 않았습니다."));
            return;
        }

        player.setItemInHand(targetHand, result.stack());
        if (!player.isCreative()) stone.shrink(result.cost());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§5[정밀 재각인] §f승천 옵션 하나를 교체했습니다. §7마력 각인석 -"
                + result.cost()));
        player.sendSystemMessage(Component.literal("§7이전: §f" + result.before()));
        player.sendSystemMessage(Component.literal("§7현재: §e" + result.after()));
    }

    private static Result improve(ItemStack stack, ServerLevel level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        boolean specialistProtection = hasAny(current, registry, PROTECTION_ALTERNATIVES);
        boolean specialistDamage = hasAny(current, registry, DAMAGE_ALTERNATIVES);

        List<Candidate> missing = new ArrayList<>();
        for (Spec spec : ADDABLE) {
            if (spec == PROTECTION && specialistProtection) continue;
            if (spec == SHARPNESS && specialistDamage) continue;

            Holder<Enchantment> holder = registry.getOrThrow(key(spec.path()));
            if (!stack.supportsEnchantment(holder) || current.getLevel(holder) > 0) continue;
            if (!EnchantmentHelper.isEnchantmentCompatible(current.keySet(), holder)) continue;
            missing.add(new Candidate(spec, holder));
        }
        if (!missing.isEmpty()) {
            Candidate chosen = missing.get(level.getRandom().nextInt(missing.size()));
            int newLevel = midLevel(chosen.holder());
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
            mutable.set(chosen.holder(), newLevel);
            EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
            return new Result(true, "없던 §e" + chosen.spec().label() + " " + newLevel
                    + "§f 인챈트를 추가했습니다.");
        }

        List<Candidate> upgradeable = new ArrayList<>();
        collectUpgradeable(upgradeable, current, registry, ADDABLE);
        collectUpgradeable(upgradeable, current, registry, UPGRADE_ONLY);
        if (upgradeable.isEmpty()) return Result.NO_CHANGE;

        Candidate chosen = upgradeable.get(level.getRandom().nextInt(upgradeable.size()));
        int oldLevel = current.getLevel(chosen.holder());
        int newLevel = Math.min(chosen.holder().value().getMaxLevel(), oldLevel + 1);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(chosen.holder(), newLevel);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        return new Result(true, "§e" + chosen.spec().label() + "§f을 §e" + oldLevel + " → " + newLevel + "§f로 강화했습니다.");
    }

    private static void collectUpgradeable(List<Candidate> result, ItemEnchantments current,
                                           net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> registry,
                                           List<Spec> specs) {
        for (Spec spec : specs) {
            Holder<Enchantment> holder = registry.getOrThrow(key(spec.path()));
            int oldLevel = current.getLevel(holder);
            if (oldLevel <= 0 || oldLevel >= holder.value().getMaxLevel()) continue;
            result.add(new Candidate(spec, holder));
        }
    }

    private static boolean hasAny(ItemEnchantments current,
                                  net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> registry,
                                  List<String> paths) {
        for (String path : paths) {
            if (current.getLevel(registry.getOrThrow(key(path))) > 0) return true;
        }
        return false;
    }

    private static int midLevel(Holder<Enchantment> holder) {
        return Math.max(1, (holder.value().getMaxLevel() + 1) / 2);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("minecraft", path));
    }

    private record Spec(String path, String label) {}
    private record Candidate(Spec spec, Holder<Enchantment> holder) {}
    private record Result(boolean changed, String description) {
        private static final Result NO_CHANGE = new Result(false, "");
    }
}
