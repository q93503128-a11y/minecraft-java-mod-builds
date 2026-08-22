package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.OutpostPlacementRequestPayload;
import kr.moonseungjun.frontiersettlement.network.OutpostPreviewPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class OutpostPlacementClient {
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.frontier_settlement.outpost_mode", GLFW.GLFW_KEY_K, BuildingPlacementClient.CATEGORY);
    public static final KeyMapping CONFIRM = new KeyMapping(
            "key.frontier_settlement.outpost_confirm", GLFW.GLFW_KEY_ENTER, BuildingPlacementClient.CATEGORY);

    private static boolean active;
    private static BlockPos target = BlockPos.ZERO;
    private static OutpostPreviewPayload preview;
    private static int nonce;
    private static int lastAcceptedNonce;
    private static int refreshTicks;

    private OutpostPlacementClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        event.register(CONFIRM);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            cancel();
            return;
        }

        while (TOGGLE.consumeClick()) {
            active = !active;
            preview = null;
            refreshTicks = 0;
            if (active) {
                BuildingPlacementClient.cancel();
                RoadPlacementClient.cancel();
                target = resolveTarget(minecraft);
            }
        }
        if (!active) return;

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

        while (CONFIRM.consumeClick()) {
            if (preview != null && preview.valid()) send(true);
        }
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
