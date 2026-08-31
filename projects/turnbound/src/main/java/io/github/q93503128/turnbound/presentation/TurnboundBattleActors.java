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
            "P01","P02","P03","P04","P05","P06","P07","P08","P07_SUMMON",
            "F01","F02","F03","F04",
            "E001","E002","E003","E004","E005","E006","E007","E008","E009","E010","E011","E012","E013","E014",
            "EL01","EL02","EL03","EL04",
            "B01","B02","B03","B04","B05");

    private static final Map<String, String> HERO_PATH = Map.ofEntries(
            Map.entry("P01", "kyren"), Map.entry("P02", "lumea"), Map.entry("P03", "bram"),
            Map.entry("P04", "elysia"), Map.entry("P05", "lynette"), Map.entry("P06", "morwen"),
            Map.entry("P07", "marion"), Map.entry("P08", "raze"), Map.entry("P07_SUMMON", "toto"));

    private static final Map<String, String> BOSS_PATH = Map.of(
            "B01", "graul", "B02", "verna", "B03", "oro7", "B04", "kolvak", "B05", "serak");

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
        return switch (id) {
            case "P01" -> 0.66F;
            case "P02" -> 0.61F;
            case "P03" -> 0.74F;
            case "P04" -> 0.63F;
            case "P05", "P06" -> 0.62F;
            case "P07" -> 0.60F;
            case "P08" -> 0.72F;
            case "P07_SUMMON" -> 0.82F;
            case "B01" -> 1.80F;
            case "B02" -> 1.65F;
            case "B03" -> 1.45F;
            case "B04" -> 1.70F;
            case "B05" -> 0.78F;
            case "E006", "E010", "E012", "EL02" -> 1.10F;
            case "E003", "E014", "EL04" -> 0.90F;
            default -> 0.72F;
        };
    }

    private static float height(String id) {
        return switch (id) {
            case "P01" -> 1.84F;
            case "P02" -> 1.66F;
            case "P03" -> 1.93F;
            case "P04" -> 1.69F;
            case "P05" -> 1.72F;
            case "P06" -> 1.77F;
            case "P07" -> 1.64F;
            case "P08" -> 1.88F;
            case "P07_SUMMON" -> 1.15F;
            case "B01" -> 2.30F;
            case "B02" -> 2.80F;
            case "B03" -> 3.10F;
            case "B04" -> 3.40F;
            case "B05" -> 2.05F;
            case "E006", "E010", "E012", "EL02" -> 1.35F;
            case "E003", "E014", "EL04" -> 2.15F;
            default -> 2.0F;
        };
    }

    private static float renderScale(String id) {
        return switch (id) {
            case "P01" -> 1.000F;
            case "P02" -> 0.902F;
            case "P03" -> 1.049F;
            case "P04" -> 0.918F;
            case "P05" -> 0.935F;
            case "P06" -> 0.962F;
            case "P07" -> 0.891F;
            case "P08" -> 1.022F;
            case "P07_SUMMON" -> 0.720F;
            case "B01" -> 1.15F;
            case "B02" -> 1.20F;
            case "B03" -> 1.28F;
            case "B04" -> 1.35F;
            case "B05" -> 1.08F;
            default -> 1.0F;
        };
    }

    private static String archetype(String id) {
        return switch (id) {
            case "P03","F04","E008","EL03" -> "shield";
            case "P05","F03","E002" -> "ranger";
            case "P02","P04","P06","P07","F02","E005","E007","E013" -> "caster";
            case "P08","E003","E014","EL04" -> "brute";
            case "P07_SUMMON","E006","E010","E012","EL02" -> "beast";
            case "E011" -> "machine";
            default -> "blade";
        };
    }

    private static Identifier modelRoot(String id) {
        String hero = HERO_PATH.get(id);
        if (hero != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + hero);
        String boss = BOSS_PATH.get(id);
        if (boss != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/" + boss);
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/" + archetype(id));
    }

    private static Identifier animationRoot(String id) {
        if (HERO_PATH.containsKey(id)) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/common");
        if (BOSS_PATH.containsKey(id)) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/common");
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/common");
    }

    private static Identifier textureRoot(String id) {
        String hero = HERO_PATH.get(id);
        if (hero != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + hero);
        String boss = BOSS_PATH.get(id);
        if (boss != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/" + boss);
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/atlas");
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
                    var model = new DefaultedEntityGeoModel<BattleActorEntity>(modelRoot(id))
                            .withAltAnimations(animationRoot(id))
                            .withAltTexture(textureRoot(id));
                    return new GeoEntityRenderer<>(context, model).withScale(renderScale(id));
                });
            }
        }
    }
}
