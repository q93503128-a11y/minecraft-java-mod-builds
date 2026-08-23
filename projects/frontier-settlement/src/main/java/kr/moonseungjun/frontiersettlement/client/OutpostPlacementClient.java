package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.OutpostPlacementRequestPayload;
import kr.moonseungjun.frontiersettlement.network.OutpostPreviewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class OutpostPlacementClient {
    private static boolean active;
    private static BlockPos target = BlockPos.ZERO;
    private static OutpostPreviewPayload preview;
    private static int nonce;
    private static int lastAcceptedNonce;
    private static int refreshTicks;

    private OutpostPlacementClient() {}

    public static void beginPlacement() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        active = true;
        preview = null;
        refreshTicks = 0;
        target = resolveTarget(minecraft);
        BuildingPlacementClient.cancel();
        RoadPlacementClient.cancel();
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

        if (refreshTicks-- <= 0) {
            send(false);
            refreshTicks = 5;
        }
    }

    public static void confirm() {
        if (!active || preview == null || !preview.valid()) return;
        send(true);
    }

    private static BlockPos resolveTarget(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult hit) return hit.getBlockPos();
        return minecraft.player.blockPosition();
    }

    private static void send(boolean confirm) {
        if (!active) return;
        int requestNonce = ++nonce;
        ClientPacketDistributor.sendToServer(new OutpostPlacementRequestPayload(
                requestNonce, target.getX(), target.getY(), target.getZ(), confirm));
    }

    public static void acceptPreview(OutpostPreviewPayload next) {
        if (next.nonce() < lastAcceptedNonce) return;
        lastAcceptedNonce = next.nonce();
        if (!active) return;
        preview = next;
        if (next.confirmed()) cancel();
    }

    public static boolean active() { return active; }
    public static OutpostPreviewPayload preview() { return preview; }

    public static String statusLine() {
        if (!active) return "";
        if (preview == null) return "전초기지 | 도로 끝을 가리키는 중";
        return "전초기지 | " + preview.message();
    }

    public static void cancel() {
        active = false;
        preview = null;
        refreshTicks = 0;
    }
}
