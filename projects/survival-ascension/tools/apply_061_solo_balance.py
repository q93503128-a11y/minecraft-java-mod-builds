#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace(rel, old, new, count=None):
    text = read(rel)
    found = text.count(old)
    expected = 1 if count is None else count
    if found != expected:
        raise SystemExit(f"{rel}: expected {expected} occurrences, found {found}: {old[:100]!r}")
    write(rel, text.replace(old, new))


def replace_re(rel, pattern, repl, count=1):
    text = read(rel)
    out, n = re.subn(pattern, repl, text, count=count, flags=re.S)
    if n != count:
        raise SystemExit(f"{rel}: regex expected {count}, got {n}: {pattern[:100]!r}")
    write(rel, out)


# ---------------------------------------------------------------------------
# 1) Infrastructure costs: tuned for a single-player survival world.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java"
replacements = {
    'new Requirement(Items.COBBLESTONE, "조약돌", 1024)': 'new Requirement(Items.COBBLESTONE, "조약돌", 192)',
    'new Requirement(Items.IRON_INGOT, "철 주괴", 256)': 'new Requirement(Items.IRON_INGOT, "철 주괴", 48)',
    'new Requirement(Items.REDSTONE, "레드스톤", 128)': 'new Requirement(Items.REDSTONE, "레드스톤", 24)',
    'new Requirement(Items.DIAMOND, "다이아몬드", 32)': 'new Requirement(Items.DIAMOND, "다이아몬드", 6)',
    'new Requirement(Items.COPPER_INGOT, "구리 주괴", 512)': 'new Requirement(Items.COPPER_INGOT, "구리 주괴", 96)',
    'new Requirement(Items.IRON_INGOT, "철 주괴", 128)': 'new Requirement(Items.IRON_INGOT, "철 주괴", 24)',
    'new Requirement(Items.GLASS, "유리", 128)': 'new Requirement(Items.GLASS, "유리", 32)',
    'new Requirement(Items.SLIME_BALL, "슬라임볼", 32)': 'new Requirement(Items.SLIME_BALL, "슬라임볼", 8)',
    'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 1024)': 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 192)',
    'new Requirement(Items.COPPER_INGOT, "구리 주괴", 256)': 'new Requirement(Items.COPPER_INGOT, "구리 주괴", 48)',
    'new Requirement(Items.OBSIDIAN, "흑요석", 64)': 'new Requirement(Items.OBSIDIAN, "흑요석", 12)',
    'new Requirement(Items.IRON_INGOT, "철 주괴", 512)': 'new Requirement(Items.IRON_INGOT, "철 주괴", 96)',
    'new Requirement(Items.GOLD_INGOT, "금 주괴", 256)': 'new Requirement(Items.GOLD_INGOT, "금 주괴", 48)',
    'new Requirement(Items.EMERALD, "에메랄드", 128)': 'new Requirement(Items.EMERALD, "에메랄드", 16)',
    'new Requirement(Items.ECHO_SHARD, "메아리 조각", 32)': 'new Requirement(Items.ECHO_SHARD, "메아리 조각", 4)',
    'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)': 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 384)',
    'new Requirement(Items.COBBLESTONE, "조약돌", 1536)': 'new Requirement(Items.COBBLESTONE, "조약돌", 256)',
    'new Requirement(Items.GRAVEL, "자갈", 1536)': 'new Requirement(Items.GRAVEL, "자갈", 256)',
    'new Requirement(Items.IRON_INGOT, "철 주괴", 512)': 'new Requirement(Items.IRON_INGOT, "철 주괴", 96)',
    'new Requirement(Items.REDSTONE, "레드스톤", 256)': 'new Requirement(Items.REDSTONE, "레드스톤", 48)',
    'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 128)': 'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 24)',
    'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 256)': 'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 48)',
    'new Requirement(Items.NETHER_STAR, "네더의 별", 4)': 'new Requirement(Items.NETHER_STAR, "네더의 별", 1)',
    'new Requirement(Items.DRAGON_BREATH, "드래곤의 숨결", 64)': 'new Requirement(Items.DRAGON_BREATH, "드래곤의 숨결", 8)',
    'new Requirement(Items.OBSIDIAN, "흑요석", 512)': 'new Requirement(Items.OBSIDIAN, "흑요석", 64)',
    'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 512)': 'new Requirement(Items.AMETHYST_SHARD, "자수정 조각", 64)',
    'new Requirement(Items.ECHO_SHARD, "메아리 조각", 64)': 'new Requirement(Items.ECHO_SHARD, "메아리 조각", 8)',
}
text = read(p)
for old, new in replacements.items():
    if old not in text:
        # Some repeated material lines were already replaced by an earlier same-value key.
        continue
    text = text.replace(old, new)
text = text.replace(
    '"combat_academy", "전투 훈련장", "전투 Lv.90 질주 충격파 · Lv.100 반경 6.5블록 최대 16체", 0,',
    '"combat_academy", "전투 훈련장", "전투 Lv.90 질주 전방 균열선 6.5블록/10체 · Lv.100 8블록/14체 · 현장 숙련 10블록/18체", 0,'
)
write(p, text)

# Ensure project-specific duplicate material lines landed on the intended solo values.
text = read(p)
text = text.replace('new Requirement(Items.IRON_INGOT, "철 주괴", 256)', 'new Requirement(Items.IRON_INGOT, "철 주괴", 48)')
text = text.replace('new Requirement(Items.COPPER_INGOT, "구리 주괴", 256)', 'new Requirement(Items.COPPER_INGOT, "구리 주괴", 48)')
text = text.replace('new Requirement(Items.IRON_INGOT, "철 주괴", 512)', 'new Requirement(Items.IRON_INGOT, "철 주괴", 96)')
text = text.replace('new Requirement(Items.GOLD_INGOT, "금 주괴", 256)', 'new Requirement(Items.GOLD_INGOT, "금 주괴", 48)')
text = text.replace('new Requirement(Items.ECHO_SHARD, "메아리 조각", 32)', 'new Requirement(Items.ECHO_SHARD, "메아리 조각", 4)')
write(p, text)

# Physical commissioning sites remain real, but stop demanding another quarry worth of blocks.
p = "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java"
for old, new in [
    ('new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 48)', 'new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 16)'),
    ('new SiteRequirement(Blocks.SCAFFOLDING, "비계", 16)', 'new SiteRequirement(Blocks.SCAFFOLDING, "비계", 8)'),
    ('new SiteRequirement(Blocks.IRON_BLOCK, "철 블록", 4)', 'new SiteRequirement(Blocks.IRON_BLOCK, "철 블록", 2)'),
    ('new SiteRequirement(Blocks.STONECUTTER, "석재 절단기", 2)', 'new SiteRequirement(Blocks.STONECUTTER, "석재 절단기", 1)'),
    ('new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 32)', 'new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 12)'),
    ('new SiteRequirement(Blocks.GOLD_BLOCK, "금 블록", 4)', 'new SiteRequirement(Blocks.GOLD_BLOCK, "금 블록", 2)'),
    ('new SiteRequirement(Blocks.TARGET, "과녁", 4)', 'new SiteRequirement(Blocks.TARGET, "과녁", 2)'),
    ('new SiteRequirement(Blocks.OBSIDIAN, "흑요석", 32)', 'new SiteRequirement(Blocks.OBSIDIAN, "흑요석", 12)'),
    ('new SiteRequirement(Blocks.CRYING_OBSIDIAN, "우는 흑요석", 8)', 'new SiteRequirement(Blocks.CRYING_OBSIDIAN, "우는 흑요석", 4)'),
]:
    text = read(p)
    if old in text:
        write(p, text.replace(old, new))
# second industrial iron-block occurrence may remain
text = read(p).replace('new SiteRequirement(Blocks.IRON_BLOCK, "철 블록", 4)', 'new SiteRequirement(Blocks.IRON_BLOCK, "철 블록", 2)')
write(p, text)

# ---------------------------------------------------------------------------
# 2) Repeated industrial batches: common materials, roughly one-sixth old scale.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java"
for old, new in [
    ('Input.item(Items.RAW_IRON, "철 원석", 96)', 'Input.item(Items.RAW_IRON, "철 원석", 16)'),
    ('Input.item(Items.RAW_COPPER, "구리 원석", 96)', 'Input.item(Items.RAW_COPPER, "구리 원석", 16)'),
    ('Input.item(Items.COAL, "석탄", 64)', 'Input.item(Items.COAL, "석탄", 12)'),
    ('Input.tag(ItemTags.LOGS, "통나무", 192)', 'Input.tag(ItemTags.LOGS, "통나무", 32)'),
    ('Input.item(Items.COBBLESTONE, "조약돌", 384)', 'Input.item(Items.COBBLESTONE, "조약돌", 64)'),
    ('Input.item(Items.IRON_INGOT, "철 주괴", 32)', 'Input.item(Items.IRON_INGOT, "철 주괴", 6)'),
    ('Input.item(Items.WHEAT, "밀", 128)', 'Input.item(Items.WHEAT, "밀", 24)'),
    ('Input.item(Items.CARROT, "당근", 64)', 'Input.item(Items.CARROT, "당근", 12)'),
    ('Input.item(Items.POTATO, "감자", 64)', 'Input.item(Items.POTATO, "감자", 12)'),
    ('Input.item(Items.BEETROOT, "비트", 32)', 'Input.item(Items.BEETROOT, "비트", 6)'),
    ('Input.item(Items.REDSTONE, "레드스톤", 128)', 'Input.item(Items.REDSTONE, "레드스톤", 24)'),
    ('Input.item(Items.AMETHYST_SHARD, "자수정 조각", 64)', 'Input.item(Items.AMETHYST_SHARD, "자수정 조각", 12)'),
    ('Input.item(Items.GOLD_INGOT, "금 주괴", 32)', 'Input.item(Items.GOLD_INGOT, "금 주괴", 6)'),
    ('Input.item(Items.QUARTZ, "네더 석영", 64)', 'Input.item(Items.QUARTZ, "네더 석영", 12)'),
]:
    replace(p, old, new)

# Local front-line consumables are repeatable too, so reduce them without removing physical logistics.
p = "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java"
for old, new in [
    ('new LocalRequirement("식량", 32, ProductionService::isFieldFood)', 'new LocalRequirement("식량", 12, ProductionService::isFieldFood)'),
    ('new LocalRequirement("철 주괴", 8, stack -> stack.is(Items.IRON_INGOT))', 'new LocalRequirement("철 주괴", 3, stack -> stack.is(Items.IRON_INGOT))'),
    ('new LocalRequirement("연료", 8, ProductionService::isFieldFuel)', 'new LocalRequirement("연료", 3, ProductionService::isFieldFuel)'),
    ('new LocalRequirement("식량", 48, ProductionService::isFieldFood)', 'new LocalRequirement("식량", 16, ProductionService::isFieldFood)'),
    ('new LocalRequirement("철 주괴", 16, stack -> stack.is(Items.IRON_INGOT))', 'new LocalRequirement("철 주괴", 5, stack -> stack.is(Items.IRON_INGOT))'),
    ('new LocalRequirement("통나무", 32, stack -> stack.is(ItemTags.LOGS))', 'new LocalRequirement("통나무", 12, stack -> stack.is(ItemTags.LOGS))'),
    ('new LocalRequirement("식량", 96, ProductionService::isFieldFood)', 'new LocalRequirement("식량", 32, ProductionService::isFieldFood)'),
    ('new LocalRequirement("철 주괴", 32, stack -> stack.is(Items.IRON_INGOT))', 'new LocalRequirement("철 주괴", 8, stack -> stack.is(Items.IRON_INGOT))'),
    ('new LocalRequirement("석재 벽돌", 128, stack -> stack.is(Items.STONE_BRICKS))', 'new LocalRequirement("석재 벽돌", 32, stack -> stack.is(Items.STONE_BRICKS))'),
]:
    replace(p, old, new)
text = read(p)
text = text.replace('원정=식량32+철8+연료8 / 방어=식량48+철16+통나무32 / 요새=식량96+철32+석재벽돌128.',
                    '원정=식량12+철3+연료3 / 방어=식량16+철5+통나무12 / 요새=식량32+철8+석재벽돌32.')
text = text.replace('가까운 사용 가능 거점/창고 통 우선 + 부족분 인벤토리.',
                    '같은 차원에서 현재 로딩된 등록 거점/창고 통 전체 + 부족분 인벤토리.')
text = text.replace('등록 앵커 하나당 반경6 실제 통 최대8개를 별도 보급권 없이 연결해 같은32/64 물류권에서 사용합니다.',
                    '등록 앵커 하나당 반경6 실제 통 최대8개를 연결하며, 같은 차원에서 로딩된 등록 창고는 플레이어와 거리 제한 없이 공용 재고로 사용합니다.')
write(p, text)

# ---------------------------------------------------------------------------
# 3) Logistics QOL: remove player-distance gate, never force-load chunks.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java"
text = read(p)
text = text.replace('            int radius = OutpostService.isActiveForLogistics(player, depot) ? OutpostService.EXTENDED_SUPPLY_RADIUS : SUPPLY_RADIUS;\n            BlockPos anchor = depot.pos();\n            if (anchor.distSqr(player.blockPosition()) > radius * radius) continue;\n            if (!level.hasChunkAt(anchor)) continue;',
                    '            BlockPos anchor = depot.pos();\n            // Registered logistics is a same-dimension network. Distance is not a gameplay tax;\n            // unloaded chunks are still skipped so this never force-loads the world.\n            if (!level.hasChunkAt(anchor)) continue;')
text = text.replace('        int radius = OutpostService.isActiveForLogistics(player, depot) ? OutpostService.EXTENDED_SUPPLY_RADIUS : SUPPLY_RADIUS;\n        BlockPos pos = depot.pos();\n        if (pos.distSqr(player.blockPosition()) > radius * radius) return false;\n        if (!level.hasChunkAt(pos)) return false;',
                    '        BlockPos pos = depot.pos();\n        // Same-dimension loaded depots are usable at any distance; no chunk tickets are created.\n        if (!level.hasChunkAt(pos)) return false;')
text = text.replace(' · 같은 차원 반경 " + SUPPLY_RADIUS + "블록에서 사용 · 주변 창고 통 최대 ',
                    ' · 같은 차원에서 로딩 중이면 거리 제한 없이 사용 · 주변 창고 통 최대 ')
text = text.replace(' §7· 일반 반경 " + SUPPLY_RADIUS + " / 전초 " + OutpostService.EXTENDED_SUPPLY_RADIUS',
                    ' §7· 같은 차원 로딩 창고는 거리 제한 없음')
text = text.replace('  §7- 재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터, 부족분만 플레이어 인벤토리에서 사용',
                    '  §7- 재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고, 부족분만 플레이어 인벤토리에서 사용')
write(p, text)

# ---------------------------------------------------------------------------
# 4) Equipment costs: cheap rerolls, rare ingredients reserved for the real final awakening.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java"
text = read(p)
# Imprint
for old, new in [
    ('new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각")', 'new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각")'),
    ('new MaterialCost(Items.IRON_INGOT, 12, "철 주괴")', 'new MaterialCost(Items.IRON_INGOT, 4, "철 주괴")'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 48, "자수정 조각")', 'new MaterialCost(Items.AMETHYST_SHARD, 12, "자수정 조각")'),
    ('new MaterialCost(Items.DIAMOND, 4, "다이아몬드")', 'new MaterialCost(Items.DIAMOND, 1, "다이아몬드")'),
    ('new MaterialCost(Items.GOLD_INGOT, 16, "금 주괴")', 'new MaterialCost(Items.GOLD_INGOT, 4, "금 주괴")'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 96, "자수정 조각")', 'new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각")'),
    ('new MaterialCost(Items.DIAMOND, 8, "다이아몬드")', 'new MaterialCost(Items.DIAMOND, 2, "다이아몬드")'),
    ('new MaterialCost(Items.NETHERITE_SCRAP, 2, "네더라이트 파편"),\n                    new MaterialCost(Items.ECHO_SHARD, 8, "메아리 조각")', 'new MaterialCost(Items.ECHO_SHARD, 2, "메아리 조각")'),
]:
    if old not in text: raise SystemExit(f"equipment imprint token missing: {old}")
    text = text.replace(old, new, 1)
# Rerolls and awakening are replaced as complete method bodies for clarity.
text = re.sub(r'    private static MaterialCost\[\] reforgeCosts\(int rarity, boolean awakened\) \{.*?\n    \}\n\n    private static MaterialCost\[\] awakeningCosts\(\) \{.*?\n    \}', '''    private static MaterialCost[] reforgeCosts(int rarity, boolean awakened) {
        if (rarity == 3 && awakened) {
            return new MaterialCost[] {
                    new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각"),
                    new MaterialCost(Items.DIAMOND, 3, "다이아몬드"),
                    new MaterialCost(Items.ECHO_SHARD, 2, "메아리 조각")
            };
        }
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 4, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 2, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 16, "자수정 조각"), new MaterialCost(Items.DIAMOND, 2, "다이아몬드") };
            default -> new MaterialCost[0];
        };
    }

    private static MaterialCost[] awakeningCosts() {
        return new MaterialCost[] {
                new MaterialCost(Items.AMETHYST_SHARD, 32, "자수정 조각"),
                new MaterialCost(Items.DIAMOND, 4, "다이아몬드"),
                new MaterialCost(Items.NETHERITE_SCRAP, 1, "네더라이트 파편"),
                new MaterialCost(Items.ECHO_SHARD, 8, "메아리 조각"),
                new MaterialCost(Items.DRAGON_BREATH, 4, "드래곤의 숨결")
        };
    }''', text, count=1, flags=re.S)
# Lower salvage to avoid a cheaper-reroll material loop.
text = re.sub(r'    private static MaterialCost\[\] salvageRewards\(int rarity\) \{.*?\n    \}', '''    private static MaterialCost[] salvageRewards(int rarity) {
        return switch (rarity) {
            case 1 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 2, "자수정 조각"), new MaterialCost(Items.IRON_INGOT, 1, "철 주괴") };
            case 2 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 4, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            case 3 -> new MaterialCost[] { new MaterialCost(Items.AMETHYST_SHARD, 8, "자수정 조각"), new MaterialCost(Items.DIAMOND, 1, "다이아몬드") };
            default -> new MaterialCost[0];
        };
    }''', text, count=1, flags=re.S)
text = text.replace('아직 affix가 없는', '아직 승천 옵션이 없는')
text = text.replace('affix 데이터가 정상적인 3-affix 신화 장비', '승천 옵션 3개가 정상적으로 붙은 신화 장비')
text = text.replace('4번째 affix가 개방되었습니다.', '4번째 승천 옵션이 개방되었습니다.')
# Explain real power immediately after each successful operation.
text = text.replace(' + " 장비가 §e" + AscensionAffixes.rarityName(held) + "§f 등급으로 편입되었습니다. §7" + AscensionAffixes.affixSummary(held)));',
                    ' + " 장비가 §e" + AscensionAffixes.rarityName(held) + "§f 등급으로 편입되었습니다. §7" + AscensionAffixes.affixSummary(held)));\n        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));')
text = text.replace('player.sendSystemMessage(Component.literal("§d[재련 완료] §f" + AscensionAffixes.rarityName(held) + " · §e" + AscensionAffixes.affixSummary(held)));',
                    'player.sendSystemMessage(Component.literal("§d[재련 완료] §f" + AscensionAffixes.rarityName(held) + " · §e" + AscensionAffixes.affixSummary(held)));\n        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));')
text = text.replace('player.sendSystemMessage(Component.literal("§5[신화 각성 완료] §f4번째 승천 옵션이 개방되었습니다. §e" + AscensionAffixes.affixSummary(held)));',
                    'player.sendSystemMessage(Component.literal("§5[신화 각성 완료] §f4번째 승천 옵션이 개방되었습니다. §e" + AscensionAffixes.affixSummary(held)));\n        player.sendSystemMessage(Component.literal("§7실제 효과: §f" + AscensionAffixes.effectSummary(held)));')
write(p, text)

# ---------------------------------------------------------------------------
# 5) Affixes: large, visible power increase. Buff bonus magnitude ~4-6x and lift caps too.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java"
text = read(p)
for old, new in [
    ('case 1 -> 1.12D; case 2 -> 1.25D; default -> 1.40D;', 'case 1 -> 1.60D; case 2 -> 2.00D; default -> 2.60D;'),
    ('case 1 -> 1.06D; case 2 -> 1.12D; default -> 1.20D;', 'case 1 -> 1.25D; case 2 -> 1.45D; default -> 1.75D;'),
    ('case 1 -> 1.08D; case 2 -> 1.15D; default -> 1.25D;', 'case 1 -> 1.40D; case 2 -> 1.75D; default -> 2.20D;'),
    ('case 1 -> 1.06D; case 2 -> 1.12D; default -> 1.20D;', 'case 1 -> 1.35D; case 2 -> 1.65D; default -> 2.00D;'),
    ('case 1 -> 1.10D; case 2 -> 1.25D; default -> 1.50D;', 'case 1 -> 1.50D; case 2 -> 2.00D; default -> 3.00D;'),
    ('case 1 -> 1080; case 2 -> 1150; default -> 1250;', 'case 1 -> 1400; case 2 -> 1800; default -> 2400;'),
    ('case 1 -> 1100; case 2 -> 1250; default -> 1500;', 'case 1 -> 1500; case 2 -> 2000; default -> 3000;'),
    ('case 1 -> 5; case 2 -> 10; default -> 15;', 'case 1 -> 15; case 2 -> 30; default -> 50;'),
    ('case 1 -> 1; case 2 -> 2; default -> 4;', 'case 1 -> 2; case 2 -> 5; default -> 10;'),
    ('case 1 -> 50; case 2 -> 100; default -> 150;', 'case 1 -> 150; case 2 -> 300; default -> 500;'),
    ('Math.min(1.25D, Math.max(1.0D,', 'Math.min(2.40D, Math.max(1.0D,'),
    ('Math.min(1.50D, Math.max(1.0D,', 'Math.min(3.00D, Math.max(1.0D,'),
    ('Math.min(1.5D, Math.max(0,', 'Math.min(5.0D, Math.max(0,'),
    ('Math.min(4, Math.max(0,', 'Math.min(10, Math.max(0,'),
    ('Math.min(0.15D, Math.max(0,', 'Math.min(0.50D, Math.max(0,'),
    ('case 1 -> 0.02D; case 2 -> 0.03D; default -> 0.04D;', 'case 1 -> 0.05D; case 2 -> 0.07D; default -> 0.10D;'),
    ('case 1 -> 0.01D; case 2 -> 0.02D; default -> 0.03D;', 'case 1 -> 0.04D; case 2 -> 0.06D; default -> 0.08D;'),
    ('case 1 -> 0.015D; case 2 -> 0.025D; default -> 0.035D;', 'case 1 -> 0.04D; case 2 -> 0.06D; default -> 0.09D;'),
    ('Math.min(0.35D, reduction)', 'Math.min(0.70D, reduction)'),
    ('case 1 -> 0.03D; case 2 -> 0.05D; case 3 -> 0.08D;', 'case 1 -> 0.08D; case 2 -> 0.12D; case 3 -> 0.20D;'),
    ('Math.min(1.32D, 1.0D + bonus)', 'Math.min(2.00D, 1.0D + bonus)'),
    ('case 1 -> 12; case 2 -> 32; case 3 -> 64;', 'case 1 -> 48; case 2 -> 128; case 3 -> 256;'),
    ('case 1 -> 16; case 2 -> 48; case 3 -> 96;', 'case 1 -> 64; case 2 -> 192; case 3 -> 384;'),
    ('case 1 -> 8; case 2 -> 24; case 3 -> 64;', 'case 1 -> 32; case 2 -> 96; case 3 -> 256;'),
    ('case 1 -> 1; case 2 -> 2; case 3 -> 4;', 'case 1 -> 4; case 2 -> 8; case 3 -> 14;'),
    ('case 1 -> 0.05D; case 2 -> 0.10D; case 3 -> 0.15D;', 'case 1 -> 0.15D; case 2 -> 0.30D; case 3 -> 0.50D;'),
    ('Math.min(0.85D, base +', 'Math.min(1.25D, base +'),
    ('case 1 -> 0.5D; case 2 -> 1.0D; case 3 -> 1.5D;', 'case 1 -> 2.0D; case 2 -> 4.0D; case 3 -> 6.0D;'),
    ('case 1 -> 1; case 2 -> 2; case 3 -> 3;', 'case 1 -> 4; case 2 -> 8; case 3 -> 12;'),
    ('case 1 -> 0.08D; case 2 -> 0.16D; case 3 -> 0.24D;', 'case 1 -> 0.20D; case 2 -> 0.40D; case 3 -> 0.60D;'),
    ('case 1 -> 2; case 2 -> 4; case 3 -> 6;', 'case 1 -> 8; case 2 -> 16; case 3 -> 24;'),
    ('case 1 -> 0.10D; case 2 -> 0.20D; case 3 -> 0.30D;', 'case 1 -> 0.25D; case 2 -> 0.50D; case 3 -> 0.75D;'),
    ('case 1 -> 0.04D; case 2 -> 0.08D; case 3 -> 0.12D;', 'case 1 -> 0.10D; case 2 -> 0.20D; case 3 -> 0.30D;'),
    ('case 1 -> 2; case 2 -> 3; case 3 -> 4;', 'case 1 -> 4; case 2 -> 8; case 3 -> 12;'),
    ('case 1 -> 1.10D; case 2 -> 1.22D; default -> 1.40D;', 'case 1 -> 1.40D; case 2 -> 1.80D; default -> 2.50D;'),
    ('return switch (rarity) { case 1, 2 -> 2; case 3 -> 4; default -> 0; };', 'return switch (rarity) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };'),
]:
    if old in text:
        text = text.replace(old, new)
# Context-specific area secondaries after broad substitutions.
text = text.replace('if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1, 2 -> 2; case 3 -> 4; default -> 0; };\n        return Math.min(13, base + bonus);',
                    'if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };\n        return Math.min(21, base + bonus);')
text = text.replace('if (has(stack, SECONDARY)) bonus += rarity(stack) >= 2 ? 2 : 0;',
                    'if (has(stack, SECONDARY)) bonus += switch (rarity(stack)) { case 1 -> 4; case 2 -> 6; case 3 -> 8; default -> 0; };')
# Ranged/armor utility values that shared old literals with unrelated features.
text = text.replace('if (environmental && has(armor, UTILITY)) reduction += switch (rarity) { case 1 -> 0.05D; case 2 -> 0.07D; default -> 0.10D; };',
                    'if (environmental && has(armor, UTILITY)) reduction += switch (rarity) { case 1 -> 0.06D; case 2 -> 0.09D; default -> 0.12D; };')
# Public human-readable effect explanation used by the equipment menu and completion messages.
marker = '    private static String baseName(ItemStack stack) {'
if marker not in text: raise SystemExit('AscensionAffixes baseName marker missing')
explain = '''    public static String effectSummary(ItemStack stack) {
        int rarity = rarity(stack);
        Category category = category(stack);
        if (rarity <= 0 || category == Category.NONE) return "승천 옵션 없음";
        List<String> out = new ArrayList<>();
        for (String key : currentAffixes(stack)) out.add(affixName(category, key) + " " + effectText(category, key, rarity));
        return String.join(" · ", out);
    }

    private static String tier(int rarity, String one, String two, String three) {
        return rarity <= 1 ? one : rarity == 2 ? two : three;
    }

    private static String effectText(Category category, String key, int rarity) {
        if (MASTERY.equals(key)) {
            if (category == Category.SHIELD) return "파동 쿨 -" + tier(rarity, "4", "8", "12") + "틱";
            if (category == Category.ARMOR) return "전투 XP +" + tier(rarity, "8", "12", "20") + "%";
            return "숙련 XP ×" + tier(rarity, "1.5", "2.0", "3.0");
        }
        if (PRIMARY.equals(key)) return switch (category) {
            case WEAPON -> "직접 피해 ×" + tier(rarity, "1.40", "1.75", "2.20");
            case SPEAR -> "직접 피해 ×" + tier(rarity, "1.35", "1.65", "2.00");
            case RANGED -> "직접 피해 ×" + tier(rarity, "1.40", "1.80", "2.40");
            case MACE, SHIELD -> "밀치기 +" + tier(rarity, "0.25", "0.50", "0.75");
            case ARMOR -> "상시 피해 -" + tier(rarity, "5", "7", "10") + "%";
            case PICKAXE, AXE, SHOVEL, HOE -> "작업 속도 ×" + tier(rarity, "1.60", "2.00", "2.60");
            default -> "강화";
        };
        if (SCALE.equals(key)) return switch (category) {
            case WEAPON -> "광역 대상 +" + tier(rarity, "4", "8", "14");
            case SPEAR -> "돌파 거리 +" + tier(rarity, "2", "4", "6") + "블록";
            case RANGED -> "파급 반경 +" + tier(rarity, "1.5", "3", "5") + "블록";
            case PICKAXE, SHOVEL, HOE -> "작업 폭 +" + tier(rarity, "4", "6", "8");
            case AXE -> "연쇄 벌목 +" + tier(rarity, "64", "192", "384");
            case MACE, SHIELD -> "영향 반경 +" + tier(rarity, "2", "4", "6") + "블록";
            case ARMOR -> "체력 절반 이하 추가 -" + tier(rarity, "4", "6", "8") + "%";
            default -> "범위 강화";
        };
        if (SECONDARY.equals(key)) return switch (category) {
            case WEAPON -> "정예 피해 ×" + tier(rarity, "1.40", "1.80", "2.50");
            case SPEAR -> "돌파 대상 +" + tier(rarity, "4", "8", "12");
            case RANGED -> "파급 대상 +" + tier(rarity, "2", "5", "10");
            case PICKAXE -> "광맥 한도 +" + tier(rarity, "48", "128", "256");
            case AXE -> "추가 벌채 +" + tier(rarity, "32", "96", "256");
            case SHOVEL, HOE -> "작업 폭 추가 +" + tier(rarity, "4", "6", "8");
            case MACE -> "충격 대상 +" + tier(rarity, "8", "16", "24");
            case SHIELD -> "파동 대상 +" + tier(rarity, "4", "8", "14");
            case ARMOR -> "큰 피해 추가 -" + tier(rarity, "4", "6", "9") + "%";
            default -> "특화 강화";
        };
        return switch (category) {
            case WEAPON, RANGED -> "광역 피해 +" + tier(rarity, "15", "30", "50") + "%p";
            case SPEAR -> "밀치기 +" + tier(rarity, "0.20", "0.40", "0.60");
            case PICKAXE, AXE, SHOVEL, HOE -> "추가 작업 속도 ×" + tier(rarity, "1.25", "1.45", "1.75");
            case MACE, SHIELD -> "띄우기 +" + tier(rarity, "0.10", "0.20", "0.30");
            case ARMOR -> "환경 피해 추가 -" + tier(rarity, "6", "9", "12") + "%";
            default -> "보조 강화";
        };
    }

'''
text = text.replace(marker, explain + marker, 1)
write(p, text)

# Combat runtime caps must allow the stronger affixes to matter.
p = "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java"
text = read(p)
for old, new in [
    ('Math.min(8.0D, radius + AscensionAffixes.shieldWaveRadiusBonus(shield))', 'Math.min(14.0D, radius + AscensionAffixes.shieldWaveRadiusBonus(shield))'),
    ('Math.min(14, targetLimit + AscensionAffixes.shieldWaveTargetBonus(shield))', 'Math.min(28, targetLimit + AscensionAffixes.shieldWaveTargetBonus(shield))'),
    ('Math.min(1.30D, knockback + AscensionAffixes.shieldWaveKnockbackBonus(shield))', 'Math.min(2.00D, knockback + AscensionAffixes.shieldWaveKnockbackBonus(shield))'),
    ('Math.min(0.28D, lift + AscensionAffixes.shieldWaveLiftBonus(shield))', 'Math.min(0.60D, lift + AscensionAffixes.shieldWaveLiftBonus(shield))'),
    ('Math.min(9.0D, reach + AscensionAffixes.spearLineReachBonus(spear))', 'Math.min(15.0D, reach + AscensionAffixes.spearLineReachBonus(spear))'),
    ('Math.min(8, targetLimit + AscensionAffixes.spearLineTargetBonus(spear))', 'Math.min(20, targetLimit + AscensionAffixes.spearLineTargetBonus(spear))'),
    ('Math.min(1.10D, basePush + Math.min(0.35D, forwardSpeed * 0.85D)', 'Math.min(1.75D, basePush + Math.min(0.35D, forwardSpeed * 0.85D)'),
    ('Math.min(10.5D, radius + AscensionAffixes.maceImpactRadiusBonus(mace))', 'Math.min(16.5D, radius + AscensionAffixes.maceImpactRadiusBonus(mace))'),
    ('Math.min(26, targetLimit + AscensionAffixes.maceImpactTargetBonus(mace))', 'Math.min(50, targetLimit + AscensionAffixes.maceImpactTargetBonus(mace))'),
    ('Math.min(1.30D, knockback + AscensionAffixes.maceImpactKnockbackBonus(mace))', 'Math.min(2.00D, knockback + AscensionAffixes.maceImpactKnockbackBonus(mace))'),
    ('Math.min(0.28D, lift + AscensionAffixes.maceImpactLiftBonus(mace))', 'Math.min(0.60D, lift + AscensionAffixes.maceImpactLiftBonus(mace))'),
    ('fraction = Math.min(0.65D, fraction + AscensionAffixes.projectileBurstFractionBonus(direct));', 'fraction = Math.min(1.00D, fraction + AscensionAffixes.projectileBurstFractionBonus(direct));'),
]:
    if old not in text: raise SystemExit(f"combat cap token missing: {old}")
    text = text.replace(old, new)
write(p, text)

# ---------------------------------------------------------------------------
# 6) Player-facing UI: show benefits first, costs second, and exact equipment effects.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java"
text = read(p)
entries = {
'new Entry("채석장 네트워크", "조약돌1024 · 철256 · 레드스톤128 · 다이아32"': 'new Entry("채석장 네트워크", "터널 5×5×8→7×7×10 · 조약돌192/철48/레드24/다이아6"',
'new Entry("관개 시설", "구리512 · 철128 · 레드스톤128 · 유리128 · 슬라임32"': 'new Entry("관개 시설", "Lv30 자동 재파종 · 구리96/철24/레드24/유리32/슬라임8"',
'new Entry("건축 공방", "석재벽돌1024 · 철256 · 구리256 · 레드스톤128 · 흑요석64"': 'new Entry("건축 공방", "입체 5³→7³ · 석재192/철48/구리48/레드24/흑요석12"',
'new Entry("전투 훈련장", "철512 · 금256 · 에메랄드128 · 레드스톤128 · 메아리32"': 'new Entry("전투 훈련장", "질주 전방 균열선 · 철96/금48/에메랄드16/레드24/메아리4"',
'new Entry("토목 공사소", "전설 · 석재벽돌2048 + 조약돌1536 + 자갈1536 + 금속"': 'new Entry("토목 공사소", "3폭 도로/교량 · 석재384/조약돌256/자갈256/철48/구리48"',
'new Entry("산업 가공소", "전설 · 대량재료 + 마지막 실제 배럴 준공 현장"': 'new Entry("산업 가공소", "생산·창고·전초·화물 개방 · 석재192/철96/구리96/레드48/자수정24"',
'new Entry("정점 추적소", "전설 · 대량재료 + 등록 배럴 기반 추적소 준공 현장"': 'new Entry("정점 추적소", "원정권 정점 사냥 · 철96/금48/자수정48/메아리4/별1"',
'new Entry("승천 중추", "종말 · 대량재료 + 등록 배럴 기반 중추 준공 현장"': 'new Entry("승천 중추", "공중돌진·승천시련 · 별1/숨결8/흑요석64/자수정64/메아리8"',
}
for old, new in entries.items():
    if old not in text: raise SystemExit(f"infrastructure UI token missing: {old}")
    text = text.replace(old, new)
text = text.replace('String caption="대량 자원 → 실제 준공 현장 → 월드에 남는 작업 체급";',
                    'String caption="기능을 먼저 보고 선택하세요 · 비용은 싱글플레이 체급으로 조정됨";')
write(p, text)

p = "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java"
text = read(p)
for old, new in [
    ('"철원석96 · 구리원석96 · 석탄64"', '"반복 생산 · 철원석16 · 구리원석16 · 석탄12"'),
    ('"통나무192 · 조약돌384 · 철32"', '"반복 생산 · 통나무32 · 조약돌64 · 철6"'),
    ('"밀128 · 당근64 · 감자64 · 비트32"', '"반복 생산 · 밀24 · 당근12 · 감자12 · 비트6"'),
    ('"레드128 · 자수정64 · 금32 · 석영64"', '"반복 생산 · 레드24 · 자수정12 · 금6 · 석영12"'),
    ('"주 인벤토리 대량자원 → 가까운 사용 가능 실제 통 · 핫바/장비 유지"', '"주 인벤토리 대량자원 → 등록 창고 · 핫바/장비 유지"'),
    ('"보급권1 + 전초재고(식량48/철16/통나무32) · 3공세"', '"보급권1 + 전초재고(식량16/철5/통나무12) · 3공세"'),
    ('"보급권2 + 전초재고(식량96/철32/석재벽돌128) · 벽+4공세"', '"보급권2 + 전초재고(식량32/철8/석재벽돌32) · 벽+4공세"'),
    ('"보급권1 + 전초재고(식량32/철8/연료8) · 전진→작업→귀환"', '"보급권1 + 전초재고(식량12/철3/연료3) · 전진→작업→귀환"'),
]:
    if old not in text: raise SystemExit(f"production UI token missing: {old}")
    text = text.replace(old, new)
text = text.replace('String caption="채집 → 통/창고 → 도로·레일 → 일반/전선 화물 → 전초 현지재고 → 방어/원정";',
                    'String caption="반복 배치는 흔한 재료 위주 · 등록 창고는 같은 차원 로딩 중이면 거리 제한 없이 사용";')
write(p, text)

p = "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java"
text = read(p)
text = text.replace('String top="자수정256 · 다이아24 · 파편8",bottom="메아리64 · 드래곤숨결16";',
                    'String top="자수정32 · 다이아4 · 파편1",bottom="메아리8 · 드래곤숨결4";')
text = text.replace('case INFO->AscensionAffixes.affixSummary(held);', 'case INFO->AscensionAffixes.effectSummary(held);')
write(p, text)

# Server status supplies rare-material sourcing hints instead of dumping numbers without context.
p = "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java"
text = read(p)
needle = '        InfrastructureSiteService.sendStatus(player, project);\n'
if needle not in text: raise SystemExit('InfrastructureService status marker missing')
hints = '''        if (project == InfrastructureProject.COMBAT_ACADEMY) {
            player.sendSystemMessage(Component.literal("  §7수급 팁: 에메랄드는 주민 거래가 주 수급처 · 메아리 조각은 고대 도시 또는 후반 산업 출고로 보조"));
        } else if (project == InfrastructureProject.ASCENSION_NEXUS) {
            player.sendSystemMessage(Component.literal("  §7수급 팁: 드래곤의 숨결은 엔드에서 병으로 채집 · 흑요석은 물+용암/채굴 · 메아리는 고대 도시/산업 보조. 최종 병목 수량은 8/64/8로 축소됨"));
        } else if (project == InfrastructureProject.QUARRY_NETWORK) {
            player.sendSystemMessage(Component.literal("  §7채석장은 광물을 생성하지 않고 실제 월드를 대형 터널로 굴착합니다. 광석 수급 속도를 올리는 작업 체급 해금입니다."));
        }
'''
text = text.replace(needle, hints + needle, 1)
write(p, text)

# Guide: update the two most misleading explanations (armor power and logistics reach).
p = "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
text = read(p)
text = re.sub(r'h\("방어구 승천 옵션"\), p\(".*?"\),',
'''h("방어구 승천 옵션"), p("각인 방어구는 착용 중에만 작동합니다. 신화 한 부위 기준 수호=상시 피해 -10%, 불굴=체력 절반 이하 추가 -8%, 숙련=전투 숙련 XP +20%, 완강=8 이상 큰 피해 추가 -9%, 보호=환경 피해 추가 -12%입니다. 네 부위 합산 승천 옵션 피해 감소 상한은 70%, 숙련 XP는 최대 2배까지 적용됩니다."),''', text, count=1, flags=re.S)
text = re.sub(r'h\("물류 통과 창고군"\), p\(".*?"\),',
'''h("물류 통과 창고군"), p("산업 가공소 완공 후 기본 통을 물류 거점으로 등록합니다. 거점 주변 실제 통을 창고군으로 연결하며, 같은 차원에서 현재 로딩된 등록 창고는 플레이어와 거리가 멀어도 제작·건축·인프라·재련의 공용 재고로 사용됩니다. 단, 청크를 강제로 로드하지 않으므로 미로딩 창고는 건드리지 않습니다. 원정/방어전의 전초 현지 보급만은 해당 전초의 실제 재고를 요구합니다."),''', text, count=1, flags=re.S)
write(p, text)

# ---------------------------------------------------------------------------
# 7) Adapt the current 0.61 wrapper to the intentionally stronger modern caps.
# Historical 0.58 source audit remains untouched.
# ---------------------------------------------------------------------------
p = "tools/test_release_source.py"
text = read(p)
anchor = '# 0.61 also replaces the player-facing developer term "affix" with "승천 옵션".\n'
if anchor not in text: raise SystemExit('release audit adaptation anchor missing')
insert = '''# 0.61 solo-balance pass intentionally raises equipment caps while preserving every old mechanic.
for old, new in [
    ('"Math.min(0.35D, reduction)"', '"Math.min(0.70D, reduction)"'),
    ('"Math.min(1.32D, 1.0D + bonus)"', '"Math.min(2.00D, 1.0D + bonus)"'),
    ('"Math.min(1.25D"', '"Math.min(2.40D"'),
    ('"Math.min(1.50D"', '"Math.min(3.00D"'),
    ('"Math.min(1.5D"', '"Math.min(5.0D"'),
    ('"Math.min(4"', '"Math.min(10"'),
    ('"Math.min(0.15D"', '"Math.min(0.50D"'),
    ('"Math.min(8.0D"', '"Math.min(14.0D"'),
    ('"Math.min(14"', '"Math.min(28"'),
    ('"Math.min(1.30D"', '"Math.min(2.00D"'),
    ('"Math.min(0.28D"', '"Math.min(0.60D"'),
    ('"Math.min(9.0D"', '"Math.min(15.0D"'),
    ('"Math.min(8,"', '"Math.min(20,"'),
    ('"Math.min(1.10D"', '"Math.min(1.75D"'),
    ('"Math.min(10.5D"', '"Math.min(16.5D"'),
    ('"Math.min(26"', '"Math.min(50"'),
    ('"Math.min(0.65D"', '"Math.min(1.00D"'),
]:
    legacy = legacy.replace(old, new)

'''
text = text.replace(anchor, insert + anchor, 1)
# Current guide contract now verifies the stronger player-facing cap text.
text = text.replace('legacy = legacy.replace(\'h("방어구 affix")\', \'h("방어구 승천 옵션")\')',
                    'legacy = legacy.replace(\'h("방어구 affix")\', \'h("방어구 승천 옵션")\')')
write(p, text)

print("Survival Ascension solo balance/QOL patch applied")
