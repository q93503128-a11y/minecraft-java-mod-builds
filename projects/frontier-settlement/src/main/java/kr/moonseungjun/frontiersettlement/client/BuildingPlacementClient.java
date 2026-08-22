package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import kr.moonseungjun.frontiersettlement.network.PlacementPreviewPayload;
import kr.moonseungjun.frontiersettlement.network.PlacementRequestPayload;
import kr.moonseungjun.frontiersettlement.settlement.BuildingRotation;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class BuildingPlacementClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "construction"));
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.frontier_settlement.build_mode", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping NEXT = new KeyMapping(
            "key.frontier_settlement.next_building", GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping ROTATE = new KeyMapping(
            "key.frontier_settlement.rotate_building", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping CONFIRM = new KeyMapping(
            "key.frontier_settlement.confirm_building", GLFW.GLFW_KEY_ENTER, CATEGORY);

    private static boolean active;
    private static int selectedIndex;
    private static BuildingRotation rotation = BuildingRotation.NONE;
    private static BlockPos target = BlockPos.ZERO;
    private static PlacementPreviewPayload preview;
    private static int nonce;
    private static int lastAcceptedNonce;
    private static int refreshTicks;

    private BuildingPlacementClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE);
        event.register(NEXT);
        event.register(ROTATE);
        event.register(CONFIRM);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            deactivate();
            return;
        }

        while (TOGGLE.consumeClick()) {
            active = !active;
            preview = null;
            refreshTicks = 0;
            if (active) {
                rotation = BuildingRotation.facingPlayerFrom(minecraft.player.getDirection());
                target = resolveTarget(minecraft);
            }
        }
        if (!active) return;

        while (NEXT.consumeClick()) {
            selectedIndex = (selectedIndex + 1) % BuildingType.values().length;
            preview = null;
            refreshTicks = 0;
        }
        while (ROTATE.consumeClick()) {
            rotation = rotation.next();
            preview = null;
            refreshTicks = 0;
        }

        BlockPos nextTarget = resolveTarget(minecraft);
        if (!nextTarget.equals(target)) {
            target = nextTarget;
            refreshTicks = 0;
        }

        if (refreshTicks-- <= 0) {
            send(false);
            refreshTicks = 5;
        }

        while (CONFIRM.consumeClick()) {
            if (preview != null && preview.valid() && previewMatchesSelection()) {
                send(true);
            }
        }
    }

    private static BlockPos resolveTarget(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult hit) return hit.getBlockPos().above();
        return minecraft.player.blockPosition().relative(minecraft.player.getDirection(), 10);
    }

    private static void send(boolean confirm) {
        if (!active) return;
        BuildingType type = selectedType();
        int requestNonce = ++nonce;
        ClientPacketDistributor.sendToServer(new PlacementRequestPayload(
                requestNonce, type.id(), target.getX(), target.getY(), target.getZ(), rotation.id(), confirm));
    }

    public static void acceptPreview(PlacementPreviewPayload next) {
        if (next.nonce() < lastAcceptedNonce) return;
        lastAcceptedNonce = next.nonce();
        if (!active) return;
        preview = next;
        if (next.confirmed()) deactivate();
    }

    private static boolean previewMatchesSelection() {
        return preview != null
                && selectedType().id().equals(preview.buildingType())
                && rotation.id() == Math.floorMod(preview.rotation(), 4);
    }

    public static boolean active() { return active; }
    public static BuildingType selectedType() { return BuildingType.values()[selectedIndex]; }
    public static BuildingRotation rotation() { return rotation; }
    public static PlacementPreviewPayload preview() { return previewMatchesSelection() ? preview : null; }

    public static BlockPos ghostOrigin() {
        PlacementPreviewPayload p = preview();
        if (p != null && p.valid()) return p.origin();
        BuildingType type = selectedType();
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        return new BlockPos(target.getX() - width / 2, target.getY(), target.getZ() - depth / 2);
    }

    public static String statusLine() {
        if (!active) return "";
        PlacementPreviewPayload p = preview();
        String state = p == null ? "확인 중" : (p.valid() ? "배치 가능" : p.message());
        return "건설 | " + selectedType().displayName()
                + " | 회전 " + (rotation.id() * 90) + "° | " + state;
    }

    private static void deactivate() {
        active = false;
        preview = null;
        refreshTicks = 0;
    }
}
