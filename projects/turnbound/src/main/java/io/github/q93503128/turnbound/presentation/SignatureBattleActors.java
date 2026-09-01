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

/**
 * Signature-only visual variants. These actors never alter combat identity or stats; they only add the
 * small canonical attachment authored for the equipped Signature item.
 */
public final class SignatureBattleActors {
    public static final String RULE_PREFIX = "VISUAL_SIGNATURE:";

    private record Spec(String visualId, String combatantId, String signatureId, String modelPath,
                        String texturePath, String animationPath, String animationPrefix,
                        float width, float height, float scale) { }

    private static final List<Spec> SPECS = List.of(
            new Spec("P01_SIG","P01","sig_p01_unending_vow","p01_kyren_signature","kyren","p01_kyren","p01_kyren",.66F,1.84F,1.000F),
            new Spec("P02_SIG","P02","sig_p02_moving_hand","p02_lumea_signature","lumea","p02_lumea","p02_lumea",.61F,1.66F,.902F),
            new Spec("P03_SIG","P03","sig_p03_gate_shield","p03_bram_signature","bram","p03_bram","p03_bram",.74F,1.93F,1.049F),
            new Spec("P04_SIG","P04","sig_p04_last_ember_chalice","p04_elysia_signature","elysia","p04_elysia","p04_elysia",.63F,1.69F,.918F),
            new Spec("P05_SIG","P05","sig_p05_never_late_scope","p05_lynette_signature","lynette","p05_lynette","p05_lynette",.62F,1.72F,.935F),
            new Spec("P06_SIG","P06","sig_p06_unnamed_epitaph","p06_morwen_signature","morwen","p06_morwen","p06_morwen",.62F,1.77F,.962F),
            new Spec("P07_SUMMON_SIG","P07_SUMMON","sig_p07_second_contract","p07_toto_signature","toto","common",null,.82F,1.15F,.720F),
            new Spec("P08_SIG","P08","sig_p08_blood_grip","p08_raze_signature","raze","p08_raze","p08_raze",.72F,1.88F,1.022F));

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Turnbound.MOD_ID);
    private static final Map<String, Spec> BY_VISUAL = new LinkedHashMap<>();
    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<BattleActorEntity>>> ACTORS = new LinkedHashMap<>();

    static {
        for (Spec spec : SPECS) {
            BY_VISUAL.put(spec.visualId(), spec);
            ACTORS.put(spec.visualId(), ENTITIES.registerEntityType("actor_" + spec.visualId().toLowerCase(), BattleActorEntity::new,
                    MobCategory.MISC, builder -> builder.sized(spec.width(), spec.height()).clientTrackingRange(16).updateInterval(1)));
        }
    }

    private SignatureBattleActors() { }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(SignatureBattleActors::attributes);
    }

    public static String rule(String signatureId) { return RULE_PREFIX + signatureId; }

    /** Resolves a visual variant only when the exact owner Signature is equipped. */
    public static String visualId(String combatantId, Iterable<String> rules) {
        if (combatantId == null || rules == null) return combatantId;
        for (Spec spec : SPECS) {
            if (!spec.combatantId().equals(combatantId)) continue;
            String wanted = rule(spec.signatureId());
            for (String value : rules) if (wanted.equals(value)) return spec.visualId();
        }
        return combatantId;
    }

    public static boolean contains(String visualId) { return ACTORS.containsKey(visualId); }

    /** Same canonical animation prefix as the base hero; Toto intentionally keeps hero/common. */
    public static String heroAnimationPrefix(EntityType<?> type) {
        if (type == null) return null;
        for (var entry : ACTORS.entrySet()) {
            if (entry.getValue().get() == type) return BY_VISUAL.get(entry.getKey()).animationPrefix();
        }
        return null;
    }

    public static BattleActorEntity spawn(ServerLevel level, String visualId, Vec3 pos, float yaw) {
        DeferredHolder<EntityType<?>, EntityType<BattleActorEntity>> holder = ACTORS.get(visualId);
        if (holder == null) return null;
        BattleActorEntity actor = new BattleActorEntity(holder.get(), level);
        actor.setPos(pos.x, pos.y, pos.z);
        actor.setYRot(yaw); actor.setYHeadRot(yaw); actor.setYBodyRot(yaw);
        level.addFreshEntity(actor);
        return actor;
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build();
        for (var holder : ACTORS.values()) event.put(holder.get(), attributes);
    }

    private static Identifier modelRoot(Spec spec) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/signature/" + spec.modelPath());
    }
    private static Identifier textureRoot(Spec spec) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + spec.texturePath());
    }
    private static Identifier animationRoot(Spec spec) {
        return Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "hero/" + spec.animationPath());
    }

    @EventBusSubscriber(modid = Turnbound.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() { }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            for (var entry : ACTORS.entrySet()) {
                Spec spec = BY_VISUAL.get(entry.getKey());
                event.registerEntityRenderer(entry.getValue().get(), context -> {
                    var model = new DefaultedEntityGeoModel<BattleActorEntity>(modelRoot(spec))
                            .withAltAnimations(animationRoot(spec)).withAltTexture(textureRoot(spec));
                    return new GeoEntityRenderer<>(context, model).withScale(spec.scale());
                });
            }
        }
    }
}
