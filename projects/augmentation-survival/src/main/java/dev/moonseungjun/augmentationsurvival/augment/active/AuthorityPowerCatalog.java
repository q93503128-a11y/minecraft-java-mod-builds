package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Player-facing descriptions of the exact twenty-four server-supported authority packages. */
public final class AuthorityPowerCatalog {
    public record Entry(String id, String group, String bossName, String effect) { }

    public static final List<Entry> ENTRIES = List.of(
            new Entry("vanilla_wither", "바닐라", "위더", "위더의 2페이즈 보호막을 해방해 8초간 투사체 피해를 무효화합니다."),
            new Entry("vanilla_ender_dragon", "바닐라", "엔더 드래곤", "6블록 근접 충격파와 넉백. 바라보는 방향 12블록·70도 범위에 드래곤 브레스 피해."),
            new Entry("vanilla_warden", "바닐라", "워든", "1.7초 충전 후 벽을 관통하는 음파 공격. 단일 적에게 고유 음파 피해와 강한 넉백."),
            new Entry("vanilla_elder_guardian", "바닐라", "엘더 가디언", "50블록 내 적에게 광역 채굴 피로 III의 저주. 몹 전투용 둔화·나약함 8초."),
            new Entry("chronodawn_time_guardian", "Chrono Dawn", "Time Guardian", "적 뒤로 순간이동한 뒤 0.75초 후 착지 지점 4블록에 시간 왜곡 충격파. 둔화 II·채굴 피로 I 4초."),
            new Entry("chronodawn_chronos_warden", "Chrono Dawn", "Chronos Warden", "4블록 지면 강타 후 5초 석화 자세. 전방·측면 공격 피해 80% 감소, 후방 공격은 그대로 받습니다."),
            new Entry("chronodawn_clockwork_colossus", "Chrono Dawn", "Clockwork Colossus", "원본 톱니바퀴 투사체를 발사하고 전방 10블록에 충격파. 신속 II·최대체력 15% 회복. 외부 모드 미설치 시 단발 공격으로 대체."),
            new Entry("chronodawn_temporal_phantom", "Chrono Dawn", "Temporal Phantom", "2초간 근접 공격 30% 위상 회피(발동 후 5초 재회피 제한), 뒤로 순간이동 공격·둔화, 전투 분신 2체 소환."),
            new Entry("chronodawn_entropy_keeper", "Chrono Dawn", "Entropy Keeper", "대상 발밑에 3×3 부패 지대 5초 생성. 지대 안의 적에게 둔화 II·독 I을 반복 적용합니다."),
            new Entry("chronodawn_time_tyrant", "Chrono Dawn", "Time Tyrant", "대상 강한 둔화·순간이동. 신속 II 8초, 5블록 광역 공격, 최대체력 10% 회복."),
            new Entry("worldbosses_sylvaron", "World Bosses", "Sylvaron", "저항 I 4초. 7블록 안의 적에게 강한 둔화·나약함·독과 피해."),
            new Entry("worldbosses_aureon", "World Bosses", "Aureon", "18블록 내 대상 집중 공격. 7블록 안에 둔화·발광·광역 피해."),
            new Entry("worldbosses_nekhraz", "World Bosses", "Nekhraz", "8블록 주변 적에게 어둠·둔화. 대상 뒤로 순간이동해 강타하고 띄움."),
            new Entry("worldbosses_varkhul", "World Bosses", "Varkhul", "주 대상 강한 둔화·나약함. 6블록 안의 적을 점화하고 피해."),
            new Entry("worldbosses_morvath", "World Bosses", "Morvath", "8블록 어둠·나약함·넉백. 대상에게 피해를 주고 최대체력 15% 한도 회복."),
            new Entry("worldbosses_elyndra", "World Bosses", "Elyndra", "9블록 주변 적을 끌어당겨 둔화·피해를 주고 위로 띄움."),
            new Entry("worldbosses_astrael", "World Bosses", "Astrael", "저항 II·신속 II·화염 저항 6초. 9블록 적에게 강한 광역 피해·둔화·나약함·넉백."),
            new Entry("worldbosses_shadow_king", "World Bosses", "Shadow King", "대상 타격·순간이동. 7블록 어둠·피해, 12초 전투 분신 2체."),
            new Entry("worldbosses_dragon_mother", "World Bosses", "Dragon Mother", "전방 화염 브레스와 4블록 넉백. 주요 대상 화염 공격, 전투 분신 2체(10초)."),
            new Entry("worldbosses_storm_griffin", "World Bosses", "Storm Griffin", "9블록 강한 돌풍·넉백·발광·광역 피해. 18블록 대상 추가 타격."),
            new Entry("worldbosses_shadow_weaver", "World Bosses", "Shadow Weaver", "저항 III 4초, 8블록 둔화·나약함·피해, 전투 분신 3체(12초)와 대상 추가 공격."),
            new Entry("mcendgame_arachne", "MCEndgame", "Arachne", "적을 당겨 둔화한 뒤 전방 4블록·3블록 폭의 근접 휩쓸기."),
            new Entry("mcendgame_bonecrusher", "MCEndgame", "Bonecrusher", "최대 50블록 적 뒤로 순간이동해 접근. 6블록 강한 충격파와 넉백."),
            new Entry("mcendgame_elf_duelist", "MCEndgame", "Elf Duelist", "근접 공격과 후방 도약. 10블록 내 전방 투사체를 50% 확률로 반사."),
            new Entry("mcendgame_beakburn", "MCEndgame", "Beakburn", "12블록 돌풍, 전방 8블록·70도 화염 브레스. 15블록 적에게 7% 추가 화염 타격."),
            new Entry("mcendgame_beastweaver", "MCEndgame", "Beastweaver", "9블록 강력한 충격파·둔화, 원거리 대상 추가 공격, 전투 분신 3체(15초)·신속.")
    );

    private AuthorityPowerCatalog() { }

    public static Entry find(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) return entry;
        }
        if (id != null && id.startsWith(AuthorityTheftService.FALLBACK_PREFIX)) {
            String entity = id.substring(AuthorityTheftService.FALLBACK_PREFIX.length());
            String display = entity;
            Identifier key = Identifier.tryParse(entity);
            if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
                display = BuiltInRegistries.ENTITY_TYPE.getValue(key).getDescription().getString();
            }
            return new Entry(id, "기타 보스", display,
                    "원본 고유 패턴이 아직 확인되지 않아 보스의 체력·공격력을 반영한 6초 전투 강화로 대체합니다.");
        }
        return null;
    }

    public static List<Entry> available(List<String> owned) {
        ArrayList<Entry> entries = new ArrayList<>(ENTRIES);
        for (String id : owned) {
            Entry entry = find(id);
            if (entry != null && entries.stream().noneMatch(value -> value.id().equals(id))) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }
}
