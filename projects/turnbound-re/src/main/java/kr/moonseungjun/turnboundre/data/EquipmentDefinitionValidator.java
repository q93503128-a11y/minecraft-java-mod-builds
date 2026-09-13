package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Semantic validation for the deliberately small one-slot equipment model. */
public final class EquipmentDefinitionValidator {
    private static final int MAX_TIERS = 5;
    private static final int MAX_PERCENT_PER_STAT = 20;

    private EquipmentDefinitionValidator() {}

    public static List<String> validate(List<EquipmentDefinition> equipment) {
        if (equipment == null) return List.of("equipment must not be null");
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (EquipmentDefinition definition : equipment) {
            if (definition == null) {
                errors.add("equipment definition must not be null");
                continue;
            }
            String id = definition.id();
            if (!validId(id)) errors.add("invalid equipment id: " + id);
            else if (!seen.add(id)) errors.add("duplicate equipment id: " + id);
            if (!validId(definition.ingredientItem())) {
                errors.add(id + ": ingredientItem must be a namespaced item id");
            }
            if (definition.tiers().isEmpty()) {
                errors.add(id + ": tiers must not be empty");
                continue;
            }
            if (definition.tiers().size() > MAX_TIERS) {
                errors.add(id + ": tier count must be <= " + MAX_TIERS);
            }

            EquipmentDefinition.Bonus previous = null;
            long previousCoin = -1L;
            int previousMaterial = -1;
            for (int index = 0; index < definition.tiers().size(); index++) {
                EquipmentDefinition.Tier tier = definition.tiers().get(index);
                int expectedLevel = index + 1;
                if (tier == null) {
                    errors.add(id + ": null tier at level " + expectedLevel);
                    continue;
                }
                if (tier.level() != expectedLevel) {
                    errors.add(id + ": tiers must be consecutive starting at 1; expected "
                            + expectedLevel + " but got " + tier.level());
                }
                if (tier.coinCost() < 0) errors.add(id + ": coinCost must be >= 0 at level " + tier.level());
                if (tier.materialCount() < 1) errors.add(id + ": materialCount must be >= 1 at level " + tier.level());
                if (tier.coinCost() < previousCoin) errors.add(id + ": coinCost must not decrease across tiers");
                if (tier.materialCount() < previousMaterial) errors.add(id + ": materialCount must not decrease across tiers");
                EquipmentDefinition.Bonus bonus = tier.bonus();
                if (bonus == null) {
                    errors.add(id + ": bonus required at level " + tier.level());
                    continue;
                }
                validateBonus(id, tier.level(), bonus, errors);
                if (previous != null && !nonDecreasing(previous, bonus)) {
                    errors.add(id + ": stat bonuses must not decrease across tiers");
                }
                previous = bonus;
                previousCoin = tier.coinCost();
                previousMaterial = tier.materialCount();
            }
        }
        return List.copyOf(errors);
    }

    private static void validateBonus(String id, int level, EquipmentDefinition.Bonus bonus, List<String> errors) {
        int[] values = {bonus.hpPercent(), bonus.atkPercent(), bonus.defPercent(), bonus.poisePercent()};
        boolean any = false;
        for (int value : values) {
            if (value < 0 || value > MAX_PERCENT_PER_STAT) {
                errors.add(id + ": bonus percent must be 0.." + MAX_PERCENT_PER_STAT + " at level " + level);
                return;
            }
            any |= value > 0;
        }
        if (!any) errors.add(id + ": each tier must provide at least one combat bonus");
    }

    private static boolean nonDecreasing(EquipmentDefinition.Bonus previous, EquipmentDefinition.Bonus next) {
        return next.hpPercent() >= previous.hpPercent()
                && next.atkPercent() >= previous.atkPercent()
                && next.defPercent() >= previous.defPercent()
                && next.poisePercent() >= previous.poisePercent();
    }

    private static boolean validId(String value) {
        if (value == null) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || colon != value.lastIndexOf(':')) return false;
        return validPart(value.substring(0, colon), false) && validPart(value.substring(colon + 1), true);
    }

    private static boolean validPart(String part, boolean path) {
        if (part.isBlank()) return false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.'
                    || path && c == '/';
            if (!ok) return false;
        }
        return true;
    }
}
