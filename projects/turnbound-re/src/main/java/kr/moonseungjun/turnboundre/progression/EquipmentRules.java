package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EquipmentDefinition;

/** Pure equipment-to-battle stat projection. Equipment never changes SPD or persisted CharacterProgress. */
public final class EquipmentRules {
    private EquipmentRules() {}

    public static boolean valid(DefinitionRegistry registry, EquipmentProgress progress) {
        if (registry == null || progress == null) return false;
        EquipmentDefinition definition = registry.equipment().get(progress.equipmentId());
        return definition != null && definition.tier(progress.level()) != null;
    }

    public static CharacterDefinition.Stats apply(
            DefinitionRegistry registry,
            EquipmentProgress progress,
            CharacterDefinition.Stats base
    ) {
        if (registry == null || progress == null || base == null) {
            throw new IllegalArgumentException("registry/progress/base required");
        }
        EquipmentDefinition definition = registry.equipment().get(progress.equipmentId());
        if (definition == null) throw new IllegalArgumentException("unknown equipment " + progress.equipmentId());
        EquipmentDefinition.Tier tier = definition.tier(progress.level());
        if (tier == null) {
            throw new IllegalArgumentException("invalid equipment level " + progress.equipmentId() + "@" + progress.level());
        }
        EquipmentDefinition.Bonus bonus = tier.bonus();
        return new CharacterDefinition.Stats(
                scale(base.hp(), bonus.hpPercent()),
                scale(base.atk(), bonus.atkPercent()),
                scale(base.def(), bonus.defPercent()),
                base.spd(),
                scale(base.poise(), bonus.poisePercent()));
    }

    private static int scale(int base, int percent) {
        if (base < 0 || percent < 0) throw new IllegalArgumentException("base/percent must be >= 0");
        long result = (long) base + ((long) base * percent) / 100L;
        if (result > Integer.MAX_VALUE) throw new IllegalArgumentException("equipment stat overflow");
        return (int) result;
    }
}
