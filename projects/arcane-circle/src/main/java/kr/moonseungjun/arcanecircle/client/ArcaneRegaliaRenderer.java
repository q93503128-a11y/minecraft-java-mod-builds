package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Volumetric one-pass mage clothing. The renderer deliberately avoids the old torso-card look:
 * robes are built from bodice, lapels, flared skirt gores, side cloth and a moving back train.
 */
final class ArcaneRegaliaRenderer {
    private ArcaneRegaliaRenderer() {}

    static void render(PoseStack stack, RenderPlayerEvent.Post<?> event, int robeStyle, int hatStyle, int bootStyle,
                       float moveX, float moveZ, float flutter, int castFamily, float castProgress, boolean release) {
        if (robeStyle <= 0 && hatStyle <= 0 && bootStyle <= 0) return;
        float castLift = castFamily > 0 ? (.035F + castProgress * .055F) : 0F;
        float sway = clamp(-moveZ * 1.25F + flutter * 1.45F, -.22F, .22F);
        float side = clamp(-moveX * .90F, -.14F, .14F);
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
            if (robeStyle > 0) outfit(out, pose, robeStyle, sway, side, castLift, release);
            if (hatStyle > 0) headwear(out, pose, hatStyle, castLift);
            if (bootStyle > 0) footwear(out, pose, bootStyle);
        });
    }

    private static void outfit(VertexConsumer o, PoseStack.Pose p, int style, float sway, float side, float lift, boolean release) {
        int main = body(style), dark = dark(main), trim = trim(style), accent = accent(style);
        float hem = .18F + lift;
        bodice(o,p,main,dark,trim);
        lapel(o,p,-.31F,1.50F,-.326F,-.04F,1.03F,-.346F,trim);
        lapel(o,p,.31F,1.50F,-.326F,.04F,1.03F,-.346F,accent);
        belt(o,p,trim,dark);
        shoulderMantle(o,p,style,main,dark,trim);

        switch (style) {
            case 1 -> {
                skirtPair(o,p,main,dark,trim,.36F,.46F,.96F,hem,.34F+sway,.02F+side);
                backTrain(o,p,dark,trim,.38F,.50F,.98F,.12F,.38F+sway,0F);
            }
            case 2 -> {
                skirtPair(o,p,main,dark,trim,.37F,.52F,1.00F,hem,.38F+sway,.03F+side);
                backTrain(o,p,main,trim,.43F,.60F,1.18F,.07F,.48F+sway,.02F);
                sideGore(o,p,-1,main,trim,.34F,.57F,.98F,.20F,.36F+sway+side);
                sideGore(o,p,1,dark,trim,.34F,.57F,.98F,.20F,.36F+sway-side);
            }
            case 3 -> {
                skirtPair(o,p,main,dark,trim,.39F,.50F,.96F,.28F,.52F+sway,.04F+side);
                splitBlade(o,p,-.32F,.90F,.36F,-.58F,.14F,.69F+sway,trim);
                splitBlade(o,p,.32F,.90F,.36F,.58F,.14F,.69F+sway,accent);
                backTrain(o,p,dark,trim,.38F,.46F,.96F,.23F,.54F+sway,0F);
            }
            case 4 -> {
                facetedSkirt(o,p,main,dark,trim,.38F,.55F,.99F,.12F,.50F+sway,side);
                backTrain(o,p,dark,trim,.42F,.54F,1.02F,.08F,.46F+sway,0F);
                crystal(o,p,-.43F,1.33F,.22F,-.62F,1.67F,.30F,trim);
                crystal(o,p,.43F,1.33F,.22F,.62F,1.67F,.30F,accent);
            }
            case 5 -> {
                skirtPair(o,p,main,dark,trim,.35F,.44F,1.02F,.32F,.38F+sway,.02F+side);
                streamer(o,p,-.38F,1.00F,.34F,-.60F,.17F,.73F+sway+side,main);
                streamer(o,p,-.13F,1.03F,.36F,-.20F,.08F,.90F+sway,trim);
                streamer(o,p,.13F,1.03F,.36F,.20F,.08F,.90F+sway,accent);
                streamer(o,p,.38F,1.00F,.34F,.60F,.17F,.73F+sway-side,dark);
            }
            case 6 -> {
                skirtPair(o,p,dark,dark,trim,.41F,.58F,1.04F,.10F,.52F+sway,.03F+side);
                backTrain(o,p,main,trim,.50F,.72F,1.24F,.02F,.66F+sway,0F);
                backTrain(o,p,dark,accent,.32F,.48F,1.10F,.06F,.76F+sway*.85F,.04F);
                ceremonialTab(o,p,trim,accent,.15F,1.08F,.02F,.90F+sway);
            }
            case 7 -> {
                asymmetricSkirt(o,p,main,dark,trim,accent,sway,side);
                backTrain(o,p,dark,accent,.42F,.60F,1.14F,.06F,.58F+sway,-.08F);
                splitBlade(o,p,-.43F,.88F,.43F,-.72F,.05F,.93F+sway,trim);
            }
            default -> skirtPair(o,p,main,dark,trim,.36F,.46F,.96F,hem,.34F+sway,.02F+side);
        }
    }

    private static void bodice(VertexConsumer o, PoseStack.Pose p, int main, int dark, int trim) {
        shellPanel(o,p,-.40F,1.58F,-.31F,-.05F,.96F,-.335F,main);
        shellPanel(o,p,.05F,1.58F,-.31F,.40F,.96F,-.335F,dark);
        shellPanel(o,p,-.38F,1.56F,.31F,-.03F,.98F,.345F,dark);
        shellPanel(o,p,.03F,1.56F,.31F,.38F,.98F,.345F,main);
        sidePanel(o,p,-.40F,1.55F,-.30F,-.38F,.98F,.31F,dark);
        sidePanel(o,p,.40F,1.55F,-.30F,.38F,.98F,.31F,dark);
        band(o,p,-.37F,.37F,1.02F,-.35F,.035F,trim);
    }

    private static void shoulderMantle(VertexConsumer o, PoseStack.Pose p, int style, int main, int dark, int trim) {
        float width = style >= 6 ? .67F : style >= 2 ? .55F : .47F;
        float drop = style >= 6 ? 1.36F : 1.40F;
        quad(o,p,-width,1.57F,-.05F,width,1.57F,-.05F,.42F,drop,.30F,-.42F,drop,.30F,main);
        quad(o,p,-width,1.57F,.02F,-.39F,drop,.31F,-.32F,1.29F,.33F,-.52F,1.46F,.15F,dark);
        quad(o,p,width,1.57F,.02F,.39F,drop,.31F,.32F,1.29F,.33F,.52F,1.46F,.15F,dark);
        band(o,p,-width,width,1.54F,-.075F,.025F,trim);
    }

    private static void skirtPair(VertexConsumer o, PoseStack.Pose p, int left, int right, int trim,
                                  float waistHalf, float hemHalf, float top, float bottom, float back, float side) {
        float gap=.055F;
        quad(o,p,-waistHalf,top,-.34F,-gap,top,-.35F,-.10F,bottom,-.39F-side,-hemHalf,bottom,-.35F+side,left);
        quad(o,p,gap,top,-.35F,waistHalf,top,-.34F,hemHalf,bottom,-.35F-side,.10F,bottom,-.39F+side,right);
        quad(o,p,-waistHalf,top,-.32F,-waistHalf,top,.32F,-hemHalf,bottom,.30F+back,-hemHalf,bottom,-.34F+side,dark(left));
        quad(o,p,waistHalf,top,.32F,waistHalf,top,-.32F,hemHalf,bottom,-.34F-side,hemHalf,bottom,.30F+back,dark(right));
        edge(o,p,-hemHalf,bottom,-.36F+side,-.10F,bottom,-.40F-side,trim);
        edge(o,p,.10F,bottom,-.40F+side,hemHalf,bottom,-.36F-side,trim);
    }

    private static void backTrain(VertexConsumer o, PoseStack.Pose p, int color, int trim,
                                  float topHalf, float bottomHalf, float top, float bottom, float back, float xShift) {
        float zTop=.33F, zBottom=back;
        quad(o,p,-topHalf+xShift,top,zTop,topHalf+xShift,top,zTop,
                bottomHalf+xShift,bottom,zBottom,-bottomHalf+xShift,bottom,zBottom,color);
        edge(o,p,-bottomHalf+xShift,bottom,zBottom,bottomHalf+xShift,bottom,zBottom,trim);
        edge(o,p,-topHalf+xShift,top,zTop,-bottomHalf+xShift,bottom,zBottom,trim);
        edge(o,p,topHalf+xShift,top,zTop,bottomHalf+xShift,bottom,zBottom,trim);
    }

    private static void sideGore(VertexConsumer o, PoseStack.Pose p, int sign, int color, int trim,
                                 float waist, float hem, float top, float bottom, float back) {
        float x0=sign*waist,x1=sign*hem;
        quad(o,p,x0,top,-.03F,x0,top,.31F,x1,bottom,back,x1,bottom,-.16F,color);
        edge(o,p,x1,bottom,-.16F,x1,bottom,back,trim);
    }

    private static void facetedSkirt(VertexConsumer o, PoseStack.Pose p, int main, int dark, int trim,
                                     float waist, float hem, float top, float bottom, float back, float side) {
        skirtPair(o,p,main,dark,trim,waist,hem,top,bottom,back,side);
        splitBlade(o,p,-.20F,.92F,-.37F,-.34F,.08F,-.43F+side,trim);
        splitBlade(o,p,.20F,.92F,-.37F,.34F,.08F,-.43F-side,trim);
        splitBlade(o,p,0F,.98F,.35F,side,.03F,.64F+back,accent(4));
    }

    private static void asymmetricSkirt(VertexConsumer o, PoseStack.Pose p, int main, int dark, int trim, int accent,
                                        float sway, float side) {
        quad(o,p,-.40F,1.04F,-.34F,-.03F,1.04F,-.35F,-.12F,.05F,-.43F+side,-.61F,.16F,-.31F+side,main);
        quad(o,p,.05F,.96F,-.35F,.40F,.96F,-.34F,.50F,.28F,-.30F-side,.12F,.20F,-.41F-side,dark);
        sideGore(o,p,-1,main,trim,.38F,.62F,1.02F,.08F,.62F+sway+side);
        sideGore(o,p,1,dark,accent,.38F,.48F,.96F,.24F,.42F+sway-side);
        edge(o,p,-.61F,.16F,-.31F+side,-.12F,.05F,-.43F+side,trim);
    }

    private static void ceremonialTab(VertexConsumer o, PoseStack.Pose p, int trim, int accent,
                                      float half, float top, float bottom, float back) {
        quad(o,p,-half,top,-.365F,half,top,-.365F,half*.72F,bottom,-.42F,-half*.72F,bottom,-.42F,trim);
        quad(o,p,-half*.30F,top-.05F,-.37F,half*.30F,top-.05F,-.37F,
                half*.22F,bottom+.10F,-.425F,-half*.22F,bottom+.10F,-.425F,accent);
    }

    private static void lapel(VertexConsumer o, PoseStack.Pose p, float ax,float ay,float az,float bx,float by,float bz,int c) {
        float sx=Math.signum(ax)*.09F;
        quad(o,p,ax,ay,az,ax-sx,ay,az-.005F,bx,by,bz,bx+sx*.35F,by+.04F,bz,c);
    }
    private static void belt(VertexConsumer o,PoseStack.Pose p,int trim,int dark){band(o,p,-.39F,.39F,.99F,-.36F,.055F,dark);band(o,p,-.13F,.13F,1.00F,-.37F,.06F,trim);}
    private static void shellPanel(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z1,x0,y1,z1,c);}
    private static void sidePanel(VertexConsumer o,PoseStack.Pose p,float x,float y,float z0,float xb,float yb,float z1,int c){quad(o,p,x,y,z0,x,y,z1,xb,yb,z1,xb,yb,z0,c);}
    private static void splitBlade(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){float mx=(ax+bx)*.5F;quad(o,p,ax-.04F,ay,az,ax+.04F,ay,az,bx,by,bz,mx,by+.09F,bz,c);}
    private static void streamer(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){quad(o,p,ax-.045F,ay,az,ax+.045F,ay,az,bx+.035F,by,bz,bx-.035F,by,bz,c);}
    private static void crystal(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){float mx=(ax+bx)*.5F;quad(o,p,ax-.055F,ay,az,ax+.055F,ay,az,bx,by,bz,mx,by+.11F,bz+.02F,c);}
    private static void edge(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,int c){float w=.018F;quad(o,p,ax-w,ay,az,bx-w,by,bz,bx+w,by,bz,ax+w,ay,az,c);}

    private static void headwear(VertexConsumer o, PoseStack.Pose p, int style, float lift) {
        int t=trim(style),c=body(style),d=dark(c);float y=2.08F+lift*.35F;
        switch(style){
            case 1 -> clothHat(o,p,c,d,t,y,.53F,.72F,.10F);
            case 2 -> clothHat(o,p,c,d,t,y,.59F,.84F,.16F);
            case 3 -> hood(o,p,c,d,t,y,.47F,.40F);
            case 4 -> circlet(o,p,t,y,.40F,true);
            case 5 -> hood(o,p,c,d,t,y,.45F,.34F);
            case 6 -> crown(o,p,t,y,.45F,7);
            case 7 -> crown(o,p,t,y,.45F,5);
            default -> clothHat(o,p,c,d,t,y,.52F,.70F,.10F);
        }
    }

    private static void clothHat(VertexConsumer o,PoseStack.Pose p,int c,int d,int t,float y,float radius,float height,float bend){
        int n=18;for(int i=0;i<n;i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;float ax=(float)Math.cos(a)*radius,az=(float)Math.sin(a)*radius,bx=(float)Math.cos(b)*radius,bz=(float)Math.sin(b)*radius;quad(o,p,ax,y,az,bx,y,bz,bx*.68F,y+.06F,bz*.68F,ax*.68F,y+.06F,az,i%5==0?t:c);}
        float midX=bend*.35F,midZ=-bend*.22F,tipX=bend,tipZ=-bend*.50F;
        for(int i=0;i<n;i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;float ax=(float)Math.cos(a)*radius*.68F,az=(float)Math.sin(a)*radius*.68F,bx=(float)Math.cos(b)*radius*.68F,bz=(float)Math.sin(b)*radius*.68F;quad(o,p,ax,y+.05F,az,bx,y+.05F,bz,midX,y+height*.48F,midZ,midX,y+height*.48F,midZ,i%2==0?c:d);quad(o,p,midX,y+height*.46F,midZ,midX,y+height*.46F,midZ,tipX,y+height,tipZ,tipX,y+height,tipZ,i%2==0?d:c);}
        band(o,p,-radius,radius,y+.025F,-radius-.012F,.035F,t);
    }
    private static void hood(VertexConsumer o,PoseStack.Pose p,int c,int d,int t,float y,float radius,float height){box(o,p,-radius,y-.20F,-.32F,radius,y+.05F,.34F,c);quad(o,p,-radius,y+.03F,.30F,radius,y+.03F,.30F,radius*.76F,y+height,.17F,-radius*.76F,y+height,.17F,d);band(o,p,-radius,radius,y-.18F,-.335F,.035F,t);}
    private static void circlet(VertexConsumer o,PoseStack.Pose p,int t,float y,float r,boolean crystalTop){band(o,p,-r,r,y-.08F,-.36F,.045F,t);if(crystalTop){crystal(o,p,0,y-.04F,-.37F,0,y+.22F,-.35F,t);crystal(o,p,-.20F,y-.05F,-.35F,-.25F,y+.10F,-.34F,t);crystal(o,p,.20F,y-.05F,-.35F,.25F,y+.10F,-.34F,t);}}
    private static void crown(VertexConsumer o,PoseStack.Pose p,int t,float y,float r,int points){band(o,p,-r,r,y-.09F,-.36F,.045F,t);for(int i=0;i<points;i++){float x=-r+i*(r*2/Math.max(1,points-1));splitBlade(o,p,x,y-.05F,-.35F,x+(i%2==0?.025F:-.025F),y+.18F+(i%3)*.04F,-.33F,t);}}

    private static void footwear(VertexConsumer o,PoseStack.Pose p,int style){int c=dark(body(style)),t=trim(style);box(o,p,-.29F,.02F,-.20F,-.03F,.34F,.23F,c);box(o,p,.03F,.02F,-.20F,.29F,.34F,.23F,c);band(o,p,-.29F,-.03F,.27F,-.205F,.035F,t);band(o,p,.03F,.29F,.27F,-.205F,.035F,t);if(style>=5){splitBlade(o,p,-.27F,.18F,.22F,-.36F,.02F,.34F,t);splitBlade(o,p,.27F,.18F,.22F,.36F,.02F,.34F,t);}}

    private static void band(VertexConsumer o,PoseStack.Pose p,float x0,float x1,float y,float z,float h,int c){quad(o,p,x0,y,z,x1,y,z,x1,y+h,z,x0,y+h,z,c);}
    private static void box(VertexConsumer o,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(o,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c);quad(o,p,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,c);quad(o,p,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,dark(c));quad(o,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,dark(c));}
    private static void quad(VertexConsumer o,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int c){v(o,p,ax,ay,az,c);v(o,p,bx,by,bz,c);v(o,p,cx,cy,cz,c);v(o,p,dx,dy,dz,c);}
    private static void v(VertexConsumer o,PoseStack.Pose p,float x,float y,float z,int c){o.addVertex(p,x,y,z).setColor(c);}

    private static int body(int s){return switch(s){case 1->0xE83A4160;case 2->0xE82F416B;case 3->0xE85B2B27;case 4->0xE8265268;case 5->0xE8245860;case 6->0xE83B3158;case 7->0xE8442D61;default->0xE8323953;};}
    private static int trim(int s){return switch(s){case 1->0xFFD5DFFF;case 2->0xFFE0E8FF;case 3->0xFFFF9A5E;case 4->0xFFBCF6FF;case 5->0xFF9AFAFF;case 6->0xFFFFD987;case 7->0xFFE9B4FF;default->0xFFE2E8FF;};}
    private static int accent(int s){return switch(s){case 1->0xFF879EFF;case 2->0xFF91B7FF;case 3->0xFFFFD074;case 4->0xFF6EDCFF;case 5->0xFF5CFFDD;case 6->0xFFC7A7FF;case 7->0xFFFF75DC;default->trim(s);};}
    private static int dark(int c){int a=(c>>>24)&255,r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;r=(int)(r*.58);g=(int)(g*.58);b=(int)(b*.64);return(a<<24)|(r<<16)|(g<<8)|b;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
