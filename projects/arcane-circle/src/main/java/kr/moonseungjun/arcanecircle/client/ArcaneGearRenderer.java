package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Extracts equipment/motion state; actual alpha.26 visuals live in dedicated renderers. */
public final class ArcaneGearRenderer {
    private static final ContextKey<Integer> ROBE_STYLE=key("robe_style_v3"),HAT_STYLE=key("hat_style_v3"),BOOT_STYLE=key("boot_style_v3"),CAST_FAMILY=key("cast_family_v3");
    private static final ContextKey<Float> MOVE_X=key("gear_move_x_v3"),MOVE_Z=key("gear_move_z_v3"),PHASE=key("gear_move_phase_v3"),CAST_PROGRESS=key("cast_progress_v3");
    private static final ContextKey<Boolean> CAST_RELEASE=key("cast_release_v3");
    private ArcaneGearRenderer(){}

    public static void registerStateModifiers(RegisterRenderStateModifiersEvent event){
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier(){
            @Override public<T extends Avatar&ClientAvatarEntity>void accept(T avatar,AvatarRenderState state){
                state.setRenderData(ROBE_STYLE,robeStyle(avatar.getItemBySlot(EquipmentSlot.CHEST)));
                state.setRenderData(HAT_STYLE,hatStyle(avatar.getItemBySlot(EquipmentSlot.HEAD)));
                state.setRenderData(BOOT_STYLE,bootStyle(avatar.getItemBySlot(EquipmentSlot.FEET)));
                Vec3 v=avatar.getDeltaMovement();double yaw=Math.toRadians(avatar.yBodyRot);float lx=(float)(v.x*Math.cos(yaw)+v.z*Math.sin(yaw)),lz=(float)(-v.x*Math.sin(yaw)+v.z*Math.cos(yaw));float speed=(float)Math.min(.42,Math.sqrt(v.x*v.x+v.z*v.z));state.setRenderData(MOVE_X,lx);state.setRenderData(MOVE_Z,lz);state.setRenderData(PHASE,(float)Math.sin(avatar.tickCount*.72)*speed);
                WorldMagicTracker.CasterPoseSnapshot cast=WorldMagicTracker.castingPose(avatar.getUUID());state.setRenderData(CAST_FAMILY,cast.family());state.setRenderData(CAST_PROGRESS,cast.progress());state.setRenderData(CAST_RELEASE,cast.release());
            }
        });
    }

    public static void onPlayerRender(RenderPlayerEvent.Post<?> event){
        int robe=event.getRenderState().getRenderDataOrDefault(ROBE_STYLE,0),hat=event.getRenderState().getRenderDataOrDefault(HAT_STYLE,0),boots=event.getRenderState().getRenderDataOrDefault(BOOT_STYLE,0),family=event.getRenderState().getRenderDataOrDefault(CAST_FAMILY,0);
        if(robe<=0&&hat<=0&&boots<=0&&family<=0)return;
        float mx=event.getRenderState().getRenderDataOrDefault(MOVE_X,0F),mz=event.getRenderState().getRenderDataOrDefault(MOVE_Z,0F),phase=event.getRenderState().getRenderDataOrDefault(PHASE,0F),progress=event.getRenderState().getRenderDataOrDefault(CAST_PROGRESS,0F);boolean release=event.getRenderState().getRenderDataOrDefault(CAST_RELEASE,false);
        PoseStack stack=event.getPoseStack();stack.pushPose();ArcaneRegaliaRenderer.render(stack,event,robe,hat,boots,mx,mz,phase,family,progress,release);ArcaneCastingPerformance.render(stack,event,robe>0?robe:hat,family,progress,release);stack.popPose();
    }

    private static int robeStyle(ItemStack s){if(s.getItem()==ModItems.SAGE_ROBE.get())return 2;if(s.getItem()==ModItems.CINDER_ROBE.get())return 3;if(s.getItem()==ModItems.GLACIER_ROBE.get())return 4;if(s.getItem()==ModItems.TEMPEST_ROBE.get())return 5;if(s.getItem()==ModItems.ARCHMAGE_ROBE.get())return 6;if(s.getItem()==ModItems.RIFT_ROBE.get())return 7;if(s.getItem()==ModItems.MAGE_ROBE.get())return 1;return 0;}
    private static int hatStyle(ItemStack s){if(s.getItem()==ModItems.SAGE_HAT.get())return 2;if(s.getItem()==ModItems.CINDER_HOOD.get())return 3;if(s.getItem()==ModItems.GLACIER_CIRCLET.get())return 4;if(s.getItem()==ModItems.TEMPEST_HOOD.get())return 5;if(s.getItem()==ModItems.ARCHMAGE_CROWN.get())return 6;if(s.getItem()==ModItems.RIFT_CROWN.get())return 7;if(s.getItem()==ModItems.MAGE_HAT.get())return 1;return 0;}
    private static int bootStyle(ItemStack s){if(s.getItem()==ModItems.CINDER_BOOTS.get())return 3;if(s.getItem()==ModItems.GLACIER_BOOTS.get())return 4;if(s.getItem()==ModItems.TEMPEST_BOOTS.get())return 5;if(s.getItem()==ModItems.FROSTSTEP_BOOTS.get())return 6;if(s.getItem()==ModItems.RIFT_BOOTS.get())return 7;if(s.getItem()==ModItems.SKYWALKER_BOOTS.get())return 2;if(s.getItem()==ModItems.MAGE_BOOTS.get())return 1;return 0;}
    private static<T>ContextKey<T> key(String path){return new ContextKey<>(Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID,path));}
}
