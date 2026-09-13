package kr.moonseungjun.riftfrontier.entity;

import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.combat.Region01BossFieldImpactResolver;
import kr.moonseungjun.riftfrontier.combat.Region01BossProductionSemantics;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossProductionPresentation;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.network.RiftfrontierNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    private boolean fieldTestCombatEnabled;
    private MinecraftBossCombatAdapter.ValidatedRuntime fieldTestRuntime;

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
            runtime.beginNextAttack(serverLevel, this, gameTick);
        }
        MinecraftBossCombatAdapter.ValidatedTickResult result = runtime.tick(serverLevel, this, gameTick);
        RiftfrontierNetworking.syncBossPresentation(this, gameTick, result);
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
