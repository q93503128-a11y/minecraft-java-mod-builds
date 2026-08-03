package kr.moonseungjun.villageguardians;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class VillageSkillEffectRenderState extends EntityRenderState {
    public String kind = "";
    public int ownerEntityId = -1;
    public int duration = 20;
    public float age;
    public Vec3 direction = new Vec3(0.0, 0.0, 1.0);
    public int seed;
    public String extra = "";
}
