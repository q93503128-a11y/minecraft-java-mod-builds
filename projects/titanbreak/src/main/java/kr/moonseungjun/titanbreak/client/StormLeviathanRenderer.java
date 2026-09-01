package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.StormLeviathanEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class StormLeviathanRenderer
        extends GeoEntityRenderer<StormLeviathanEntity, StormLeviathanRenderer.StormLeviathanRenderState> {
    public StormLeviathanRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.STORM_LEVIATHAN.get());
        withScale(32.0F);
    }

    @Override
    public StormLeviathanRenderState createRenderState(StormLeviathanEntity animatable, Void relatedObject) {
        return new StormLeviathanRenderState();
    }

    @Override
    public void addRenderData(StormLeviathanEntity animatable, Void relatedObject,
                              StormLeviathanRenderState state, float partialTick) {
        state.brokenParts = animatable.brokenPartsMask();
        state.phase = animatable.phase();
        state.organExposed = animatable.stormOrganExposed();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<StormLeviathanRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        StormLeviathanRenderState state = renderPassInfo.renderState();
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.WING_0, "wing_0");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.WING_1, "wing_1");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.WING_2, "wing_2");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.WING_3, "wing_3");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_0, "electric_sac_0");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_1, "electric_sac_1");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_2, "electric_sac_2");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_3, "electric_sac_3");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_4, "electric_sac_4");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.ELECTRIC_SAC_5, "electric_sac_5");
        hideIfBroken(snapshots, state.brokenParts, StormLeviathanEntity.HEAD_SENSOR, "head_sensor");
        if (!state.organExposed) hideBone(snapshots, "storm_organ");
        else hideBone(snapshots, "storm_organ_shell");
        if (state.phase < 2) hideBone(snapshots, "storm_ring");
        if (state.phase < 3) hideBone(snapshots, "low_field");
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
    protected AABB getBoundingBoxForCulling(StormLeviathanEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class StormLeviathanRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private boolean organExposed;
        @Override public Map<DataTicket<?>, Object> getDataMap() { return geckoData; }
    }
}
