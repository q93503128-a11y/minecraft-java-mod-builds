package dev.moonseungjun.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class YinYangRenderer {
    private static final int INK = 0xFF11131D, PEARL = 0xFFFFF3D6, GOLD = 0xFFD3C18D;
    private YinYangRenderer() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            SubmitNodeCollector collector = context.submitNodeCollector();
            Vec3 camera = context.levelState().cameraRenderState.pos;
            for (YinYangClient.CastState cast : YinYangClient.casts()) {
                Player caster = client.level.getPlayerByUUID(cast.casterId);
                if (caster == null || !caster.isAlive()) continue;
                float t = client.level.getGameTime() - cast.startedAt + 1.0F;
                if (t < 58) { hands(collector, caster, camera, t); forming(collector, caster, camera, t); }
                else if (cast.launched && !cast.impact) projectile(collector, cast, t, camera);
                else if (cast.impact) impact(collector, cast, t, camera);
            }
        });
    }

    private static void hands(SubmitNodeCollector c, Player player, Vec3 camera, float t) {
        float gather = ease((t - 19) / 18.0F), fade = 1 - ease((t - 31) / 7.0F);
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.72, 0).add(player.getLookAngle().scale(0.36)).subtract(camera);
        Vec3 left = hand(player, "yin_yang_left_hand", camera, -1).lerp(center, gather);
        Vec3 right = hand(player, "yin_yang_right_hand", camera, 1).lerp(center, gather);
        float s = (0.78F + 0.06F * (float) Math.sin(t * 0.42F)) * fade;
        PoseStack yin = at(left); yin.scale(s, s, s);
        c.submitCustomGeometry(yin, RenderTypes.lightning(), (p, out) -> {
            sphere(out, p, 0.19F, INK); orbit(out, p, 0.26F, t * -0.095F, 0xFF777B8D); orbit(out, p, 0.31F, t * 0.065F, 0xFF292D3C);
        });
        PoseStack yang = at(right); yang.scale(s, s, s);
        c.submitCustomGeometry(yang, RenderTypes.lightning(), (p, out) -> {
            sphere(out, p, 0.19F, PEARL); sphere(out, p, 0.073F, 0xFFFFFDF5); orbit(out, p, 0.26F, t * 0.095F, 0xFFE9D8A9); orbit(out, p, 0.31F, t * -0.065F, 0xFF9E895B);
        });
    }

    private static Vec3 hand(Player player, String bone, Vec3 camera, int side) {
        var controller = YinYangClient.controller(player);
        if (controller != null) {
            PoseStack stack = controller.getBoneWorldPositionPoseStack(bone, 1.0F, camera);
            if (stack != null) return new Vec3(stack.last().pose().m30(), stack.last().pose().m31(), stack.last().pose().m32());
        }
        return player.position().subtract(camera).add(side * 0.28, player.getBbHeight() * 0.78, 0).add(player.getLookAngle().scale(0.24));
    }

    private static void forming(SubmitNodeCollector c, Player player, Vec3 camera, float t) {
        if (t < 27) return;
        float scale = 0.12F + 0.72F * ease((t - 27) / 12.0F);
        Vec3 look = player.getLookAngle(), origin = player.position().add(0, player.getBbHeight() * 0.70, 0).add(look.scale(0.43)).subtract(camera);
        PoseStack stack = oriented(origin, look, (t - 27) * 3.0F); stack.scale(scale, scale, scale);
        c.submitCustomGeometry(stack, RenderTypes.lightning(), (p, out) -> taiji(out, p, t * 0.11F));
    }

    private static void projectile(SubmitNodeCollector c, YinYangClient.CastState cast, float t, Vec3 camera) {
        float age = Math.max(0, t - 58);
        Vec3 location = cast.projectileOrigin.add(cast.projectileDirection.scale(age * 0.82F)).subtract(camera);
        PoseStack stack = oriented(location, cast.projectileDirection, t * 12.5F);
        c.submitCustomGeometry(stack, RenderTypes.lightning(), (p, out) -> {
            taiji(out, p, t * 0.14F);
            for (int i = 1; i <= 5; i++) orbit(out, p, 0.48F + i * 0.025F, t * 0.17F + i * 0.8F, i % 2 == 0 ? 0xFF9E9BA7 : 0xFFE4D4AE);
        });
    }

    private static void impact(SubmitNodeCollector c, YinYangClient.CastState cast, float t, Vec3 camera) {
        float age = t - cast.impactAt;
        if (age < 0 || age >= 18) return;
        float spread = ease(age / 16.0F); int alpha = Math.max(0, (int) (255 * (1 - age / 18.0F)));
        PoseStack stack = oriented(cast.impactPosition.subtract(camera), cast.projectileDirection, 0); stack.scale(1 + spread * 1.7F, 1 + spread * 1.7F, 1);
        c.submitCustomGeometry(stack, RenderTypes.lightning(), (p, out) -> {
            ring(out, p, 0.78F, 0.84F, alpha << 24 | 0x00EAD9AA); ring(out, p, 0.52F, 0.56F, alpha << 24 | 0x00F6F0E3);
            for (int i = 0; i < 8; i++) { float a = (float) (Math.PI * 2 * i / 8 + 0.18); sphereAt(out, p, (float) Math.cos(a) * 0.64F, (float) Math.sin(a) * 0.64F, 0, 0.07F * (1 - spread), alpha << 24 | 0x00F5EAD0); }
        });
    }

    private static PoseStack at(Vec3 v) { PoseStack p = new PoseStack(); p.translate(v.x, v.y, v.z); return p; }
    private static PoseStack oriented(Vec3 pos, Vec3 dir, float roll) {
        PoseStack p = at(pos); p.rotateDegrees(Axis.YP, (float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        p.rotateDegrees(Axis.XP, (float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, dir.y))))); p.rotateDegrees(Axis.ZP, roll); return p;
    }

    private static void taiji(VertexConsumer out, PoseStack.Pose world, float spin) {
        float r = 0.68F, d = 0.055F; PoseStack turn = new PoseStack(); turn.rotateDegrees(Axis.ZP, spin);
        for (int i = 0; i < 64; i++) {
            float y0 = -r + 2 * r * i / 64.0F, y1 = -r + 2 * r * (i + 1) / 64.0F;
            float e0 = (float) Math.sqrt(Math.max(0, r * r - y0 * y0)), e1 = (float) Math.sqrt(Math.max(0, r * r - y1 * y1));
            float s0 = sCurve(y0, r), s1 = sCurve(y1, r); PoseStack.Pose tr = turn.last();
            quad(out, world, rotate(tr, -e0,y0,d),rotate(tr,s0,y0,d),rotate(tr,s1,y1,d),rotate(tr,-e1,y1,d),PEARL);
            quad(out, world, rotate(tr,s0,y0,d),rotate(tr,e0,y0,d),rotate(tr,e1,y1,d),rotate(tr,s1,y1,d),INK);
            quad(out, world, rotate(tr,-e0,y0,-d),rotate(tr,s0,y0,-d),rotate(tr,s1,y1,-d),rotate(tr,-e1,y1,-d),0xFFB9AE95);
            quad(out, world, rotate(tr,s0,y0,-d),rotate(tr,e0,y0,-d),rotate(tr,e1,y1,-d),rotate(tr,s1,y1,-d),0xFF252735);
        }
        for (int i = 0; i < 96; i++) {
            float a = (float)(Math.PI*2*i/96), b=(float)(Math.PI*2*(i+1)/96); PoseStack.Pose tr=turn.last();
            quad(out,world,rotate(tr,r*(float)Math.cos(a),r*(float)Math.sin(a),-d),rotate(tr,r*(float)Math.cos(b),r*(float)Math.sin(b),-d),
                    rotate(tr,r*(float)Math.cos(b),r*(float)Math.sin(b),d),rotate(tr,r*(float)Math.cos(a),r*(float)Math.sin(a),d),GOLD);
        }
        dot(out,world,turn.last(),0,r*.5F,r*.102F,-d-.012F,INK); dot(out,world,turn.last(),0,-r*.5F,r*.102F,-d-.012F,PEARL);
        ring(out,world,turn.last(),r*.96F,r,GOLD);
    }
    private static float sCurve(float y,float r) { return y>=0?(float)Math.sqrt(Math.max(0,y*(r-y))):-(float)Math.sqrt(Math.max(0,-y*(r+y))); }
    private static Vec3 rotate(PoseStack.Pose p,float x,float y,float z) { var m=p.pose(); return new Vec3(m.m00()*x+m.m10()*y+m.m20()*z,m.m01()*x+m.m11()*y+m.m21()*z,m.m02()*x+m.m12()*y+m.m22()*z); }
    private static void dot(VertexConsumer out,PoseStack.Pose world,PoseStack.Pose turn,float x,float y,float r,float z,int color) {
        for(int i=0;i<32;i++){float a=(float)(Math.PI*2*i/32),b=(float)(Math.PI*2*(i+1)/32);quad(out,world,rotate(turn,x,y,z),rotate(turn,x+r*(float)Math.cos(a),y+r*(float)Math.sin(a),z),rotate(turn,x+r*(float)Math.cos(b),y+r*(float)Math.sin(b),z),rotate(turn,x,y,z-.01F),color);}
    }
    private static void sphere(VertexConsumer out,PoseStack.Pose p,float r,int color) {
        for(int lat=0;lat<10;lat++)for(int lon=0;lon<16;lon++){float t0=(float)(-Math.PI/2+Math.PI*lat/10),t1=(float)(-Math.PI/2+Math.PI*(lat+1)/10),a0=(float)(Math.PI*2*lon/16),a1=(float)(Math.PI*2*(lon+1)/16);quad(out,p,sp(r,t0,a0),sp(r,t1,a0),sp(r,t1,a1),sp(r,t0,a1),color);}
    }
    private static Vec3 sp(float r,float t,float a){float c=(float)Math.cos(t);return new Vec3(r*c*Math.cos(a),r*Math.sin(t),r*c*Math.sin(a));}
    private static void orbit(VertexConsumer out,PoseStack.Pose pose,float r,float phase,int color) {
        PoseStack tr=new PoseStack();tr.rotateDegrees(Axis.XP,57);tr.rotateDegrees(Axis.YP,phase*48);Vec3 last=null;
        for(int i=0;i<=48;i++){float a=(float)(Math.PI*2*i/48+phase);Vec3 v=rotate(tr.last(),r*(float)Math.cos(a),r*(float)Math.sin(a),0);if(last!=null){Vec3 mid=last.add(v).scale(.5),t=v.subtract(last).normalize().scale(.018);quad(out,pose,mid.subtract(t),mid.add(t),v.add(t),v.subtract(t),color);}last=v;}
    }
    private static void ring(VertexConsumer out,PoseStack.Pose p,float inner,float outer,int color) {
        ring(out,p,null,inner,outer,color);
    }
    private static void ring(VertexConsumer out,PoseStack.Pose p,PoseStack.Pose turn,float inner,float outer,int color) {
        for(int i=0;i<64;i++){float a=(float)(Math.PI*2*i/64),b=(float)(Math.PI*2*(i+1)/64);Vec3 v0=new Vec3(inner*Math.cos(a),inner*Math.sin(a),0),v1=new Vec3(outer*Math.cos(a),outer*Math.sin(a),0),v2=new Vec3(outer*Math.cos(b),outer*Math.sin(b),0),v3=new Vec3(inner*Math.cos(b),inner*Math.sin(b),0);quad(out,p,turn==null?v0:rotate(turn,(float)v0.x,(float)v0.y,(float)v0.z),turn==null?v1:rotate(turn,(float)v1.x,(float)v1.y,(float)v1.z),turn==null?v2:rotate(turn,(float)v2.x,(float)v2.y,(float)v2.z),turn==null?v3:rotate(turn,(float)v3.x,(float)v3.y,(float)v3.z),color);}
    }
    private static void sphereAt(VertexConsumer out,PoseStack.Pose p,float x,float y,float z,float r,int color) {
        for(int i=0;i<8;i++){float a=(float)(Math.PI*2*i/8),b=(float)(Math.PI*2*(i+1)/8);quad(out,p,new Vec3(x,y,z+r),new Vec3(x+Math.cos(a)*r,y+Math.sin(a)*r,z),new Vec3(x+Math.cos(b)*r,y+Math.sin(b)*r,z),new Vec3(x,y,z-r),color);}
    }
    private static void quad(VertexConsumer out,PoseStack.Pose p,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){vertex(out,p,a,color);vertex(out,p,b,color);vertex(out,p,c,color);vertex(out,p,a,color);vertex(out,p,c,color);vertex(out,p,d,color);}
    private static void vertex(VertexConsumer out,PoseStack.Pose p,Vec3 v,int color){out.addVertex(p,(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(p,0,0,1);}
    private static float ease(float x){x=Math.max(0,Math.min(1,x));return 1-(1-x)*(1-x)*(1-x);}
}
