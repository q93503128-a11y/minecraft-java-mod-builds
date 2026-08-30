#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(rel, old, new):
    text = read(rel)
    if old not in text:
        raise SystemExit(f"{rel}: token missing: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# 1) Exact player-facing material names.
# ---------------------------------------------------------------------------
p = "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java"
text = read(p)
for old, new in [
    ("터널 5×5×8→7×7×10 · 조약돌192/철48/레드24/다이아6", "터널 5×5×8→7×7×10 · 조약돌 192 · 철 주괴 48 · 레드스톤 24 · 다이아몬드 6"),
    ("Lv30 자동 재파종 · 구리96/철24/레드24/유리32/슬라임8", "Lv.30 자동 재파종 · 구리 주괴 96 · 철 주괴 24 · 레드스톤 24 · 유리 32 · 슬라임볼 8"),
    ("입체 5³→7³ · 석재192/철48/구리48/레드24/흑요석12", "입체 5³→7³ · 석재 벽돌 192 · 철 주괴 48 · 구리 주괴 48 · 레드스톤 24 · 흑요석 12"),
    ("질주 전방 균열선 · 철96/금48/에메랄드16/레드24/메아리4", "질주 전방 균열선 · 철 주괴 96 · 금 주괴 48 · 에메랄드 16 · 레드스톤 24 · 메아리 조각 4"),
    ("3폭 도로/교량 · 석재384/조약돌256/자갈256/철48/구리48", "3폭 도로/교량 · 석재 벽돌 384 · 조약돌 256 · 자갈 256 · 철 주괴 48 · 구리 주괴 48"),
    ("생산·창고·전초·화물 개방 · 석재192/철96/구리96/레드48/자수정24", "생산·창고·전초·화물 · 석재 벽돌 192 · 철 주괴 96 · 구리 주괴 96 · 레드스톤 48 · 자수정 조각 24"),
    ("원정권 정점 사냥 · 철96/금48/자수정48/메아리4/별1", "원정권 정점 사냥 · 철 주괴 96 · 금 주괴 48 · 자수정 조각 48 · 메아리 조각 4 · 네더의 별 1"),
    ("공중돌진·승천시련 · 별1/숨결8/흑요석64/자수정64/메아리8", "공중 돌진·승천 시련 · 네더의 별 1 · 드래곤의 숨결 8 · 흑요석 64 · 자수정 조각 64 · 메아리 조각 8"),
]:
    if old not in text:
        raise SystemExit(f"infrastructure exact-name token missing: {old}")
    text = text.replace(old, new, 1)
write(p, text)

p = "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java"
text = read(p)
for old, new in [
    ('"반복 생산 · 철원석16 · 구리원석16 · 석탄12"', '"반복 생산 · 철 원석 16 · 구리 원석 16 · 석탄 12"'),
    ('"반복 생산 · 통나무32 · 조약돌64 · 철6"', '"반복 생산 · 아무 종류의 통나무 32 · 조약돌 64 · 철 주괴 6"'),
    ('"반복 생산 · 밀24 · 당근12 · 감자12 · 비트6"', '"반복 생산 · 밀 24 · 당근 12 · 감자 12 · 비트 6"'),
    ('"반복 생산 · 레드24 · 자수정12 · 금6 · 석영12"', '"반복 생산 · 레드스톤 24 · 자수정 조각 12 · 금 주괴 6 · 네더 석영 12"'),
    ('"4블록 내 기본 통 앵커 등록/해제 · 보급권1 · 한도3→토목6→중추9"', '"산업 가공소 완공 → 통 4블록 이내 → 선택 · 등록 보급권 1 · 다시 선택하면 해제"'),
    ('"4블록 내 실제 통 ↔ 반경6 자신의 거점 · 거점당 최대8 · 보급권 없음"', '"통 4블록 이내에서 선택 · 자신의 거점 6블록 안에 연결 · 거점당 최대 8 · 추가 보급권 없음"'),
    ('"주 인벤토리 대량자원 → 등록 창고 · 핫바/장비 유지"', '"주 인벤토리 대량 자원 → 같은 차원에서 로딩된 등록 물류 통 · 핫바/장비 유지"'),
    ('"등록 통+침대+모닥불+작업대+화로 · 보급권2/철32/금8/석탄32"', '"등록 통+침대+모닥불+작업대+화로 · 보급권 2 · 철 주괴 32 · 금 주괴 8 · 석탄 32"'),
    ('"보급권1 + 전초재고(식량16/철5/통나무12) · 3공세"', '"보급권 1 + 전초 재고(식량 16 · 철 주괴 5 · 아무 종류의 통나무 12) · 3공세"'),
    ('"보급권2 + 전초재고(식량32/철8/석재벽돌32) · 벽+4공세"', '"보급권 2 + 전초 재고(식량 32 · 철 주괴 8 · 석재 벽돌 32) · 벽+4공세"'),
    ('"보급권1 + 전초재고(식량12/철3/연료3) · 전진→작업→귀환"', '"보급권 1 + 전초 재고(식량 12 · 철 주괴 3 · 연료: 석탄 또는 숯 3) · 전진→작업→귀환"'),
    ('"보급권1 → 금32 · 자수정16 · 메아리2"', '"보급권 1 → 금 주괴 32 · 자수정 조각 16 · 메아리 조각 2"'),
]:
    if old not in text:
        raise SystemExit(f"production exact-name token missing: {old}")
    text = text.replace(old, new, 1)
text = text.replace(
    'String caption="반복 배치는 흔한 재료 위주 · 등록 창고는 같은 차원 로딩 중이면 거리 제한 없이 사용";',
    'String caption="등록 물류 통은 같은 차원 로딩 중이면 원격 사용 · 등록 통 파괴 시 내용물째 포장 이동";'
)
write(p, text)

p = "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java"
text = read(p)
for old, new in [
    ('new LocalRequirement("식량", 12, ProductionService::isFieldFood)', 'new LocalRequirement("식량(밀/당근/감자/비트)", 12, ProductionService::isFieldFood)'),
    ('new LocalRequirement("연료", 3, ProductionService::isFieldFuel)', 'new LocalRequirement("연료(석탄 또는 숯)", 3, ProductionService::isFieldFuel)'),
    ('new LocalRequirement("식량", 16, ProductionService::isFieldFood)', 'new LocalRequirement("식량(밀/당근/감자/비트)", 16, ProductionService::isFieldFood)'),
    ('new LocalRequirement("통나무", 12, stack -> stack.is(ItemTags.LOGS))', 'new LocalRequirement("아무 종류의 통나무", 12, stack -> stack.is(ItemTags.LOGS))'),
    ('new LocalRequirement("식량", 32, ProductionService::isFieldFood)', 'new LocalRequirement("식량(밀/당근/감자/비트)", 32, ProductionService::isFieldFood)'),
]:
    if old not in text:
        raise SystemExit(f"production server exact-name token missing: {old}")
    text = text.replace(old, new, 1)
text = text.replace('§7· 식량 §e" + food\n                + " §7· 철 §e" + iron + " §7· 연료 §e" + fuel + " §7· 통나무 §e" + logs + " §7· 석재벽돌 §e" + bricks',
                    '§7· 식량(밀/당근/감자/비트) §e" + food\n                + " §7· 철 주괴 §e" + iron + " §7· 연료(석탄/숯) §e" + fuel + " §7· 아무 종류의 통나무 §e" + logs + " §7· 석재 벽돌 §e" + bricks')
text = text.replace('원정=식량12+철3+연료3 / 방어=식량16+철5+통나무12 / 요새=식량32+철8+석재벽돌32.',
                    '원정=식량(밀/당근/감자/비트)12+철 주괴3+연료(석탄/숯)3 / 방어=식량16+철 주괴5+아무 종류의 통나무12 / 요새=식량32+철 주괴8+석재 벽돌32.')
text = text.replace('§7실물 출고1회는 금32+자수정16+메아리2이며 플레이어에게 직접 지급됩니다.',
                    '§7실물 출고 1회는 금 주괴 32 + 자수정 조각 16 + 메아리 조각 2이며 플레이어에게 직접 지급됩니다.')
text = text.replace('§b[산업 출고] §f현장 보급 물자 지급: §6금32 §7· §d자수정16 §7· §b메아리2',
                    '§b[산업 출고] §f현장 보급 물자 지급: §6금 주괴 32 §7· §d자수정 조각 16 §7· §b메아리 조각 2')
text = text.replace('§c[전선 현지 보급] §f방어전 시작 직후 전초 재고가 예상과 달라졌습니다. §7서버 관리자는 로그/모드 상호작용을 확인하세요.',
                    '§c[전선 현지 보급] §f방어전 시작 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요.')
text = text.replace('§c[전선 현지 보급] §f원정 출발 직후 전초 재고가 예상과 달라졌습니다. §7서버 관리자는 로그/모드 상호작용을 확인하세요.',
                    '§c[전선 현지 보급] §f원정 출발 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요.')
text = text.replace('§3[현장 일괄 적재] §f현재 범위 안에 사용할 수 있는 거점/창고 통이 없습니다.',
                    '§3[현장 일괄 적재] §f같은 차원에서 현재 로딩된 사용할 수 있는 등록 물류 통이 없습니다.')
text = text.replace('개를 가까운 실제 통부터 적재했습니다.', '개를 사용할 수 있는 등록 물류 통부터 적재했습니다.')
write(p, text)

p = "src/main/java/kr/moonseungjun/survivalascension/production/OutpostService.java"
text = read(p)
text = text.replace('자신의 등록된 §6배럴 물류 거점§f', '자신의 등록된 §6물류 통 거점§f')
text = text.replace('§2[전초기지] §f승격 재료 부족: §7철32 · 금8 · 석탄32.',
                    '§2[전초기지] §f승격 재료 부족: §7철 주괴 32 · 금 주괴 8 · 석탄 32.')
write(p, text)

# Field-depot wording, registration tutorial, and no enum leaks.
p = "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java"
text = read(p)
text = text.replace('§3[현장 물류] §f거점을 등록하지 못했습니다. §7(" + result.name() + ")',
                    '§3[현장 물류] §f거점을 등록하지 못했습니다. §7현재 다른 연결 상태나 거점 한도를 확인하세요.')
text = text.replace('§3[물류 창고군] §f창고 통을 연결하지 못했습니다. §7(" + result.name() + ")',
                    '§3[물류 창고군] §f창고 통을 연결하지 못했습니다. §7거점 거리·창고 한도·기존 연결 상태를 확인하세요.')
needle = '        player.sendSystemMessage(Component.literal("  §7- 지역 한도: 산업 3 · 토목 6 · 승천 중추 9"));\n'
if needle not in text:
    raise SystemExit('FieldDepotService tutorial insertion marker missing')
tutorial = (
    '        player.sendSystemMessage(Component.literal("  §7- 등록 방법: 산업 가공소 완공 → 등록할 통에서 4블록 이내 → M→인프라→산업 가공소→물류 거점 연결 · 최초 등록 보급권 1"));\n'
    '        player.sendSystemMessage(Component.literal("  §7- 확장 방법: 거점 6블록 안의 다른 통에서 창고 통 연결 · 거점당 최대 8개 · 추가 보급권 없음"));\n'
    '        player.sendSystemMessage(Component.literal("  §7- 이동 방법: 등록된 일반 거점/창고 통을 파괴하면 내용물이 바닥에 쏟아지지 않고 포장된 물류 통 1개로 보존됩니다. 다시 설치하면 연결을 자동 복구합니다."));\n'
)
text = text.replace(needle, needle + tutorial, 1)
write(p, text)

# Infrastructure funding text must match the new same-dimension loaded logistics rule.
p = "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java"
text = read(p)
text = text.replace('§7인벤토리 우선 → 가까운 실제 물류 배럴 순으로 인출',
                    '§7인벤토리와 같은 차원에서 현재 로딩된 등록 물류 통 재고를 함께 사용')
text = text.replace('현재 사용 가능한 물류 배럴들', '현재 사용 가능한 등록 물류 통들')
text = text.replace('  §7투입원: 인벤토리 + 현재 사용 가능한 거점 앵커/창고 배럴/전초 재고',
                    '  §7투입원: 인벤토리 + 같은 차원에서 현재 로딩된 등록 거점 통/창고 통/전초 재고')
write(p, text)

# ---------------------------------------------------------------------------
# 2) Portable registered logistics barrels.
# ---------------------------------------------------------------------------
portable = r'''package kr.moonseungjun.survivalascension.production;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Comparator;
import java.util.UUID;

/**
 * Registered logistics barrels can be moved without dumping 27 slots on the ground.
 * Only player-owned SA logistics barrels are affected; ordinary vanilla barrels keep vanilla behavior.
 * No chunk is force-loaded and physical outposts cannot be packed into a portable item.
 */
public final class PortableLogisticsBarrelService {
    private static final String OWNER_KEY = "survivalascension:packed_logistics_owner";
    private static final String ROLE_KEY = "survivalascension:packed_logistics_role";
    private static final String TOKEN_KEY = "survivalascension:packed_logistics_token";
    private static final String ROLE_ANCHOR = "anchor";
    private static final String ROLE_LINKED = "linked";

    private PortableLogisticsBarrelService() {}

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator() || !event.getState().is(Blocks.BARREL)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        boolean anchor = data.owns(player, dimension, pos);
        boolean linked = data.isLinkedByOwner(player, dimension, pos);
        if (!anchor && !linked) return;
        if (!level.mayInteract(player, pos)) return;

        FieldDepotData.DepotEntry depot = null;
        if (anchor) {
            depot = data.depots(player).stream()
                    .filter(entry -> entry.dimension().equals(dimension) && entry.pos().equals(pos))
                    .findFirst().orElse(null);
            if (depot == null) return;
            if (OutpostService.isOutpost(player, depot)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f전초기지로 승격된 거점 통은 포장할 수 없습니다. §7전초기지는 실제 위치에 남는 시설입니다."));
                return;
            }
            int linkedCount = data.linkedCount(player, depot);
            if (linkedCount > 0) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f이 거점에는 연결된 창고 통이 §e" + linkedCount
                        + "개§f 있습니다. §7창고 통부터 포장하거나 연결 해제한 뒤 거점 통을 옮기세요."));
                return;
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) return;
        if (containsPackedLogisticsBarrel(container)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f포장된 물류 통 안에 또 다른 포장된 물류 통을 넣을 수 없습니다. §7무한 중첩 방지를 위해 먼저 꺼내세요."));
            return;
        }

        String token = UUID.randomUUID().toString();
        CompoundTag persistent = blockEntity.getPersistentData();
        persistent.putString(OWNER_KEY, player.getUUID().toString());
        persistent.putString(ROLE_KEY, anchor ? ROLE_ANCHOR : ROLE_LINKED);
        persistent.putString(TOKEN_KEY, token);
        blockEntity.setChanged();

        ItemStack packed = new ItemStack(Items.BARREL);
        blockEntity.saveToItem(packed, level.registryAccess());
        CompoundTag marker = new CompoundTag();
        marker.putString(TOKEN_KEY, token);
        marker.putString(OWNER_KEY, player.getUUID().toString());
        marker.putString(ROLE_KEY, anchor ? ROLE_ANCHOR : ROLE_LINKED);
        CustomData.set(DataComponents.CUSTOM_DATA, packed, marker);
        packed.set(DataComponents.MAX_STACK_SIZE, 1);
        packed.set(DataComponents.ITEM_NAME, Component.literal(anchor ? "포장된 물류 거점 통" : "포장된 물류 창고 통"));

        event.setCanceled(true);
        if (anchor) {
            data.remove(player, depot);
        } else {
            data.removeLink(player, dimension, pos);
        }
        container.clearContent();
        blockEntity.setChanged();
        level.removeBlock(pos, false);

        if (!player.addItem(packed)) player.drop(packed, false);
        player.sendSystemMessage(Component.literal("§b[물류 통 포장] §f내용물 27칸을 그대로 보존해 §e"
                + (anchor ? "물류 거점 통" : "물류 창고 통") + "§f을 포장했습니다. §7새 위치에 설치하면 연결을 자동 복구합니다."));
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!event.getPlacedBlock().is(Blocks.BARREL)) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;
        CompoundTag persistent = blockEntity.getPersistentData();
        if (!persistent.contains(TOKEN_KEY) || !persistent.contains(OWNER_KEY) || !persistent.contains(ROLE_KEY)) return;

        String owner = persistent.getString(OWNER_KEY);
        String role = persistent.getString(ROLE_KEY);
        persistent.remove(TOKEN_KEY);
        persistent.remove(OWNER_KEY);
        persistent.remove(ROLE_KEY);
        blockEntity.setChanged();

        if (!owner.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("§3[물류 통 설치] §f내용물은 복원했지만 다른 플레이어가 포장한 통이므로 물류 연결은 자동 복구하지 않았습니다."));
            return;
        }

        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        if (ROLE_ANCHOR.equals(role)) {
            FieldDepotData.AddResult result = data.add(player, dimension, pos, FieldDepotData.registrationLimit(player));
            if (result == FieldDepotData.AddResult.ADDED || result == FieldDepotData.AddResult.ALREADY_OWNED) {
                player.sendSystemMessage(Component.literal("§b[물류 거점 이전] §f내용물과 거점 등록을 새 위치로 복구했습니다. §7현장 보급권은 추가로 소비하지 않았습니다."));
            } else {
                player.sendSystemMessage(Component.literal("§6[물류 거점 이전] §f내용물은 복원했지만 거점 등록은 자동 복구하지 못했습니다. §7거점 한도나 다른 연결을 확인한 뒤 물류 거점 연결을 선택하세요."));
            }
            return;
        }

        if (ROLE_LINKED.equals(role)) {
            FieldDepotData.DepotEntry nearest = data.depots(player).stream()
                    .filter(depot -> depot.dimension().equals(dimension))
                    .filter(depot -> depot.pos().distSqr(pos) <= FieldDepotData.MAX_LINK_RADIUS * FieldDepotData.MAX_LINK_RADIUS)
                    .filter(depot -> level.hasChunkAt(depot.pos()))
                    .filter(depot -> level.getBlockState(depot.pos()).is(Blocks.BARREL))
                    .filter(depot -> level.mayInteract(player, depot.pos()))
                    .min(Comparator.comparingDouble(depot -> depot.pos().distSqr(pos)))
                    .orElse(null);
            if (nearest == null) {
                player.sendSystemMessage(Component.literal("§6[물류 창고 이전] §f내용물은 복원했습니다. §7자동 연결하려면 자신의 등록 거점 통에서 6블록 안에 설치하세요. 지금 위치에서는 '창고 통 연결'로 수동 연결할 수 있습니다."));
                return;
            }
            FieldDepotData.LinkResult result = data.addLink(player, nearest, pos);
            if (result == FieldDepotData.LinkResult.ADDED || result == FieldDepotData.LinkResult.ALREADY_LINKED) {
                player.sendSystemMessage(Component.literal("§b[물류 창고 이전] §f내용물과 창고 연결을 새 위치로 복구했습니다. §7추가 보급권은 필요하지 않습니다."));
            } else {
                player.sendSystemMessage(Component.literal("§6[물류 창고 이전] §f내용물은 복원했지만 창고 연결은 자동 복구하지 못했습니다. §7거점당 창고 한도와 기존 연결을 확인하세요."));
            }
        }
    }

    private static boolean containsPackedLogisticsBarrel(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(Items.BARREL)) continue;
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.contains(TOKEN_KEY)) return true;
        }
        return false;
    }
}
'''
write("src/main/java/kr/moonseungjun/survivalascension/production/PortableLogisticsBarrelService.java", portable)

# Wire the new service onto the game event bus.
p = "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
text = read(p)
import_marker = 'import kr.moonseungjun.survivalascension.production.OutpostSiegeSystem;\n'
if import_marker not in text:
    raise SystemExit('SurvivalAscension import marker missing')
text = text.replace(import_marker, import_marker + 'import kr.moonseungjun.survivalascension.production.PortableLogisticsBarrelService;\n', 1)
listener_marker = '        NeoForge.EVENT_BUS.addListener(ConstructionProgression::onBlockPlaced);\n'
if listener_marker not in text:
    raise SystemExit('SurvivalAscension placement listener marker missing')
text = text.replace(listener_marker,
                    '        NeoForge.EVENT_BUS.addListener(PortableLogisticsBarrelService::onBlockBreak);\n'
                    '        NeoForge.EVENT_BUS.addListener(PortableLogisticsBarrelService::onBlockPlaced);\n'
                    + listener_marker,
                    1)
write(p, text)

# ---------------------------------------------------------------------------
# 3) Source-level acceptance checks for this migration itself.
# ---------------------------------------------------------------------------
checks = {
    "src/main/java/kr/moonseungjun/survivalascension/production/PortableLogisticsBarrelService.java": [
        "blockEntity.saveToItem(packed, level.registryAccess())",
        "container.clearContent()",
        "DataComponents.MAX_STACK_SIZE",
        "containsPackedLogisticsBarrel",
        "OutpostService.isOutpost(player, depot)",
        "현장 보급권은 추가로 소비하지 않았습니다",
    ],
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java": [
        "철 원석 16", "자수정 조각 12", "연료: 석탄 또는 숯 3", "등록 통 파괴 시 내용물째 포장 이동",
    ],
    "src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java": [
        "등록 방법: 산업 가공소 완공", "이동 방법: 등록된 일반 거점/창고 통을 파괴하면",
    ],
}
for rel, needles in checks.items():
    value = read(rel)
    for needle in needles:
        if needle not in value:
            raise SystemExit(f"logistics QOL acceptance missing in {rel}: {needle}")

print("Survival Ascension logistics QOL migration applied")
