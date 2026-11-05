package dev.moonseungjun.augmentationsurvival.augment.active;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Player-facing descriptions of the 26 explicitly bound boss authority packages. */
public final class AuthorityPowerCatalog {
    public record Entry(String id, String group, String bossName, String effect) { }

    public static final List<Entry> ENTRIES = List.of(
            new Entry("vanilla_wither", "바닐라", "위더", "위더의 2페이즈 보호막을 해방해 8초간 투사체 피해를 무효화합니다."),
            new Entry("vanilla_ender_dragon", "바닐라", "엔더 드래곤", "6블록 근접 충격파와 넉백. 바라보는 방향 12블록·70도 범위에 드래곤 브레스 피해."),
            new Entry("vanilla_warden", "바닐라", "워든", "1.7초 충전 후 벽을 관통하는 음파 공격. 단일 적에게 고유 음파 피해와 강한 넉백."),
            new Entry("vanilla_elder_guardian", "바닐라", "엘더 가디언", "50블록 내 적에게 광역 채굴 피로 III의 저주. 몹 전투용 둔화·나약함 8초."),
            new Entry("chronodawn_time_guardian", "Chrono Dawn", "Time Guardian", "원본 Time Blast 원거리탄·적 뒤 순간이동, 0.75초 뒤 4블록 시간 왜곡 충격파. 둔화 II·채굴 피로 I."),
            new Entry("chronodawn_chronos_warden", "Chrono Dawn", "Chronos Warden", "4블록 지면 강타 후 5초 석화 자세. 전방·측면 공격 피해 80% 감소, 후방 공격은 그대로 받습니다."),
            new Entry("chronodawn_clockwork_colossus", "Chrono Dawn", "Clockwork Colossus", "원본 톱니바퀴 투사체를 발사하고 전방 10블록에 충격파. 신속 II·최대체력 15% 회복. 외부 모드 미설치 시 단발 공격으로 대체."),
            new Entry("chronodawn_temporal_phantom", "Chrono Dawn", "Temporal Phantom", "원본 Time Blast(Warp Bolt) 발사·적 뒤 순간이동 타격. 2초 위상 회피: 근접 공격 30% 회피 확률, 성공 후 5초 재회피 제한."),
            new Entry("chronodawn_entropy_keeper", "Chrono Dawn", "Entropy Keeper", "대상 발밑 3×3 부패 지대 5초(둔화 II·독 I). 2초 후 6블록 엔트로피 폭발·시듦 II 10초·넉백."),
            new Entry("chronodawn_time_tyrant", "Chrono Dawn", "Time Tyrant", "3초간 둔화 V 시간왜곡, 적 뒤 순간이동·신속 II·광역 공격, 체력 10% 회복과 8초간 투사체 피해 70% 감소."),
            new Entry("worldbosses_sylvaron", "World Bosses", "Sylvaron", "적에게 5블록 뿌리·포자 지대를 4초 생성. 독과 구속을 반복하며 시전자에게 저항 I."),
            new Entry("worldbosses_aureon", "World Bosses", "Aureon", "적에게 번개 강타 후 4블록 Thunder Cage 5초 설치. 경고 1초 뒤 주기적으로 둔화 III·발광·전기 피해."),
            new Entry("worldbosses_nekhraz", "World Bosses", "Nekhraz", "모래폭풍으로 5.5블록을 어둡게 만들고 1.25초 후 적 뒤로 잠복 이동·기습."),
            new Entry("worldbosses_varkhul", "World Bosses", "Varkhul", "적에게 Infernal Chains(강한 둔화·나약함)를 걸고, 5초 Flame Prison 설치. 주변 적을 불태우며 감금."),
            new Entry("worldbosses_morvath", "World Bosses", "Morvath", "8블록 공포 파동 후 영혼 흡수 4초. 실제 적에게 입힌 피해량에 비례해 체력 회복."),
            new Entry("worldbosses_elyndra", "World Bosses", "Elyndra", "공허 균열로 적을 6.5블록 범위 안으로 끌어당기고 2초 뒤 폭발·공중 띄우기."),
            new Entry("worldbosses_astrael", "World Bosses", "Astrael", "Primordial Chains로 9블록 적을 구속하고 1.3초 후 Cataclysm Nova 폭발·넉백. 저항 II·신속 II·화염 저항."),
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
