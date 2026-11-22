package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.List;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Additive blessing increases: greatest at full value, all others at half. */
public final class PactBlessingService {
    private static final ResourceLocation HP = key("pact_health"), ATTACK = key("pact_attack"),
            SPEED = key("pact_speed"), KNOCKBACK = key("pact_knockback");
    private PactBlessingService() {}

    public static double relationFactor(int relation) {
        return Math.clamp(1.0 + (relation - 50) * .005, .75, 1.25);
    }
    public static double focusFactor(PlayerClassProfile profile) {
        ClassFocus focus = profile.equippedFocus().orElse(null);
        if (focus == null || focus.classId() != ClassId.PACT_CASTER) return 1;
        return new double[]{1.05, 1.10, 1.15, 1.20, 1.40}[focus.tier().index()];
    }
    public static double effective(PactCatalog.Patron patron, PactSavedData.Bond bond, String stat) {
        double base = switch (stat) {
            case "hp" -> patron.health();
            case "attack" -> patron.attack();
            case "speed" -> patron.speed();
            case "mining" -> patron.mining();
            case "knockback" -> switch (patron.id()) {
                case "urgen" -> .25;
                case "taron" -> .30;
                case "ormas" -> .55;
                default -> 0.0;
            };
            default -> 0;
        };
        return base * relationFactor(bond.relation())
                * deepeningFactor(patron,bond.deepening());
    }
    /** Provisional first-tier rite bonus affects only the borrowed blessing. */
    public static double deepeningFactor(int depth) {
        return switch(Math.clamp(depth,0,2)) {
            case 1 -> 1.12;
            case 2 -> 1.25;
            default -> 1.0;
        };
    }
    /** Deepening multiplies only that patron's borrowed boon, not base player stats. */
    public static double deepeningFactor(PactCatalog.Patron patron,int depth) {
        if (patron.tier()==1) return deepeningFactor(depth);
        if (patron.tier()==2) return switch(Math.clamp(depth,0,2)) {
            case 1 -> 1.10;
            case 2 -> 1.22;
            default -> 1.0;
        };
        if (patron.tier()==3) return switch(Math.clamp(depth,0,2)) {
            case 1 -> 1.08;
            case 2 -> 1.18;
            default -> 1.0;
        };
        if (patron.tier()==4) return switch(Math.clamp(depth,0,2)) {
            case 1 -> 1.08;
            case 2 -> 1.20;
            default -> 1.0;
        };
        return 1.0;
    }
    public static double combined(PactSavedData.Player state, String stat) {
        List<Double> bonuses = new ArrayList<>();
        for (PactSavedData.Bond b : state.bonds()) {
            PactCatalog.Patron patron = PactCatalog.get(b.id());
            if (patron != null) bonuses.add(effective(patron, b, stat));
        }
        return PactCatalog.aggregate(bonuses);
    }
    public static void apply(ServerPlayer player) {
        if (player.getServer() == null) return;
        float oldMax = player.getMaxHealth(), oldHp = player.getHealth();
        remove(player, HP, Attributes.MAX_HEALTH);
        remove(player, ATTACK, Attributes.ATTACK_DAMAGE);
        remove(player, SPEED, Attributes.MOVEMENT_SPEED);
        remove(player, KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE);
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (profile.currentClass().orElse(null) == ClassId.PACT_CASTER) {
            PactSavedData.Player state = PactSavedData.get(player.getServer()).player(player.getUUID());
            double focus = focusFactor(profile);
            add(player, HP, Attributes.MAX_HEALTH, 20.0 * combined(state,"hp") * focus,
                    AttributeModifier.Operation.ADD_VALUE);
            add(player, ATTACK, Attributes.ATTACK_DAMAGE, combined(state,"attack") * focus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            add(player, SPEED, Attributes.MOVEMENT_SPEED, combined(state,"speed") * focus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            add(player, KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE,
                    combined(state,"knockback"), AttributeModifier.Operation.ADD_VALUE);
        }
        if (oldMax > 0 && player.isAlive()) {
            float ratio = Math.clamp(oldHp / oldMax, 0, 1);
            player.setHealth(Math.max(.5F, Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio)));
        }
    }
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (profile.currentClass().orElse(null) != ClassId.PACT_CASTER) return;
        double bonus = combined(PactSavedData.get(player.getServer()).player(player.getUUID()), "mining")
                * focusFactor(profile);
        if (bonus > 0) event.setNewSpeed((float) Math.min(1000, event.getNewSpeed() * (1.0 + bonus)));
    }
    private static void remove(ServerPlayer player, ResourceLocation id, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
    private static void add(ServerPlayer player, ResourceLocation id, Holder<Attribute> attribute,
            double value, AttributeModifier.Operation operation) {
        if (value == 0 || !Double.isFinite(value)) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.addOrReplacePermanentModifier(new AttributeModifier(id, value, operation));
    }
    private static ResourceLocation key(String key) {
        return ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,key);
    }
}
