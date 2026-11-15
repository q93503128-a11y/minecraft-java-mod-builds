package kr.moonseungjun.anomalousclasses.entity;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProfileSavedData;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetCombatService;
import kr.moonseungjun.anomalousclasses.state.PuppetRegistrySavedData;
import kr.moonseungjun.anomalousclasses.state.PuppetService;
import kr.moonseungjun.anomalousclasses.state.PuppetAbilityService;
import kr.moonseungjun.anomalousclasses.state.PuppeteerRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A real persistent Minecraft entity, not a borrowed vanilla mob. */
public final class PuppetEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEVICE = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.INT);
    private PuppetBlueprint blueprint;
    private int revision;
    private long nextAttack;
    private int statRefresh;
    private boolean dropHandled;
    private boolean guarding;
    private boolean registryValidated;
    private UUID priorityTarget;
    private UUID cachedTarget;
    private long nextTargetScan;

    public PuppetEntity(EntityType<? extends PuppetEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FRAME, 0);
        builder.define(DEVICE, 0);
        builder.define(HEIGHT, 1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void initialize(PuppetBlueprint data, int registryRevision, PlayerClassProfile profile) {
        this.blueprint = data;
        this.revision = registryRevision;
        this.nextAttack = data.nextAttackTick();
        this.entityData.set(FRAME, data.frame().ordinal());
        this.entityData.set(DEVICE, data.device().ordinal());
        this.entityData.set(HEIGHT, data.size().ordinal());
        this.refreshDimensions();
        applyStats(profile, true);
    }

    public int frameIndex() { return this.entityData.get(FRAME); }
    public int deviceIndex() { return this.entityData.get(DEVICE); }
    public PuppetBlueprint blueprint() { return blueprint; }
    public PuppetBlueprint.Frame frame() { return blueprint == null ? PuppetBlueprint.Frame.LIGHT : blueprint.frame(); }
    public PuppetBlueprint.Device device() { return blueprint == null ? PuppetBlueprint.Device.BLADE : blueprint.device(); }
    public float puppetScale() {
        int value = Math.max(0, Math.min(PuppetBlueprint.Size.values().length - 1,
                this.entityData.get(HEIGHT)));
        return (float) (PuppetBlueprint.Size.values()[value].height() / 1.5);
    }

    /**
     * The visible 1–3-block body and the server/client physical hitbox must
     * agree. A larger puppet cannot walk through a two-block-tall doorway.
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.entityData == null) return super.getDefaultDimensions(pose);
        int value = Math.max(0, Math.min(PuppetBlueprint.Size.values().length - 1,
                this.entityData.get(HEIGHT)));
        float height = (float) PuppetBlueprint.Size.values()[value].height();
        // Model is 1.5 blocks at scale 1.0; the physical width scales with it.
        return EntityDimensions.scalable(.68F * height / 1.5F, height);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEIGHT.equals(key)) refreshDimensions();
    }
    public UUID puppetId() { return blueprint == null ? null : blueprint.puppetId(); }
    public UUID puppetOwner() { return blueprint == null ? null : blueprint.ownerId(); }
    public int revision() { return revision; }
    public boolean guarding() { return guarding; }

    public void designateTarget(UUID target) {
        priorityTarget = target;
        cachedTarget = target;
        nextTargetScan = 0L;
        // Target changes must not reset the stored attack recovery.
    }

    public void syncSnapshot() {
        if (blueprint != null && level() instanceof ServerLevel world)
            PuppetRegistrySavedData.get(world.getServer()).updateDeployedSnapshot(snapshot(), revision);
    }

    public PuppetBlueprint snapshot() {
        if (blueprint == null) throw new IllegalStateException("Unbound puppet");
        return blueprint.withCondition(Math.max(0, getHealth() / Math.max(1, getMaxHealth())), nextAttack);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer owner && blueprint != null && hand == InteractionHand.MAIN_HAND) {
            return PuppetService.recall(owner, this) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (blueprint == null || !(level() instanceof ServerLevel world)) return;
        if (!registryValidated || tickCount % 40 == 0) {
            registryValidated = true;
            var entry = PuppetRegistrySavedData.get(world.getServer()).find(blueprint.puppetId());
            if (entry.isEmpty() || entry.get().location() != PuppetRegistrySavedData.Location.DEPLOYED
                    || entry.get().revision() != revision) {
                discard(); // Stale or duplicated entity must never mint another item.
                return;
            }
        }
        ServerPlayer owner = world.getServer().getPlayerList().getPlayer(blueprint.ownerId());
        if (owner == null || owner.level() != this.level()
                || ClassProfileSavedData.get(world.getServer()).profile(owner.getUUID())
                        .currentClass().orElse(null) != ClassId.PUPPETEER) {
            getNavigation().stop();
            setTarget(null);
            guarding = false;
            return;
        }
        PlayerClassProfile profile = ClassProfileSavedData.get(world.getServer()).profile(owner.getUUID());
        if (PuppetAbilityService.tickPull(this)) {
            guarding = false;
            return;
        }
        if (++statRefresh >= 40) {
            statRefresh = 0;
            applyStats(profile, false);
            syncSnapshot();
        }
        Monster target = selectHostile(world, owner);
        guarding = target != null && blueprint.device() == PuppetBlueprint.Device.SHIELD;
        if (target != null) {
            PuppeteerRules.Stats stats = PuppeteerRules.stats(blueprint,
                    profile.progress(ClassId.PUPPETEER).level(), profile);
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double driveDamage = PuppetAbilityService.driveActive(world.getServer(), owner.getUUID())
                    ? 1.20 : 1.0;
            switch (blueprint.device()) {
                case BLADE -> {
                    if (tickCount % 10 == 0 && distanceToSqr(target) > stats.reach() * stats.reach())
                        getNavigation().moveTo(target, 1.03);
                    if (inReach(target, stats.reach()) && ready(world)) {
                        if (target.hurt(damageSources().mobAttack(this), (float) (stats.damage() * driveDamage)))
                            markOwnerCombat(owner);
                        finishAttack(world, stats);
                    }
                }
                case LAUNCHER -> {
                    double distance = Math.sqrt(distanceToSqr(target));
                    if (tickCount % 12 == 0) {
                        if (distance < 5.5) {
                            // Reposition instead of orbiting inside melee range.
                            Vec3 away = position().subtract(target.position());
                            if (away.horizontalDistanceSqr() > .01) {
                                away = new Vec3(away.x, 0, away.z).normalize();
                                getNavigation().moveTo(getX() + away.x * 5.0, getY(),
                                        getZ() + away.z * 5.0, 1.08);
                            }
                        } else if (distance > stats.reach() - 1.5)
                            getNavigation().moveTo(target, 1.0);
                        else getNavigation().stop();
                    }
                    if (inReach(target, stats.reach()) && distance > 3.0 && ready(world))
                        shootBolt(world, target, stats, owner, driveDamage);
                }
                case SHIELD -> {
                    // Body blocking is location-based. Guard physically between the
                    // nearest threat and its owner rather than granting radius immunity.
                    Vec3 towardThreat = target.position().subtract(owner.position());
                    if (towardThreat.horizontalDistanceSqr() > .1) {
                        Vec3 direction = new Vec3(towardThreat.x, 0, towardThreat.z).normalize();
                        Vec3 intercept = owner.position().add(direction.scale(2.1));
                        if (tickCount % 8 == 0 &&
                                distanceToSqr(intercept) > 1.8)
                            getNavigation().moveTo(intercept.x, intercept.y, intercept.z, 1.1);
                    }
                    if (inReach(target, stats.reach()) && ready(world)) {
                        if (target.hurt(damageSources().mobAttack(this), (float) (stats.damage() * driveDamage))) {
                            target.knockback(.36, target.getX() - getX(), target.getZ() - getZ());
                            markOwnerCombat(owner);
                        }
                        finishAttack(world, stats);
                    }
                }
            }
        } else if (distanceToSqr(owner) > 16 && distanceToSqr(owner) < 24 * 24) {
            if (tickCount % 12 == 0) getNavigation().moveTo(owner, 1.05);
        } else if (distanceToSqr(owner) <= 9) {
            getNavigation().stop();
        }
    }

    private boolean ready(ServerLevel world) {
        return world.getGameTime() >= nextAttack;
    }

    private boolean inReach(Monster target, double reach) {
        return distanceToSqr(target) <= reach * reach && getSensing().hasLineOfSight(target);
    }

    private void finishAttack(ServerLevel world, PuppeteerRules.Stats stats) {
        swing(InteractionHand.MAIN_HAND);
        boolean powered = blueprint != null
                && PuppetAbilityService.driveActive(world.getServer(), blueprint.ownerId());
        nextAttack = world.getGameTime() + (powered
                ? Math.max(4, (int) Math.ceil(stats.attackIntervalTicks() / 1.10))
                : stats.attackIntervalTicks());
        syncSnapshot();
    }

    private void markOwnerCombat(ServerPlayer owner) {
        kr.moonseungjun.anomalousclasses.state.CombatTracker.markOwnedCombat(owner);
    }

    private void shootBolt(ServerLevel world, Monster target, PuppeteerRules.Stats stats,
                           ServerPlayer owner, double driveDamage) {
        Arrow bolt = new Arrow(world, this, new ItemStack(Items.ARROW), ItemStack.EMPTY);
        bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
        bolt.setBaseDamage(Math.max(.1, (stats.damage() * driveDamage) / 1.7));
        Vec3 muzzle = position().add(0, getBbHeight() * .72, 0);
        Vec3 aim = target.position().add(0, target.getBbHeight() * .55, 0);
        Vec3 direction = aim.subtract(muzzle);
        if (direction.lengthSqr() < .01) return;
        bolt.setPos(muzzle.x, muzzle.y, muzzle.z);
        // Real projectile collision/flight; shots cannot hit through solid walls.
        float spread = frame() == PuppetBlueprint.Frame.LIGHT ? 2.5F
                : frame() == PuppetBlueprint.Frame.HEAVY ? .45F : 1.2F;
        bolt.shoot(direction.x, direction.y + direction.horizontalDistance() * .02,
                direction.z, 1.7F, spread);
        if (world.addFreshEntity(bolt)) {
            finishAttack(world, stats);
            markOwnerCombat(owner);
        }
    }

    private Monster selectHostile(ServerLevel world, ServerPlayer owner) {
        if (priorityTarget != null && world.getEntity(priorityTarget) instanceof Monster forced
                && forced.isAlive() && !PuppetCombatService.allied(this, forced) && distanceToSqr(forced) <= 24 * 24)
            return forced;
        if (world.getGameTime() < nextTargetScan) {
            if (cachedTarget != null && world.getEntity(cachedTarget) instanceof Monster cached
                    && cached.isAlive() && !PuppetCombatService.allied(this, cached)
                    && distanceToSqr(cached) < 13 * 13) return cached;
            return null;
        }
        nextTargetScan = world.getGameTime() + 20L;
        AABB range = getBoundingBox().inflate(11.0);
        Monster closest = null;
        double best = Double.MAX_VALUE;
        for (Monster candidate : world.getEntitiesOfClass(Monster.class, range,
                mob -> mob.isAlive() && !PuppetCombatService.allied(this, mob))) {
            double distance = distanceToSqr(candidate);
            if (distance < best && getSensing().hasLineOfSight(candidate)) {
                closest = candidate;
                best = distance;
            }
        }
        cachedTarget = closest == null ? null : closest.getUUID();
        return closest;
    }

    private void applyStats(PlayerClassProfile profile, boolean initial) {
        PuppeteerRules.Stats stats = PuppeteerRules.stats(blueprint,
                profile.progress(ClassId.PUPPETEER).level(), profile);
        float ratio = initial ? (float) blueprint.healthRatio()
                : Math.max(0, getHealth() / Math.max(1, getMaxHealth()));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.maxHealth());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28 * stats.moveSpeedFactor());
        getAttribute(Attributes.ARMOR).setBaseValue(stats.armor());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE)
                .setBaseValue(frame() == PuppetBlueprint.Frame.HEAVY ? .55
                        : frame() == PuppetBlueprint.Frame.BALANCED ? .15 : 0.0);
        setHealth(Math.max(.01F, (float) stats.maxHealth() * ratio));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (blueprint != null && level() instanceof ServerLevel world) {
            var attacker = source.getEntity();
            ServerPlayer owner = world.getServer().getPlayerList().getPlayer(blueprint.ownerId());
            if (attacker instanceof Player player
                    && (attacker.getUUID().equals(blueprint.ownerId())
                    || owner == null || owner.isAlliedTo(player))) return false;
            if (attacker instanceof PuppetEntity other
                    && other.puppetOwner() != null && other.puppetOwner().equals(blueprint.ownerId())) return false;
        }
        boolean damaged = super.hurt(source, amount);
        if (damaged && blueprint != null && level() instanceof ServerLevel world) {
            syncSnapshot();
            ServerPlayer owner = world.getServer().getPlayerList().getPlayer(blueprint.ownerId());
            if (owner != null && source.getEntity() != null)
                kr.moonseungjun.anomalousclasses.state.CombatTracker.markOwnedCombat(owner);
        }
        return damaged;
    }

    @Override
    public void die(DamageSource source) {
        PuppetAbilityService.stopPull(puppetId());
        if (!dropHandled && blueprint != null && level() instanceof ServerLevel) {
            dropHandled = true;
            PuppetService.breakEntity(this);
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (blueprint != null) {
            tag.put("puppet", snapshot().save());
            tag.putInt("puppetRevision", revision);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.blueprint = PuppetBlueprint.load(tag.getCompound("puppet")).orElse(null);
        this.revision = tag.getInt("puppetRevision");
        if (blueprint != null) {
            nextAttack = blueprint.nextAttackTick();
            entityData.set(FRAME, blueprint.frame().ordinal());
            entityData.set(DEVICE, blueprint.device().ordinal());
            entityData.set(HEIGHT, blueprint.size().ordinal());
            refreshDimensions();
        }
    }
}
