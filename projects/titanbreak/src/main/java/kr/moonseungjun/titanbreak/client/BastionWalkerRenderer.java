package kr.moonseungjun.titanbreak.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kr.moonseungjun.titanbreak.entity.BastionWalkerEntity;
import kr.moonseungjun.titanbreak.registry.ModBossEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class BastionWalkerRenderer extends GeoEntityRenderer<BastionWalkerEntity, BastionWalkerRenderer.BastionRenderState> {
    public BastionWalkerRenderer(EntityRendererProvider.Context context) {
        super(context, ModBossEntities.BASTION_WALKER.get());
        withScale(60.0F);
    }

    @Override
    public BastionRenderState createRenderState(BastionWalkerEntity animatable, Void relatedObject) {
        return new BastionRenderState();
    }

    @Override
    public void addRenderData(BastionWalkerEntity animatable, Void relatedObject,
                              BastionRenderState renderState, float partialTick) {
        renderState.brokenParts = animatable.brokenPartsMask();
        renderState.coreExposed = animatable.coreExposed();
        renderState.armorClosed = animatable.armorClosed();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<BastionRenderState> renderPassInfo,
                                          BoneSnapshots snapshots) {
        BastionRenderState state = renderPassInfo.renderState();
        int broken = state.brokenParts;

        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_LEG_NW, "leg_nw_armor");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_LEG_NE, "leg_ne_armor");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_LEG_SW, "leg_sw_armor");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_LEG_SE, "leg_se_armor");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_NORTH_LOWER, "plate_north_lower");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_NORTH_UPPER, "plate_north_upper");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_EAST_LOWER, "plate_east_lower");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_EAST_UPPER, "plate_east_upper");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_SOUTH_LOWER, "plate_south_lower");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_SOUTH_UPPER, "plate_south_upper");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_WEST_LOWER, "plate_west_lower");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_PLATE_WEST_UPPER, "plate_west_upper");
        hideIfBroken(snapshots, broken, BastionWalkerEntity.PART_UPPER_NODE, "upper_node");

        if (state.coreExposed) {
            snapshots.ifPresent("core_shutters", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
        } else {
            snapshots.ifPresent("power_core", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
        }

        if (!state.armorClosed) {
            snapshots.ifPresent("closure_shields", snapshot -> {
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
    protected AABB getBoundingBoxForCulling(BastionWalkerEntity entity) {
        return entity.getBoundingBoxForCulling();
    }

    public static final class BastionRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckoData = new HashMap<>();
        private int brokenParts;
        private boolean coreExposed;
        private boolean armorClosed;

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return geckoData;
        }
    }
}
