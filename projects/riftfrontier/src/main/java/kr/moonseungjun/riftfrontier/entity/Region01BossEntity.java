package kr.moonseungjun.riftfrontier.entity;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldAimPolicy;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldImpactProfile;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldImpactResolver;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldImpulseResolver;
import kr.moonseungjun.riftfrontier.combat.Region01BossProductionSemantics;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossProductionPresentation;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.network.RiftfrontierNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Physical server/client actor identity for the Region 01 boss.
 *
 * <p>The entity is still excluded from natural spawning and production Region 01 encounter composition. A narrow
 * field-play harness can be enabled explicitly by the development command so the already-authored authoritative boss
 * attack/phase semantics can be exercised in a real Minecraft world before the production encounter gate opens.</p>
 */
public final class Region01BossEntity extends LivingEntity {
    /** Deliberately diagnostic until human boss-field evidence approves final damage. */
    private static final float FIELD_TEST_DAMAGE = 1.0F;
    private static final Region01BossFieldImpactResolver FIELD_TEST_HIT_RESOLVER = new Region01BossFieldImpactResolver();
    private static final Region01BossFieldImpulseResolver FIELD_TEST_IMPULSE_RESOLVER = new Region01BossFieldImpulseResolver();

    private boolean fieldTestCombatEnabled;
    private MinecraftBossCombatAdapter.ValidatedRuntime fieldTestRuntime;
    private ContentId fieldTestPreviousPattern;
    private AttackTimeline.Phase fieldTestPreviousPhase;

    public Region01BossEntity(EntityType<? extends Region01BossEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Enables the non-production field harness. The runtime is rebuilt from the current published content generation
     * instead of persisting a stale process-local capability through reloads/restarts.
     */
    public void enableFieldTestCombat() {
        if (level().isClientSide()) {
            throw new IllegalStateException("Region 01 boss field-test combat is server-authoritative");
        }
        fieldTestCombatEnabled = true;
        fieldTestRuntime = null;
        fieldTestPreviousPattern = null;
        fieldTestPreviousPhase = null;
    }

    public boolean fieldTestCombatEnabled() {
        return fieldTestCombatEnabled;
    }

    /** Server-authoritative phase switch for the explicit field harness only. */
    public void setFieldTestPhase(int phase) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Region 01 boss field-test phase can only change on the server");
        }
        MinecraftBossCombatAdapter.ValidatedRuntime runtime = requireFieldTestRuntime();
        runtime.transitionToPhase(serverLevel, this, phase);
    }

    @Override
    public void tick() {
        super.tick();
        if (!fieldTestCombatEnabled || !(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }

        MinecraftBossCombatAdapter.ValidatedRuntime runtime = requireFieldTestRuntime();
        long gameTick = serverLevel.getGameTime();
        if (!runtime.attackExecuting()) {
            ServerPlayer target = nearestFieldTestTarget(serverLevel);
            if (target == null) {
                return;
            }
            setYRot(Region01BossFieldAimPolicy.committedYawDegrees(
                getX(), getZ(), target.getX(), target.getZ()
            ));
            runtime.beginNextAttack(serverLevel, this, gameTick);
        }
        MinecraftBossCombatAdapter.ValidatedTickResult result = runtime.tick(serverLevel, this, gameTick);
        applyFieldTestActiveEntryImpulse(serverLevel, result);
        applyFieldTestActiveTravel(result);
        RiftfrontierNetworking.syncBossPresentation(this, gameTick, result);
    }

    /**
     * Gives the local arena-pressure role one server-owned outward displacement when authoritative ACTIVE begins.
     *
     * <p>This is field-play calibration, not final knockback balance or VFX. It reuses the same eligible-target and
     * local-area geometry contract as the damage resolver, adds no second attack clock, and deliberately does not
     * reapply every ACTIVE tick.</p>
     */
    private void applyFieldTestActiveEntryImpulse(
        ServerLevel serverLevel,
        MinecraftBossCombatAdapter.ValidatedTickResult result
    ) {
        var frame = result.combat().presentation().orElse(null);
        if (frame == null) {
            fieldTestPreviousPattern = null;
            fieldTestPreviousPhase = null;
            return;
        }

        boolean enteringActive = frame.attackPhase() == AttackTimeline.Phase.ACTIVE
            && (fieldTestPreviousPhase != AttackTimeline.Phase.ACTIVE
                || fieldTestPreviousPattern == null
                || !fieldTestPreviousPattern.equals(frame.patternId()));

        fieldTestPreviousPattern = frame.patternId();
        fieldTestPreviousPhase = frame.attackPhase();
        if (!enteringActive) {
            return;
        }
        FIELD_TEST_IMPULSE_RESOLVER.applyActiveEntry(serverLevel, this, frame.patternId());
    }

    /**
     * Gives the authored {@code line_charge} role real server-owned travel in the field harness.
     *
     * <p>The step length comes from {@link Region01BossFieldImpactProfile}, so provisional travel and provisional
     * threat geometry are reviewed as one calibration surface. Movement happens only while the authoritative attack
     * clock is ACTIVE, uses the already-committed facing, and goes through Minecraft's normal entity collision move.
     * This is intentionally not a final boss locomotion/AI policy.</p>
     */
    private void applyFieldTestActiveTravel(MinecraftBossCombatAdapter.ValidatedTickResult result) {
        var frame = result.combat().presentation().orElse(null);
        if (frame == null || !frame.hitWindowOpen()) {
            return;
        }

        double forwardStep = Region01BossFieldImpactProfile.find(frame.patternId())
            .map(Region01BossFieldImpactProfile.Profile::activeForwardStep)
            .orElse(0.0D);
        if (forwardStep <= 0.0D) {
            return;
        }

        Vec3 look = getLookAngle();
        double horizontalLength = Math.hypot(look.x, look.z);
        if (horizontalLength < 1.0E-6D) {
            return;
        }
        move(MoverType.SELF, new Vec3(
            look.x / horizontalLength * forwardStep,
            0.0D,
            look.z / horizontalLength * forwardStep
        ));
    }

    /**
     * Acquires only when a new attack is about to begin. Once TELEGRAPH starts, facing is deliberately left untouched
     * until the authoritative attack finishes so line/arc counterplay remains readable instead of homing mid-swing.
     */
    private ServerPlayer nearestFieldTestTarget(ServerLevel serverLevel) {
        ServerPlayer nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            double horizontalX = player.getX() - getX();
            double horizontalZ = player.getZ() - getZ();
            if (horizontalX * horizontalX + horizontalZ * horizontalZ < 1.0E-12D) continue;
            double distance = distanceToSqr(player);
            if (!Region01BossFieldAimPolicy.withinAcquisitionRadius(distance) || distance >= nearestDistance) continue;
            nearest = player;
            nearestDistance = distance;
        }
        return nearest;
    }

    private MinecraftBossCombatAdapter.ValidatedRuntime requireFieldTestRuntime() {
        if (!fieldTestCombatEnabled) {
            throw new IllegalStateException("Region 01 boss field-test combat is not enabled");
        }
        if (fieldTestRuntime != null) {
            try {
                fieldTestRuntime.semanticCapability();
                return fieldTestRuntime;
            } catch (IllegalStateException stale) {
                // A content reload retires the old capability. Rebuild below from the newly published snapshot.
                fieldTestRuntime.cancelAttack();
                fieldTestRuntime = null;
                fieldTestPreviousPattern = null;
                fieldTestPreviousPhase = null;
            }
        }

        var snapshot = ContentRuntime.requireCurrent();
        var catalog = new CombatRuntimeCatalog(snapshot);
        ValidatedBossCombatSemantics semantics = ValidatedBossCombatSemantics.validate(
            catalog,
            Region01BossProductionSemantics.load(),
            Region01BossProductionPresentation.load()
        );
        fieldTestRuntime = MinecraftBossCombatAdapter.validated(
            catalog,
            semantics,
            FIELD_TEST_HIT_RESOLVER,
            FIELD_TEST_DAMAGE
        );
        return fieldTestRuntime;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // Region 01 boss equipment is not an authored gameplay axis; no vanilla equipment slots are exposed.
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
