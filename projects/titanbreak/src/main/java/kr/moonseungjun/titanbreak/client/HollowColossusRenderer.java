package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GiantMobRenderer;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

public final class HollowColossusRenderer extends GiantMobRenderer {
    public HollowColossusRenderer(EntityRendererProvider.Context context) {
        super(context, 6.0F);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Giant entity) {
        if (!(entity instanceof HollowColossusEntity colossus)) {
            return super.getBoundingBoxForCulling(entity);
        }

        AABB bounds = entity.getBoundingBox();
        for (PartEntity<?> part : colossus.getParts()) {
            bounds = bounds.minmax(part.getBoundingBox());
        }
        return bounds;
    }
}
