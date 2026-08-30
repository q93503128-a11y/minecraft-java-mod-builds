package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

public final class PursuerRenderer extends GeoEntityRenderer<PursuerEntity, LivingEntityRenderState> {
    private static final DataTicket<Integer> BROKEN_PARTS =
            DataTicket.create("titanbreak:pursuer_broken_parts", Integer.class);

    public PursuerRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.THE_PURSUER.get());
        withScale(18.0F);
    }

    @Override
    public void addRenderData(PursuerEntity animatable, Void relatedObject,
                              LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(BROKEN_PARTS, animatable.brokenPartsMask());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        int broken = renderPassInfo.getOrDefaultGeckolibData(BROKEN_PARTS, 0);
        hideIfBroken(snapshots, broken, PursuerEntity.PART_LEFT_EYE, "left_eye");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_RIGHT_EYE, "right_eye");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_LEFT_FORE_UPPER, "left_fore_upper");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_LEFT_FORE_LOWER, "left_fore_lower");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_RIGHT_FORE_UPPER, "right_fore_upper");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_RIGHT_FORE_LOWER, "right_fore_lower");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_SPINE_REACTION, "spine_reactor");
        hideIfBroken(snapshots, broken, PursuerEntity.PART_CHEST_CORE, "chest_core");
    }

    private static void hideIfBroken(BoneSnapshots snapshots, int broken, int mask, String bone) {
        if ((broken & mask) == 0) return;
        snapshots.ifPresent(bone, snapshot -> {
            snapshot.skipRender(true);
            snapshot.skipChildrenRender(true);
        });
    }

    @Override
    protected AABB getBoundingBoxForCulling(PursuerEntity entity) {
        return entity.getBoundingBoxForCulling();
    }
}
