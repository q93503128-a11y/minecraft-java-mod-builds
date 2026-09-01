package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.WorldbreakerEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class WorldbreakerRenderer
        extends GeoEntityRenderer<WorldbreakerEntity, WorldbreakerRenderer.WorldbreakerRenderState> {
    public WorldbreakerRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.WORLDBREAKER.get());
        withScale(48.0F);
    }

    @Override
    public WorldbreakerRenderState createRenderState(WorldbreakerEntity animatable, Void relatedObject) {
        return new WorldbreakerRenderState();
    }

    @Override
    public void addRenderData(WorldbreakerEntity animatable, Void relatedObject,
                              WorldbreakerRenderState state, float partialTick) {
        state.brokenParts = animatable.brokenPartsMask();
        state.phase = animatable.phase();
        state.coreExposed = animatable.centralCoreExposed();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<WorldbreakerRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        WorldbreakerRenderState state = renderPassInfo.renderState();
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.LEG_AXIS_0, "leg_axis_0");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.LEG_AXIS_1, "leg_axis_1");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.LEG_AXIS_2, "leg_axis_2");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.LEG_AXIS_3, "leg_axis_3");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.ARM_LEFT, "arm_left_weapon");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.ARM_RIGHT, "arm_right_weapon");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_0, "outer_core_0");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_1, "outer_core_1");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_2, "outer_core_2");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_3, "outer_core_3");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_4, "outer_core_4");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.OUTER_CORE_5, "outer_core_5");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.TEMPORAL_AUX, "temporal_aux");
        hideIfBroken(snapshots, state.brokenParts, WorldbreakerEntity.ENERGY_AUX, "energy_aux");
        if (!state.coreExposed) hideBone(snapshots, "central_core");
        else hideBone(snapshots, "central_core_shell");
        if (state.phase < 4) hideBone(snapshots, "frenzy_crown");
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
    protected AABB getBoundingBoxForCulling(WorldbreakerEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class WorldbreakerRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private boolean coreExposed;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
