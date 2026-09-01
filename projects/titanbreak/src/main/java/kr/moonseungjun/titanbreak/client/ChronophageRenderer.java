package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.ChronophageEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class ChronophageRenderer extends GeoEntityRenderer<ChronophageEntity, ChronophageRenderer.ChronophageRenderState> {
    public ChronophageRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.CHRONOPHAGE.get());
        withScale(30.0F);
    }

    @Override
    public ChronophageRenderState createRenderState(ChronophageEntity animatable, Void relatedObject) {
        return new ChronophageRenderState();
    }

    @Override
    public void addRenderData(ChronophageEntity animatable, Void relatedObject,
                              ChronophageRenderState state, float partialTick) {
        state.brokenParts = animatable.brokenPartsMask();
        state.phase = animatable.phase();
        state.fieldOverride = animatable.fieldOverrideActive();
        state.coreExposed = animatable.centralRingExposed();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<ChronophageRenderState> renderPassInfo, BoneSnapshots snapshots) {
        ChronophageRenderState state = renderPassInfo.renderState();
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.TIME_ORGAN_0, "time_organ_0");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.TIME_ORGAN_1, "time_organ_1");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.TIME_ORGAN_2, "time_organ_2");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.PHASE_JOINT_0, "phase_joint_0");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.PHASE_JOINT_1, "phase_joint_1");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.PHASE_JOINT_2, "phase_joint_2");
        hideIfBroken(snapshots, state.brokenParts, ChronophageEntity.PHASE_JOINT_3, "phase_joint_3");
        if (!state.coreExposed) hideBone(snapshots, "central_ring");
        else hideBone(snapshots, "ring_shell");
        if (state.phase < 2) hideBone(snapshots, "temporal_field");
        if (!state.fieldOverride) hideBone(snapshots, "override_ring");
    }

    private static void hideIfBroken(BoneSnapshots snapshots, int broken, int mask, String bone) {
        if ((broken & mask) != 0) hideBone(snapshots, bone);
    }

    private static void hideBone(BoneSnapshots snapshots, String bone) {
        snapshots.ifPresent(bone, snapshot -> {
            snapshot.skipRender(true);
            snapshot.skipChildrenRender(true);
        });
    }

    @Override
    protected AABB getBoundingBoxForCulling(ChronophageEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class ChronophageRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private boolean fieldOverride;
        private boolean coreExposed;
        @Override public Map<DataTicket<?>, Object> getDataMap() { return geckoData; }
    }
}
