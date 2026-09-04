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
            "F01","F01_ALT","F02","F03","F04",
            "E001","E002","E003","E004","E005","E006","E007","E008","E009","E010","E011","E012","E013","E014",
            "EL01","EL02","EL03","EL04",
            "B01","B02","B03","B04","B05");

    private static final Map<String, String> HERO_PATH = Map.ofEntries(
            Map.entry("P01", "kyren"), Map.entry("P02", "lumea"), Map.entry("P03", "bram"),
            Map.entry("P04", "elysia"), Map.entry("P05", "lynette"), Map.entry("P06", "morwen"),
            Map.entry("P07", "marion"), Map.entry("P08", "raze"), Map.entry("P07_SUMMON", "toto"));

    /** Canon §17.6: filename <character_id>.animation.json and clip animation.<character_id>.<clip>. */
    private static final Map<String, String> HERO_ANIMATION = Map.of(
            "P01", "p01_kyren", "P02", "p02_lumea", "P03", "p03_bram", "P04", "p04_elysia",
            "P05", "p05_lynette", "P06", "p06_morwen", "P07", "p07_marion", "P08", "p08_raze");

    private static final Map<String, String> FILLER_PATH = Map.of(
            "F01", "f01_militia_male", "F01_ALT", "f01_militia_female",
            "F02", "f02_field_apprentice", "F03", "f03_border_hunter", "F04", "f04_shield_mercenary");

    private static final Map<String, String> ENEMY_PATH = Map.ofEntries(
            Map.entry("E001", "e001_rotted_walker"), Map.entry("E002", "e002_bone_marksman"),
            Map.entry("E003", "e003_unstable_burster"), Map.entry("E004", "e004_road_bandit"),
            Map.entry("E005", "e005_field_medic"), Map.entry("E006", "e006_moss_boar"),
            Map.entry("E007", "e007_spore_lantern"), Map.entry("E008", "e008_root_guard"),
            Map.entry("E009", "e009_aqueduct_sentry"), Map.entry("E010", "e010_flood_leech"),
            Map.entry("E011", "e011_rusted_support"), Map.entry("E012", "e012_ash_hound"),
            Map.entry("E013", "e013_cinder_adept"), Map.entry("E014", "e014_lava_driller"));

    private static final Map<String, String> ELITE_PATH = Map.of(
            "EL01", "el01_rot_captain", "EL02", "el02_briar_stag",
            "EL03", "el03_rusted_centurion", "EL04", "el04_magma_drill_king");

    private static final Map<String, String> BOSS_PATH = Map.of(
            "B01", "graul", "B02", "verna", "B03", "oro7", "B04", "kolvak", "B05", "serak");

    private static final Map<String, String> SPECIAL_FILLER_ANIMATION = Map.of(
            "F02", "f02_field_apprentice", "F03", "f03_border_hunter", "F04", "f04_shield_mercenary");

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

    /** Returns canonical hero animation prefix for a concrete registered actor type; non-core actors return null. */
    public static String heroAnimationPrefix(EntityType<?> type) {
        if (type == null) return null;
        for (var entry : ACTORS.entrySet()) {
            if (entry.getValue().get() == type) return HERO_ANIMATION.get(entry.getKey());
        }
        return null;
    }

    /** True only for the five authored boss actor types that provide boss.hit_light / boss.hit_heavy clips. */
    public static boolean bossAnimationType(EntityType<?> type) {
        if (type == null) return false;
        for (var entry : ACTORS.entrySet()) {
            if (entry.getValue().get() == type) return BOSS_PATH.containsKey(entry.getKey());
        }
        return false;
    }

    public static BattleActorEntity spawn(ServerLevel level, String combatantId, Vec3 pos, float yaw) {
        String visualId = "F01".equals(combatantId) && level.getRandom().nextBoolean() ? "F01_ALT" : combatantId;
        DeferredHolder<EntityType<?>, EntityType<BattleActorEntity>> holder = ACTORS.get(visualId);
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
                .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build();
        for (var holder : ACTORS.values()) event.put(holder.get(), attributes);
    }

    private static float width(String id) {
        return switch (id) {
            case "P01" -> 0.66F; case "P02" -> 0.61F; case "P03" -> 0.74F; case "P04" -> 0.63F;
            case "P05", "P06" -> 0.62F; case "P07" -> 0.60F; case "P08" -> 0.72F; case "P07_SUMMON" -> 0.82F;
            case "F01", "F01_ALT" -> 0.62F; case "F02" -> 0.59F; case "F03" -> 0.61F; case "F04" -> 0.70F;
            case "B01" -> 1.80F; case "B02" -> 1.65F; case "B03" -> 1.45F; case "B04" -> 1.70F; case "B05" -> 0.78F;
            case "E006", "E010", "E012", "EL02" -> 1.10F; case "E003", "E014" -> 0.90F;
            case "EL01" -> 0.85F; case "EL03" -> 0.90F; case "EL04" -> 1.17F; default -> 0.72F;
        };
    }

    private static float height(String id) {
        return switch (id) {
            case "P01" -> 1.84F; case "P02" -> 1.66F; case "P03" -> 1.93F; case "P04" -> 1.69F;
            case "P05" -> 1.72F; case "P06" -> 1.77F; case "P07" -> 1.64F; case "P08" -> 1.88F; case "P07_SUMMON" -> 1.15F;
            case "F01", "F01_ALT" -> 1.76F; case "F02" -> 1.67F; case "F03" -> 1.75F; case "F04" -> 1.82F;
            case "B01" -> 2.30F; case "B02" -> 2.80F; case "B03" -> 3.10F; case "B04" -> 3.40F; case "B05" -> 2.05F;
            case "E006", "E010", "E012", "EL02" -> 1.35F; case "E003", "E014" -> 2.15F;
            case "EL01" -> 2.36F; case "EL03" -> 2.50F; case "EL04" -> 2.80F; default -> 2.0F;
        };
    }

    private static float renderScale(String id) {
        return switch (id) {
            case "P01" -> 1.000F; case "P02" -> 0.902F; case "P03" -> 1.049F; case "P04" -> 0.918F;
            case "P05" -> 0.935F; case "P06" -> 0.962F; case "P07" -> 0.891F; case "P08" -> 1.022F; case "P07_SUMMON" -> 0.720F;
            case "F01", "F01_ALT" -> 0.957F; case "F02" -> 0.908F; case "F03" -> 0.951F; case "F04" -> 0.989F;
            case "B01" -> 1.15F; case "B02" -> 1.20F; case "B03" -> 1.28F; case "B04" -> 1.35F; case "B05" -> 1.08F;
            case "EL01" -> 1.18F; case "EL03" -> 1.25F; default -> 1.0F;
        };
    }

    private static String archetype(String id) {
        return switch (id) {
            case "P03","F04" -> "shield"; case "P05","F03" -> "ranger";
            case "P02","P04","P06","P07","F02" -> "caster"; case "P08" -> "brute";
            case "P07_SUMMON" -> "beast"; default -> "blade";
        };
    }

    private static Identifier modelRoot(String id) {
        String hero = HERO_PATH.get(id); if (hero != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + hero);
        String filler = FILLER_PATH.get(id); if (filler != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "filler/" + filler);
        String enemy = ENEMY_PATH.get(id); if (enemy != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "enemy/" + enemy);
        String elite = ELITE_PATH.get(id); if (elite != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "elite/" + elite);
        String boss = BOSS_PATH.get(id); if (boss != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/" + boss);
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/" + archetype(id));
    }

    private static Identifier animationRoot(String id) {
        String heroAnimation = HERO_ANIMATION.get(id);
        if (heroAnimation != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + heroAnimation);
        if (HERO_PATH.containsKey(id)) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/common");
        String filler = SPECIAL_FILLER_ANIMATION.get(id); if (filler != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "filler/" + filler);
        if (FILLER_PATH.containsKey(id)) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "filler/common");
        String enemy = ENEMY_PATH.get(id); if (enemy != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "enemy/" + enemy);
        String elite = ELITE_PATH.get(id); if (elite != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "elite/" + elite);
        String boss = BOSS_PATH.get(id); if (boss != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/" + boss);
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/common");
    }

    private static Identifier textureRoot(String id) {
        String hero = HERO_PATH.get(id); if (hero != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + hero);
        String filler = FILLER_PATH.get(id); if (filler != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "filler/" + filler);
        String enemy = ENEMY_PATH.get(id); if (enemy != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "enemy/" + enemy);
        String elite = ELITE_PATH.get(id); if (elite != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "elite/" + elite);
        String boss = BOSS_PATH.get(id); if (boss != null) return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "boss/" + boss);
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "battle/atlas");
    }

    @EventBusSubscriber(modid = Turnbound.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() { }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            for (var entry : ACTORS.entrySet()) {
                String id = entry.getKey(); var holder = entry.getValue();
                event.registerEntityRenderer(holder.get(), context -> {
                    var model = new DefaultedEntityGeoModel<BattleActorEntity>(modelRoot(id))
                            .withAltAnimations(animationRoot(id)).withAltTexture(textureRoot(id));
                    return new GeoEntityRenderer<>(context, model).withScale(renderScale(id));
                });
            }
        }
    }
}
