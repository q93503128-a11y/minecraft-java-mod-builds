package kr.moonseungjun.arcanecircle.registry;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem;
import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.item.ArcaneTestKitItem;
import kr.moonseungjun.arcanecircle.item.BeginnerGrimoireItem;
import kr.moonseungjun.arcanecircle.item.SpellbookItem;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneCircle.MOD_ID);

    public static final StaffProfile NOVICE_PROFILE = new StaffProfile(
            "novice_staff", "견습 마도봉", "불안정한 1써클 마력핵을 보조하는 입문 지팡이.", "첫 각성 시 최초 1회 지급",
            25, 1.02, 0.96, 1.04, 1.0, 0.96, 0.96, 1.15, null, 1.0);
    public static final StaffProfile EMBER_PROFILE = new StaffProfile(
            "ember_staff", "홍염 지팡이", "화염 회로를 과충전하는 2써클 공격형 지팡이.", "견습 마도봉 + 블레이즈 막대 + 마그마 크림",
            20, 1.04, 0.90, 1.15, 1.08, 0.91, 0.92, 1.10, SpellDefinition.School.FIRE, 1.45);
    public static final StaffProfile GLACIAL_PROFILE = new StaffProfile(
            "glacial_staff", "빙하 지팡이", "서리 주문의 범위와 동결력을 크게 확장한다.", "견습 마도봉 + 푸른 얼음 + 자수정 조각",
            35, 1.05, 0.88, 1.16, 1.24, 0.90, 0.88, 1.18, SpellDefinition.School.FROST, 1.50);
    public static final StaffProfile ZEPHYR_PROFILE = new StaffProfile(
            "zephyr_staff", "청풍 지팡이", "풍류 주문의 연속 시전과 기동력을 높인다.", "견습 마도봉 + 팬텀 막 + 브리즈 막대",
            30, 1.05, 0.86, 1.14, 1.22, 0.82, 0.70, 1.28, SpellDefinition.School.WIND, 1.55);
    public static final StaffProfile AEGIS_PROFILE = new StaffProfile(
            "aegis_staff", "수호 지팡이", "고위 방벽술의 흡수량과 유지력을 증폭한다.", "견습 마도봉 + 방패 + 금 블록",
            80, 1.08, 0.82, 1.24, 1.12, 0.88, 0.76, 1.30, SpellDefinition.School.WARD, 1.75);
    public static final StaffProfile VERDANT_PROFILE = new StaffProfile(
            "verdant_staff", "생명의 지팡이", "치유와 자연 회복을 고위 성직술 수준으로 강화한다.", "견습 마도봉 + 황금 사과 + 에메랄드 블록",
            95, 1.09, 0.76, 1.30, 1.18, 0.86, 0.74, 1.68, SpellDefinition.School.LIFE, 1.85);
    public static final StaffProfile RIFT_PROFILE = new StaffProfile(
            "rift_staff", "균열 지팡이", "공간을 찢어 사거리와 전이 안정성을 크게 높인다.", "견습 마도봉 + 엔더의 눈 + 우는 흑요석 + 메아리 조각",
            110, 1.10, 0.72, 1.36, 1.48, 0.78, 0.68, 1.35, SpellDefinition.School.SPACE, 1.80);
    public static final StaffProfile SAGE_PROFILE = new StaffProfile(
            "sage_staff", "현자의 지팡이", "고위 회로를 적은 마력으로 빠르게 반복하는 범용 지팡이.", "균열 지팡이 + 마법이 부여된 책 + 금 블록 + 다이아몬드 블록",
            620, 1.16, 0.44, 2.10, 1.75, 0.58, 0.42, 2.20, SpellDefinition.School.ARCANE, 1.70);
    public static final StaffProfile ARCHMAGE_PROFILE = new StaffProfile(
            "archmage_staff", "대마도사의 지팡이", "최고위 영역술을 전제로 모든 마법 수치를 증폭한다.", "현자의 지팡이 + 네더의 별 + 메아리 조각 + 네더라이트 블록",
            1800, 1.25, 0.24, 3.80, 2.85, 0.38, 0.23, 3.60, null, 1.0);

    public static final DeferredItem<ArcaneStaffItem> NOVICE_STAFF = registerStaff("novice_staff", NOVICE_PROFILE, Rarity.COMMON);
    public static final DeferredItem<ArcaneStaffItem> EMBER_STAFF = registerStaff("ember_staff", EMBER_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> GLACIAL_STAFF = registerStaff("glacial_staff", GLACIAL_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> ZEPHYR_STAFF = registerStaff("zephyr_staff", ZEPHYR_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> AEGIS_STAFF = registerStaff("aegis_staff", AEGIS_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> VERDANT_STAFF = registerStaff("verdant_staff", VERDANT_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> RIFT_STAFF = registerStaff("rift_staff", RIFT_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> SAGE_STAFF = registerStaff("sage_staff", SAGE_PROFILE, Rarity.EPIC);
    public static final DeferredItem<ArcaneStaffItem> ARCHMAGE_STAFF = registerStaff("archmage_staff", ARCHMAGE_PROFILE, Rarity.EPIC);

    public static final DeferredItem<Item> MAGE_HAT = ITEMS.registerItem("mage_hat",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArcaneArmorMaterials.MAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> MAGE_ROBE = ITEMS.registerItem("mage_robe",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArcaneArmorMaterials.MAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> MAGE_ROBE_HEM = ITEMS.registerItem("mage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArcaneArmorMaterials.MAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> MAGE_BOOTS = ITEMS.registerItem("mage_boots",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArcaneArmorMaterials.MAGE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> SAGE_HAT = ITEMS.registerItem("sage_hat",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> SAGE_ROBE = ITEMS.registerItem("sage_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> SAGE_ROBE_HEM = ITEMS.registerItem("sage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SKYWALKER_BOOTS = ITEMS.registerItem("skywalker_boots",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> ARCHMAGE_CROWN = ITEMS.registerItem("archmage_crown",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> ARCHMAGE_ROBE = ITEMS.registerItem("archmage_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> ARCHMAGE_ROBE_HEM = ITEMS.registerItem("archmage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> FROSTSTEP_BOOTS = ITEMS.registerItem("froststep_boots",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.BOOTS)));


    public static final DeferredItem<Item> CINDER_HOOD = ITEMS.registerItem("cinder_hat",
            properties -> new Item(properties.rarity(Rarity.RARE).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> CINDER_ROBE = ITEMS.registerItem("cinder_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> CINDER_ROBE_HEM = ITEMS.registerItem("cinder_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> CINDER_BOOTS = ITEMS.registerItem("cinder_boots",
            properties -> new Item(properties.rarity(Rarity.RARE).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> GLACIER_CIRCLET = ITEMS.registerItem("glacier_hat",
            properties -> new Item(properties.rarity(Rarity.RARE).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> GLACIER_ROBE = ITEMS.registerItem("glacier_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> GLACIER_ROBE_HEM = ITEMS.registerItem("glacier_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> GLACIER_BOOTS = ITEMS.registerItem("glacier_boots",
            properties -> new Item(properties.rarity(Rarity.RARE).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> TEMPEST_HOOD = ITEMS.registerItem("tempest_hat",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> TEMPEST_ROBE = ITEMS.registerItem("tempest_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> TEMPEST_ROBE_HEM = ITEMS.registerItem("tempest_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> TEMPEST_BOOTS = ITEMS.registerItem("tempest_boots",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.SAGE, ArmorType.BOOTS)));
    public static final DeferredItem<Item> RIFT_CROWN = ITEMS.registerItem("rift_hat",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.HELMET)));
    public static final DeferredItem<Item> RIFT_ROBE = ITEMS.registerItem("rift_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> RIFT_ROBE_HEM = ITEMS.registerItem("rift_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> RIFT_BOOTS = ITEMS.registerItem("rift_boots",
            properties -> new Item(properties.rarity(Rarity.EPIC).humanoidArmor(ArcaneArmorMaterials.ARCHMAGE, ArmorType.BOOTS)));

    public static final DeferredItem<BeginnerGrimoireItem> BEGINNER_GRIMOIRE = ITEMS.registerItem(
            "beginner_grimoire", properties -> new BeginnerGrimoireItem(properties.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ArcaneTestKitItem> ARCANE_TEST_KIT = ITEMS.registerItem(
            "arcane_test_kit", properties -> new ArcaneTestKitItem(properties.rarity(Rarity.EPIC)));
    private static final Map<String, DeferredItem<SpellbookItem>> SPELLBOOKS = registerSpellbooks();

    private static final List<StaffProfile> PROFILES = List.of(
            NOVICE_PROFILE, EMBER_PROFILE, GLACIAL_PROFILE, ZEPHYR_PROFILE,
            AEGIS_PROFILE, VERDANT_PROFILE, RIFT_PROFILE, SAGE_PROFILE, ARCHMAGE_PROFILE);
    private static final Map<String, StaffProfile> PROFILE_BY_ID = buildProfileIndex();

    private ModItems() {}

    private static DeferredItem<ArcaneStaffItem> registerStaff(String id, StaffProfile profile, Rarity rarity) {
        return ITEMS.registerItem(id, properties -> new ArcaneStaffItem(properties.rarity(rarity), profile));
    }

    private static Map<String, DeferredItem<SpellbookItem>> registerSpellbooks() {
        Map<String, DeferredItem<SpellbookItem>> result = new LinkedHashMap<>();
        for (SpellDefinition spell : SpellCatalog.bookSpells()) {
            Rarity rarity = switch (spell.circle()) {
                case 2 -> Rarity.UNCOMMON;
                case 3 -> Rarity.RARE;
                case 4, 5 -> Rarity.EPIC;
                default -> Rarity.COMMON;
            };
            String itemId = SpellCatalog.bookItemId(spell.id());
            result.put(spell.id(), ITEMS.registerItem(itemId,
                    properties -> new SpellbookItem(properties.rarity(rarity), spell.id())));
        }
        return Map.copyOf(result);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (DeferredItem<ArcaneStaffItem> item : all()) event.accept(item.get());
            event.accept(MAGE_HAT.get());
            event.accept(MAGE_ROBE.get());
            event.accept(MAGE_BOOTS.get());
            event.accept(SAGE_HAT.get());
            event.accept(SAGE_ROBE.get());
            event.accept(SKYWALKER_BOOTS.get());
            event.accept(ARCHMAGE_CROWN.get());
            event.accept(ARCHMAGE_ROBE.get());
            event.accept(FROSTSTEP_BOOTS.get());
            event.accept(CINDER_HOOD.get()); event.accept(CINDER_ROBE.get()); event.accept(CINDER_BOOTS.get());
            event.accept(GLACIER_CIRCLET.get()); event.accept(GLACIER_ROBE.get()); event.accept(GLACIER_BOOTS.get());
            event.accept(TEMPEST_HOOD.get()); event.accept(TEMPEST_ROBE.get()); event.accept(TEMPEST_BOOTS.get());
            event.accept(RIFT_CROWN.get()); event.accept(RIFT_ROBE.get()); event.accept(RIFT_BOOTS.get());
            event.accept(BEGINNER_GRIMOIRE.get());
            event.accept(ARCANE_TEST_KIT.get());
            for (DeferredItem<SpellbookItem> item : SPELLBOOKS.values()) event.accept(item.get());
        }
    }

    public static List<DeferredItem<ArcaneStaffItem>> all() {
        return List.of(NOVICE_STAFF, EMBER_STAFF, GLACIAL_STAFF, ZEPHYR_STAFF,
                AEGIS_STAFF, VERDANT_STAFF, RIFT_STAFF, SAGE_STAFF, ARCHMAGE_STAFF);
    }

    public static List<StaffProfile> profiles() {
        return PROFILES;
    }

    public static Map<String, DeferredItem<SpellbookItem>> spellbooks() {
        return SPELLBOOKS;
    }

    public static DeferredItem<SpellbookItem> spellbook(String spellId) {
        return SPELLBOOKS.get(spellId);
    }

    public static DeferredItem<? extends net.minecraft.world.item.Item> staffItem(String id) {
        return switch (id) {
            case "ember_staff" -> EMBER_STAFF;
            case "glacial_staff" -> GLACIAL_STAFF;
            case "zephyr_staff" -> ZEPHYR_STAFF;
            case "aegis_staff" -> AEGIS_STAFF;
            case "verdant_staff" -> VERDANT_STAFF;
            case "rift_staff" -> RIFT_STAFF;
            case "sage_staff" -> SAGE_STAFF;
            case "archmage_staff" -> ARCHMAGE_STAFF;
            default -> NOVICE_STAFF;
        };
    }

    public static StaffProfile profile(String id) {
        return PROFILE_BY_ID.getOrDefault(id, StaffProfile.NONE);
    }

    public static DeferredItem<? extends Item> gearItem(String id) {
        return switch (id) {
            case "mage_hat" -> MAGE_HAT;
            case "mage_robe" -> MAGE_ROBE;
            case "mage_boots" -> MAGE_BOOTS;
            case "sage_hat" -> SAGE_HAT;
            case "sage_robe" -> SAGE_ROBE;
            case "skywalker_boots" -> SKYWALKER_BOOTS;
            case "archmage_crown" -> ARCHMAGE_CROWN;
            case "archmage_robe" -> ARCHMAGE_ROBE;
            case "froststep_boots" -> FROSTSTEP_BOOTS;
            case "cinder_hat" -> CINDER_HOOD;
            case "cinder_robe" -> CINDER_ROBE;
            case "cinder_boots" -> CINDER_BOOTS;
            case "glacier_hat" -> GLACIER_CIRCLET;
            case "glacier_robe" -> GLACIER_ROBE;
            case "glacier_boots" -> GLACIER_BOOTS;
            case "tempest_hat" -> TEMPEST_HOOD;
            case "tempest_robe" -> TEMPEST_ROBE;
            case "tempest_boots" -> TEMPEST_BOOTS;
            case "rift_hat" -> RIFT_CROWN;
            case "rift_robe" -> RIFT_ROBE;
            case "rift_boots" -> RIFT_BOOTS;
            default -> MAGE_HAT;
        };
    }

    private static Map<String, StaffProfile> buildProfileIndex() {
        Map<String, StaffProfile> result = new LinkedHashMap<>();
        for (StaffProfile profile : PROFILES) result.put(profile.id(), profile);
        return Map.copyOf(result);
    }

    public static StaffProfile equipped(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ArcaneStaffItem staff) return staff.profile();
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof ArcaneStaffItem staff) return staff.profile();
        return StaffProfile.NONE;
    }
}
