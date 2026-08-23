package kr.moonseungjun.survivalascension.infrastructure;

import kr.moonseungjun.survivalascension.production.FieldDepotData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit commissioning-site validation for the three large stage-1/2 infrastructure projects.
 * The scan only touches already-loaded blocks around a real Barrel anchor and never force-loads chunks.
 */
public final class InfrastructureSiteService {
    public static final int ANCHOR_RADIUS = 4;
    public static final int SITE_RADIUS = 6;

    private static final SiteProfile INDUSTRIAL_SITE = new SiteProfile(false, List.of(
            new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 48),
            new SiteRequirement(Blocks.IRON_BLOCK, "철 블록", 4),
            new SiteRequirement(Blocks.BLAST_FURNACE, "용광로", 2),
            new SiteRequirement(Blocks.STONECUTTER, "석재 절단기", 1),
            new SiteRequirement(Blocks.HOPPER, "호퍼", 2)
    ));
    private static final SiteProfile APEX_SITE = new SiteProfile(true, List.of(
            new SiteRequirement(Blocks.STONE_BRICKS, "석재 벽돌", 32),
            new SiteRequirement(Blocks.GOLD_BLOCK, "금 블록", 4),
            new SiteRequirement(Blocks.LODESTONE, "자석석", 1),
            new SiteRequirement(Blocks.CARTOGRAPHY_TABLE, "지도 제작대", 1),
            new SiteRequirement(Blocks.TARGET, "과녁", 4)
    ));
    private static final SiteProfile NEXUS_SITE = new SiteProfile(true, List.of(
            new SiteRequirement(Blocks.OBSIDIAN, "흑요석", 32),
            new SiteRequirement(Blocks.CRYING_OBSIDIAN, "우는 흑요석", 8),
            new SiteRequirement(Blocks.BEACON, "신호기", 1),
            new SiteRequirement(Blocks.ENCHANTING_TABLE, "마법 부여대", 1),
            new SiteRequirement(Blocks.ENDER_CHEST, "엔더 상자", 1)
    ));

    private InfrastructureSiteService() {}

    public static boolean requiresSite(InfrastructureProject project) {
        return profile(project) != null;
    }

    /**
     * Called only when the current funding action could finish every remaining material requirement.
     * Failure consumes no funding material in that action.
     */
    public static boolean validateForFinalFunding(ServerPlayer player, InfrastructureProject project) {
        SiteProfile profile = profile(project);
        if (profile == null) return true;
        SiteInspection inspection = inspect(player, profile);
        if (inspection.complete(profile)) return true;

        String anchorKind = profile.registeredAnchor() ? "자신의 등록 물류 배럴" : "실제 배럴";
        if (inspection.anchor() == null) {
            player.sendSystemMessage(Component.literal("§6[물리 준공] §e" + project.koreanName()
                    + "§f의 마지막 투입 전에는 §e4블록 안 " + anchorKind + "§f을 중심으로 준공 현장을 만들어야 합니다."));
        } else {
            BlockPos anchor = inspection.anchor();
            player.sendSystemMessage(Component.literal("§6[물리 준공] §e" + project.koreanName() + "§f 현장이 아직 미완성입니다. §7배럴 "
                    + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ() + " · 반경 " + SITE_RADIUS));
        }
        sendCounts(player, profile, inspection);
        player.sendSystemMessage(Component.literal("§7현장이 완성되기 전에는 이번 마지막 자원 투입을 처리하지 않습니다."));
        return false;
    }

    public static void sendStatus(ServerPlayer player, InfrastructureProject project) {
        SiteProfile profile = profile(project);
        if (profile == null) return;
        SiteInspection inspection = inspect(player, profile);
        String anchorKind = profile.registeredAnchor() ? "자기 등록 배럴" : "실제 배럴";
        if (inspection.anchor() == null) {
            player.sendSystemMessage(Component.literal("  §6준공 현장: §c앵커 없음 §7· 4블록 안 " + anchorKind + " 필요 · 반경 " + SITE_RADIUS));
            sendCounts(player, profile, inspection);
            return;
        }
        BlockPos anchor = inspection.anchor();
        player.sendSystemMessage(Component.literal("  §6준공 현장: " + (inspection.complete(profile) ? "§a완성" : "§e건설 중")
                + " §7· 배럴 " + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ() + " · 반경 " + SITE_RADIUS));
        sendCounts(player, profile, inspection);
    }

    private static void sendCounts(ServerPlayer player, SiteProfile profile, SiteInspection inspection) {
        for (SiteRequirement requirement : profile.requirements()) {
            int current = inspection.count(requirement.block());
            String color = current >= requirement.amount() ? "§a" : "§e";
            player.sendSystemMessage(Component.literal("    §7- §f" + requirement.label() + " " + color + current + "§7/§f" + requirement.amount()));
        }
    }

    private static SiteInspection inspect(ServerPlayer player, SiteProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos anchor = profile.registeredAnchor()
                ? nearestOwnedDepotAnchor(player, level)
                : nearestPhysicalBarrel(player, level);
        Map<Block, Integer> counts = new HashMap<>();
        if (anchor == null) return new SiteInspection(null, counts);

        int radiusSq = SITE_RADIUS * SITE_RADIUS;
        for (int dx = -SITE_RADIUS; dx <= SITE_RADIUS; dx++) {
            for (int dy = -SITE_RADIUS; dy <= SITE_RADIUS; dy++) {
                for (int dz = -SITE_RADIUS; dz <= SITE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    for (SiteRequirement requirement : profile.requirements()) {
                        if (state.is(requirement.block())) {
                            counts.merge(requirement.block(), 1, Integer::sum);
                            break;
                        }
                    }
                }
            }
        }
        return new SiteInspection(anchor, counts);
    }

    private static BlockPos nearestPhysicalBarrel(ServerPlayer player, ServerLevel level) {
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int radiusSq = ANCHOR_RADIUS * ANCHOR_RADIUS;
        for (int dx = -ANCHOR_RADIUS; dx <= ANCHOR_RADIUS; dx++) {
            for (int dy = -ANCHOR_RADIUS; dy <= ANCHOR_RADIUS; dy++) {
                for (int dz = -ANCHOR_RADIUS; dz <= ANCHOR_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!isRealBarrel(player, level, pos)) continue;
                    double distance = pos.distSqr(origin);
                    if (distance >= bestDistance) continue;
                    best = pos.immutable();
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static BlockPos nearestOwnedDepotAnchor(ServerPlayer player, ServerLevel level) {
        String dimension = level.dimension().toString();
        BlockPos origin = player.blockPosition();
        double maxDistance = ANCHOR_RADIUS * ANCHOR_RADIUS;
        return FieldDepotData.get(player).depots(player).stream()
                .filter(depot -> depot.dimension().equals(dimension))
                .filter(depot -> depot.pos().distSqr(origin) <= maxDistance)
                .filter(depot -> isRealBarrel(player, level, depot.pos()))
                .min(Comparator.comparingDouble(depot -> depot.pos().distSqr(origin)))
                .map(FieldDepotData.DepotEntry::pos)
                .map(BlockPos::immutable)
                .orElse(null);
    }

    private static boolean isRealBarrel(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        if (!level.getBlockState(pos).is(Blocks.BARREL)) return false;
        if (!level.mayInteract(player, pos)) return false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container;
    }

    private static SiteProfile profile(InfrastructureProject project) {
        return switch (project) {
            case INDUSTRIAL_WORKS -> INDUSTRIAL_SITE;
            case APEX_TRACKING_POST -> APEX_SITE;
            case ASCENSION_NEXUS -> NEXUS_SITE;
            default -> null;
        };
    }

    private record SiteRequirement(Block block, String label, int amount) {}
    private record SiteProfile(boolean registeredAnchor, List<SiteRequirement> requirements) {}
    private record SiteInspection(BlockPos anchor, Map<Block, Integer> counts) {
        int count(Block block) { return counts.getOrDefault(block, 0); }
        boolean complete(SiteProfile profile) {
            if (anchor == null) return false;
            for (SiteRequirement requirement : profile.requirements()) {
                if (count(requirement.block()) < requirement.amount()) return false;
            }
            return true;
        }
    }
}
