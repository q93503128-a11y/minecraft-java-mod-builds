package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** One-pass outfit silhouettes. Each style owns its cut; there is no generic robe plus decorations. */
final class ArcaneRegaliaRenderer {
    private ArcaneRegaliaRenderer(){}

    static void render(PoseStack stack,RenderPlayerEvent.Post<?> event,int robeStyle,int hatStyle,int bootStyle,
                       float moveX,float moveZ,float flutter,int castFamily,float castProgress,boolean release){
        if(robeStyle<=0&&hatStyle<=0&&bootStyle<=0)return;
        float sway=clamp(-moveZ*1.35F+flutter*1.6F,-.24F,.24F),side=clamp(-moveX*.95F,-.16F,.16F);
        event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{
            if(robeStyle>0){switch(robeStyle){
                case 1 -> apprentice(out,pose,sway,side);
                case 2 -> sage(out,pose,sway,side);
                case 3 -> cinder(out,pose,sway,side);
                case 4 -> glacier(out,pose,sway,side);
                case 5 -> tempest(out,pose,sway,side);
                case 6 -> archmage(out,pose,sway,side);
                case 7 -> rift(out,pose,sway,side);
                default -> apprentice(out,pose,sway,side);
            }}
            if(hatStyle>0)headwear(out,pose,hatStyle);
            if(bootStyle>0)footwear(out,pose,bootStyle);
        });
    }

    private static void apprentice(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF02D3448,d=0xF01B2130,t=0xFFB8C4DE;torso(o,p,.38F,1.47F,.30F,.30F,.86F,c,d);coatPanel(o,p,-.30F,.90F,.31F,-.06F,.12F,.42F+sway,c);coatPanel(o,p,.06F,.90F,.31F,.30F,.12F,.42F+sway,d);band(o,p,-.30F,.30F,.92F,-.325F,.035F,t);clasp(o,p,0,1.34F,-.34F,.055F,t);}
    private static void sage(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF02A3858,d=0xF018243A,t=0xFFC8D6FF;torso(o,p,.40F,1.48F,.31F,.29F,.90F,c,d);cape(o,p,-.48F,1.49F,.27F,.48F,1.49F,.27F,.36F,.60F,.49F+sway,c);coatPanel(o,p,-.30F,1.14F,-.34F,-.06F,.15F,-.43F+side,t);coatPanel(o,p,.06F,1.14F,-.34F,.30F,.15F,-.43F-side,d);band(o,p,-.37F,.37F,1.05F,-.345F,.045F,t);clasp(o,p,0,1.38F,-.355F,.065F,t);}
    private static void cinder(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF0502824,d=0xF0261919,t=0xFFFF8A50;torso(o,p,.42F,1.47F,.30F,.33F,.88F,c,d);shoulder(o,p,-.58F,1.44F,-.05F,-.35F,1.60F,.18F,c);shoulder(o,p,.35F,1.44F,-.05F,.58F,1.60F,.18F,d);coatPanel(o,p,-.42F,.94F,.31F,-.06F,.38F,.55F+sway,c);coatPanel(o,p,.06F,.94F,.31F,.42F,.38F,.55F+sway,d);blade(o,p,-.30F,.46F,.46F,-.50F,.05F,.72F+sway,t);blade(o,p,.30F,.46F,.46F,.50F,.05F,.72F+sway,t);clasp(o,p,0,1.34F,-.35F,.07F,t);}
    private static void glacier(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF023465A,d=0xF0142B39,t=0xFFB8F2FF;torso(o,p,.39F,1.48F,.30F,.29F,.91F,c,d);crystal(o,p,-.44F,1.38F,.20F,-.62F,1.72F,.30F,t);crystal(o,p,.44F,1.38F,.20F,.62F,1.72F,.30F,t);facetPanel(o,p,-.32F,.92F,.34F,-.50F,.05F,.54F+sway,c,t);facetPanel(o,p,.32F,.92F,.34F,.50F,.05F,.54F+sway,d,t);facetPanel(o,p,0,.92F,.38F,side,.02F,.72F+sway,c,t);clasp(o,p,0,1.38F,-.36F,.075F,t);}
    private static void tempest(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF0224A51,d=0xF0143036,t=0xFF8EF4FF;torso(o,p,.37F,1.46F,.29F,.28F,.96F,c,d);streamer(o,p,-.34F,.96F,.34F,-.56F,.23F,.72F+sway+side,c);streamer(o,p,-.10F,.96F,.36F,-.16F,.17F,.88F+sway,t);streamer(o,p,.10F,.96F,.36F,.16F,.17F,.88F+sway,t);streamer(o,p,.34F,.96F,.34F,.56F,.23F,.72F+sway-side,d);band(o,p,-.34F,.34F,1.10F,-.33F,.032F,t);}
    private static void archmage(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF0312949,d=0xF0191730,t=0xFFFFD98B;torso(o,p,.44F,1.50F,.31F,.31F,.88F,c,d);cape(o,p,-.62F,1.52F,.26F,.62F,1.52F,.26F,.52F,.08F,.68F+sway,c);shoulder(o,p,-.68F,1.47F,-.04F,-.36F,1.70F,.20F,c);shoulder(o,p,.36F,1.47F,-.04F,.68F,1.70F,.20F,c);coatPanel(o,p,-.36F,1.05F,.37F,-.09F,.04F,.75F+sway,d);coatPanel(o,p,.09F,1.05F,.37F,.36F,.04F,.75F+sway,d);coatPanel(o,p,-.10F,1.10F,.40F,.10F,.01F,.92F+sway,t);band(o,p,-.44F,.44F,1.03F,-.36F,.045F,t);clasp(o,p,0,1.38F,-.39F,.09F,t);}
    private static void rift(VertexConsumer o,PoseStack.Pose p,float sway,float side){int c=0xF0392750,d=0xF01B1630,t=0xFFDEA8FF;torso(o,p,.41F,1.48F,.30F,.30F,.92F,c,d);shoulder(o,p,-.66F,1.46F,-.03F,-.33F,1.72F,.18F,c);crystal(o,p,.42F,1.40F,.18F,.70F,1.63F,.31F,d);coatPanel(o,p,-.50F,1.10F,.34F,-.08F,.03F,.84F+sway+side,c);coatPanel(o,p,.10F,.95F,.34F,.45F,.20F,.56F-sway-side,d);blade(o,p,-.43F,.78F,.48F,-.72F,.06F,.91F+sway,t);blade(o,p,.34F,.68F,.44F,.56F,.24F,.64F-sway,d);clasp(o,p,-.08F,1.35F,-.38F,.075F,t);}

    private static void headwear(VertexConsumer o,PoseStack.Pose p,int style){int t=trim(style),c=body(style),d=dark(c);switch(style){
        case 1,2 -> pointedHat(o,p,c,d,t,style==2?.57F:.51F,style==2?.82F:.70F);
        case 3 -> hood(o,p,c,d,t,.46F,.42F);
        case 4 -> circlet(o,p,t,.39F,true);
        case 5 -> hood(o,p,c,d,t,.43F,.34F);
        case 6 -> crown(o,p,t,.43F,6);
        case 7 -> crown(o,p,t,.43F,5);
        default -> pointedHat(o,p,c,d,t,.50F,.68F);
    }}
    private static void footwear(VertexConsumer o,PoseStack.Pose p,int style){int c=dark(body(style)),t=trim(style);box(o,p,-.29F,.02F,-.20F,-.03F,.34F,.23F,c);box(o,p,.03F,.02F,-.20F,.29F,.34F,.23F,c);band(o,p,-.29F,-.03F,.27F,-.205F,.035F,t);band(o,p,.03F,.29F,.27F,-.205F,.035F,t);if(style>=5){blade(o,p,-.27F,.18F,.22F,-.36F,.02F,.34F,t);blade(o,p,.27F,.18F,.22F,.36F,.02F,.34F,t);}}

    private static void torso(VertexConsumer o,PoseStack.Pose p,float shoulder,float top,float depth,float waist,float bottom,int front,int back){quad(o,p,-shoulder,top,-depth,shoulder,top,-depth,waist,bottom,-depth-.02F,-waist,bottom,-depth-.02F,front);quad(o,p,shoulder,top,depth,-shoulder,top,depth,-waist,bottom,depth+.02F,waist,bottom,depth+.02F,back);quad(o,p,-shoulder,top,depth,-shoulder,top,-depth,-waist,bottom,-depth-.02F,-waist,bottom,depth+.02F,dark(front));quad(o,p,shoulder,top,-depth,shoulder,top,depth,waist,bottom,depth+.02F,waist,bottom,-depth-.02F,dark(back));}
    private static void coatPanel(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z1,x0,y1,z1,c);}
    private static void cape(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,float hem,float bottom,float back,int c){quad(o,p,x0,y0,z0,x1,y1,z1,hem,bottom,back,-hem,bottom,back,c);}
    private static void shoulder(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z1,x0,y1,z1,c);}
    private static void facetPanel(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c,int edge){float mx=(ax+bx)*.5F;quad(o,p,ax-.14F,ay,az,ax+.14F,ay,az,bx+.10F,by,bz,mx,by-.10F,bz+.04F,c);blade(o,p,ax,ay-.05F,az-.01F,bx,by+.06F,bz-.01F,edge);}
    private static void streamer(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){quad(o,p,ax-.045F,ay,az,ax+.045F,ay,az,bx+.035F,by,bz,bx-.035F,by,bz,c);}
    private static void crystal(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){float mx=(ax+bx)*.5F;quad(o,p,ax-.055F,ay,az,ax+.055F,ay,az,bx,by,bz,mx,by+.11F,bz+.02F,c);}
    private static void blade(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){float mx=(ax+bx)*.5F;quad(o,p,ax-.045F,ay,az,ax+.045F,ay,az,bx,by,bz,mx,by+.08F,bz,c);}
    private static void band(VertexConsumer o,PoseStack.Pose p,float x0,float x1,float y,float z,float h,int c){quad(o,p,x0,y,z,x1,y,z,x1,y+h,z,x0,y+h,z,c);}
    private static void clasp(VertexConsumer o,PoseStack.Pose p,float x,float y,float z,float s,int c){quad(o,p,x,y-s,z,x+s,y,z,x,y+s,z,x-s,y,z,c);}
    private static void pointedHat(VertexConsumer o,PoseStack.Pose p,int c,int d,int t,float radius,float height){int n=18;float y=2.10F;for(int i=0;i<n;i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;float ax=(float)Math.cos(a)*radius,az=(float)Math.sin(a)*radius,bx=(float)Math.cos(b)*radius,bz=(float)Math.sin(b)*radius;quad(o,p,ax,y,az,bx,y,bz,bx*.66F,y+.06F,bz*.66F,ax*.66F,y+.06F,az,i%4==0?t:c);float tipX=.10F,tipZ=-.04F;quad(o,p,ax*.66F,y+.05F,az*.66F,bx*.66F,y+.05F,bz*.66F,tipX,y+height,tipZ,tipX,y+height,tipZ,i%2==0?c:d);}}
    private static void hood(VertexConsumer o,PoseStack.Pose p,int c,int d,int t,float radius,float height){box(o,p,-radius,1.87F,-.32F,radius,2.12F,.34F,c);quad(o,p,-radius,2.10F,.30F,radius,2.10F,.30F,radius*.78F,2.10F+height,.16F,-radius*.78F,2.10F+height,.16F,d);band(o,p,-radius,radius,1.89F,-.335F,.035F,t);}
    private static void circlet(VertexConsumer o,PoseStack.Pose p,int t,float r,boolean crystalTop){band(o,p,-r,r,2.02F,-.36F,.045F,t);if(crystalTop){crystal(o,p,0,2.05F,-.37F,0,2.30F,-.35F,t);crystal(o,p,-.20F,2.04F,-.35F,-.25F,2.18F,-.34F,t);crystal(o,p,.20F,2.04F,-.35F,.25F,2.18F,-.34F,t);}}
    private static void crown(VertexConsumer o,PoseStack.Pose p,int t,float r,int points){band(o,p,-r,r,2.01F,-.36F,.045F,t);for(int i=0;i<points;i++){float x=-r+i*(r*2/Math.max(1,points-1));blade(o,p,x,2.05F,-.35F,x+(i%2==0?.03F:-.03F),2.28F+(i%3)*.04F,-.33F,t);}}
    private static void box(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c);quad(o,p,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,c);quad(o,p,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,c);quad(o,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,c);quad(o,p,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,c);}
    private static void quad(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int c){o.addVertex(p,ax,ay,az).setColor(c);o.addVertex(p,bx,by,bz).setColor(c);o.addVertex(p,cx,cy,cz).setColor(c);o.addVertex(p,dx,dy,dz).setColor(c);}
    private static int body(int s){return switch(s){case 3->0xF0502824;case 4->0xF023465A;case 5->0xF0224A51;case 6->0xF0312949;case 7->0xF0392750;case 2->0xF02A3858;default->0xF02D3448;};}
    private static int trim(int s){return switch(s){case 3->0xFFFF8A50;case 4->0xFFB8F2FF;case 5->0xFF8EF4FF;case 6->0xFFFFD98B;case 7->0xFFDEA8FF;case 2->0xFFC8D6FF;default->0xFFB8C4DE;};}
    private static int dark(int c){int a=c&0xFF000000,r=(int)(((c>>16)&255)*.52),g=(int)(((c>>8)&255)*.52),b=(int)((c&255)*.52);return a|(r<<16)|(g<<8)|b;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
