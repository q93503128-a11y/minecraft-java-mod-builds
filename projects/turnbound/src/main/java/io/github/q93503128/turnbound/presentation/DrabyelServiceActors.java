package io.github.q93503128.turnbound.presentation;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Externally sourced, non-combat service NPC visuals for the New Drabyel free-roam hub. */
public final class DrabyelServiceActors {
    private record Spec(String visualAsset,String entityId,String modelPath,String texturePath,float width,float height,float scale){}

    private static final List<Spec> SPECS=List.of(
            new Spec("DRABYEL_MERCHANT","drabyel_merchant","npc/drabyel_merchant","npc/drabyel_merchant",0.64F,1.86F,1.0F),
            new Spec("DRABYEL_BLACKSMITH","drabyel_blacksmith","npc/drabyel_blacksmith","npc/drabyel_blacksmith",0.68F,1.90F,1.04F)
    );

    public static final DeferredRegister.Entities ENTITIES=DeferredRegister.createEntities(Turnbound.MOD_ID);
    private static final Map<String,Spec> BY_ASSET=new LinkedHashMap<>();
    private static final Map<String,DeferredHolder<EntityType<?>,EntityType<BattleActorEntity>>> ACTORS=new LinkedHashMap<>();

    static{
        for(Spec spec:SPECS){
            BY_ASSET.put(spec.visualAsset(),spec);
            ACTORS.put(spec.visualAsset(),ENTITIES.registerEntityType(spec.entityId(),BattleActorEntity::new,
                    MobCategory.MISC,builder->builder.sized(spec.width(),spec.height()).clientTrackingRange(16).updateInterval(1)));
        }
    }

    private DrabyelServiceActors(){}

    public static void register(IEventBus bus){
        ENTITIES.register(bus);
        bus.addListener(DrabyelServiceActors::attributes);
    }

    public static boolean supports(String visualAsset){return ACTORS.containsKey(visualAsset);}

    public static boolean serviceAnimationType(EntityType<?> type){
        if(type==null)return false;
        for(var holder:ACTORS.values())if(holder.get()==type)return true;
        return false;
    }

    public static BattleActorEntity spawn(ServerLevel level,String visualAsset,Vec3 pos,float yaw){
        var holder=ACTORS.get(visualAsset);
        if(holder==null)return null;
        BattleActorEntity actor=new BattleActorEntity(holder.get(),level);
        actor.setPos(pos.x,pos.y,pos.z);
        actor.setYRot(yaw);actor.setYHeadRot(yaw);actor.setYBodyRot(yaw);
        actor.setFieldWalking(false);
        level.addFreshEntity(actor);
        return actor;
    }

    private static void attributes(EntityAttributeCreationEvent event){
        AttributeSupplier attributes=Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,20.0)
                .add(Attributes.MOVEMENT_SPEED,0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,1.0)
                .build();
        for(var holder:ACTORS.values())event.put(holder.get(),attributes);
    }

    private static Identifier model(Spec spec){return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,spec.modelPath());}
    private static Identifier texture(Spec spec){return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,spec.texturePath());}
    private static Identifier animation(){return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"npc/drabyel_service");}

    @EventBusSubscriber(modid=Turnbound.MOD_ID,value=Dist.CLIENT)
    public static final class ClientEvents{
        private ClientEvents(){}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
            for(var entry:ACTORS.entrySet()){
                Spec spec=BY_ASSET.get(entry.getKey());
                event.registerEntityRenderer(entry.getValue().get(),context->{
                    var model=new DefaultedEntityGeoModel<BattleActorEntity>(model(spec))
                            .withAltAnimations(animation()).withAltTexture(texture(spec));
                    return new GeoEntityRenderer<>(context,model).withScale(spec.scale());
                });
            }
        }
    }
}
