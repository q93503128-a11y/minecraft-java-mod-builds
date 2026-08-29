package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GiantMobRenderer;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

public final class PursuerRenderer extends GiantMobRenderer {
    public PursuerRenderer(EntityRendererProvider.Context context) {
        super(context, 18.0F);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Giant entity) {
        if (!(entity instanceof PursuerEntity pursuer)) return super.getBoundingBoxForCulling(entity);
        AABB bounds = entity.getBoundingBox();
        for (PartEntity<?> part : pursuer.getParts()) bounds = bounds.minmax(part.getBoundingBox());
        return bounds;
    }
}
