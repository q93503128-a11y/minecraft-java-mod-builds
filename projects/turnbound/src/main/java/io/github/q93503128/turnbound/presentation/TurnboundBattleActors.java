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
import java.util.Locale;
import java.util.Map;

/** Registry for authored v0.4 battle presentation entities. */
public final class TurnboundBattleActors {
    private static final List<String> IDS = List.of(
            "P01","P02","P03","P04","P05","P06","P07","P08","F01","F02","F03","F04",
            "E001","E002","E003","E004","E005","E006","E007","E008","E009","E010","E011","E012","E013","E014",
            "B01","B02","B03","B04","B05");

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Turnbound.MOD_ID);
    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<BattleActorEntity>>> ACTORS = new LinkedHashMap<>();

    static {
        for (String id : IDS) {
            ACTORS.put(id, ENTITIES.registerEntityType("actor_" + id.toLowerCase(Locale.ROOT), BattleActorEntity::new,
                    MobCategory.MISC, builder -> builder.sized(width(id), height(id)).clientTrackingRange(16).updateInterval(1)));
        }
    }

    private TurnboundBattleActors() { }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(TurnboundBattleActors::attributes);
    }

    public static boolean contains(String combatantId) { return ACTORS.containsKey(combatantId); }

    public static BattleActorEntity spawn(ServerLevel level, String combatantId, Vec3 pos, float yaw) {
        DeferredHolder<EntityType<?>, EntityType<BattleActorEntity>> holder = ACTORS.get(combatantId);
        if (holder == null) return null;
        BattleActorEntity actor = new BattleActorEntity(holder.get(), level);
        actor.setPos(pos.x, pos.y, pos.z);
        actor.setYRot(yaw);
        actor.setYHeadRot(yaw);
        actor.setYBodyRot(yaw);
        level.addFreshEntity(actor);
        return actor;
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .build();
        for (var holder : ACTORS.values()) event.put(holder.get(), attributes);
    }

    private static float width(String id) {
        if (id.startsWith("B")) return 1.15F;
        if (id.equals("E006") || id.equals("E010") || id.equals("E012")) return 1.10F;
        if (id.equals("E003") || id.equals("E014") || id.equals("P08")) return 0.90F;
        return 0.72F;
    }

    private static float height(String id) {
        if (id.startsWith("B")) return 2.55F;
        if (id.equals("E006") || id.equals("E010") || id.equals("E012")) return 1.35F;
        if (id.equals("E003") || id.equals("E014") || id.equals("P08")) return 2.15F;
        return 2.0F;
    }

    private static String archetype(String id) {
        return switch (id) {
            case "P03","F04","E008" -> "shield";
            case "P05","F03","E002" -> "ranger";
            case "P02","P04","P06","P07","F02","E005","E007","E013" -> "caster";
            case "P08","E003","E014","B01","B04" -> "brute";
            case "E006","E010","E012","B02" -> "beast";
            case "E011","B03","B05" -> "machine";
            default -> "blade";
        };
    }

    @EventBusSubscriber(modid = Turnbound.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() { }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            for (var entry : ACTORS.entrySet()) {
                String id = entry.getKey();
                var holder = entry.getValue();
                event.registerEntityRenderer(holder.get(), context -> {
                    var model = new DefaultedEntityGeoModel<BattleActorEntity>(
                            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/" + archetype(id)))
                            .withAltAnimations(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/common"))
                            .withAltTexture(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/atlas"));
                    return new GeoEntityRenderer<>(context, model).withScale(id.startsWith("B") ? 1.22F : 1.0F);
                });
            }
        }
    }
}
