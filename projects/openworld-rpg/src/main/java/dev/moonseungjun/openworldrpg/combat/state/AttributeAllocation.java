package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player-spent attribute points for one active root-class profile.
 *
 * <p>All six attributes start at canonical value 5. This record stores only earned points spent
 * above that baseline. Equipment bonuses are deliberately separate and may push effective values
 * above the pre-equipment cap.</p>
 */
public record AttributeAllocation(
        int vit,
        int end,
        int str,
        int dex,
        int intel,
        int wil
) {
    public static final int BASE_ATTRIBUTE = 5;
    public static final int MAX_PRE_EQUIPMENT_ATTRIBUTE = 60;
    private static final Codec<Integer> ALLOCATION_CODEC = Codec.intRange(
            0,
            MAX_PRE_EQUIPMENT_ATTRIBUTE - BASE_ATTRIBUTE
    );

    public static final Codec<AttributeAllocation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ALLOCATION_CODEC.fieldOf("vit").forGetter(AttributeAllocation::vit),
                    ALLOCATION_CODEC.fieldOf("end").forGetter(AttributeAllocation::end),
                    ALLOCATION_CODEC.fieldOf("str").forGetter(AttributeAllocation::str),
                    ALLOCATION_CODEC.fieldOf("dex").forGetter(AttributeAllocation::dex),
                    ALLOCATION_CODEC.fieldOf("int").forGetter(AttributeAllocation::intel),
                    ALLOCATION_CODEC.fieldOf("wil").forGetter(AttributeAllocation::wil)
            ).apply(instance, AttributeAllocation::new)
    );

    public AttributeAllocation {
        if (vit < 0 || end < 0 || str < 0 || dex < 0 || intel < 0 || wil < 0) {
            throw new IllegalArgumentException("Allocated attribute points cannot be negative.");
        }
        if (BASE_ATTRIBUTE + vit > MAX_PRE_EQUIPMENT_ATTRIBUTE
                || BASE_ATTRIBUTE + end > MAX_PRE_EQUIPMENT_ATTRIBUTE
                || BASE_ATTRIBUTE + str > MAX_PRE_EQUIPMENT_ATTRIBUTE
                || BASE_ATTRIBUTE + dex > MAX_PRE_EQUIPMENT_ATTRIBUTE
                || BASE_ATTRIBUTE + intel > MAX_PRE_EQUIPMENT_ATTRIBUTE
                || BASE_ATTRIBUTE + wil > MAX_PRE_EQUIPMENT_ATTRIBUTE) {
            throw new IllegalArgumentException("Pre-equipment attribute value cannot exceed 60.");
        }
    }

    public int spentPoints() {
        return vit + end + str + dex + intel + wil;
    }

    public int value(CombatAttribute attribute) {
        return BASE_ATTRIBUTE + switch (attribute) {
            case VIT -> vit;
            case END -> end;
            case STR -> str;
            case DEX -> dex;
            case INT -> intel;
            case WIL -> wil;
        };
    }

    public static AttributeAllocation unspent() {
        return new AttributeAllocation(0, 0, 0, 0, 0, 0);
    }
}
