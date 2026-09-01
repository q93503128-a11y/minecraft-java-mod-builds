package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.RegnantRewardService;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class RegnantFleshEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int PART_REGEN_0 = 1 << 0;
    public static final int PART_REGEN_1 = 1 << 1;
    public static final int PART_REGEN_2 = 1 << 2;
    public static final int PART_REGEN_3 = 1 << 3;
    public static final int PART_CIRC_0 = 1 << 4;
    public static final int PART_CIRC_1 = 1 << 5;
    public static final int PART_BRAIN = 1 << 6;
    public static final int PART_LIMB_LEFT_ARM = 1 << 7;
    public static final int PART_LIMB_RIGHT_ARM = 1 << 8;
    public static final int PART_LIMB_LEFT_LEG = 1 << 9;
    public static final int PART_LIMB_RIGHT_LEG = 1 << 10;
    public static final int ALL_PARTS_MASK = (1 << 11) - 1;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 13_000.0D;

    private static final int P2_LOCKS_REQUIRED = 4;
    private static final int P3_BURST_TICKS = 240;
    private static final int CORE_SHIFT_INTERVAL = 90;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(RegnantFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> P2_CORE_LOCKS =
            SynchedEntityData.defineId(RegnantFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTIVE_CORE =
            SynchedEntityData.defineId(RegnantFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BURST_TICKS =
            SynchedEntityData.defineId(RegnantFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TISSUE_WALL =
            SynchedEntityData.defineId(RegnantFleshEntity.class, EntityDataSerializers.BOOLEAN);

    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.REGEN_0, -14.0D, 38.0D, -12.0D, 9.0F, 9.0F, 280.0F),
            new PartSpec(PartSlot.REGEN_1, 14.0D, 42.0D, -8.0D, 9.0F, 9.0F, 280.0F),
            new PartSpec(PartSlot.REGEN_2, -11.0D, 48.0D, 12.0D, 9.0F, 9.0F, 280.0F),
            new PartSpec(PartSlot.REGEN_3, 12.0D, 34.0D, 13.0D, 9.0F, 9.0F, 280.0F),
            new PartSpec(PartSlot.CIRC_0, -9.0D, 30.0D, -5.0D, 10.0F, 11.0F, 420.0F),
            new PartSpec(PartSlot.CIRC_1, 9.0D, 30.0D, 5.0D, 10.0F, 11.0F, 420.0F),
            new PartSpec(PartSlot.BRAIN, 0.0D, 56.0D, 0.0D, 14.0F, 13.0F, 620.0F),
            new PartSpec(PartSlot.LIMB_LEFT_ARM, -20.0D, 36.0D, 0.0D, 11.0F, 24.0F, 240.0F),
            new PartSpec(PartSlot.LIMB_RIGHT_ARM, 20.0D, 36.0D, 0.0D, 11.0F, 24.0F, 240.0F),
            new PartSpec(PartSlot.LIMB_LEFT_LEG, -8.0D, 14.0D, 0.0D, 12.0F, 27.0F, 280.0F),
            new PartSpec(PartSlot.LIMB_RIGHT_LEG, 8.0D, 14.0D, 0.0D, 12.0F, 27.0F, 280.0F)
    };

    private final RegnantPart[] parts = new RegnantPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private boolean partsInitialized;
    private boolean rewardsGranted;
    private int actionCooldown = 42;
    private int coreShiftCooldown = CORE_SHIFT_INTERVAL;
    private int tissueWallTicks;
    private int cultureCooldown;
    private int shedCooldown;
    private int spearDelay;
    private Vec3 spearImpact;

    public RegnantFleshEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.regnant_flesh"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (int i = 0; i < SPECS.length; i++) {
            PartSpec spec = SPECS[i];
            parts[i] = new RegnantPart(this, spec.slot(), spec.width(), spec.height(), spec.health());
        }
        xpReward = 150;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BROKEN_PARTS, 0);
        builder.define(P2_CORE_LOCKS, 0);
        builder.define(ACTIVE_CORE, 0);
        builder.define(BURST_TICKS, 0);
        builder.define(TISSUE_WALL, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RegnantCombatGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int temporalRating() {
        return 25;
    }

    public double canonicalVisibleHealth() {
        return CANONICAL_VISIBLE_MAX_HEALTH * Math.max(0.0D, getHealth()) / Math.max(1.0D, getMaxHealth());
    }

    public int brokenPartsMask() {
        return getEntityData().get(BROKEN_PARTS) & ALL_PARTS_MASK;
    }

    public boolean isPartBroken(int mask) {
        return (brokenPartsMask() & mask) != 0;
    }

    public int activeCoreIndex() {
        return Math.floorMod(getEntityData().get(ACTIVE_CORE), 4);
    }

    public int p2CoreLocks() {
        return Math.max(0, getEntityData().get(P2_CORE_LOCKS));
    }

    public int burstTicksRemaining() {
        return Math.max(0, getEntityData().get(BURST_TICKS));
    }

    public boolean tissueWallActive() {
        return getEntityData().get(TISSUE_WALL);
    }

    public int phase() {
        if (p2CoreLocks() >= P2_LOCKS_REQUIRED) return 3;
        if (isPartBroken(PART_CIRC_0) && isPartBroken(PART_CIRC_1)) return 2;
        return 1;
    }

    public boolean brainExposed() {
        return phase() == 3 && isPartBroken(PART_REGEN_0) && isPartBroken(PART_REGEN_1)
                && isPartBroken(PART_REGEN_2) && isPartBroken(PART_REGEN_3);
    }

    private boolean isBroken(PartSlot slot) {
        return isPartBroken(slot.mask());
    }

    private void markBroken(PartSlot slot) {
        getEntityData().set(BROKEN_PARTS, brokenPartsMask() | slot.mask());
    }

    private void clearBroken(PartSlot slot) {
        getEntityData().set(BROKEN_PARTS, brokenPartsMask() & ~slot.mask());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float effective = amount * (brainExposed() ? 0.08F : 0.025F);
        if (tissueWallActive()) effective *= 0.30F;
        if (effective <= 0.0F) return false;
        float before = getHealth();
        setHealth(Math.max(1.0F, before - effective));
        return getHealth() < before;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updatePartPositions();
        if (!level().isClientSide()) {
            bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (tissueWallTicks > 0 && --tissueWallTicks == 0) {
            getEntityData().set(TISSUE_WALL, false);
        }
        if (cultureCooldown > 0) cultureCooldown--;
        if (shedCooldown > 0) shedCooldown--;

        if (spearDelay > 0 && --spearDelay == 0 && spearImpact != null) {
            fleshSpearImpact(serverLevel, spearImpact);
            spearImpact = null;
        }

        regenerateBody();
        tickPhaseLogic(serverLevel);
        tickAnalysisReadout(serverLevel);
    }

    private void regenerateBody() {
        if (brainExposed()) return;
        double visiblePerTick = switch (phase()) {
            case 1 -> 0.20D;
            case 2 -> 0.13D;
            default -> 0.07D;
        };
        int brokenCirculation = (isPartBroken(PART_CIRC_0) ? 1 : 0) + (isPartBroken(PART_CIRC_1) ? 1 : 0);
        visiblePerTick *= Math.max(0.20D, 1.0D - brokenCirculation * 0.35D);
        if (getHealth() < getMaxHealth()) heal((float) CombatScale.toInternal(visiblePerTick));

        if (phase() == 1 && tickCount % 100 == 0) {
            regrowLimb(PartSlot.LIMB_LEFT_ARM);
            regrowLimb(PartSlot.LIMB_RIGHT_ARM);
            regrowLimb(PartSlot.LIMB_LEFT_LEG);
            regrowLimb(PartSlot.LIMB_RIGHT_LEG);
        }
    }

    private void regrowLimb(PartSlot slot) {
        if (!isBroken(slot)) return;
        RegnantPart part = part(slot);
        if (part == null) return;
        part.setPartHealth(part.maxPartHealth);
        clearBroken(slot);
    }

    private void tickPhaseLogic(ServerLevel level) {
        if (phase() == 2) {
            if (--coreShiftCooldown <= 0) {
                coreShiftCooldown = CORE_SHIFT_INTERVAL;
                getEntityData().set(ACTIVE_CORE, (activeCoreIndex() + 1 + getRandom().nextInt(3)) & 3);
            }
        } else if (phase() == 3 && !brainExposed()) {
            int ticks = burstTicksRemaining();
            if (ticks <= 0) startBurstWindow();
            else {
                getEntityData().set(BURST_TICKS, ticks - 1);
                if (ticks == 1 && !brainExposed()) failBurstWindow();
            }
        }
    }

    private void startBurstWindow() {
        for (PartSlot slot : new PartSlot[]{PartSlot.REGEN_0, PartSlot.REGEN_1, PartSlot.REGEN_2, PartSlot.REGEN_3}) {
            RegnantPart part = part(slot);
            if (part != null) part.setPartHealth(part.maxPartHealth);
            clearBroken(slot);
        }
        getEntityData().set(BURST_TICKS, P3_BURST_TICKS);
    }

    private void failBurstWindow() {
        heal((float) CombatScale.toInternal(700.0D));
        startBurstWindow();
    }

    private void tickAnalysisReadout(ServerLevel level) {
        if (tickCount % 20 != 0) return;
        AABB area = getBoundingBox().inflate(90.0D, 70.0D, 90.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, Player::isAlive)) {
            TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
            if (phase() == 2 && state.hasInstalled("weakpoint_analysis_eye")) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.regnant_active_core", activeCoreIndex() + 1), true);
            } else if (phase() == 3 && !brainExposed()) {
                int seconds = Math.max(1, (burstTicksRemaining() + 19) / 20);
                player.sendSystemMessage(Component.translatable("message.titanbreak.regnant_burst_window", seconds), true);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    private void updatePartPositions() {
        Vec3[] previous = new Vec3[parts.length];
        for (int i = 0; i < parts.length; i++) previous[i] = parts[i].position();
        double yaw = Math.toRadians(-getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        for (int i = 0; i < parts.length; i++) {
            PartSpec spec = SPECS[i];
            double x = spec.x() * cos - spec.z() * sin;
            double z = spec.x() * sin + spec.z() * cos;
            parts[i].setPos(getX() + x, getY() + spec.y(), getZ() + z);
        }
        for (int i = 0; i < parts.length; i++) {
            RegnantPart part = parts[i];
            Vec3 old = partsInitialized ? previous[i] : part.position();
            part.xo = old.x;
            part.yo = old.y;
            part.zo = old.z;
            part.xOld = old.x;
            part.yOld = old.y;
            part.zOld = old.z;
        }
        partsInitialized = true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (!partsInitialized) return getBoundingBox().inflate(34.0D, 82.0D, 34.0D);
        AABB bounds = getBoundingBox();
        for (RegnantPart part : parts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds.inflate(5.0D);
    }

    private boolean partPickable(PartSlot slot) {
        if (slot == PartSlot.BRAIN) return brainExposed() && !isBroken(slot);
        if (slot.isRegenCore()) {
            if (phase() == 2) return slot.regenIndex() == activeCoreIndex();
            return phase() == 3 && !isBroken(slot);
        }
        if (slot.isCirculation()) return phase() == 1 && !isBroken(slot);
        return !isBroken(slot);
    }

    private boolean hurtPart(RegnantPart part, ServerLevel level, DamageSource source, float amount) {
        if (!partPickable(part.slot)) return false;
        float effective = tissueWallActive() ? amount * 0.45F : amount;
        part.applyPartDamage(effective);
        if (!part.broken()) {
            damageBodyFromPart(effective, part.slot);
            return true;
        }

        if (part.slot.isRegenCore() && phase() == 2) {
            int locks = Math.min(P2_LOCKS_REQUIRED, p2CoreLocks() + 1);
            getEntityData().set(P2_CORE_LOCKS, locks);
            part.setPartHealth(part.maxPartHealth);
            if (locks < P2_LOCKS_REQUIRED) {
                getEntityData().set(ACTIVE_CORE, (activeCoreIndex() + 1 + getRandom().nextInt(3)) & 3);
                coreShiftCooldown = CORE_SHIFT_INTERVAL;
            } else {
                startBurstWindow();
            }
            damageBodyFromPart(effective * 1.2F, part.slot);
            return true;
        }

        markBroken(part.slot);
        damageBodyFromPart(effective, part.slot);
        if (part.slot == PartSlot.BRAIN) {
            return super.hurtServer(level, source, Float.MAX_VALUE);
        }
        return true;
    }

    private void damageBodyFromPart(float amount, PartSlot slot) {
        float multiplier = switch (slot) {
            case CIRC_0, CIRC_1 -> 0.65F;
            case REGEN_0, REGEN_1, REGEN_2, REGEN_3 -> 0.85F;
            case BRAIN -> 1.0F;
            default -> 0.25F;
        };
        setHealth(Math.max(1.0F, getHealth() - Math.max(0.0F, amount * multiplier)));
    }

    private void scheduleFleshSpear(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND);
        spearImpact = target.position().add(target.getDeltaMovement().scale(10.0D));
        spearDelay = 18;
    }

    private void fleshSpearImpact(ServerLevel level, Vec3 center) {
        for (Player player : level.getEntitiesOfClass(Player.class,
                new AABB(center, center).inflate(5.5D, 7.0D, 5.5D), Player::isAlive)) {
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(48.0D));
            Vec3 away = player.position().subtract(center);
            if (away.horizontalDistanceSqr() > 1.0E-6D) {
                away = new Vec3(away.x, 0.0D, away.z).normalize();
                player.push(away.x * 0.9D, 0.55D, away.z * 0.9D);
            }
        }
    }

    private void raiseTissueWall(ServerLevel level, LivingEntity target) {
        getEntityData().set(TISSUE_WALL, true);
        tissueWallTicks = phase() == 3 ? 65 : 50;
        Vec3 forward = target.position().subtract(position());
        if (forward.horizontalDistanceSqr() < 1.0E-6D) return;
        forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(16.0D, 12.0D, 16.0D), Player::isAlive)) {
            Vec3 offset = player.position().subtract(position());
            if (offset.horizontalDistance() > 16.0D || offset.dot(forward) < 0.0D) continue;
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(24.0D));
            player.push(forward.x * 1.25D, 0.20D, forward.z * 1.25D);
        }
    }

    private void cultureMinions(ServerLevel level, LivingEntity target) {
        if (cultureCooldown > 0) return;
        cultureCooldown = phase() == 3 ? 120 : 170;
        spawnCultured(level, target, ModEntities.REGROWER.get(), -5.0D);
        spawnCultured(level, target, ModEntities.SKITTER.get(), 5.0D);
        if (phase() >= 2) spawnCultured(level, target, ModEntities.RIPPER.get(), 0.0D);
    }

    private void spawnCultured(ServerLevel level, LivingEntity target, EntityType<?> type, double side) {
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (!(entity instanceof Mob mob)) return;
        Vec3 right = new Vec3(-getLookAngle().z, 0.0D, getLookAngle().x);
        Vec3 spawn = position().add(right.scale(side)).add(0.0D, 1.0D, 0.0D);
        mob.setPos(spawn.x, spawn.y, spawn.z);
        mob.setTarget(target);
        if (level.noCollision(mob)) level.addFreshEntity(mob);
    }

    private void shedBody(ServerLevel level, LivingEntity target) {
        if (shedCooldown > 0) return;
        shedCooldown = 220;
        Vec3 away = position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        Vec3 origin = position();
        for (int i = 0; i < 6; i++) {
            double distance = 10.0D + i * 2.0D;
            setPos(origin.x + away.x * distance, origin.y, origin.z + away.z * distance);
            if (level.noCollision(this)) break;
            setPos(origin.x, origin.y, origin.z);
        }
        heal((float) CombatScale.toInternal(120.0D));
        cultureMinions(level, target);
    }

    private RegnantPart part(PartSlot slot) {
        for (RegnantPart part : parts) if (part.slot == slot) return part;
        return null;
    }

    @Override
    public void die(DamageSource source) {
        if (!rewardsGranted && level() instanceof ServerLevel level) {
            rewardsGranted = true;
            RegnantRewardService.award(this, level, source);
        }
        super.die(source);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int saved = input.getIntOr("TitanbreakRegnantBrokenParts", 0) & ALL_PARTS_MASK;
        getEntityData().set(BROKEN_PARTS, saved);
        getEntityData().set(P2_CORE_LOCKS, input.getIntOr("TitanbreakRegnantP2Locks", 0));
        getEntityData().set(ACTIVE_CORE, input.getIntOr("TitanbreakRegnantActiveCore", 0));
        getEntityData().set(BURST_TICKS, input.getIntOr("TitanbreakRegnantBurstTicks", 0));
        getEntityData().set(TISSUE_WALL, false);
        for (int i = 0; i < parts.length; i++) {
            float hp = input.getFloatOr("TitanbreakRegnantPartHealth" + i, SPECS[i].health());
            if ((saved & parts[i].slot.mask()) != 0) hp = 0.0F;
            parts[i].setPartHealth(hp);
        }
        partsInitialized = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TitanbreakRegnantBrokenParts", brokenPartsMask());
        output.putInt("TitanbreakRegnantP2Locks", p2CoreLocks());
        output.putInt("TitanbreakRegnantActiveCore", activeCoreIndex());
        output.putInt("TitanbreakRegnantBurstTicks", burstTicksRemaining());
        for (int i = 0; i < parts.length; i++) {
            output.putFloat("TitanbreakRegnantPartHealth" + i, parts[i].partHealth);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < parts.length; i++) parts[i].setId(packet.getId() + i + 1);
        partsInitialized = false;
        updatePartPositions();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    private final class RegnantCombatGoal extends Goal {
        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel serverLevel)) return;
            getNavigation().moveTo(target, phase() == 1 ? 0.72D : phase() == 2 ? 0.86D : 0.96D);
            getLookControl().setLookAt(target, 48.0F, 32.0F);
            if (getHealth() < getMaxHealth() * 0.32F && shedCooldown <= 0) {
                shedBody(serverLevel, target);
                actionCooldown = 35;
                return;
            }
            if (actionCooldown > 0) {
                actionCooldown--;
                return;
            }
            double distance = distanceTo(target);
            int choice = getRandom().nextInt(phase() == 1 ? 4 : 5);
            if (distance <= 14.0D && choice == 0) {
                raiseTissueWall(serverLevel, target);
                actionCooldown = 48;
            } else if (choice == 1) {
                cultureMinions(serverLevel, target);
                actionCooldown = 55;
            } else if (choice == 2 && phase() >= 2) {
                shedBody(serverLevel, target);
                actionCooldown = 52;
            } else {
                scheduleFleshSpear(target);
                actionCooldown = phase() == 3 ? 34 : 46;
            }
        }
    }

    private enum PartSlot {
        REGEN_0(PART_REGEN_0, 0), REGEN_1(PART_REGEN_1, 1), REGEN_2(PART_REGEN_2, 2), REGEN_3(PART_REGEN_3, 3),
        CIRC_0(PART_CIRC_0, -1), CIRC_1(PART_CIRC_1, -1), BRAIN(PART_BRAIN, -1),
        LIMB_LEFT_ARM(PART_LIMB_LEFT_ARM, -1), LIMB_RIGHT_ARM(PART_LIMB_RIGHT_ARM, -1),
        LIMB_LEFT_LEG(PART_LIMB_LEFT_LEG, -1), LIMB_RIGHT_LEG(PART_LIMB_RIGHT_LEG, -1);

        private final int mask;
        private final int regenIndex;

        PartSlot(int mask, int regenIndex) {
            this.mask = mask;
            this.regenIndex = regenIndex;
        }

        int mask() { return mask; }
        int regenIndex() { return regenIndex; }
        boolean isRegenCore() { return regenIndex >= 0; }
        boolean isCirculation() { return this == CIRC_0 || this == CIRC_1; }
    }

    private record PartSpec(PartSlot slot, double x, double y, double z, float width, float height, float health) {}

    private static final class RegnantPart extends PartEntity<RegnantFleshEntity> {
        private final PartSlot slot;
        private final EntityDimensions dimensions;
        private final float maxPartHealth;
        private float partHealth;

        private RegnantPart(RegnantFleshEntity parent, PartSlot slot, float width, float height, float health) {
            super(parent);
            this.slot = slot;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.maxPartHealth = health;
            this.partHealth = health;
            refreshDimensions();
        }

        private boolean broken() { return partHealth <= 0.0F; }
        private void setPartHealth(float health) { partHealth = Math.max(0.0F, Math.min(maxPartHealth, health)); }
        private void applyPartDamage(float amount) { setPartHealth(partHealth - Math.max(0.0F, amount)); }

        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
        @Override protected void readAdditionalSaveData(ValueInput input) {}
        @Override protected void addAdditionalSaveData(ValueOutput output) {}
        @Override public boolean isPickable() { return getParent().partPickable(slot); }
        @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            return !isInvulnerableToBase(source) && getParent().hurtPart(this, level, source, amount);
        }
        @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) { throw new UnsupportedOperationException(); }
        @Override public EntityDimensions getDimensions(Pose pose) { return dimensions; }
        @Override public boolean shouldBeSaved() { return false; }
    }
}
