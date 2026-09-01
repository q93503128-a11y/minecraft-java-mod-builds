package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.AshTitanEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class AshTitanRenderer
        extends GeoEntityRenderer<AshTitanEntity, AshTitanRenderer.AshTitanRenderState> {
    public AshTitanRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.ASH_TITAN.get());
        withScale(30.0F);
    }

    @Override
    public AshTitanRenderState createRenderState(AshTitanEntity animatable, Void relatedObject) {
        return new AshTitanRenderState();
    }

    @Override
    public void addRenderData(AshTitanEntity animatable, Void relatedObject,
                              AshTitanRenderState state, float partialTick) {
        state.brokenParts = animatable.brokenPartsMask();
        state.phase = animatable.phase();
        state.heartExposed = animatable.radiantHeartExposed();
        state.heatLevel = animatable.heatLevel();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<AshTitanRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        AshTitanRenderState state = renderPassInfo.renderState();
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_0, "cooling_plate_0");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_1, "cooling_plate_1");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_2, "cooling_plate_2");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_3, "cooling_plate_3");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_4, "cooling_plate_4");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.COOLING_PLATE_5, "cooling_plate_5");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.RADIATION_ARM_LEFT, "radiation_arm_left");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.RADIATION_ARM_RIGHT, "radiation_arm_right");
        hideIfBroken(snapshots, state.brokenParts, AshTitanEntity.HEAD_SENSOR, "head_sensor");
        if (!state.heartExposed) hideBone(snapshots, "radiant_heart");
        else hideBone(snapshots, "heart_shell");
        if (state.phase < 2) hideBone(snapshots, "overheat_ring");
        if (state.phase < 3) hideBone(snapshots, "radiant_crown");
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
    protected AABB getBoundingBoxForCulling(AshTitanEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class AshTitanRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private int heatLevel;
        private boolean heartExposed;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
