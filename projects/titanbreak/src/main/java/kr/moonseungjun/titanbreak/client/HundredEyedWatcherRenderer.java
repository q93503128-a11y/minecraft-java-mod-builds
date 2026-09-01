package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.HundredEyedWatcherEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class HundredEyedWatcherRenderer extends GeoEntityRenderer<HundredEyedWatcherEntity, HundredEyedWatcherRenderer.WatcherRenderState> {
    public HundredEyedWatcherRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.HUNDRED_EYED_WATCHER.get());
        withScale(26.0F);
    }

    @Override
    public WatcherRenderState createRenderState(HundredEyedWatcherEntity animatable, Void relatedObject) {
        return new WatcherRenderState();
    }

    @Override
    public void addRenderData(HundredEyedWatcherEntity animatable, Void relatedObject,
                              WatcherRenderState renderState, float partialTick) {
        renderState.brokenParts = animatable.brokenPartsMask();
        renderState.phase = animatable.phase();
        renderState.decoyEye = animatable.decoyEyeIndex();
        renderState.coreExposed = animatable.centralCoreExposed();
        renderState.predictionField = animatable.predictionFieldActive();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<WatcherRenderState> renderPassInfo, BoneSnapshots snapshots) {
        WatcherRenderState state = renderPassInfo.renderState();
        for (int i = 0; i < HundredEyedWatcherEntity.EYE_COUNT; i++) {
            if ((state.brokenParts & (1 << i)) != 0) hideBone(snapshots, "eye_" + i);
        }
        hideIfBroken(snapshots, state.brokenParts, HundredEyedWatcherEntity.BRAIN_0, "brain_0");
        hideIfBroken(snapshots, state.brokenParts, HundredEyedWatcherEntity.BRAIN_1, "brain_1");
        hideIfBroken(snapshots, state.brokenParts, HundredEyedWatcherEntity.BRAIN_2, "brain_2");
        if (!state.coreExposed) hideBone(snapshots, "central_core");
        else hideBone(snapshots, "core_shell");
        if (!state.predictionField) hideBone(snapshots, "prediction_field");

        for (int i = 0; i < 3; i++) {
            if (state.phase < 2 || state.decoyEye % 3 != i) hideBone(snapshots, "false_core_" + i);
        }
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
    protected AABB getBoundingBoxForCulling(HundredEyedWatcherEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class WatcherRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private int decoyEye;
        private boolean coreExposed;
        private boolean predictionField;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() { return geckoData; }
    }
}
