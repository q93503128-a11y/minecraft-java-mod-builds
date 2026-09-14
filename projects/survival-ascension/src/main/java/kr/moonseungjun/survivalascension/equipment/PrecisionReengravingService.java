package kr.moonseungjun.survivalascension.equipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Endgame sink for the enchantment stone that deepens the existing affix system instead of adding a new
 * currency or menu. A precision re-engraving keeps rarity, awakened state, item components, enchantments
 * and all but one existing ascension affix. Exactly one current affix is exchanged for one missing affix.
 */
public final class PrecisionReengravingService {
    private static final String ROOT = "survivalascension_affix";
    private static final List<String> AFFIX_KEYS = List.of(
            "primary", "scale", "mastery", "secondary", "utility"
    );
    private static final int MAX_CANDIDATE_ROLLS = 16;

    private PrecisionReengravingService() {}

    public static int stoneCost(ItemStack stack) {
        if (AscensionAffixes.rarity(stack) != 3) return 0;
        int count = currentAffixes(stack).size();
        if (AscensionAffixes.isAwakened(stack)) return count == 4 ? 3 : 0;
        return count == 3 ? 2 : 0;
    }

    public static Result rerollOne(ItemStack stack, RandomSource random) {
        int cost = stoneCost(stack);
        if (cost <= 0) return Result.NO_CHANGE;

        List<String> before = currentAffixes(stack);
        String beforeSummary = AscensionAffixes.affixSummary(stack);
        for (int attempt = 0; attempt < MAX_CANDIDATE_ROLLS; attempt++) {
            ItemStack candidate = stack.copy();
            if (!AscensionAffixes.reroll(candidate, random)) continue;

            List<String> after = currentAffixes(candidate);
            if (after.size() != before.size() || differenceCount(before, after) != 2) continue;
            return new Result(true, candidate, cost, beforeSummary, AscensionAffixes.affixSummary(candidate));
        }
        return Result.NO_CHANGE;
    }

    private static List<String> currentAffixes(ItemStack stack) {
        List<String> result = new ArrayList<>();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return result;
        CompoundTag root = data.copyTag().getCompoundOrEmpty(ROOT);
        if (root.isEmpty()) return result;
        for (String key : AFFIX_KEYS) {
            if (root.getBooleanOr(key, false)) result.add(key);
        }
        return result;
    }

    private static int differenceCount(List<String> left, List<String> right) {
        int differences = 0;
        for (String key : AFFIX_KEYS) {
            if (left.contains(key) != right.contains(key)) differences++;
        }
        return differences;
    }

    public record Result(boolean changed, ItemStack stack, int cost, String before, String after) {
        private static final Result NO_CHANGE = new Result(false, ItemStack.EMPTY, 0, "", "");
    }
}
