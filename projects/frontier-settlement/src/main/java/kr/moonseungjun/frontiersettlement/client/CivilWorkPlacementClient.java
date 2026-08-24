package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.CivilWorkPreviewPayload;
import kr.moonseungjun.frontiersettlement.network.CivilWorkRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Uses the existing B / Enter / Backspace interaction budget: first corner fixes grade Y, second sets area. */
public final class CivilWorkPlacementClient {
    private static boolean active;
    private static BlockPos first;
    private static BlockPos target = BlockPos.ZERO;
    private static CivilWorkPreviewPayload preview;
    private static int nonce;
    private static int lastAcceptedNonce;
    private static int refreshTicks;

    private CivilWorkPlacementClient() {}

    public static void beginPlacement() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        active = true;
        first = null;
        preview = null;
        refreshTicks = 0;
        target = resolveTarget(minecraft);
        BuildingPlacementClient.cancel();
        RoadPlacementClient.cancel();
        OutpostPlacementClient.cancel();
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) { cancel(); return; }
        if (!active || minecraft.gui.screen() != null) return;
        BlockPos next = resolveTarget(minecraft);
        if (!next.equals(target)) {
            target = next;
            preview = null;
            refreshTicks = 0;
        }
        if (first != null && refreshTicks-- <= 0) {
            send(false);
            refreshTicks = 5;
        }
    }

    public static void confirm() {
        if (!active) return;
        if (first == null) {
            first = target;
            preview = null;
            refreshTicks = 0;
        } else if (preview != null && preview.valid()) {
            send(true);
        }
    }

    public static void resetStart() {
        if (!active) return;
        first = null;
        preview = null;
        refreshTicks = 0;
    }

    private static BlockPos resolveTarget(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult hit) return hit.getBlockPos();
        return minecraft.player.blockPosition();
    }

    private static void send(boolean confirm) {
        if (!active || first == null) return;
        int requestNonce = ++nonce;
        ClientPacketDistributor.sendToServer(new CivilWorkRequestPayload(requestNonce,
                first.getX(), first.getY(), first.getZ(),
                target.getX(), target.getY(), target.getZ(), confirm));
    }

    public static void acceptPreview(CivilWorkPreviewPayload next) {
        if (next.nonce() < lastAcceptedNonce) return;
        lastAcceptedNonce = next.nonce();
        if (!active) return;
        preview = next;
        if (next.confirmed()) cancel();
    }

    public static List<BlockPos> ghostBlocks() {
        if (!active || first == null) return List.of();
        int minX, maxX, minZ, maxZ, y;
        if (preview != null) {
            minX = preview.minX(); maxX = preview.maxX(); minZ = preview.minZ(); maxZ = preview.maxZ(); y = preview.gradeY();
        } else {
            minX = Math.min(first.getX(), target.getX()); maxX = Math.max(first.getX(), target.getX());
            minZ = Math.min(first.getZ(), target.getZ()); maxZ = Math.max(first.getZ(), target.getZ()); y = first.getY();
        }
        if (maxX - minX + 1 > 17 || maxZ - minZ + 1 > 17) return List.of();
        List<BlockPos> out = new ArrayList<>((maxX - minX + 1) * (maxZ - minZ + 1));
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) out.add(new BlockPos(x, y, z));
        return List.copyOf(out);
    }

    public static String statusLine() {
        if (!active) return "";
        if (first == null) return "토목 | 첫 모서리 선택 · 이 블록 높이가 최종 기준면";
        String state = preview == null ? "영역 검사 중" : preview.message();
        return "토목 | 기준 Y " + first.getY() + " | " + state;
    }

    public static boolean active() { return active; }
    public static BlockPos first() { return first; }
    public static CivilWorkPreviewPayload preview() { return preview; }
    public static boolean ghostValid() { return preview != null && preview.valid(); }

    public static void cancel() {
        active = false;
        first = null;
        preview = null;
        refreshTicks = 0;
    }
}
