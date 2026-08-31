package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime reaction, stagger recovery, and pain-suppression mechanics for neural augmentations. */
public final class NeuralResponseService {
    private static final Identifier CRISIS_MOVE = id("reflex_crisis_move");
    private static final Identifier CRISIS_ATTACK = id("reflex_crisis_attack");
    private static final double DODGE_TURN_DOT = Math.cos(Math.toRadians(32.0D));
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        Vec3 lastDirection = Vec3.ZERO;
        Vec3 preHitMotion = Vec3.ZERO;
        double lastHorizontalSpeed;
        int dodgeTicks;
        int dodgeCooldown;
        int recoveryTicks;
        boolean restoreMomentum;
        int crisisTicks;
        int crisisCooldown;
    }

    private NeuralResponseService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        TitanPlayerData.AugmentInstance reflex = state.firstInstalledInstance("reflex_accelerator");
        TitanPlayerData.AugmentInstance pain = state.firstInstalledInstance("pain_suppressor");

        if (runtime.dodgeCooldown > 0) runtime.dodgeCooldown--;
        if (runtime.dodgeTicks > 0) runtime.dodgeTicks--;
        if (runtime.crisisCooldown > 0) runtime.crisisCooldown--;
        if (runtime.crisisTicks > 0) runtime.crisisTicks--;

        if (reflex != null) detectDodgeWindow(player, runtime, reflex.enhancement());
        else resetDodgeTracking(runtime);

        if (runtime.recoveryTicks > 0) {
            applyRecovery(player, runtime, reflex, pain);
            runtime.recoveryTicks--;
        }

        boolean crisisActive = reflex != null && reflex.enhancement() >= 10 && runtime.crisisTicks > 0;
        setModifier(player, Attributes.MOVEMENT_SPEED, CRISIS_MOVE, crisisActive, 0.10D);
        setModifier(player, Attributes.ATTACK_SPEED, CRISIS_ATTACK, crisisActive, 0.15D);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        float incoming = event.getAmount();
        if (incoming <= 1.0E-4F) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        TitanPlayerData.AugmentInstance reflex = state.firstInstalledInstance("reflex_accelerator");
        TitanPlayerData.AugmentInstance pain = state.firstInstalledInstance("pain_suppressor");

        runtime.preHitMotion = player.getDeltaMovement();
        runtime.restoreMomentum = false;

        if (reflex != null && reflex.enhancement() >= 5 && runtime.dodgeTicks > 0
                && spendReflex(player, state, 0.50D, 0.35D)) {
            event.setAmount(incoming * 0.55F);
            event.getContainer().setShouldCauseSideEffects(false);
            runtime.dodgeTicks = 0;
            runtime.recoveryTicks = Math.max(runtime.recoveryTicks, 2);
            runtime.restoreMomentum = true;
            data.addMasteryXp(player, "reflex_accelerator", 2);
            incoming = event.getAmount();
        }

        double predictedHealth = Math.max(0.0D, player.getHealth() - incoming);
        double predictedRatio = predictedHealth / Math.max(1.0D, player.getMaxHealth());
        boolean painActive = pain != null && predictedRatio <= painThreshold(pain.enhancement());

        if ((reflex != null && reflex.enhancement() >= 7) || painActive) {
            runtime.recoveryTicks = Math.max(runtime.recoveryTicks,
                    painActive ? (pain.enhancement() >= 5 ? 5 : 7) : 4);
            runtime.restoreMomentum = true;
        }

        if (pain != null && pain.enhancement() >= 10 && predictedHealth > 0.0D && predictedRatio <= 0.25D) {
            event.getContainer().setShouldCauseSideEffects(false);
            runtime.recoveryTicks = Math.max(runtime.recoveryTicks, 2);
            runtime.restoreMomentum = true;
        }
    }

    /** Final-damage stage: only the crisis trigger needs post-mitigation health prediction. */
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        float damage = event.getNewDamage();
        if (damage <= 1.0E-4F) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        TitanPlayerData.AugmentInstance reflex = state.firstInstalledInstance("reflex_accelerator");
        if (reflex == null || reflex.enhancement() < 10) return;

        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        double predictedHealth = player.getHealth() - damage;
        if (predictedHealth <= 0.0D || predictedHealth > player.getMaxHealth() * 0.30D || runtime.crisisCooldown > 0) return;
        if (!spendReflex(player, state, 1.50D, 0.90D)) return;

        runtime.crisisTicks = 60;
        runtime.crisisCooldown = 240;
        runtime.dodgeTicks = Math.max(runtime.dodgeTicks, 4);
        data.addMasteryXp(player, "reflex_accelerator", 3);
    }

    /** +7 pain suppressor milestone: reduce every sanity loss to 65% of its original drain. */
    public static double sanityDrainMultiplier(TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance pain = state.firstInstalledInstance("pain_suppressor");
        return pain != null && pain.enhancement() >= 7 ? 0.65D : 1.0D;
    }

    private static void detectDodgeWindow(ServerPlayer player, RuntimeState runtime, int enhancement) {
        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        double speed = Math.sqrt(horizontal.x * horizontal.x + horizontal.z * horizontal.z);
        Vec3 direction = speed > 1.0E-6D ? horizontal.scale(1.0D / speed) : Vec3.ZERO;

        if (enhancement >= 5 && runtime.dodgeCooldown <= 0 && speed >= 0.18D
                && (player.isSprinting() || !player.onGround())) {
            boolean burstStart = runtime.lastHorizontalSpeed < 0.08D && speed >= 0.18D;
            boolean sharpTurn = runtime.lastDirection.lengthSqr() > 1.0E-6D
                    && direction.lengthSqr() > 1.0E-6D
                    && runtime.lastDirection.dot(direction) <= DODGE_TURN_DOT;
            if (burstStart || sharpTurn) {
                runtime.dodgeTicks = 4;
                runtime.dodgeCooldown = 16;
            }
        }

        runtime.lastHorizontalSpeed = speed;
        runtime.lastDirection = direction;
    }

    private static void resetDodgeTracking(RuntimeState runtime) {
        runtime.lastDirection = Vec3.ZERO;
        runtime.lastHorizontalSpeed = 0.0D;
        runtime.dodgeTicks = 0;
        runtime.dodgeCooldown = 0;
    }

    private static void applyRecovery(ServerPlayer player, RuntimeState runtime,
                                      TitanPlayerData.AugmentInstance reflex,
                                      TitanPlayerData.AugmentInstance pain) {
        int hurtCap = Integer.MAX_VALUE;
        double momentumBlend = 0.0D;

        if (reflex != null && reflex.enhancement() >= 7) {
            hurtCap = Math.min(hurtCap, 4);
            momentumBlend = Math.max(momentumBlend, 0.42D);
        }

        if (pain != null) {
            double ratio = player.getHealth() / Math.max(1.0D, player.getMaxHealth());
            if (ratio <= painThreshold(pain.enhancement())) {
                hurtCap = Math.min(hurtCap, pain.enhancement() >= 5 ? 5 : 7);
                momentumBlend = Math.max(momentumBlend, pain.enhancement() >= 5 ? 0.40D : 0.24D);
                if (pain.enhancement() >= 10 && ratio <= 0.25D) {
                    hurtCap = Math.min(hurtCap, 1);
                    momentumBlend = Math.max(momentumBlend, 0.72D);
                }
            }
        }

        if (hurtCap != Integer.MAX_VALUE && player.hurtTime > hurtCap) player.hurtTime = hurtCap;
        if (!runtime.restoreMomentum || momentumBlend <= 0.0D) return;

        Vec3 current = player.getDeltaMovement();
        Vec3 before = runtime.preHitMotion;
        double x = current.x + (before.x - current.x) * momentumBlend;
        double z = current.z + (before.z - current.z) * momentumBlend;
        player.setDeltaMovement(x, current.y, z);
        player.hurtMarked = true;
        runtime.restoreMomentum = false;
    }

    private static double painThreshold(int enhancement) {
        return enhancement >= 5 ? 0.70D : 0.50D;
    }

    private static boolean spendReflex(ServerPlayer player, TitanPlayerData.State state,
                                       double powerFactor, double heatFactor) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId("reflex_accelerator");
        if (definition == null || state.heat() >= 99.0D) return false;
        double power = Math.max(0.0D, definition.powerLoad()) * powerFactor
                * state.powerLoadMultiplier("reflex_accelerator");
        if (!AugmentationResourceService.trySpendBurstPower(player, state, power)) return false;

        if (definition.heatLoad() > 0 && heatFactor > 0.0D && player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            double rawHeat = definition.heatLoad() * heatFactor * state.heatLoadMultiplier("reflex_accelerator");
            data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
        }
        return true;
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id,
                                    boolean enabled, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (!enabled) {
            if (instance.hasModifier(id)) instance.removeModifier(id);
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && Math.abs(current.amount() - amount) < 1.0E-8D
                && current.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void clear(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        setModifier(player, Attributes.MOVEMENT_SPEED, CRISIS_MOVE, false, 0.0D);
        setModifier(player, Attributes.ATTACK_SPEED, CRISIS_ATTACK, false, 0.0D);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path);
    }
}
