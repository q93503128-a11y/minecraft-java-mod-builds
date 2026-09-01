package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.NullSeraphEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class NullSeraphRenderer
        extends GeoEntityRenderer<NullSeraphEntity, NullSeraphRenderer.NullSeraphRenderState> {
    public NullSeraphRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.NULL_SERAPH.get());
        withScale(16.0F);
    }

    @Override
    public NullSeraphRenderState createRenderState(NullSeraphEntity animatable, Void relatedObject) {
        return new NullSeraphRenderState();
    }

    @Override
    public void addRenderData(NullSeraphEntity animatable, Void relatedObject,
                              NullSeraphRenderState state, float partialTick) {
        state.brokenParts = animatable.brokenPartsMask();
        state.phase = animatable.phase();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<NullSeraphRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        NullSeraphRenderState state = renderPassInfo.renderState();
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.SUPPRESSION_WING_0, "suppression_wing_0");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.SUPPRESSION_WING_1, "suppression_wing_1");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.SUPPRESSION_WING_2, "suppression_wing_2");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.SUPPRESSION_WING_3, "suppression_wing_3");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.NULL_CORE_LEFT, "null_core_left");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.NULL_CORE_RIGHT, "null_core_right");
        hideIfBroken(snapshots, state.brokenParts, NullSeraphEntity.HEAD_RESONATOR, "head_resonator");
        if (state.phase == 3) hideBone(snapshots, "suppression_halo");
        else hideBone(snapshots, "lance_crown");
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
    protected AABB getBoundingBoxForCulling(NullSeraphEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class NullSeraphRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
