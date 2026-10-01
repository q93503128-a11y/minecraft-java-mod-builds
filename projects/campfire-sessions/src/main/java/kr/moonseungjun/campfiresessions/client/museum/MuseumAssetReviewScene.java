package kr.moonseungjun.campfiresessions.client.museum;

import com.mojang.brigadier.Command;
import com.mojang.math.Axis;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.client.museum.MuseumExhibitAssets.MuseumStaticMeshAsset;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development-only world placement for visually reviewing real museum assets.
 *
 * <p>This class is registered only by the explicit asset-review JVM profile.
 * It never invents replacement geometry: the command refuses to activate until
 * the verified Smithsonian mesh and its real base-color texture are available
 * from an enabled review resource pack.</p>
 */
public final class MuseumAssetReviewScene {
    private static final ContextKey<ReviewPlacement> RENDER_DATA =
            new ContextKey<>(id("museum_asset_review"));

    // Verified 100k Triceratops accessor bounds from the Smithsonian-derived CFMS.
    // Source geometry is effectively Z-up: X=width, Y=length, Z=height.
    // The review transform maps source Z to Minecraft Y and keeps 1 source metre
    // equal to 1 Minecraft block. Final exhibit transform still requires visual acceptance.
    private static final double TRI_MIN_X = -1.018591046333313;
    private static final double TRI_MAX_X = 1.7879759073257446;
    private static final double TRI_MIN_Y = -3.501384973526001;
    private static final double TRI_MAX_Y = 2.3829238414764404;
    private static final double TRI_MIN_Z = 0.2274170070886612;
    private static final double TRI_MAX_Z = 1.912650227546692;

    private static final double TRI_CENTER_X = (TRI_MIN_X + TRI_MAX_X) * 0.5;
    private static final double TRI_CENTER_Y = (TRI_MIN_Y + TRI_MAX_Y) * 0.5;
    private static final double TRI_HEIGHT = TRI_MAX_Z - TRI_MIN_Z;
    private static final double TRI_CULL_RADIUS = 3.35;
    private static final double PLACE_DISTANCE = 8.0;

    private static ReviewPlacement activePlacement;

    private MuseumAssetReviewScene() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MuseumAssetReviewScene::registerClientCommands);
        NeoForge.EVENT_BUS.addListener(MuseumAssetReviewScene::extractRenderState);
        NeoForge.EVENT_BUS.addListener(MuseumAssetReviewScene::submitGeometry);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("campfire_museum_review")
                        .executes(context -> status())
                        .then(Commands.literal("triceratops")
                                .executes(context -> placeTriceratops()))
                        .then(Commands.literal("clear")
                                .executes(context -> clear()))
        );
    }

    private static int status() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }

        if (activePlacement == null) {
            minecraft.player.sendSystemMessage(
                    Component.literal("Campfire museum review: no exhibit is active.")
            );
        } else {
            minecraft.player.sendSystemMessage(
                    Component.literal("Campfire museum review: Triceratops exhibit active. Use /campfire_museum_review clear to remove it.")
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int placeTriceratops() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return 0;
        }

        MuseumStaticMeshAsset asset = MuseumExhibitAssets.TRICERATOPS_100K;
        if (minecraft.getResourceManager().getResource(asset.mesh()).isEmpty()
                || minecraft.getResourceManager().getResource(asset.baseColorTexture()).isEmpty()) {
            minecraft.player.sendSystemMessage(
                    Component.literal("Campfire museum review pack is not active. Enable campfire-museum-review-pack.zip first.")
            );
            activePlacement = null;
            return 0;
        }

        Vec3 look = minecraft.player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            double yawRadians = Math.toRadians(minecraft.player.getYRot());
            horizontal = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        } else {
            horizontal = horizontal.normalize();
        }

        Vec3 anchor = new Vec3(
                minecraft.player.getX() + horizontal.x * PLACE_DISTANCE,
                minecraft.player.getY(),
                minecraft.player.getZ() + horizontal.z * PLACE_DISTANCE
        );

        activePlacement = new ReviewPlacement(
                minecraft.level.dimension(),
                anchor,
                minecraft.player.getYRot() + 90.0F,
                asset
        );

        minecraft.player.sendSystemMessage(
                Component.literal(
                        "Campfire museum review: placed verified Smithsonian Triceratops 100k at 1 source metre = 1 block."
                )
            );
        return Command.SINGLE_SUCCESS;
    }

    private static int clear() {
        Minecraft minecraft = Minecraft.getInstance();
        activePlacement = null;
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                    Component.literal("Campfire museum review exhibit cleared.")
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void extractRenderState(ExtractLevelRenderStateEvent event) {
        ReviewPlacement placement = activePlacement;
        if (placement == null || !placement.dimension().equals(event.getLevel().dimension())) {
            return;
        }

        AABB bounds = new AABB(
                placement.anchor().x - TRI_CULL_RADIUS,
                placement.anchor().y,
                placement.anchor().z - TRI_CULL_RADIUS,
                placement.anchor().x + TRI_CULL_RADIUS,
                placement.anchor().y + TRI_HEIGHT + 0.25,
                placement.anchor().z + TRI_CULL_RADIUS
        );
        if (!event.getFrustum().isVisible(bounds)) {
            return;
        }

        event.getRenderState().setRenderData(RENDER_DATA, placement);
    }

    private static void submitGeometry(SubmitCustomGeometryEvent event) {
        ReviewPlacement placement = event.getLevelRenderState().getRenderData(RENDER_DATA);
        if (placement == null) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(
                    placement.anchor().x - camera.x,
                    placement.anchor().y - camera.y,
                    placement.anchor().z - camera.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(placement.yawDegrees()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(-TRI_CENTER_X, -TRI_CENTER_Y, -TRI_MIN_Z);
            MuseumStaticMeshRenderer.submit(event.getSubmitNodeCollector(), poseStack, placement.asset());
        } finally {
            poseStack.popPose();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, path);
    }

    private record ReviewPlacement(
            ResourceKey<Level> dimension,
            Vec3 anchor,
            float yawDegrees,
            MuseumStaticMeshAsset asset
    ) {}
}
