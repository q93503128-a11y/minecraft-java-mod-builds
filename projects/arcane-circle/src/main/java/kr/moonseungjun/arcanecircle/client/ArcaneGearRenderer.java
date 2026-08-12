package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Open-faced pointed hats and directional cloth geometry that trails player movement. */
public final class ArcaneGearRenderer {
    private static final ContextKey<Integer> HAT=key("hat_tier"),ROBE=key("robe_tier"),BOOTS=key("boots_tier");
    private static final ContextKey<Float> MOVE_X=key("gear_move_x"),MOVE_Z=key("gear_move_z"),PHASE=key("gear_move_phase");
    private ArcaneGearRenderer(){}
    private static final ContextKey<Integer> ROBE_STYLE=key("robe_style"),CAST_FAMILY=key("cast_family");
    private static final ContextKey<Float> CAST_PROGRESS=key("cast_progress");
    private static final ContextKey<Boolean> CAST_RELEASE=key("cast_release");

    public static void registerStateModifiers(RegisterRenderStateModifiersEvent event){
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier(){
            @Override public<T extends Avatar&ClientAvatarEntity>void accept(T avatar,AvatarRenderState state){
                ItemStack chest=avatar.getItemBySlot(EquipmentSlot.CHEST);
                state.setRenderData(HAT,hatTier(avatar.getItemBySlot(EquipmentSlot.HEAD)));
                state.setRenderData(ROBE,robeTier(chest));
                state.setRenderData(ROBE_STYLE,robeStyle(chest));
                state.setRenderData(BOOTS,bootsTier(avatar.getItemBySlot(EquipmentSlot.FEET)));
                Vec3 v=avatar.getDeltaMovement();double yaw=Math.toRadians(avatar.yBodyRot);
                float localX=(float)(v.x*Math.cos(yaw)+v.z*Math.sin(yaw));
                float localZ=(float)(-v.x*Math.sin(yaw)+v.z*Math.cos(yaw));
                float speed=(float)Math.min(.42,Math.sqrt(v.x*v.x+v.z*v.z));
                state.setRenderData(MOVE_X,localX);state.setRenderData(MOVE_Z,localZ);
                state.setRenderData(PHASE,(float)Math.sin(avatar.tickCount*.72)*speed);
                WorldMagicTracker.CasterPoseSnapshot cast=WorldMagicTracker.castingPose(avatar.getUUID());
                state.setRenderData(CAST_FAMILY,cast.family());
                state.setRenderData(CAST_PROGRESS,cast.progress());
                state.setRenderData(CAST_RELEASE,cast.release());
            }
        });
    }
    public static void onPlayerRender(RenderPlayerEvent.Post<?> event){
        int h=event.getRenderState().getRenderDataOrDefault(HAT,0),r=event.getRenderState().getRenderDataOrDefault(ROBE,0),
                b=event.getRenderState().getRenderDataOrDefault(BOOTS,0),style=event.getRenderState().getRenderDataOrDefault(ROBE_STYLE,0),
                family=event.getRenderState().getRenderDataOrDefault(CAST_FAMILY,0);
        if(h<=0&&r<=0&&b<=0&&family<=0)return;
        float mx=event.getRenderState().getRenderDataOrDefault(MOVE_X,0F),mz=event.getRenderState().getRenderDataOrDefault(MOVE_Z,0F),
                phase=event.getRenderState().getRenderDataOrDefault(PHASE,0F),
                castProgress=event.getRenderState().getRenderDataOrDefault(CAST_PROGRESS,0F);
        boolean release=event.getRenderState().getRenderDataOrDefault(CAST_RELEASE,false);
        PoseStack stack=event.getPoseStack();stack.pushPose();
        if(r>0){robe(stack,event,r,mx,mz,phase);CastingSilhouetteRenderer.robeOverlay(stack,event,style,mx,mz,phase);}
        if(b>0)boots(stack,event,b);if(h>0)hat(stack,event,h);
        CastingSilhouetteRenderer.render(stack,event,style,family,castProgress,release);
        stack.popPose();
    }

    private static void hat(PoseStack stack,RenderPlayerEvent.Post<?> event,int tier){int body=body(tier),dark=dark(body),trim=trim(tier);double outer=.50+tier*.045,inner=.345,y=2.105,height=.66+tier*.14;int n=20+tier*4;event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{for(int i=0;i<n;i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;float ax=(float)(Math.cos(a)*outer),az=(float)(Math.sin(a)*outer),bx=(float)(Math.cos(b)*outer),bz=(float)(Math.sin(b)*outer),iax=(float)(Math.cos(a)*inner),iaz=(float)(Math.sin(a)*inner),ibx=(float)(Math.cos(b)*inner),ibz=(float)(Math.sin(b)*inner);quad(out,pose,ax,(float)y,az,bx,(float)y,bz,ibx,(float)(y+.035),ibz,iax,(float)(y+.035),iaz,(i%4==0)?trim:body);double twist=.08*Math.sin(a*1.8+tier);float apexX=(float)(.12+tier*.025),apexZ=(float)(-.03+twist);quad(out,pose,iax,(float)(y+.03),iaz,ibx,(float)(y+.03),ibz,apexX,(float)(y+height),apexZ,apexX,(float)(y+height),apexZ,(i&1)==0?body:dark);}});filledBand(stack,event,inner*.98,inner*1.14,y+.12,18,trim,.035);filledBand(stack,event,outer*.80,outer*.90,y+.02,24,trim,.025);}

    private static void robe(PoseStack stack,RenderPlayerEvent.Post<?> event,int tier,float moveX,float moveZ,float phase){int body=body(tier),dark=dark(body),shadow=dark(dark),trim=trim(tier),lining=tier>=3?0xDF3D1558:tier==2?0xDE123759:0xDC321A3E;float shoulder=.43F+tier*.03F,waist=.31F,hem=.50F+tier*.065F,top=1.48F,chest=1.15F,middle=.86F,bottom=.055F;float speed=Math.min(.34F,(float)Math.sqrt(moveX*moveX+moveZ*moveZ));float lagX=clamp(-moveX*1.25F,-.20F,.20F),lagZ=clamp(-moveZ*1.25F,-.24F,.24F);float flutter=clamp(phase*1.7F,-.13F,.13F),lift=Math.min(.16F,speed*.48F);event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{
        quad(out,pose,-shoulder,top,-.30F,shoulder,top,-.30F,waist,chest,-.325F,-waist,chest,-.325F,body);quad(out,pose,shoulder,top,.30F,-shoulder,top,.30F,-waist,chest,.325F,waist,chest,.325F,dark);quad(out,pose,-shoulder,top,.30F,-shoulder,top,-.30F,-waist,chest,-.325F,-waist,chest,.325F,shadow);quad(out,pose,shoulder,top,-.30F,shoulder,top,.30F,waist,chest,.325F,waist,chest,-.325F,dark);
        quad(out,pose,-waist,chest,-.315F,waist,chest,-.315F,waist,middle,-.29F,-waist,middle,-.29F,body);quad(out,pose,waist,chest,.315F,-waist,chest,.315F,-waist,middle,.29F,waist,middle,.29F,dark);quad(out,pose,-waist,chest,.315F,-waist,chest,-.315F,-waist,middle,-.29F,-waist,middle,.29F,shadow);quad(out,pose,waist,chest,-.315F,waist,chest,.315F,waist,middle,.29F,waist,middle,-.29F,dark);
        float lX=lagX-flutter,rX=lagX+flutter,frontZ=-.40F+lagZ,backZ=.40F+lagZ;float by=bottom+lift;quad(out,pose,-waist,middle,-.29F,-.04F,middle,-.30F,-.08F+lX,by,frontZ,-hem+lX,by,frontZ+.035F,body);quad(out,pose,.04F,middle,-.30F,waist,middle,-.29F,hem+rX,by,frontZ+.035F,.08F+rX,by,frontZ,dark);quad(out,pose,-.04F,middle-.01F,-.305F,.04F,middle-.01F,-.305F,.10F+lagX,by+.018F,frontZ-.012F,-.10F+lagX,by+.018F,frontZ-.012F,lining);quad(out,pose,waist,middle,.29F,-waist,middle,.29F,-hem+lX,by,backZ,hem+rX,by,backZ,body);quad(out,pose,-waist,middle,.28F,-waist,middle,-.28F,-hem+lX,by,frontZ+.03F,-hem+lX,by,backZ,shadow);quad(out,pose,waist,middle,-.28F,waist,middle,.28F,hem+rX,by,backZ,hem+rX,by,frontZ+.03F,dark);
        float bandY=by+.045F;quad(out,pose,-hem+lX,bandY,frontZ-.008F,hem+rX,bandY,frontZ-.008F,hem+rX,bandY+.035F,frontZ-.010F,-hem+lX,bandY+.035F,frontZ-.010F,trim);
    });sleeve(stack,event,-1,tier,body,dark,lining);sleeve(stack,event,1,tier,dark,shadow,lining);filledBand(stack,event,.315,.345,.92,18,trim,.028);}

    private static void sleeve(PoseStack stack,RenderPlayerEvent.Post<?> event,int side,int tier,int upper,int lower,int lining){float x0=side<0?-.66F-tier*.016F:.39F,x1=side<0?-.39F:.66F+tier*.016F,outer=side<0?x0:x1;event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{quad(out,pose,x0,1.38F,-.27F,x1,1.38F,-.27F,x1,.69F,-.23F,x0,.69F,-.23F,upper);quad(out,pose,x1,1.38F,.27F,x0,1.38F,.27F,x0,.69F,.23F,x1,.69F,.23F,lower);quad(out,pose,outer,1.38F,.27F,outer,1.38F,-.27F,outer,.69F,-.23F,outer,.69F,.23F,lower);quad(out,pose,x0,.69F,-.23F,x1,.69F,-.23F,x1,.69F,.23F,x0,.69F,.23F,lining);});}
    private static void boots(PoseStack stack,RenderPlayerEvent.Post<?> event,int tier){int body=dark(body(tier)),trim=trim(tier);event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{box(out,pose,-.29F,.02F,-.19F,-.03F,.36F,.22F,body);box(out,pose,.03F,.02F,-.19F,.29F,.36F,.22F,body);quad(out,pose,-.29F,.27F,-.195F,-.03F,.27F,-.195F,-.03F,.31F,-.195F,-.29F,.31F,-.195F,trim);quad(out,pose,.03F,.27F,-.195F,.29F,.27F,-.195F,.29F,.31F,-.195F,.03F,.31F,-.195F,trim);});}
    private static void filledBand(PoseStack stack,RenderPlayerEvent.Post<?> event,double inner,double outer,double y,int n,int color,double height){event.getSubmitNodeCollector().submitCustomGeometry(stack,RenderTypes.debugFilledBox(),(pose,out)->{for(int i=0;i<n;i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;quad(out,pose,(float)(Math.cos(a)*outer),(float)y,(float)(Math.sin(a)*outer),(float)(Math.cos(b)*outer),(float)y,(float)(Math.sin(b)*outer),(float)(Math.cos(b)*inner),(float)(y+height),(float)(Math.sin(b)*inner),(float)(Math.cos(a)*inner),(float)(y+height),(float)(Math.sin(a)*inner),color);}});}
    private static void box(VertexConsumer out,PoseStack.Pose p,float x0,float y0,float z0,float x1,float y1,float z1,int c){quad(out,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c);quad(out,p,x1,y0,z1,x0,y0,z1,x0,y1,z1,x1,y1,z1,c);quad(out,p,x0,y0,z1,x0,y0,z0,x0,y1,z0,x0,y1,z1,c);quad(out,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,c);quad(out,p,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,c);}
    private static void quad(VertexConsumer out,PoseStack.Pose p,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int c){out.addVertex(p,ax,ay,az).setColor(c);out.addVertex(p,bx,by,bz).setColor(c);out.addVertex(p,cx,cy,cz).setColor(c);out.addVertex(p,dx,dy,dz).setColor(c);}
    private static int hatTier(ItemStack s){if(s.getItem()==ModItems.ARCHMAGE_CROWN.get()||s.getItem()==ModItems.RIFT_CROWN.get())return 3;if(s.getItem()==ModItems.SAGE_HAT.get()||s.getItem()==ModItems.CINDER_HOOD.get()||s.getItem()==ModItems.GLACIER_CIRCLET.get()||s.getItem()==ModItems.TEMPEST_HOOD.get())return 2;if(s.getItem()==ModItems.MAGE_HAT.get())return 1;return 0;}
    private static int robeTier(ItemStack s){if(s.getItem()==ModItems.ARCHMAGE_ROBE.get()||s.getItem()==ModItems.RIFT_ROBE.get())return 3;if(s.getItem()==ModItems.SAGE_ROBE.get()||s.getItem()==ModItems.CINDER_ROBE.get()||s.getItem()==ModItems.GLACIER_ROBE.get()||s.getItem()==ModItems.TEMPEST_ROBE.get())return 2;if(s.getItem()==ModItems.MAGE_ROBE.get())return 1;return 0;}
    private static int robeStyle(ItemStack s){
        if(s.getItem()==ModItems.SAGE_ROBE.get())return 2;
        if(s.getItem()==ModItems.CINDER_ROBE.get())return 3;
        if(s.getItem()==ModItems.GLACIER_ROBE.get())return 4;
        if(s.getItem()==ModItems.TEMPEST_ROBE.get())return 5;
        if(s.getItem()==ModItems.ARCHMAGE_ROBE.get())return 6;
        if(s.getItem()==ModItems.RIFT_ROBE.get())return 7;
        if(s.getItem()==ModItems.MAGE_ROBE.get())return 1;
        return 0;
    }
    private static int bootsTier(ItemStack s){if(s.getItem()==ModItems.FROSTSTEP_BOOTS.get()||s.getItem()==ModItems.TEMPEST_BOOTS.get()||s.getItem()==ModItems.RIFT_BOOTS.get())return 3;if(s.getItem()==ModItems.SKYWALKER_BOOTS.get()||s.getItem()==ModItems.CINDER_BOOTS.get()||s.getItem()==ModItems.GLACIER_BOOTS.get())return 2;if(s.getItem()==ModItems.MAGE_BOOTS.get())return 1;return 0;}
    private static int body(int t){return switch(t){case 1->0xF034234D;case 2->0xF0273D72;case 3->0xF02B153F;default->0xEE302044;};}private static int trim(int t){return switch(t){case 1->0xFFF0B6FF;case 2->0xFF86DFFF;case 3->0xFFFFD56A;default->0xFFE8C0FF;};}private static int dark(int c){int a=c&0xFF000000,r=(int)(((c>>16)&255)*.58),g=(int)(((c>>8)&255)*.58),b=(int)((c&255)*.58);return a|(r<<16)|(g<<8)|b;}private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private static<T>ContextKey<T> key(String p){return new ContextKey<>(Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID,p));}
}
