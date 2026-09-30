package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical five-slot root-class skill layout.
 *
 * <p>Slots 0..3 are the four ordinary active skills and slot 4 is the ultimate. A slot is
 * published only after its production spell resource/runtime binding exists; missing slots stay
 * empty instead of being filled with donor or placeholder skills.</p>
 */
public final class ProjectClassSkillLoadout {
    public static final int ACTIVE_SLOT_COUNT = 4;
    public static final int ULTIMATE_SLOT = 4;
    public static final int TOTAL_SLOT_COUNT = 5;

    private ProjectClassSkillLoadout() {
    }

    public static Optional<String> spellId(RootClass rootClass, int slotIndex) {
        Objects.requireNonNull(rootClass, "rootClass");
        validateSlot(slotIndex);

        return switch (rootClass) {
            case MAGE -> slotIndex == 0
                    ? Optional.of(ProjectSpellSpec.ARC_BOLT_ID)
                    : Optional.empty();
            case CLERIC -> switch (slotIndex) {
                case 0 -> Optional.of(ProjectSpellSpec.RADIANT_LANCE_ID);
                case 1 -> Optional.of(ProjectSpellSpec.MEND_ID);
                case 2 -> Optional.of(ProjectSpellSpec.CONSECRATED_GROUND_ID);
                case 3 -> Optional.of(ProjectSpellSpec.REBUKE_ID);
                case 4 -> Optional.of(ProjectSpellSpec.SANCTUARY_ID);
                default -> Optional.empty();
            };
            case WARRIOR -> switch (slotIndex) {
                case 0 -> Optional.of(ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID);
                case 1 -> Optional.of(ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID);
                case 2 -> Optional.of(ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID);
                case 3 -> Optional.of(ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID);
                case 4 -> Optional.of(ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID);
                default -> Optional.empty();
            };
            case HUNTER -> switch (slotIndex) {
                case 0 -> Optional.of(
                        ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID
                );
                case 1 -> Optional.of(
                        ProjectSpellSpec.HUNTER_PINNING_SHOT_ID
                );
                case 2 -> Optional.of(
                        ProjectSpellSpec.HUNTER_FAN_OF_ARROWS_ID
                );
                default -> Optional.empty();
            };
            case GUARDIAN -> Optional.empty();
        };
    }

    public static List<Slot> implementedSlots(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        List<Slot> slots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < TOTAL_SLOT_COUNT; slotIndex++) {
            int index = slotIndex;
            spellId(rootClass, index).ifPresent(
                    spellId -> slots.add(new Slot(index, spellId))
            );
        }
        return List.copyOf(slots);
    }

    private static void validateSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= TOTAL_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "Class skill slot must be inside [0, 4]: " + slotIndex
            );
        }
    }

    public record Slot(int slotIndex, String spellId) {
        public Slot {
            validateSlot(slotIndex);
            Objects.requireNonNull(spellId, "spellId");
            if (!spellId.startsWith("openworld_rpg:")) {
                throw new IllegalArgumentException(
                        "Project class skill must use openworld_rpg namespace."
                );
            }
        }
    }
}
