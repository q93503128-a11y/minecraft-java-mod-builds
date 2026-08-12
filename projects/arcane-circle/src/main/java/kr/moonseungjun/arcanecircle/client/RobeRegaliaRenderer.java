package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Fine regalia pass for robe identity. The logical equipment remains one chest item; this class only
 * adds silhouette accents owned by Arcane Circle.
 */
final class RobeRegaliaRenderer {
    private RobeRegaliaRenderer() {}

    static void render(PoseStack stack, RenderPlayerEvent.Post<?> event, int style,
                       float moveX, float moveZ, float flutter) {
        if (style <= 0) return;
        float sway = clamp(-moveZ * 1.2F + flutter * 1.4F, -.22F, .22F);
        float side = clamp(-moveX * .9F, -.14F, .14F);
        int trim = trim(style), glow = glow(style), dark = dark(trim);
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
            switch (style) {
                case 1 -> apprentice(out, pose, trim, dark);
                case 2 -> sage(out, pose, trim, glow, side);
                case 3 -> cinder(out, pose, trim, glow, sway);
                case 4 -> glacier(out, pose, trim, glow, sway);
                case 5 -> tempest(out, pose, trim, glow, sway, side);
                case 6 -> archmage(out, pose, trim, glow, sway);
                case 7 -> rift(out, pose, trim, glow, sway, side);
                default -> {}
            }
        });
    }

    private static void apprentice(VertexConsumer o, PoseStack.Pose p, int trim, int dark) {
        collar(o,p,-.34F,1.49F,-.31F,.34F,1.59F,-.27F,trim);
        band(o,p,-.30F,.30F,.88F,-.318F,.032F,dark);
        clasp(o,p,0F,1.34F,-.34F,.06F,trim);
    }

    private static void sage(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float side) {
        collar(o,p,-.42F,1.48F,-.315F,.42F,1.64F,-.28F,trim);
        band(o,p,-.31F,.31F,1.06F,-.352F,.035F,glow);
        clasp(o,p,side*.18F,1.37F,-.365F,.075F,glow);
        quad(o,p,-.30F,1.04F,-.36F,-.22F,1.04F,-.36F,-.20F,.55F,-.43F+side,-.29F,.55F,-.43F+side,trim);
        quad(o,p,.22F,1.04F,-.36F,.30F,1.04F,-.36F,.29F,.55F,-.43F-side,.20F,.55F,-.43F-side,dark(trim));
    }

    private static void cinder(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float sway) {
        shoulder(o,p,-.56F,1.47F,-.12F,-.39F,1.60F,.16F,trim);
        shoulder(o,p,.39F,1.47F,-.12F,.56F,1.60F,.16F,dark(trim));
        clasp(o,p,0F,1.34F,-.37F,.075F,glow);
        spike(o,p,-.26F,.59F,.42F,-.38F,.10F,.68F+sway,trim);
        spike(o,p,.26F,.59F,.42F,.38F,.10F,.68F+sway,glow);
    }

    private static void glacier(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float sway) {
        fin(o,p,-.44F,1.42F,.20F,-.62F,1.70F,.28F,trim);
        fin(o,p,.44F,1.42F,.20F,.62F,1.70F,.28F,trim);
        clasp(o,p,0F,1.38F,-.38F,.085F,glow);
        crystal(o,p,-.34F,.53F,.43F,-.47F,.08F,.63F+sway,trim);
        crystal(o,p,.34F,.53F,.43F,.47F,.08F,.63F+sway,glow);
    }

    private static void tempest(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float sway, float side) {
        collar(o,p,-.36F,1.47F,-.31F,.36F,1.62F,-.25F,trim);
        clasp(o,p,side*.15F,1.35F,-.36F,.065F,glow);
        streamer(o,p,-.31F,1.20F,.34F,-.52F,.56F,.57F+sway+side,trim);
        streamer(o,p,.31F,1.20F,.34F,.52F,.56F,.57F+sway-side,glow);
    }

    private static void archmage(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float sway) {
        shoulder(o,p,-.64F,1.48F,-.10F,-.36F,1.70F,.19F,trim);
        shoulder(o,p,.36F,1.48F,-.10F,.64F,1.70F,.19F,trim);
        collar(o,p,-.42F,1.49F,-.32F,.42F,1.70F,-.25F,glow);
        clasp(o,p,0F,1.35F,-.39F,.10F,glow);
        band(o,p,-.38F,.38F,1.02F,-.36F,.042F,trim);
        quad(o,p,-.075F,1.00F,.43F,.075F,1.00F,.43F,.14F,.10F,.86F+sway,-.14F,.10F,.86F+sway,glow);
    }

    private static void rift(VertexConsumer o, PoseStack.Pose p, int trim, int glow, float sway, float side) {
        shoulder(o,p,-.62F,1.46F,-.08F,-.33F,1.73F,.16F,trim);
        fin(o,p,.39F,1.44F,.18F,.67F,1.60F,.30F,dark(trim));
        clasp(o,p,-.08F+side*.12F,1.34F,-.38F,.085F,glow);
        spike(o,p,-.36F,.72F,.45F,-.66F,.08F,.88F+sway+side,trim);
        spike(o,p,.24F,.63F,.43F,.48F,.22F,.62F-sway-side,glow);
    }

    private static void collar(VertexConsumer o, PoseStack.Pose p,
                               float x0,float y0,float z0,float x1,float y1,float z1,int c) {
        quad(o,p,x0,y0,z0,x1,y0,z0,x1*.78F,y1,z1,x0*.78F,y1,z1,c);
    }
    private static void shoulder(VertexConsumer o, PoseStack.Pose p,
                                 float x0,float y0,float z0,float x1,float y1,float z1,int c) {
        quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z1,x0,y1,z1,c);
    }
    private static void fin(VertexConsumer o, PoseStack.Pose p,
                            float ax,float ay,float az,float bx,float by,float bz,int c) {
        float mx=(ax+bx)*.5F;
        quad(o,p,ax-.07F,ay,az,ax+.07F,ay,az,bx,by,bz,mx,by+.12F,bz+.02F,c);
    }
    private static void crystal(VertexConsumer o, PoseStack.Pose p,
                                float ax,float ay,float az,float bx,float by,float bz,int c) {
        float mx=(ax+bx)*.5F;
        quad(o,p,ax-.055F,ay,az,ax+.055F,ay,az,bx+.045F,by,bz,mx,by-.10F,bz+.03F,c);
    }
    private static void streamer(VertexConsumer o, PoseStack.Pose p,
                                 float ax,float ay,float az,float bx,float by,float bz,int c) {
        quad(o,p,ax-.045F,ay,az,ax+.045F,ay,az,bx+.035F,by,bz,bx-.035F,by,bz,c);
    }
    private static void spike(VertexConsumer o, PoseStack.Pose p,
                              float ax,float ay,float az,float bx,float by,float bz,int c) {
        float mx=(ax+bx)*.5F;
        quad(o,p,ax-.055F,ay,az,ax+.055F,ay,az,bx,by,bz,mx,by+.10F,bz,c);
    }
    private static void clasp(VertexConsumer o, PoseStack.Pose p, float x,float y,float z,float s,int c) {
        quad(o,p,x,y-s,z,x+s,y,z,x,y+s,z,x-s,y,z,c);
    }
    private static void band(VertexConsumer o, PoseStack.Pose p,
                             float x0,float x1,float y,float z,float h,int c) {
        quad(o,p,x0,y,z,x1,y,z,x1,y+h,z,x0,y+h,z,c);
    }
    private static void quad(VertexConsumer o, PoseStack.Pose p,
                             float ax,float ay,float az,float bx,float by,float bz,
                             float cx,float cy,float cz,float dx,float dy,float dz,int c) {
        o.addVertex(p,ax,ay,az).setColor(c);
        o.addVertex(p,bx,by,bz).setColor(c);
        o.addVertex(p,cx,cy,cz).setColor(c);
        o.addVertex(p,dx,dy,dz).setColor(c);
    }
    private static int trim(int s){return switch(s){case 3->0xFFFF7A42;case 4->0xFFA9EDFF;case 5->0xFF7FE9FF;case 6->0xFFFFD56A;case 7->0xFFD492FF;default->0xFFB9C7FF;};}
    private static int glow(int s){return switch(s){case 3->0xFFFFB06A;case 4->0xFFD7FAFF;case 5->0xFFB9FFFF;case 6->0xFFFFE89B;case 7->0xFFE6B9FF;default->0xFFE3D8FF;};}
    private static int dark(int c){int a=c&0xFF000000,r=(int)(((c>>16)&255)*.55),g=(int)(((c>>8)&255)*.55),b=(int)((c&255)*.55);return a|(r<<16)|(g<<8)|b;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
