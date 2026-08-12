package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Client-only casting choreography. Server timing never depends on this layer. */
final class ArcaneCastingPerformance {
    private ArcaneCastingPerformance(){}

    static void render(PoseStack stack,RenderPlayerEvent.Post<?> event,int style,int family,float progress,boolean release){
        if(family<=0)return;float p=clamp(progress,0,1),kick=release?(1-p):0;int cloth=body(style),edge=trim(style),glow=glow(style);
        event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{
            switch(family){
                case SpellCinematicDirector.CAST_SNAP -> snap(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_AIM -> aim(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_HEAVY -> heavy(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_GROUND -> ground(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_WARD -> ward(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_PORTAL -> portal(out,pose,p,kick,cloth,edge);
                case SpellCinematicDirector.CAST_RITUAL -> ritual(out,pose,p,kick,cloth,edge);
                default -> {}
            }
            focus(out,pose,family,p,kick,glow);
        });
    }

    private static void snap(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.48F,1.36F,-.12F,-.62F,1.05F,-.36F,body);
        arm(o,p,.48F,1.36F,-.12F,.70F,1.18F,-.78F-k*.16F,edge);
    }
    private static void aim(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.48F,1.36F,-.10F,-.22F,1.15F,-.78F+k*.10F,body);
        arm(o,p,.48F,1.36F,-.10F,.18F,1.17F,-.94F-k*.18F,edge);
        strap(o,p,-.16F,1.20F,-.81F,.16F,1.20F,-.81F,.18F,1.10F,-1.01F,-.18F,1.10F,-1.01F,edge);
    }
    private static void heavy(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        float z=-.50F-.20F*q+k*.15F;arm(o,p,-.49F,1.35F,-.08F,-.30F,1.03F,z,body);arm(o,p,.49F,1.35F,-.08F,.30F,1.03F,z,body);
        strap(o,p,-.27F,1.05F,z-.02F,.27F,1.05F,z-.02F,.19F,.90F,z-.26F,-.19F,.90F,z-.26F,edge);
    }
    private static void ground(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.50F,1.34F,-.08F,-.71F,.72F,-.42F,body);arm(o,p,.50F,1.34F,-.08F,.71F,.72F,-.42F,body);
        blade(o,p,-.71F,.72F,-.39F,-.86F,.30F,-.44F,edge);blade(o,p,.71F,.72F,-.39F,.86F,.30F,-.44F,edge);
    }
    private static void ward(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.49F,1.35F,-.10F,-.17F,1.08F,-.55F+k*.08F,body);arm(o,p,.49F,1.35F,-.10F,.17F,1.08F,-.55F+k*.08F,body);
        strap(o,p,-.34F,1.13F,-.56F,.34F,1.13F,-.56F,.30F,.82F,-.47F,-.30F,.82F,-.47F,edge);
    }
    private static void portal(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.48F,1.34F,-.06F,-.92F,1.15F,-.34F-k*.07F,body);arm(o,p,.48F,1.34F,-.06F,.92F,1.15F,-.34F+k*.07F,edge);
        blade(o,p,-.92F,1.15F,-.31F,-1.05F,1.52F,-.24F,edge);blade(o,p,.92F,1.15F,-.31F,1.05F,1.52F,-.24F,edge);
    }
    private static void ritual(VertexConsumer o,PoseStack.Pose p,float q,float k,int body,int edge){
        arm(o,p,-.48F,1.34F,-.05F,-.76F,1.66F,-.24F-k*.06F,body);arm(o,p,.48F,1.34F,-.05F,.83F,1.78F,-.20F+k*.06F,edge);
        blade(o,p,-.76F,1.65F,-.22F,-.58F,1.90F,-.14F,body);blade(o,p,.83F,1.77F,-.18F,.65F,2.02F,-.10F,edge);
    }

    private static void focus(VertexConsumer o,PoseStack.Pose p,int family,float q,float kick,int color){
        if(q<.08&&kick<=0)return;float s=.035F+.055F*q;float x=0,y=family==SpellCinematicDirector.CAST_GROUND?.42F:family==SpellCinematicDirector.CAST_RITUAL?1.94F:1.06F,z=family==SpellCinematicDirector.CAST_PORTAL?-.28F:-.80F;
        box(o,p,x-s,y-s,z-s,x+s,y+s,z+s,color);
    }
    private static void arm(VertexConsumer o,PoseStack.Pose p,float sx,float sy,float sz,float ex,float ey,float ez,int color){float w=.115F;strap(o,p,sx-w,sy,sz-w,sx+w,sy,sz+w,ex+w,ey,ez+w,ex-w,ey,ez-w,color);strap(o,p,sx-w,sy,sz+w,sx+w,sy,sz-w,ex+w,ey,ez-w,ex-w,ey,ez+w,dark(color));}
    private static void blade(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int color){float mx=(ax+bx)*.5F;strap(o,p,ax-.045F,ay,az,ax+.045F,ay,az,bx,by,bz,mx,by+.08F,bz,color);}
    private static void box(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){strap(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c);strap(o,p,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,c);strap(o,p,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,c);strap(o,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,c);}
    private static void strap(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int c){o.addVertex(p,ax,ay,az).setColor(c);o.addVertex(p,bx,by,bz).setColor(c);o.addVertex(p,cx,cy,cz).setColor(c);o.addVertex(p,dx,dy,dz).setColor(c);}
    private static int body(int s){return switch(s){case 3->0xF0472422;case 4->0xF0244355;case 5->0xF0224851;case 6->0xF02C2544;case 7->0xF0352349;case 2->0xF02A344E;default->0xF02A3042;};}
    private static int trim(int s){return switch(s){case 3->0xFFFF8A50;case 4->0xFFB6F1FF;case 5->0xFF8DF4FF;case 6->0xFFFFD889;case 7->0xFFDEA8FF;case 2->0xFFC8D4FF;default->0xFFB8C3DC;};}
    private static int glow(int s){return switch(s){case 3->0xFFFFC07A;case 4->0xFFE1FBFF;case 5->0xFFC9FFFF;case 6->0xFFFFEEB3;case 7->0xFFEBCBFF;default->0xFFE4E8F4;};}
    private static int dark(int c){int a=c&0xFF000000,r=(int)(((c>>16)&255)*.55),g=(int)(((c>>8)&255)*.55),b=(int)((c&255)*.55);return a|(r<<16)|(g<<8)|b;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
