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
import java.util.Collection;
import java.util.List;

/**
 * Late-game enchantment completion without another menu or abstract currency.
 *
 * The stone is used in one hand while the equipment sits in the other. Every successful use moves the
 * equipment toward a conventional finished set: general Protection replaces mutually-exclusive protection
 * variants, Sharpness replaces Smite/Bane of Arthropods, then missing compatible enchants are added before
 * existing curated enchants are upgraded. Vanilla maximum levels and compatibility rules are respected.
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

    /** Curses and mutually-exclusive specialist damage/protection variants are deliberately omitted. */
    private static final List<Spec> CURATED = List.of(
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

        Result result = improve(target, level);
        if (!result.changed()) {
            player.sendSystemMessage(Component.literal(
                    "§d[마력 각인석] §f이 장비에는 현재 각인석으로 더 완성할 수 있는 호환 인챈트가 없습니다. §7재화는 소비하지 않았습니다."));
            return;
        }

        if (!player.isCreative()) stone.shrink(1);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("§d[마력 각인] §f" + result.description()));
    }

    private static Result improve(ItemStack stack, ServerLevel level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        Holder<Enchantment> protection = registry.getOrThrow(key(PROTECTION.path()));
        if (stack.supportsEnchantment(protection) && current.getLevel(protection) <= 0) {
            int oldSpecialist = highestLevel(current, registry, PROTECTION_ALTERNATIVES);
            Collection<Holder<Enchantment>> retained = without(current.keySet(), PROTECTION_ALTERNATIVES);
            if (EnchantmentHelper.isEnchantmentCompatible(retained, protection)) {
                int levelToSet = Math.min(protection.value().getMaxLevel(),
                        Math.max(midLevel(protection), oldSpecialist));
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
                mutable.removeIf(holder -> isAny(holder, PROTECTION_ALTERNATIVES));
                mutable.set(protection, levelToSet);
                EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
                return new Result(true, oldSpecialist > 0
                        ? "보호 계열을 §e보호 " + levelToSet + "§f로 통합했습니다."
                        : "핵심 방어 인챈트 §e보호 " + levelToSet + "§f를 부여했습니다.");
            }
        }

        Holder<Enchantment> sharpness = registry.getOrThrow(key(SHARPNESS.path()));
        if (stack.supportsEnchantment(sharpness) && current.getLevel(sharpness) <= 0) {
            int oldSpecialist = highestLevel(current, registry, DAMAGE_ALTERNATIVES);
            Collection<Holder<Enchantment>> retained = without(current.keySet(), DAMAGE_ALTERNATIVES);
            if (EnchantmentHelper.isEnchantmentCompatible(retained, sharpness)) {
                int levelToSet = Math.min(sharpness.value().getMaxLevel(),
                        Math.max(midLevel(sharpness), oldSpecialist));
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
                mutable.removeIf(holder -> isAny(holder, DAMAGE_ALTERNATIVES));
                mutable.set(sharpness, levelToSet);
                EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
                return new Result(true, oldSpecialist > 0
                        ? "공격 계열을 §e날카로움 " + levelToSet + "§f로 통합했습니다."
                        : "핵심 공격 인챈트 §e날카로움 " + levelToSet + "§f를 부여했습니다.");
            }
        }

        current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        List<Candidate> missing = new ArrayList<>();
        for (Spec spec : CURATED) {
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
        for (Spec spec : CURATED) {
            Holder<Enchantment> holder = registry.getOrThrow(key(spec.path()));
            int oldLevel = current.getLevel(holder);
            if (oldLevel <= 0 || oldLevel >= holder.value().getMaxLevel()) continue;
            upgradeable.add(new Candidate(spec, holder));
        }
        if (upgradeable.isEmpty()) return Result.NO_CHANGE;

        Candidate chosen = upgradeable.get(level.getRandom().nextInt(upgradeable.size()));
        int oldLevel = current.getLevel(chosen.holder());
        int newLevel = Math.min(chosen.holder().value().getMaxLevel(), oldLevel + 1);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(chosen.holder(), newLevel);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        return new Result(true, "§e" + chosen.spec().label() + "§f을 §e" + oldLevel + " → " + newLevel + "§f로 강화했습니다.");
    }

    private static int highestLevel(ItemEnchantments current, net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> registry,
                                    List<String> paths) {
        int highest = 0;
        for (String path : paths) highest = Math.max(highest, current.getLevel(registry.getOrThrow(key(path))));
        return highest;
    }

    private static Collection<Holder<Enchantment>> without(Collection<Holder<Enchantment>> source, List<String> blocked) {
        List<Holder<Enchantment>> retained = new ArrayList<>();
        for (Holder<Enchantment> holder : source) if (!isAny(holder, blocked)) retained.add(holder);
        return retained;
    }

    private static boolean isAny(Holder<Enchantment> holder, List<String> paths) {
        for (String path : paths) if (holder.is(key(path))) return true;
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
