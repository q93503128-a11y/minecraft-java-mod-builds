package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.GravemarchColossusEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class GravemarchRenderer extends GeoEntityRenderer<GravemarchColossusEntity, GravemarchRenderer.GravemarchRenderState> {
    public GravemarchRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.GRAVEMARCH_COLOSSUS.get());
        withScale(50.0F);
    }

    @Override
    public GravemarchRenderState createRenderState(GravemarchColossusEntity animatable, Void relatedObject) {
        return new GravemarchRenderState();
    }

    @Override
    public void addRenderData(GravemarchColossusEntity animatable, Void relatedObject,
                              GravemarchRenderState renderState, float partialTick) {
        renderState.brokenParts = animatable.brokenPartsMask();
        renderState.chestExposed = animatable.chestExposed();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GravemarchRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        GravemarchRenderState state = renderPassInfo.renderState();
        int broken = state.brokenParts;
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_LEFT_ANKLE, "left_ankle_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_RIGHT_ANKLE, "right_ankle_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_LEFT_KNEE, "left_knee_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_RIGHT_KNEE, "right_knee_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_LEFT_ELBOW, "left_elbow_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_RIGHT_ELBOW, "right_elbow_armor");
        hideIfBroken(snapshots, broken, GravemarchColossusEntity.PART_SKULL_ARMOR, "skull_armor");

        if (state.chestExposed) {
            snapshots.ifPresent("chest_armor", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
        } else {
            snapshots.ifPresent("heart_core", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
        }
    }

    private static void hideIfBroken(BoneSnapshots snapshots, int broken, int mask, String bone) {
        if ((broken & mask) == 0) return;
        snapshots.ifPresent(bone, snapshot -> {
            snapshot.skipRender(true);
            snapshot.skipChildrenRender(true);
        });
    }

    @Override
    protected AABB getBoundingBoxForCulling(GravemarchColossusEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class GravemarchRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private boolean chestExposed;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
