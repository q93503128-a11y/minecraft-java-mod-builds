package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.RoadPlacementRequestPayload;
import kr.moonseungjun.frontiersettlement.network.RoadPreviewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RoadPlacementClient {
    private static boolean active;
    private static BlockPos start;
    private static BlockPos target = BlockPos.ZERO;
    private static RoadPreviewPayload preview;
    private static int nonce;
    private static int lastAcceptedNonce;
    private static int refreshTicks;

    private RoadPlacementClient() {}

    public static void beginPlacement() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        active = true;
        start = null;
        preview = null;
        refreshTicks = 0;
        target = resolveTarget(minecraft);
        BuildingPlacementClient.cancel();
        OutpostPlacementClient.cancel();
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            cancel();
            return;
        }
        if (!active || minecraft.gui.screen() != null) return;

        BlockPos nextTarget = resolveTarget(minecraft);
        if (!nextTarget.equals(target)) {
            target = nextTarget;
            preview = null;
            refreshTicks = 0;
        }

        if (start != null && refreshTicks-- <= 0) {
            send(false);
            refreshTicks = 5;
        }
    }

    public static void confirm() {
        if (!active) return;
        if (start == null) {
            start = target;
            preview = null;
            refreshTicks = 0;
        } else if (preview != null && preview.valid()) {
            send(true);
        }
    }

    public static void resetStart() {
        if (!active) return;
        start = null;
        preview = null;
        refreshTicks = 0;
    }

    private static BlockPos resolveTarget(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult hit) return hit.getBlockPos();
        return minecraft.player.blockPosition();
    }

    private static void send(boolean confirm) {
        if (!active || start == null) return;
        int requestNonce = ++nonce;
        ClientPacketDistributor.sendToServer(new RoadPlacementRequestPayload(
                requestNonce,
                start.getX(), start.getY(), start.getZ(),
                target.getX(), target.getY(), target.getZ(),
                confirm));
    }

    public static void acceptPreview(RoadPreviewPayload next) {
        if (next.nonce() < lastAcceptedNonce) return;
        lastAcceptedNonce = next.nonce();
        if (!active) return;
        preview = next;
        if (next.confirmed()) cancel();
    }

    public static boolean active() { return active; }
    public static BlockPos start() { return start; }
    public static RoadPreviewPayload preview() { return preview; }

    public static boolean ghostValid() {
        return preview != null && preview.valid();
    }

    public static List<BlockPos> ghostBlocks() {
        List<BlockPos> centers = preview != null && !preview.centers().isEmpty()
                ? preview.centers()
                : fallbackCenters();
        if (centers.size() < 2) return List.of();

        Set<BlockPos> blocks = new LinkedHashSet<>();
        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            int[] direction = directionAt(centers, i);
            for (int side = -1; side <= 1; side++) {
                blocks.add(new BlockPos(
                        center.getX() - direction[1] * side,
                        center.getY(),
                        center.getZ() + direction[0] * side));
            }
        }
        return List.copyOf(blocks);
    }

    private static List<BlockPos> fallbackCenters() {
        if (start == null || target == null || start.equals(target)) return List.of();
        List<BlockPos> out = new ArrayList<>();
        int x = start.getX();
        int z = start.getZ();
        int y = start.getY();
        out.add(new BlockPos(x, y, z));
        int dx = Math.abs(target.getX() - x);
        int dz = Math.abs(target.getZ() - z);
        if (dx >= dz) {
            while (x != target.getX()) { x += Integer.signum(target.getX() - x); out.add(new BlockPos(x, y, z)); }
            while (z != target.getZ()) { z += Integer.signum(target.getZ() - z); out.add(new BlockPos(x, y, z)); }
        } else {
            while (z != target.getZ()) { z += Integer.signum(target.getZ() - z); out.add(new BlockPos(x, y, z)); }
            while (x != target.getX()) { x += Integer.signum(target.getX() - x); out.add(new BlockPos(x, y, z)); }
        }
        return List.copyOf(out);
    }

    private static int[] directionAt(List<BlockPos> centers, int index) {
        BlockPos from;
        BlockPos to;
        if (index < centers.size() - 1) {
            from = centers.get(index);
            to = centers.get(index + 1);
        } else {
            from = centers.get(index - 1);
            to = centers.get(index);
        }
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        return Math.abs(dx) + Math.abs(dz) == 1 ? new int[] {dx, dz} : new int[] {1, 0};
    }

    public static String statusLine() {
        if (!active) return "";
        if (start == null) return "도로 | 시작점 선택";
        String state = preview == null ? "경로 계산 중" : preview.message();
        return "도로 | 시작 " + start.getX() + "," + start.getZ() + " | " + state;
    }

    public static void cancel() {
        active = false;
        start = null;
        preview = null;
        refreshTicks = 0;
    }
}
