package kr.moonseungjun.survivalascension.production;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loaded-only validation for optional player-built fortification rings around an existing outpost.
 * The structure is never auto-built and grants no passive combat percentage; its blocks matter
 * through normal collision/pathing and by admitting the harder bastion defense encounter.
 */
public final class OutpostFortificationService {
    public static final int INNER_RADIUS = 6;
    public static final int OUTER_RADIUS = 12;
    public static final int VERTICAL_DOWN = 3;
    public static final int VERTICAL_UP = 4;
    public static final int MIN_COLUMNS_PER_QUADRANT = 12;
    public static final int MIN_TOTAL_COLUMNS = MIN_COLUMNS_PER_QUADRANT * 4;

    private OutpostFortificationService() {}

    public static boolean validateForBastion(ServerPlayer player, OutpostData.OutpostEntry outpost, boolean message) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        if (!outpost.dimension().equals(level.dimension().toString())) {
            if (message) player.sendSystemMessage(Component.literal("§4[요새 방어] §f현재 차원의 전초에서 준비해야 합니다."));
            return false;
        }
        if (!OutpostService.isRecoveryOperational(player, level, outpost.dimension(), outpost.pos())) {
            if (message) player.sendSystemMessage(Component.literal("§4[요새 방어] §f앵커 배럴과 침대·모닥불·작업대·화로 전초 구조가 먼저 작동해야 합니다."));
            return false;
        }
        ScanResult result = scan(level, outpost.pos());
        if (result.complete()) return true;
        if (message) {
            player.sendSystemMessage(Component.literal("§4[요새 방어] §f물리 방어진지가 부족합니다. §7앵커 반경 "
                    + INNER_RADIUS + "~" + OUTER_RADIUS + "블록에 벽 계열/철창/네더벽돌 울타리를 사분면마다 §e"
                    + MIN_COLUMNS_PER_QUADRANT + "열§7 이상 세우세요."));
            player.sendSystemMessage(Component.literal("  §7NE §f" + result.northEast() + "/" + MIN_COLUMNS_PER_QUADRANT
                    + " §7· NW §f" + result.northWest() + "/" + MIN_COLUMNS_PER_QUADRANT
                    + " §7· SE §f" + result.southEast() + "/" + MIN_COLUMNS_PER_QUADRANT
                    + " §7· SW §f" + result.southWest() + "/" + MIN_COLUMNS_PER_QUADRANT
                    + " §7· 총 §f" + result.total() + "/" + MIN_TOTAL_COLUMNS));
        }
        return false;
    }

    public static void sendStatus(ServerPlayer player) {
        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, OutpostSiegeSystem.START_RADIUS);
        if (outpost == null || !(player.level() instanceof ServerLevel level)) {
            player.sendSystemMessage(Component.literal("§4[요새 방어진지] §f활성 전초 앵커4블록 안에서 현재 물리 방어진지를 검사할 수 있습니다."));
            return;
        }
        ScanResult result = scan(level, outpost.pos());
        String state = result.complete() ? "§a준비 완료" : "§e미완성";
        player.sendSystemMessage(Component.literal("§4[요새 방어진지] " + state + " §7· 반경 " + INNER_RADIUS + "~" + OUTER_RADIUS
                + " · 사분면 최소 " + MIN_COLUMNS_PER_QUADRANT + "열 · 총 " + result.total() + "/" + MIN_TOTAL_COLUMNS));
        player.sendSystemMessage(Component.literal("  §7NE §f" + result.northEast() + " §7· NW §f" + result.northWest()
                + " §7· SE §f" + result.southEast() + " §7· SW §f" + result.southWest()
                + " §7· 같은 x/z 기둥은 높이와 무관하게 1열로 계산"));
    }

    public static ScanResult scan(ServerLevel level, BlockPos anchor) {
        int northEast = 0;
        int northWest = 0;
        int southEast = 0;
        int southWest = 0;
        int innerSq = INNER_RADIUS * INNER_RADIUS;
        int outerSq = OUTER_RADIUS * OUTER_RADIUS;

        for (int dx = -OUTER_RADIUS; dx <= OUTER_RADIUS; dx++) {
            for (int dz = -OUTER_RADIUS; dz <= OUTER_RADIUS; dz++) {
                int horizontalSq = dx * dx + dz * dz;
                if (horizontalSq < innerSq || horizontalSq > outerSq) continue;
                if (!fortifiedColumn(level, anchor, dx, dz)) continue;
                if (dx >= 0 && dz < 0) northEast++;
                else if (dx < 0 && dz < 0) northWest++;
                else if (dx >= 0) southEast++;
                else southWest++;
            }
        }
        return new ScanResult(northEast, northWest, southEast, southWest);
    }

    private static boolean fortifiedColumn(ServerLevel level, BlockPos anchor, int dx, int dz) {
        for (int dy = -VERTICAL_DOWN; dy <= VERTICAL_UP; dy++) {
            BlockPos pos = anchor.offset(dx, dy, dz);
            if (!level.hasChunkAt(pos)) continue;
            if (isFortificationBlock(level.getBlockState(pos))) return true;
        }
        return false;
    }

    private static boolean isFortificationBlock(BlockState state) {
        return state.is(BlockTags.WALLS) || state.is(Blocks.IRON_BARS) || state.is(Blocks.NETHER_BRICK_FENCE);
    }

    public record ScanResult(int northEast, int northWest, int southEast, int southWest) {
        public int total() { return northEast + northWest + southEast + southWest; }
        public boolean complete() {
            return northEast >= MIN_COLUMNS_PER_QUADRANT
                    && northWest >= MIN_COLUMNS_PER_QUADRANT
                    && southEast >= MIN_COLUMNS_PER_QUADRANT
                    && southWest >= MIN_COLUMNS_PER_QUADRANT;
        }
    }
}
