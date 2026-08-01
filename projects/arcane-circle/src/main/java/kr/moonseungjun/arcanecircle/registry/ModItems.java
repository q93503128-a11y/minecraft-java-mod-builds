package kr.moonseungjun.arcanecircle.registry;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem;
import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
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
            "novice_staff", "견습 마도봉", "마력핵의 용량과 회복을 안정화한다.", "각성 시 최초 1회 지급",
            20, 1.0, 1.0, 1.0, 1.0, 1.10, null, 1.0);
    public static final StaffProfile EMBER_PROFILE = new StaffProfile(
            "ember_staff", "홍염 지팡이", "화염술을 과충전하는 공격형 지팡이.", "견습 마도봉 + 블레이즈 막대 + 마그마 크림",
            0, 1.10, 1.0, 1.0, 1.0, 1.0, SpellDefinition.School.FIRE, 1.28);
    public static final StaffProfile GLACIAL_PROFILE = new StaffProfile(
            "glacial_staff", "빙하 지팡이", "서리 주문의 도달 범위와 결빙력을 확장한다.", "견습 마도봉 + 푸른 얼음 + 자수정 조각",
            10, 1.0, 1.0, 1.18, 1.05, 1.0, SpellDefinition.School.FROST, 1.24);
    public static final StaffProfile ZEPHYR_PROFILE = new StaffProfile(
            "zephyr_staff", "청풍 지팡이", "기초 위력을 낮추고 이동·연속 시전을 강화한다.", "견습 마도봉 + 팬텀 막 + 깃털",
            0, 0.96, 0.90, 1.05, 0.80, 1.10, SpellDefinition.School.WIND, 1.22);
    public static final StaffProfile AEGIS_PROFILE = new StaffProfile(
            "aegis_staff", "수호 지팡이", "방벽의 흡수량과 유지력을 높인다.", "견습 마도봉 + 방패 + 금괴",
            25, 0.95, 1.0, 0.92, 1.08, 1.0, SpellDefinition.School.WARD, 1.36);
    public static final StaffProfile VERDANT_PROFILE = new StaffProfile(
            "verdant_staff", "생명의 지팡이", "치유력과 자연 마력 회복을 강화한다.", "견습 마도봉 + 황금 사과 + 이끼 블록",
            15, 0.95, 1.0, 1.0, 1.05, 1.30, SpellDefinition.School.LIFE, 1.35);
    public static final StaffProfile RIFT_PROFILE = new StaffProfile(
            "rift_staff", "균열 지팡이", "공간술의 사거리와 전이 후 보호막을 확장한다.", "견습 마도봉 + 엔더의 눈 + 우는 흑요석",
            20, 1.18, 1.0, 1.20, 1.06, 1.0, SpellDefinition.School.SPACE, 1.22);
    public static final StaffProfile SAGE_PROFILE = new StaffProfile(
            "sage_staff", "현자의 지팡이", "마력 효율과 회로 냉각을 우선하는 범용 지팡이.", "견습 마도봉 + 마법이 부여된 책 + 금 블록",
            30, 0.84, 0.94, 1.0, 0.88, 1.15, SpellDefinition.School.ARCANE, 1.14);
    public static final StaffProfile ARCHMAGE_PROFILE = new StaffProfile(
            "archmage_staff", "대마도사의 지팡이", "모든 회로를 고르게 증폭하는 최고위 지팡이.", "현자의 지팡이 + 네더의 별 + 메아리 조각",
            55, 0.90, 1.18, 1.15, 0.86, 1.18, null, 1.0);

    public static final DeferredItem<ArcaneStaffItem> NOVICE_STAFF = register("novice_staff", NOVICE_PROFILE, Rarity.COMMON);
    public static final DeferredItem<ArcaneStaffItem> EMBER_STAFF = register("ember_staff", EMBER_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> GLACIAL_STAFF = register("glacial_staff", GLACIAL_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> ZEPHYR_STAFF = register("zephyr_staff", ZEPHYR_PROFILE, Rarity.UNCOMMON);
    public static final DeferredItem<ArcaneStaffItem> AEGIS_STAFF = register("aegis_staff", AEGIS_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> VERDANT_STAFF = register("verdant_staff", VERDANT_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> RIFT_STAFF = register("rift_staff", RIFT_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> SAGE_STAFF = register("sage_staff", SAGE_PROFILE, Rarity.RARE);
    public static final DeferredItem<ArcaneStaffItem> ARCHMAGE_STAFF = register("archmage_staff", ARCHMAGE_PROFILE, Rarity.EPIC);

    private static final List<StaffProfile> PROFILES = List.of(
            NOVICE_PROFILE, EMBER_PROFILE, GLACIAL_PROFILE, ZEPHYR_PROFILE,
            AEGIS_PROFILE, VERDANT_PROFILE, RIFT_PROFILE, SAGE_PROFILE, ARCHMAGE_PROFILE);
    private static final Map<String, StaffProfile> PROFILE_BY_ID = buildProfileIndex();

    private ModItems() {}

    private static DeferredItem<ArcaneStaffItem> register(String id, StaffProfile profile, Rarity rarity) {
        return ITEMS.registerItem(id, properties -> new ArcaneStaffItem(properties.rarity(rarity), profile));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (DeferredItem<ArcaneStaffItem> item : all()) event.accept(item.get());
        }
    }

    public static List<DeferredItem<ArcaneStaffItem>> all() {
        return List.of(NOVICE_STAFF, EMBER_STAFF, GLACIAL_STAFF, ZEPHYR_STAFF,
                AEGIS_STAFF, VERDANT_STAFF, RIFT_STAFF, SAGE_STAFF, ARCHMAGE_STAFF);
    }

    public static List<StaffProfile> profiles() {
        return PROFILES;
    }

    public static StaffProfile profile(String id) {
        return PROFILE_BY_ID.getOrDefault(id, StaffProfile.NONE);
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
