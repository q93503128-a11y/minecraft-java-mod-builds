package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.Locale;

/**
 * Canonical armor protection archetype.
 *
 * <p>Each armor slot is rounded independently after Item-Lv scaling, exactly as specified by
 * EQUIPMENT_BALANCE.md. Accessories and off-hands never receive an implicit armor baseline.</p>
 */
public enum ProjectArmorArchetype {
    LIGHT,
    MEDIUM,
    HEAVY;

    public static final Codec<ProjectArmorArchetype> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(
                            ProjectArmorArchetype.valueOf(value.trim().toUpperCase(Locale.ROOT))
                    );
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown Openworld RPG armor archetype: " + value);
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public int scaledDefense(ProjectEquipmentSlot slot, int itemLevel) {
        SlotBaseline baseline = baseline(slot);
        return (int) Math.round(
                baseline.defense() * ProjectCombatRules.gearScale(itemLevel)
        );
    }

    public int scaledMagicResistance(ProjectEquipmentSlot slot, int itemLevel) {
        SlotBaseline baseline = baseline(slot);
        return (int) Math.round(
                baseline.magicResistance() * ProjectCombatRules.gearScale(itemLevel)
        );
    }

    public static boolean isArmorSlot(ProjectEquipmentSlot slot) {
        return switch (slot) {
            case HEAD, CHEST, LEGS, GLOVES, BOOTS -> true;
            default -> false;
        };
    }

    private SlotBaseline baseline(ProjectEquipmentSlot slot) {
        if (!isArmorSlot(slot)) {
            throw new IllegalArgumentException("Armor archetype cannot apply to slot " + slot + ".");
        }

        return switch (this) {
            case LIGHT -> switch (slot) {
                case HEAD -> new SlotBaseline(2, 3);
                case CHEST -> new SlotBaseline(5, 7);
                case LEGS -> new SlotBaseline(4, 5);
                case GLOVES, BOOTS -> new SlotBaseline(2, 3);
                default -> throw new IllegalArgumentException("Unsupported Light armor slot " + slot + ".");
            };
            case MEDIUM -> switch (slot) {
                case HEAD -> new SlotBaseline(4, 2);
                case CHEST -> new SlotBaseline(8, 4);
                case LEGS -> new SlotBaseline(6, 3);
                case GLOVES -> new SlotBaseline(3, 2);
                case BOOTS -> new SlotBaseline(4, 2);
                default -> throw new IllegalArgumentException("Unsupported Medium armor slot " + slot + ".");
            };
            case HEAVY -> switch (slot) {
                case HEAD -> new SlotBaseline(5, 1);
                case CHEST -> new SlotBaseline(11, 2);
                case LEGS -> new SlotBaseline(8, 2);
                case GLOVES -> new SlotBaseline(4, 1);
                case BOOTS -> new SlotBaseline(5, 1);
                default -> throw new IllegalArgumentException("Unsupported Heavy armor slot " + slot + ".");
            };
        };
    }

    private record SlotBaseline(int defense, int magicResistance) {
    }
}
