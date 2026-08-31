#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
S = ROOT / "projects/survival-ascension"
INFRA = S / "src/main/java/kr/moonseungjun/survivalascension/infrastructure"


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


write(INFRA / "InfrastructureProject.java", r'''package kr.moonseungjun.survivalascension.infrastructure;

import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public enum InfrastructureProject {
    QUARRY_NETWORK(
            "quarry_network", "채석장 네트워크", "채굴 Lv.90 터널 5×5×8 · Lv.100 7×7×10", 0,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 48, 1),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 2),
                    exact("diamond", Items.DIAMOND, "다이아몬드", 6, 3)
            )),
    IRRIGATION_WORKS(
            "irrigation_works", "관개 시설", "농사 Lv.30 실제 씨앗 소비 자동 재파종", 0,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 120, 0, 1),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 2),
                    exact("glass", Items.GLASS, "유리", 32, 3),
                    exact("slime", Items.SLIME_BALL, "슬라임볼", 8, 4)
            )),
    BUILDER_FOUNDRY(
            "builder_foundry", "건축 공방", "건축 Lv.90 입체 5×5×5 · Lv.100 7×7×7", 0,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 96, 1, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 3),
                    exact("obsidian", Items.OBSIDIAN, "흑요석", 12, 4)
            )),
    COMBAT_ACADEMY(
            "combat_academy", "전투 훈련장", "전투 Lv.90 질주 전방 균열선 6.5블록/10체 · Lv.100 8블록/14체 · 현장 숙련 10블록/18체", 0,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 144, 0, 1),
                    exact("emerald", Items.EMERALD, "에메랄드", 16, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 3),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 4, 4)
            )),
    CIVIL_WORKS(
            "civil_works", "토목 공사소", "전설 단계 · 건축 Lv.60 3폭 도로/교량 17칸 → Lv.90 33 · Lv.100 49 · 현장 숙련 65", 1,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 896, 0, 1, 2),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 96, 3, 4)
            )),
    INDUSTRIAL_WORKS(
            "industrial_works", "산업 가공소", "전설 단계 · 채굴·벌목·농사·정밀자원을 4계통 대량 생산망으로 연결", 1,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 192, 1, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 48, 3),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 24, 4)
            )),
    APEX_TRACKING_POST(
            "apex_tracking_post", "정점 추적소", "전설 단계 · 완수한 원정권에서 반복 정점 사냥 개방", 1,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 144, 0, 1),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 48, 2),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 4, 3),
                    exact("nether_star", Items.NETHER_STAR, "네더의 별", 1, 4)
            )),
    ASCENSION_NEXUS(
            "ascension_nexus", "승천 중추", "종말 단계 · 기동 Lv.90 공중 돌진 2회 / Lv.100 3회 · 완공 후 승천 시련", 2,
            List.of(
                    exact("nether_star", Items.NETHER_STAR, "네더의 별", 1, 0),
                    exact("dragon_breath", Items.DRAGON_BREATH, "드래곤의 숨결", 8, 1),
                    exact("obsidian", Items.OBSIDIAN, "흑요석", 64, 2),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 64, 3),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 8, 4)
            ));

    private final String id;
    private final String koreanName;
    private final String benefit;
    private final int requiredWorldStage;
    private final List<Requirement> requirements;

    InfrastructureProject(String id, String koreanName, String benefit, int requiredWorldStage, List<Requirement> requirements) {
        this.id = id;
        this.koreanName = koreanName;
        this.benefit = benefit;
        this.requiredWorldStage = requiredWorldStage;
        this.requirements = requirements;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public String benefit() { return benefit; }
    public int requiredWorldStage() { return requiredWorldStage; }
    public List<Requirement> requirements() { return requirements; }

    public static InfrastructureProject fromId(String id) {
        for (InfrastructureProject project : values()) if (project.id.equals(id)) return project;
        return null;
    }

    private static Requirement shared(String key, SharedEconomyCompat.ResourceCategory category, int amount, Integer... legacyIndices) {
        return new Requirement(key, category.koreanName() + " 재화", amount, null, category, List.of(legacyIndices));
    }

    private static Requirement exact(String key, Item item, String label, int amount, Integer... legacyIndices) {
        return new Requirement(key, label, amount, item, null, List.of(legacyIndices));
    }

    public record Requirement(
            String key,
            String label,
            int amount,
            Item item,
            SharedEconomyCompat.ResourceCategory resourceCategory,
            List<Integer> legacyIndices
    ) {
        public boolean isSharedResource() { return resourceCategory != null; }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            return resourceCategory != null ? SharedEconomyCompat.matches(resourceCategory, stack) : item != null && stack.is(item);
        }
    }
}
''')

write(INFRA / "InfrastructureData.java", r'''package kr.moonseungjun.survivalascension.infrastructure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InfrastructureData extends SavedData {
    private static final int CURRENT_SCHEMA = 2;
    private static final Codec<Map<String, Integer>> FUNDING_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    public static final SavedDataType<InfrastructureData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "infrastructure_v1"),
            InfrastructureData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    FUNDING_CODEC.optionalFieldOf("funding", Map.of()).forGetter(InfrastructureData::fundingSnapshot),
                    Codec.INT.optionalFieldOf("schema_version", 1).forGetter(InfrastructureData::schemaVersion)
            ).apply(instance, InfrastructureData::new))
    );

    private final Map<String, Integer> funding = new HashMap<>();
    private int schemaVersion = CURRENT_SCHEMA;

    public InfrastructureData() {}

    private InfrastructureData(Map<String, Integer> funding, int schemaVersion) {
        funding.forEach((key, value) -> this.funding.put(key, Math.max(0, value)));
        this.schemaVersion = Math.max(1, schemaVersion);
    }

    private Map<String, Integer> fundingSnapshot() { return Map.copyOf(funding); }
    private int schemaVersion() { return schemaVersion; }

    public static InfrastructureData get(MinecraftServer server) {
        InfrastructureData data = server.getDataStorage().computeIfAbsent(TYPE);
        data.migrateLegacyFundingIfNeeded();
        return data;
    }

    public static InfrastructureData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    public int contributed(InfrastructureProject project, int requirementIndex) {
        return contributed(project, project.requirements().get(requirementIndex));
    }

    public int contributed(InfrastructureProject project, InfrastructureProject.Requirement requirement) {
        return funding.getOrDefault(stableKey(project, requirement), 0);
    }

    public int remaining(InfrastructureProject project, int requirementIndex) {
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        return Math.max(0, requirement.amount() - contributed(project, requirement));
    }

    public int addContribution(InfrastructureProject project, int requirementIndex, int amount) {
        if (amount <= 0) return 0;
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        String key = stableKey(project, requirement);
        int before = funding.getOrDefault(key, 0);
        int after = Math.min(requirement.amount(), before + amount);
        if (after != before) {
            funding.put(key, after);
            setDirty();
        }
        return after - before;
    }

    public boolean isComplete(InfrastructureProject project) {
        for (int i = 0; i < project.requirements().size(); i++) {
            if (remaining(project, i) > 0) return false;
        }
        return true;
    }

    private void migrateLegacyFundingIfNeeded() {
        if (schemaVersion >= CURRENT_SCHEMA) return;

        boolean changed = false;
        for (InfrastructureProject project : InfrastructureProject.values()) {
            Set<Integer> oldIndices = new HashSet<>();
            for (InfrastructureProject.Requirement requirement : project.requirements()) {
                oldIndices.addAll(requirement.legacyIndices());
                String targetKey = stableKey(project, requirement);
                if (funding.containsKey(targetKey)) continue;

                int migrated = 0;
                for (int legacyIndex : requirement.legacyIndices()) {
                    migrated += funding.getOrDefault(legacyKey(project, legacyIndex), 0);
                }
                migrated = Math.min(requirement.amount(), Math.max(0, migrated));
                if (migrated > 0) {
                    funding.put(targetKey, migrated);
                    changed = true;
                }
            }
            for (int legacyIndex : oldIndices) {
                changed |= funding.remove(legacyKey(project, legacyIndex)) != null;
            }
        }

        schemaVersion = CURRENT_SCHEMA;
        setDirty();
    }

    private static String stableKey(InfrastructureProject project, InfrastructureProject.Requirement requirement) {
        return project.id() + ":req:" + requirement.key();
    }

    private static String legacyKey(InfrastructureProject project, int requirementIndex) {
        return project.id() + ":" + requirementIndex;
    }
}
''')

service_path = INFRA / "InfrastructureService.java"
service = service_path.read_text(encoding="utf-8")
replacements = {
    'int take = Math.min(remaining, countItem(player, requirement.item()));':
        'int take = Math.min(remaining, countRequirement(player, requirement));',
    'if (!consumeItem(player, requirement.item(), take)) continue;':
        'if (!consumeRequirement(player, requirement, take)) continue;',
    'if (countItem(player, requirement.item()) < remaining) return false;':
        'if (countRequirement(player, requirement) < remaining) return false;',
    'player.sendSystemMessage(Component.literal("§6[인프라] §f인벤토리와 현재 사용 가능한 등록 물류 통들에 이 프로젝트가 더 필요로 하는 재료가 없습니다."));':
        'player.sendSystemMessage(Component.literal("§6[인프라] §f인벤토리·공용 보급고·현재 사용 가능한 등록 물류 통에 이 프로젝트가 더 필요로 하는 재료가 없습니다."));',
    '+ "개§f를 투입했습니다. §7인벤토리와 같은 차원에서 현재 로딩된 등록 물류 통 재고를 함께 사용"));':
        '+ "개§f를 투입했습니다. §7공용 재화는 같은 분류의 실제 아이템을 대체 사용하며, 인벤토리·공용 보급고·사용 가능한 등록 물류 재고를 함께 사용"));',
    '''        if (InfrastructureData.get(player).isComplete(InfrastructureProject.INDUSTRIAL_WORKS)) {
            player.sendSystemMessage(Component.literal("  §7투입원: 인벤토리 + 같은 차원에서 현재 로딩된 등록 거점 통/창고 통/전초 재고"));
        }
''':
        '''        player.sendSystemMessage(Component.literal("  §7투입원: 인벤토리 + 가까운 공용 보급고 + 현재 사용 가능한 등록 물류 재고"));
        player.sendSystemMessage(Component.literal("  §7공용 재화: 석재/금속처럼 묶인 항목은 같은 분류의 실제 아이템끼리 서로 대체 가능 · 희귀 촉매는 지정 아이템 그대로 필요"));
''',
    '''    private static int countItem(ServerPlayer player, Item item) {
        return FieldDepotService.countMaterial(player, item);
    }

    private static boolean consumeItem(ServerPlayer player, Item item, int amount) {
        return FieldDepotService.consume(player, item, amount);
    }
''':
        '''    private static int countRequirement(ServerPlayer player, InfrastructureProject.Requirement requirement) {
        return FieldDepotService.countMatching(player, requirement::matches);
    }

    private static boolean consumeRequirement(ServerPlayer player, InfrastructureProject.Requirement requirement, int amount) {
        return FieldDepotService.consumeMatching(player, requirement::matches, amount);
    }
'''
}
for old, new in replacements.items():
    if new in service:
        continue
    if old not in service:
        raise RuntimeError(f"InfrastructureService patch anchor missing: {old[:100]!r}")
    service = service.replace(old, new)
service = service.replace('import net.minecraft.world.item.Item;\n', '')
write(service_path, service)

# Reproducible acceptance checks: old index-based funding must no longer be the runtime key,
# and infrastructure funding must consume through the same category matcher as production.
project = (INFRA / "InfrastructureProject.java").read_text(encoding="utf-8")
data = (INFRA / "InfrastructureData.java").read_text(encoding="utf-8")
service = service_path.read_text(encoding="utf-8")
assert 'shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 896, 0, 1, 2)' in project
assert 'shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 192, 1, 2)' in project
assert 'CURRENT_SCHEMA = 2' in data
assert 'project.id() + ":req:" + requirement.key()' in data
assert 'migrateLegacyFundingIfNeeded' in data
assert 'requirement::matches' in service
assert 'requirement.item()' not in service
print("INFRASTRUCTURE SHARED ECONOMY PATCH PASS")
