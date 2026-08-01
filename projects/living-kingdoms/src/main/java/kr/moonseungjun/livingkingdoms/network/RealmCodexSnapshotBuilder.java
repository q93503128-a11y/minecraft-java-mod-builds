package kr.moonseungjun.livingkingdoms.network;

import kr.moonseungjun.livingkingdoms.crime.CrimeSavedData;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.skill.MasteryProgressionSavedData;
import kr.moonseungjun.livingkingdoms.skill.SkillProgressionManager;
import kr.moonseungjun.livingkingdoms.world.RealmSiteLayoutSavedData;
import kr.moonseungjun.livingkingdoms.world.RealmSitePlanner;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RealmCodexSnapshotBuilder {
    private static final String[] MASTERY_TRACKS = {
            MasteryProgressionSavedData.COMBAT,
            MasteryProgressionSavedData.DEFENSE,
            MasteryProgressionSavedData.MINING,
            MasteryProgressionSavedData.LOGGING,
            MasteryProgressionSavedData.FARMING,
            MasteryProgressionSavedData.GATHERING,
            MasteryProgressionSavedData.EXPLORATION
    };

    private RealmCodexSnapshotBuilder() {
    }

    public static OpenCodexPayload build(ServerPlayer player, String requestedPage) {
        String page = switch (requestedPage) {
            case "equipment", "map", "skills" -> requestedPage;
            default -> "overview";
        };
        Map<String, String> values = new LinkedHashMap<>();
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);

        values.put("player", player.getGameProfile().name());
        values.put("species_id", profile == null ? "human" : profile.speciesId());
        values.put("species", profile == null ? "미정" : speciesName(profile.speciesId()));
        values.put("homeland", profile == null ? "미정" : homelandName(profile.homelandId()));
        values.put("affiliation", profile == null ? "무소속" : affiliationName(profile.homelandId()));
        values.put("citizenship", profile == null ? "등록되지 않음" : citizenshipName(profile.homelandId()));
        values.put("background", profile == null ? "미정" : backgroundName(profile.backgroundId()));
        values.put("residence", profile == null ? "미정" : residenceName(profile.residenceId()));
        values.put("trait_title", SkillProgressionManager.traitTitle(player));
        values.put("trait_description", SkillProgressionManager.traitDescription(player));
        values.put("health", oneDecimal(player.getHealth()) + " / " + oneDecimal(player.getMaxHealth()));
        values.put("armor", Integer.toString(player.getArmorValue()));
        values.put("food", player.getFoodData().getFoodLevel() + " / 20");
        values.put("level", Integer.toString(player.experienceLevel));
        values.put("experience", Integer.toString(player.totalExperience));
        values.put("position", player.blockPosition().getX() + ", " + player.blockPosition().getY()
                + ", " + player.blockPosition().getZ());
        values.put("region", regionName(realm, player.blockPosition().getX(), player.blockPosition().getZ()));
        values.put("realm", player.level().dimension().equals(StarterRealmManager.REALM_KEY)
                ? "살아있는 왕국 대륙" : "외부 차원");

        values.put("mainhand", itemName(player.getMainHandItem()));
        values.put("offhand", itemName(player.getOffhandItem()));
        values.put("head", itemName(player.getItemBySlot(EquipmentSlot.HEAD)));
        values.put("chest", itemName(player.getItemBySlot(EquipmentSlot.CHEST)));
        values.put("legs", itemName(player.getItemBySlot(EquipmentSlot.LEGS)));
        values.put("feet", itemName(player.getItemBySlot(EquipmentSlot.FEET)));

        CrimeSavedData.CrimeRecord crime = realm == null
                ? new CrimeSavedData.CrimeRecord(0, 0L, "wilderness", 0, 0)
                : realm.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE).record(player.getUUID());
        values.put("wanted", Integer.toString(crime.wanted()));
        values.put("resistance", Integer.toString(crime.resistance()));
        values.put("jurisdiction", jurisdictionName(crime.jurisdiction()));
        values.put("arrest", Math.min(100, crime.arrestTicks()) + "%");

        var skills = SkillProgressionManager.state(player);
        values.put("skill_points", Integer.toString(skills.points()));
        values.put("skill_milestone", Integer.toString(skills.levelMilestone()));
        values.put("unlocked_skills", String.join(",", skills.unlocked()));
        values.put("growth_rule", "행동 숙련은 계속 성장하며 기술 트리는 부가 효과만 해금합니다.");
        for (String track : MASTERY_TRACKS) {
            values.put("mastery_" + track + "_name", MasteryProgressionSavedData.displayName(track));
            values.put("mastery_" + track + "_level",
                    Integer.toString(SkillProgressionManager.masteryLevel(player, track)));
            values.put("mastery_" + track + "_xp",
                    Long.toString(SkillProgressionManager.masteryXp(player, track)));
            values.put("mastery_" + track + "_progress",
                    String.format(Locale.ROOT, "%.4f", SkillProgressionManager.masteryProgress(player, track)));
        }

        putSite(values, realm, "erden_kingdom", "erden");
        putSite(values, realm, "silvana_forest", "silvana");
        putSite(values, realm, "kardum_league", "kardum");

        if (profile != null && realm != null) {
            BlockPos home = RealmSitePlanner.residencePosition(realm, profile.homelandId(), profile.residenceId());
            values.put("home_x", Integer.toString(home.getX()));
            values.put("home_z", Integer.toString(home.getZ()));
        }
        values.putIfAbsent("home_x", "0");
        values.putIfAbsent("home_z", "0");
        values.put("player_x", Integer.toString(player.blockPosition().getX()));
        values.put("player_z", Integer.toString(player.blockPosition().getZ()));
        return new OpenCodexPayload(page, encode(values));
    }

    private static void putSite(Map<String, String> values, ServerLevel realm,
                                String homelandId, String prefix) {
        RealmSiteLayoutSavedData.RealmSite site = realm == null ? null : RealmSitePlanner.site(realm, homelandId);
        int[] fallback = RealmSitePlanner.nominalCenter(homelandId);
        values.put(prefix + "_x", Integer.toString(site == null ? fallback[0] : site.centerX()));
        values.put(prefix + "_z", Integer.toString(site == null ? fallback[1] : site.centerZ()));
    }

    private static String encode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach((key, value) -> builder.append(safe(key)).append('\t').append(safe(value)).append('\n'));
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String itemName(ItemStack stack) {
        return stack.isEmpty() ? "없음" : stack.getHoverName().getString()
                + (stack.getCount() > 1 ? " ×" + stack.getCount() : "");
    }

    private static String speciesName(String id) {
        return switch (id) { case "elf" -> "엘프"; case "dwarf" -> "드워프"; default -> "인간"; };
    }

    private static String homelandName(String id) {
        return switch (id) {
            case "silvana_forest" -> "실바나 수림 공동체";
            case "kardum_league" -> "카르둠 산악 연맹";
            default -> "에르덴 왕국";
        };
    }

    private static String affiliationName(String id) {
        return switch (id) {
            case "silvana_forest" -> "실바나 수림 의회";
            case "kardum_league" -> "카르둠 산악 연맹";
            default -> "에르덴 왕국 · 로엔 변경백령";
        };
    }

    private static String citizenshipName(String id) {
        return switch (id) {
            case "silvana_forest" -> "수림 공동체 구성원";
            case "kardum_league" -> "연맹 등록 자유민";
            default -> "에르덴 왕국 시민";
        };
    }

    private static String backgroundName(String id) {
        return switch (id) {
            case "fisher_family" -> "어부 집안";
            case "wanderer" -> "방랑자";
            case "scholar_student" -> "학술 수련생";
            default -> "평범한 주민";
        };
    }

    private static String residenceName(String id) {
        PlayableOriginCatalog.ResidenceOption option = PlayableOriginCatalog.residences().get(id);
        return option == null ? id : option.displayName();
    }

    private static String jurisdictionName(String id) {
        return switch (id) {
            case "erden_kingdom" -> "에르덴 사법권";
            case "silvana_forest" -> "실바나 수림법";
            case "kardum_league" -> "카르둠 연맹법";
            default -> "수배 없음";
        };
    }

    private static String regionName(ServerLevel realm, int x, int z) {
        if (realm == null) return "미개척 대륙";
        if (near(x, z, RealmSitePlanner.site(realm, "erden_kingdom"), 420)) return "에르덴 로엔 변경백령";
        if (near(x, z, RealmSitePlanner.site(realm, "silvana_forest"), 360)) return "실바나 수림권";
        if (near(x, z, RealmSitePlanner.site(realm, "kardum_league"), 360)) return "카르둠 산악권";
        return "미개척 대륙";
    }

    private static boolean near(int x, int z, RealmSiteLayoutSavedData.RealmSite site, int radius) {
        if (site == null) return false;
        long dx = x - site.centerX();
        long dz = z - site.centerZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
