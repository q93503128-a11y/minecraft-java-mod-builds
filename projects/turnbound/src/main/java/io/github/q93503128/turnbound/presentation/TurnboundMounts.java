package io.github.q93503128.turnbound.presentation;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry and renderer for TURNBOUND's external-asset-based physical mounts. */
public final class TurnboundMounts {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Turnbound.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RoadhornMountEntity>> ROADHORN =
            ENTITIES.registerEntityType(
                    "roadhorn_mount",
                    RoadhornMountEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder.sized(1.08F, 1.92F).clientTrackingRange(12).updateInterval(1));

    private TurnboundMounts() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(TurnboundMounts::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 44.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.JUMP_STRENGTH, 1.05D)
                .add(Attributes.STEP_HEIGHT, 1.25D)
                .add(Attributes.SAFE_FALL_DISTANCE, 8.0D)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.35D)
                .build();
        event.put(ROADHORN.get(), attributes);
    }

    public static RoadhornMountEntity spawn(ServerLevel level, Vec3 pos, float yaw) {
        if (level == null || pos == null) return null;
        RoadhornMountEntity mount = new RoadhornMountEntity(ROADHORN.get(), level);
        mount.setPos(pos.x, pos.y, pos.z);
        mount.setYRot(yaw);
        mount.setYHeadRot(yaw);
        mount.setYBodyRot(yaw);
        if (!level.noCollision(mount)) return null;
        level.addFreshEntity(mount);
        return mount;
    }

    @EventBusSubscriber(modid = Turnbound.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {}

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ROADHORN.get(), context -> {
                var model = new DefaultedEntityGeoModel<RoadhornMountEntity>(
                        Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "mount/roadhorn_mount"))
                        .withAltAnimations(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "mount/roadhorn_mount"))
                        .withAltTexture(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "elite/elite_cv_cavehorn_ravager"));
                return new GeoEntityRenderer<>(context, model).withScale(1.04F);
            });
        }
    }
}
