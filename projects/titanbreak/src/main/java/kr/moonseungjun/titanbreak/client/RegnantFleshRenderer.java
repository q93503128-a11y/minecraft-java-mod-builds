package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.RegnantFleshEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class RegnantFleshRenderer extends GeoEntityRenderer<RegnantFleshEntity, RegnantFleshRenderer.RegnantRenderState> {
    public RegnantFleshRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.REGNANT_FLESH.get());
        withScale(30.0F);
    }

    @Override
    public RegnantRenderState createRenderState(RegnantFleshEntity animatable, Void relatedObject) {
        return new RegnantRenderState();
    }

    @Override
    public void addRenderData(RegnantFleshEntity animatable, Void relatedObject,
                              RegnantRenderState renderState, float partialTick) {
        renderState.brokenParts = animatable.brokenPartsMask();
        renderState.phase = animatable.phase();
        renderState.activeCore = animatable.activeCoreIndex();
        renderState.brainExposed = animatable.brainExposed();
        renderState.tissueWall = animatable.tissueWallActive();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<RegnantRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        RegnantRenderState state = renderPassInfo.renderState();
        int broken = state.brokenParts;

        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_CIRC_0, "circ_0");
        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_CIRC_1, "circ_1");
        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_LIMB_LEFT_ARM, "left_arm");
        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_LIMB_RIGHT_ARM, "right_arm");
        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_LIMB_LEFT_LEG, "left_leg");
        hideIfBroken(snapshots, broken, RegnantFleshEntity.PART_LIMB_RIGHT_LEG, "right_leg");

        int[] masks = {
                RegnantFleshEntity.PART_REGEN_0, RegnantFleshEntity.PART_REGEN_1,
                RegnantFleshEntity.PART_REGEN_2, RegnantFleshEntity.PART_REGEN_3
        };
        for (int i = 0; i < 4; i++) {
            hideIfBroken(snapshots, broken, masks[i], "regen_core_" + i);
            boolean glowVisible = state.phase == 3 || (state.phase == 2 && state.activeCore == i);
            if (!glowVisible || (broken & masks[i]) != 0) hideBone(snapshots, "core_glow_" + i);
        }

        if (!state.brainExposed) hideBone(snapshots, "brain");
        else hideBone(snapshots, "brain_shell");
        if (!state.tissueWall) hideBone(snapshots, "tissue_wall");
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
    protected AABB getBoundingBoxForCulling(RegnantFleshEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class RegnantRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private int phase;
        private int activeCore;
        private boolean brainExposed;
        private boolean tissueWall;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
