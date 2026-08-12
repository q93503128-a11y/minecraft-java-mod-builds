package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Phase 4 silhouette layer. It animates the mod-owned sleeves, mantle and casting cloth around the
 * vanilla avatar, so input timing/server authority never depends on client animation state.
 */
final class CastingSilhouetteRenderer {
    static final int SNAP=1, AIM=2, HEAVY=3, GROUND=4, WARD=5, PORTAL=6, RITUAL=7;
    private CastingSilhouetteRenderer(){}

    static void render(PoseStack stack, RenderPlayerEvent.Post<?> event, int robeStyle,
                       int family, float progress, boolean release) {
        if (family <= 0) return;
        float p=clamp(progress,0F,1F), recoil=release?(1F-p):0F;
        int body=styleBody(robeStyle), trim=styleTrim(robeStyle), glow=styleGlow(robeStyle);
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose,out)->{
            switch(family){
                case SNAP -> snap(out,pose,p,recoil,body,trim);
                case AIM -> aim(out,pose,p,recoil,body,trim);
                case HEAVY -> heavy(out,pose,p,recoil,body,trim);
                case GROUND -> ground(out,pose,p,recoil,body,trim);
                case WARD -> ward(out,pose,p,recoil,body,trim);
                case PORTAL -> portal(out,pose,p,recoil,body,trim);
                case RITUAL -> ritual(out,pose,p,recoil,body,trim);
                default -> {}
            }
            castingAccent(out,pose,family,p,recoil,glow);
        });
    }

    static void robeOverlay(PoseStack stack, RenderPlayerEvent.Post<?> event, int style,
                            float moveX,float moveZ,float flutter) {
        if(style<=1)return;
        int body=styleBody(style),dark=dark(body),trim=styleTrim(style);
        float sway=clamp(-moveZ*1.4F+flutter,-.24F,.24F),side=clamp(-moveX*1.0F,-.16F,.16F);
        event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{
            switch(style){
                case 2 -> { // sage: layered scholarly stole
                    panel(out,pose,-.34F,1.42F,-.335F,-.10F,.22F,-.43F+side,body);
                    panel(out,pose,.10F,1.42F,-.335F,.34F,.22F,-.43F-side,dark);
                    band(out,pose,-.36F,.36F,.96F,-.345F,.035F,trim);
                }
                case 3 -> { // cinder: cropped combat coat + split flame tails
                    panel(out,pose,-.45F,1.30F,.34F,-.08F,.40F,.52F+sway,body);
                    panel(out,pose,.08F,1.30F,.34F,.45F,.40F,.52F+sway,dark);
                    spike(out,pose,-.36F,.45F,.38F,-.58F,.08F,.65F+sway,trim);
                    spike(out,pose,.36F,.45F,.38F,.58F,.08F,.65F+sway,trim);
                }
                case 4 -> { // glacier: long faceted crystalline panels
                    crystalPanel(out,pose,-.48F,.82F,.36F,-.62F,.03F,.48F+sway,body,trim);
                    crystalPanel(out,pose,.48F,.82F,.36F,.62F,.03F,.48F+sway,dark,trim);
                    crystalPanel(out,pose,0F,.82F,.39F,side,.02F,.66F+sway,dark,trim);
                }
                case 5 -> { // tempest: light short cloth, separated streamers
                    streamer(out,pose,-.38F,.88F,.35F,-.56F,.24F,.66F+sway,body);
                    streamer(out,pose,-.12F,.88F,.36F,-.18F,.19F,.78F+sway,trim);
                    streamer(out,pose,.12F,.88F,.36F,.18F,.19F,.78F+sway,trim);
                    streamer(out,pose,.38F,.88F,.35F,.56F,.24F,.66F+sway,dark);
                }
                case 6 -> { // archmage: broad ceremonial mantle and central train
                    panel(out,pose,-.55F,1.48F,.34F,-.08F,.05F,.58F+sway,body);
                    panel(out,pose,.08F,1.48F,.34F,.55F,.05F,.58F+sway,dark);
                    panel(out,pose,-.18F,1.08F,.40F,.18F,.01F,.78F+sway,trim);
                    band(out,pose,-.57F,.57F,1.31F,.345F,.045F,trim);
                }
                case 7 -> { // rift: intentionally asymmetric split planes
                    panel(out,pose,-.53F,1.42F,.33F,-.10F,.04F,.72F+sway,body);
                    panel(out,pose,.12F,1.20F,.34F,.48F,.18F,.48F-sway,dark);
                    spike(out,pose,-.46F,.76F,.46F,-.72F,.08F,.84F+sway,trim);
                    spike(out,pose,.38F,.70F,.42F,.58F,.22F,.62F-sway,dark);
                }
                default -> {}
            }
        });
    }

    private static void snap(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.58F,1.32F,-.24F,-.74F,.88F,-.58F-.16F*q,body);
        sleeve(o,p,1,.58F,1.32F,-.24F,.72F,.88F,-.72F-.28F*q+r*.12F,trim);
    }
    private static void aim(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.54F,1.30F,-.20F,-.36F,1.02F,-.76F+r*.16F,body);
        sleeve(o,p,1,.54F,1.30F,-.20F,.32F,1.06F,-.90F-r*.22F,trim);
        quad(o,p,-.10F,1.18F,-.72F,.10F,1.18F,-.72F,.12F,1.04F,-.96F,-.12F,1.04F,-.96F,trim);
    }
    private static void heavy(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        float z=-.52F-.22F*q+r*.18F;
        sleeve(o,p,-1,-.55F,1.31F,-.18F,-.28F,1.02F,z,body);
        sleeve(o,p,1,.55F,1.31F,-.18F,.28F,1.02F,z,body);
        quad(o,p,-.26F,1.04F,z-.05F,.26F,1.04F,z-.05F,.18F,.88F,z-.28F,-.18F,.88F,z-.28F,trim);
    }
    private static void ground(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.55F,1.28F,-.18F,-.74F,.70F,-.42F,body);
        sleeve(o,p,1,.55F,1.28F,-.18F,.74F,.70F,-.42F,body);
        spike(o,p,-.72F,.72F,-.36F,-.92F,.18F,-.42F,trim);spike(o,p,.72F,.72F,-.36F,.92F,.18F,-.42F,trim);
    }
    private static void ward(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.53F,1.31F,-.18F,-.18F,1.02F,-.54F+r*.12F,body);
        sleeve(o,p,1,.53F,1.31F,-.18F,.18F,1.02F,-.54F+r*.12F,body);
        quad(o,p,-.32F,1.12F,-.55F,.32F,1.12F,-.55F,.36F,.78F,-.42F,-.36F,.78F,-.42F,trim);
    }
    private static void portal(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.53F,1.30F,-.18F,-.92F,1.08F,-.38F-r*.10F,body);
        sleeve(o,p,1,.53F,1.30F,-.18F,.92F,1.08F,-.38F+r*.10F,trim);
        spike(o,p,-.90F,1.08F,-.35F,-1.12F,1.44F,-.28F,trim);spike(o,p,.90F,1.08F,-.35F,1.12F,1.44F,-.28F,trim);
    }
    private static void ritual(VertexConsumer o,PoseStack.Pose p,float q,float r,int body,int trim){
        sleeve(o,p,-1,-.52F,1.31F,-.16F,-.82F,1.72F,-.28F-r*.10F,body);
        sleeve(o,p,1,.52F,1.31F,-.16F,.82F,1.72F,-.28F+r*.10F,trim);
        spike(o,p,-.80F,1.70F,-.26F,-.62F,1.94F,-.18F,trim);spike(o,p,.80F,1.70F,-.26F,.62F,1.94F,-.18F,trim);
    }

    private static void castingAccent(VertexConsumer o,PoseStack.Pose p,int family,float q,float recoil,int color){
        float y=family==GROUND?.36F:family==RITUAL?1.88F:1.02F;
        float z=family==PORTAL?-.30F:-.76F;
        float s=.08F+.07F*q;box(o,p,-s,y-s,z-s,s,y+s,z+s,color);
    }
    private static void sleeve(VertexConsumer o,PoseStack.Pose p,int side,float sx,float sy,float sz,float ex,float ey,float ez,int color){
        float w=.13F;Vec3f a=new Vec3f(sx-w,sy,sz-w),b=new Vec3f(sx+w,sy,sz+w),c=new Vec3f(ex+w,ey,ez+w),d=new Vec3f(ex-w,ey,ez-w);
        quad(o,p,a.x,a.y,a.z,b.x,b.y,b.z,c.x,c.y,c.z,d.x,d.y,d.z,color);
        quad(o,p,sx-w,sy,sz+w,sx+w,sy,sz-w,ex+w,ey,ez-w,ex-w,ey,ez+w,dark(color));
    }
    private static void panel(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){
        quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z1,x0,y1,z1,c);
    }
    private static void band(VertexConsumer o,PoseStack.Pose p,float x0,float x1,float y,float z,float h,int c){quad(o,p,x0,y,z,x1,y,z,x1,y+h,z,x0,y+h,z,c);}
    private static void spike(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){
        float mx=(ax+bx)*.5F;quad(o,p,ax-.06F,ay,az,ax+.06F,ay,az,bx,by,bz,mx,by+.10F,bz,c);
    }
    private static void crystalPanel(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c,int trim){
        float mx=(ax+bx)*.5F;quad(o,p,ax-.18F,ay,az,ax+.18F,ay,az,bx+.12F,by,bz,mx,by-.10F,bz+.04F,c);
        quad(o,p,ax-.04F,ay-.06F,az-.01F,ax+.04F,ay-.06F,az-.01F,bx+.03F,by+.08F,bz-.01F,bx-.03F,by+.08F,bz-.01F,trim);
    }
    private static void streamer(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){
        quad(o,p,ax-.055F,ay,az,ax+.055F,ay,az,bx+.045F,by,bz,bx-.045F,by,bz,c);
    }
    private static void box(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){
        quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c);quad(o,p,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,c);
        quad(o,p,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,c);quad(o,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,c);
    }
    private static void quad(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int c){
        o.addVertex(p,ax,ay,az).setColor(c);o.addVertex(p,bx,by,bz).setColor(c);o.addVertex(p,cx,cy,cz).setColor(c);o.addVertex(p,dx,dy,dz).setColor(c);
    }
    private static int styleBody(int s){return switch(s){case 3->0xF04A1F22;case 4->0xF01D4360;case 5->0xF01B4657;case 6->0xF02C1748;case 7->0xF035174F;default->0xF0273D72;};}
    private static int styleTrim(int s){return switch(s){case 3->0xFFFF7A42;case 4->0xFFA9EDFF;case 5->0xFF7FE9FF;case 6->0xFFFFD56A;case 7->0xFFD492FF;default->0xFFB9C7FF;};}
    private static int styleGlow(int s){return switch(s){case 3->0xFFFFB06A;case 4->0xFFD7FAFF;case 5->0xFFB9FFFF;case 6->0xFFFFE89B;case 7->0xFFE6B9FF;default->0xFFE3D8FF;};}
    private static int dark(int c){int a=c&0xFF000000,r=(int)(((c>>16)&255)*.58),g=(int)(((c>>8)&255)*.58),b=(int)((c&255)*.58);return a|(r<<16)|(g<<8)|b;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private record Vec3f(float x,float y,float z){}
}
