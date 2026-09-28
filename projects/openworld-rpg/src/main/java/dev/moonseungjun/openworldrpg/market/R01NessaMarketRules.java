package dev.moonseungjun.openworldrpg.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Canon-locked deterministic five-slot Nessa Bell market generation. */
public final class R01NessaMarketRules {
    public static final String MERCHANT_ID = "openworld_rpg:merchant/nessa_bell";
    public static final String MARKET_SERVICE_ID =
            "openworld_rpg:service/alderford/market";
    public static final long CYCLE_TICKS = 10L * 60L * 20L;
    public static final int STOCK_SLOTS = 5;

    public static final String HEARTLAND_ARMING_SWORD =
            "openworld_rpg:heartland_arming_sword";
    public static final String WAYFARER_DAGGERS =
            "openworld_rpg:wayfarer_daggers";
    public static final String QUARRY_MAUL =
            "openworld_rpg:quarry_maul";
    public static final String RIVER_PIKE =
            "openworld_rpg:river_pike";
    public static final String RIVERWOOD_BOW =
            "openworld_rpg:riverwood_bow";
    public static final String INITIATE_STAFF =
            "openworld_rpg:initiate_staff";
    public static final String INITIATE_WAND =
            "openworld_rpg:initiate_wand";
    public static final String WATCH_BUCKLER =
            "openworld_rpg:watch_buckler";
    public static final String APPRENTICE_FOCUS =
            "openworld_rpg:apprentice_focus";

    public static final String RIVER_SCHOLAR_GARB =
            "openworld_rpg:river_scholar_garb";
    public static final String WAYFARER_LEATHERS =
            "openworld_rpg:wayfarer_leathers";
    public static final String IRONBOUND_GUARD =
            "openworld_rpg:ironbound_guard";

    public static final String GREENWATER_PENDANT =
            "openworld_rpg:greenwater_pendant";
    public static final String ROADWORN_BAND =
            "openworld_rpg:roadworn_band";
    public static final String WAYFARERS_TOKEN =
            "openworld_rpg:wayfarers_token";
    public static final String QUARRY_SEAL =
            "openworld_rpg:quarry_seal";

    private static final List<BaseDefinition> WEAPONS = List.of(
            fixed(HEARTLAND_ARMING_SWORD, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(WAYFARER_DAGGERS, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(QUARRY_MAUL, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(RIVER_PIKE, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(RIVERWOOD_BOW, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(INITIATE_STAFF, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON),
            fixed(INITIATE_WAND, MarketCategory.WEAPON,
                    ProjectEquipmentSlot.MAIN_WEAPON)
    );

    private static final List<BaseDefinition> OFF_HANDS = List.of(
            fixed(WATCH_BUCKLER, MarketCategory.OFF_HAND,
                    ProjectEquipmentSlot.OFF_HAND),
            fixed(APPRENTICE_FOCUS, MarketCategory.OFF_HAND,
                    ProjectEquipmentSlot.OFF_HAND)
    );

    private static final Set<ProjectEquipmentSlot> ARMOR_SLOTS = Set.of(
            ProjectEquipmentSlot.HEAD,
            ProjectEquipmentSlot.CHEST,
            ProjectEquipmentSlot.LEGS,
            ProjectEquipmentSlot.GLOVES,
            ProjectEquipmentSlot.BOOTS
    );

    private static final List<ProjectEquipmentSlot> ARMOR_SLOT_ORDER = List.of(
            ProjectEquipmentSlot.HEAD,
            ProjectEquipmentSlot.CHEST,
            ProjectEquipmentSlot.LEGS,
            ProjectEquipmentSlot.GLOVES,
            ProjectEquipmentSlot.BOOTS
    );

    private static final List<ArmorPair> ARMOR_PAIRS = buildArmorPairs();

    private static final List<BaseDefinition> ARMOR_FAMILIES = List.of(
            new BaseDefinition(
                    RIVER_SCHOLAR_GARB,
                    MarketCategory.ARMOR,
                    ARMOR_SLOTS
            ),
            new BaseDefinition(
                    WAYFARER_LEATHERS,
                    MarketCategory.ARMOR,
                    ARMOR_SLOTS
            ),
            new BaseDefinition(
                    IRONBOUND_GUARD,
                    MarketCategory.ARMOR,
                    ARMOR_SLOTS
            )
    );

    private static final List<BaseDefinition> ACCESSORIES = List.of(
            fixed(GREENWATER_PENDANT, MarketCategory.ACCESSORY,
                    ProjectEquipmentSlot.NECKLACE),
            fixed(ROADWORN_BAND, MarketCategory.ACCESSORY,
                    ProjectEquipmentSlot.RING_1),
            fixed(WAYFARERS_TOKEN, MarketCategory.ACCESSORY,
                    ProjectEquipmentSlot.CHARM),
            fixed(QUARRY_SEAL, MarketCategory.ACCESSORY,
                    ProjectEquipmentSlot.RELIC)
    );

    private static final List<BaseDefinition> ALL_BASES = allBases();

    private R01NessaMarketRules() {
    }

    public static long cycleIndex(long activeTicks, long shrineEpochTicks) {
        if (activeTicks < 0L
                || shrineEpochTicks < 0L
                || shrineEpochTicks > activeTicks) {
            throw new IllegalArgumentException(
                    "Nessa cycle requires activeTicks >= shrineEpochTicks >= 0."
            );
        }
        return (activeTicks - shrineEpochTicks) / CYCLE_TICKS;
    }

    public static List<StockItem> generateCycle(
            long worldSeed,
            String playerUuid,
            long cycleIndex
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        java.util.UUID.fromString(playerUuid);
        if (cycleIndex < 0L) {
            throw new IllegalArgumentException(
                    "Nessa cycle index must be non-negative."
            );
        }

        String root = worldSeed
                + "|" + playerUuid
                + "|nessa_bell|"
                + cycleIndex;

        BaseDefinition slot1 = choose(WEAPONS, root + "|slot1_base", 0);

        BaseDefinition slot2;
        if (unit(root + "|slot2_category", 0) < 0.50) {
            List<BaseDefinition> alternatives = WEAPONS.stream()
                    .filter(value -> !value.id().equals(slot1.id()))
                    .toList();
            slot2 = choose(alternatives, root + "|slot2_weapon", 0);
        } else {
            slot2 = choose(OFF_HANDS, root + "|slot2_offhand", 0);
        }

        ArmorPair pair = choose(ARMOR_PAIRS, root + "|armor_pair", 0);
        BaseDefinition armor3 =
                choose(ARMOR_FAMILIES, root + "|slot3_armor_family", 0);
        BaseDefinition armor4 =
                choose(ARMOR_FAMILIES, root + "|slot4_armor_family", 0);
        BaseDefinition accessory =
                choose(ACCESSORIES, root + "|slot5_accessory", 0);

        List<SlotDraft> drafts = List.of(
                new SlotDraft(1, slot1, slot1.onlySlot()),
                new SlotDraft(2, slot2, slot2.onlySlot()),
                new SlotDraft(3, armor3, pair.first()),
                new SlotDraft(4, armor4, pair.second()),
                new SlotDraft(5, accessory, accessory.onlySlot())
        );

        List<StockItem> result = new ArrayList<>(STOCK_SLOTS);
        boolean superiorSeen = false;
        for (SlotDraft draft : drafts) {
            ProjectItemGrade grade = primaryGrade(
                    root + "|slot" + draft.slotIndex() + "|grade"
            );
            if (grade == ProjectItemGrade.SUPERIOR) {
                if (superiorSeen) {
                    grade = normalizedStandardRefinedGrade(
                            root + "|slot" + draft.slotIndex()
                                    + "|grade_superior_reroll"
                    );
                } else {
                    superiorSeen = true;
                }
            }

            result.add(new StockItem(
                    draft.slotIndex(),
                    draft.base().id(),
                    draft.base().category(),
                    draft.equipmentSlot(),
                    grade,
                    itemLevel(grade),
                    priceGold(draft.base().category(), grade),
                    stableLong(
                            root + "|slot" + draft.slotIndex()
                                    + "|affix_seed"
                    )
            ));
        }
        return List.copyOf(result);
    }

    public static int itemLevel(ProjectItemGrade grade) {
        Objects.requireNonNull(grade, "grade");
        return switch (grade) {
            case STANDARD -> 2;
            case REFINED -> 4;
            case SUPERIOR -> 6;
            case EXALTED, MYTHIC -> throw new IllegalArgumentException(
                    "Nessa R01 rotation never generates " + grade + "."
            );
        };
    }

    public static long priceGold(
            MarketCategory category,
            ProjectItemGrade grade
    ) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(grade, "grade");
        return switch (category) {
            case WEAPON -> switch (grade) {
                case STANDARD -> 150L;
                case REFINED -> 240L;
                case SUPERIOR -> 500L;
                default -> invalidGrade(grade);
            };
            case OFF_HAND -> switch (grade) {
                case STANDARD -> 130L;
                case REFINED -> 210L;
                case SUPERIOR -> 450L;
                default -> invalidGrade(grade);
            };
            case ARMOR -> switch (grade) {
                case STANDARD -> 120L;
                case REFINED -> 200L;
                case SUPERIOR -> 420L;
                default -> invalidGrade(grade);
            };
            case ACCESSORY -> switch (grade) {
                case STANDARD -> 110L;
                case REFINED -> 180L;
                case SUPERIOR -> 380L;
                default -> invalidGrade(grade);
            };
        };
    }

    public static long sellBackGold(long originalPurchasePrice) {
        if (originalPurchasePrice <= 0L) {
            throw new IllegalArgumentException(
                    "Equipment purchase price must be positive."
            );
        }
        return originalPurchasePrice / 4L;
    }

    public static BaseDefinition base(String baseId) {
        Objects.requireNonNull(baseId, "baseId");
        return ALL_BASES.stream()
                .filter(value -> value.id().equals(baseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown R01 Nessa base: " + baseId
                ));
    }

    private static ProjectItemGrade primaryGrade(String seed) {
        double roll = unit(seed, 0);
        if (roll < 0.50) return ProjectItemGrade.STANDARD;
        if (roll < 0.90) return ProjectItemGrade.REFINED;
        return ProjectItemGrade.SUPERIOR;
    }

    private static ProjectItemGrade normalizedStandardRefinedGrade(
            String seed
    ) {
        return unit(seed, 0) < (5.0 / 9.0)
                ? ProjectItemGrade.STANDARD
                : ProjectItemGrade.REFINED;
    }

    private static <T> T choose(List<T> values, String seed, int subRoll) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Cannot choose from empty list.");
        }
        int index = Math.min(
                values.size() - 1,
                (int) Math.floor(unit(seed, subRoll) * values.size())
        );
        return values.get(index);
    }

    private static double unit(String material, int subRoll) {
        byte[] digest = digest(material, subRoll);
        long value = ByteBuffer.wrap(digest).getLong();
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long stableLong(String material) {
        return ByteBuffer.wrap(digest(material, 0)).getLong();
    }

    private static byte[] digest(String material, int subRoll) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(material.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(
                    Integer.toString(subRoll)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable for Nessa market RNG.",
                    exception
            );
        }
    }

    private static long invalidGrade(ProjectItemGrade grade) {
        throw new IllegalArgumentException(
                "Nessa R01 rotation never prices " + grade + "."
        );
    }

    private static BaseDefinition fixed(
            String id,
            MarketCategory category,
            ProjectEquipmentSlot slot
    ) {
        return new BaseDefinition(id, category, Set.of(slot));
    }

    private static List<ArmorPair> buildArmorPairs() {
        List<ArmorPair> pairs = new ArrayList<>();
        for (int i = 0; i < ARMOR_SLOT_ORDER.size(); i++) {
            for (int j = i + 1; j < ARMOR_SLOT_ORDER.size(); j++) {
                pairs.add(new ArmorPair(
                        ARMOR_SLOT_ORDER.get(i),
                        ARMOR_SLOT_ORDER.get(j)
                ));
            }
        }
        return List.copyOf(pairs);
    }

    private static List<BaseDefinition> allBases() {
        List<BaseDefinition> result = new ArrayList<>();
        result.addAll(WEAPONS);
        result.addAll(OFF_HANDS);
        result.addAll(ARMOR_FAMILIES);
        result.addAll(ACCESSORIES);
        return List.copyOf(result);
    }

    public enum MarketCategory {
        WEAPON,
        OFF_HAND,
        ARMOR,
        ACCESSORY;

        public static final Codec<MarketCategory> CODEC =
                Codec.STRING.comapFlatMap(
                        value -> {
                            try {
                                return DataResult.success(
                                        MarketCategory.valueOf(
                                                value.trim()
                                                        .toUpperCase(Locale.ROOT)
                                        )
                                );
                            } catch (IllegalArgumentException exception) {
                                return DataResult.error(
                                        () -> "Unknown Nessa market category: "
                                                + value
                                );
                            }
                        },
                        value -> value.name().toLowerCase(Locale.ROOT)
                );
    }

    public record BaseDefinition(
            String id,
            MarketCategory category,
            Set<ProjectEquipmentSlot> allowedSlots
    ) {
        public BaseDefinition {
            requireStableId(id);
            Objects.requireNonNull(category, "category");
            allowedSlots = Set.copyOf(
                    Objects.requireNonNull(allowedSlots, "allowedSlots")
            );
            if (allowedSlots.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nessa base must allow at least one equipment slot."
                );
            }
        }

        public ProjectEquipmentSlot onlySlot() {
            if (allowedSlots.size() != 1) {
                throw new IllegalStateException(
                        "Nessa base does not have one fixed slot: " + id
                );
            }
            return allowedSlots.iterator().next();
        }
    }

    public record StockItem(
            int slotIndex,
            String baseId,
            MarketCategory category,
            ProjectEquipmentSlot equipmentSlot,
            ProjectItemGrade grade,
            int itemLevel,
            long priceGold,
            long affixSeed
    ) {
        public static final Codec<StockItem> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(1, STOCK_SLOTS)
                                .fieldOf("slot_index")
                                .forGetter(StockItem::slotIndex),
                        Codec.STRING.fieldOf("base_id")
                                .forGetter(StockItem::baseId),
                        MarketCategory.CODEC.fieldOf("category")
                                .forGetter(StockItem::category),
                        ProjectEquipmentSlot.CODEC.fieldOf("equipment_slot")
                                .forGetter(StockItem::equipmentSlot),
                        ProjectItemGrade.CODEC.fieldOf("grade")
                                .forGetter(StockItem::grade),
                        Codec.INT.fieldOf("item_level")
                                .forGetter(StockItem::itemLevel),
                        Codec.LONG.fieldOf("price_gold")
                                .forGetter(StockItem::priceGold),
                        Codec.LONG.fieldOf("affix_seed")
                                .forGetter(StockItem::affixSeed)
                ).apply(instance, StockItem::new));

        public StockItem {
            if (slotIndex < 1 || slotIndex > STOCK_SLOTS) {
                throw new IllegalArgumentException(
                        "Nessa slot index must be inside 1..5."
                );
            }
            BaseDefinition base = base(baseId);
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(equipmentSlot, "equipmentSlot");
            Objects.requireNonNull(grade, "grade");
            if (base.category() != category
                    || !base.allowedSlots().contains(equipmentSlot)) {
                throw new IllegalArgumentException(
                        "Nessa base/category/equipment-slot mismatch: "
                                + baseId
                );
            }
            if (grade == ProjectItemGrade.EXALTED
                    || grade == ProjectItemGrade.MYTHIC) {
                throw new IllegalArgumentException(
                        "Nessa R01 stock cannot be Exalted/Mythic."
                );
            }
            if (itemLevel != itemLevel(grade)) {
                throw new IllegalArgumentException(
                        "Nessa item level does not match grade."
                );
            }
            if (priceGold != priceGold(category, grade)) {
                throw new IllegalArgumentException(
                        "Nessa price does not match category/grade."
                );
            }
        }
    }

    private record SlotDraft(
            int slotIndex,
            BaseDefinition base,
            ProjectEquipmentSlot equipmentSlot
    ) {
    }

    private record ArmorPair(
            ProjectEquipmentSlot first,
            ProjectEquipmentSlot second
    ) {
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced Nessa market id."
            );
        }
    }
}
