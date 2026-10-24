package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative bridge for the advanced passive/utility batch.
 *
 * <p>The service deliberately reuses existing ROOT provenance, persistent player state and vanilla
 * item/enchantment boundaries instead of creating parallel combat or movement engines.</p>
 */
public final class AdvancedUtilityAugmentService {
    private static final String ARMOR_PIERCE_ID = id("armor_pierce");
    private static final String HUNT_RECORD_ID = id("hunt_record");
    private static final String TREE_CLIMB_ID = id("tree_climb");
    private static final String PEARL_MASTERY_ID = id("pearl_mastery");
    private static final String PORTABLE_RIPTIDE_ID = id("portable_riptide");

    private static final String HUNT_STACK_KEY = "hunt_record/stacks";
    private static final String HUNT_TYPE_PREFIX = "hunt_record/type/";
    private static final Identifier HUNT_MAX_HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_advanced/hunt_record_max_health"
    );

    private static final Set<UUID> TREE_CLIMBERS = new HashSet<>();

    private AdvancedUtilityAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[] {
                ARMOR_PIERCE_ID,
                HUNT_RECORD_ID,
                TREE_CLIMB_ID,
                PEARL_MASTERY_ID,
                PORTABLE_RIPTIDE_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> reconcile(
                player,
                AugmentationPlayerStateService.state(player)
        ));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> reconcile(
                newPlayer,
                AugmentationPlayerStateService.state(newPlayer)
        ));
        ServerPlayerEvents.LEAVE.register(AdvancedUtilityAugmentService::clearPlayer);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
                AdvancedUtilityAugmentService::afterKill
        );
        ServerTickEvents.END_SERVER_TICK.register(AdvancedUtilityAugmentService::tickTreeClimb);
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        applyHuntRecordHealth(player, state);
        if (boundedLevel(state, TREE_CLIMB_ID) > 0) {
            TREE_CLIMBERS.add(player.getUUID());
        } else {
            TREE_CLIMBERS.remove(player.getUUID());
        }
    }

    /** Multiplier applied to the armor and armor-toughness inputs of vanilla CombatRules. */
    public static double armorEffectivenessMultiplier(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return 1.0D;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        int level = boundedLevel(state, ARMOR_PIERCE_ID);
        if (level <= 0) {
            return 1.0D;
        }
        return Math.max(
                0.0D,
                1.0D - AdvancedAugmentTuning.current().armorEffectivenessReduction(level)
        );
    }

    /** Adds the permanent Hunt Record ROOT-direct bonus after the existing direct-damage pipeline. */
    public static float modifyRootOutgoingDamage(DamageSource source, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)
                || !(source.getEntity() instanceof ServerPlayer attacker)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return damage;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        if (boundedLevel(state, HUNT_RECORD_ID) <= 0) {
            return damage;
        }
        int stacks = huntStacks(state);
        if (stacks <= 0) {
            return damage;
        }
        double multiplier = 1.0D
                + stacks * AdvancedAugmentTuning.current().huntRecordRootDamageBonusPerStack();
        return safeScale(damage, multiplier);
    }

    /** Pearl self-damage reduction runs before augment protection pools consume the hit. */
    public static float modifyIncomingBeforeProtection(
            LivingEntity victim,
            DamageSource source,
            float damage
    ) {
        if (!(victim instanceof ServerPlayer player)
                || !(damage > 0.0F)
                || !Float.isFinite(damage)
                || !source.typeHolder().is(DamageTypes.ENDER_PEARL)) {
            return damage;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, PEARL_MASTERY_ID);
        if (level <= 0) {
            return damage;
        }
        return safeScale(
                damage,
                1.0D - AdvancedAugmentTuning.current().pearlSelfDamageReduction(level)
        );
    }

    public static int modifyPearlCooldownTicks(ServerPlayer player, int vanillaTicks) {
        if (vanillaTicks <= 0) {
            return vanillaTicks;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, PEARL_MASTERY_ID);
        if (level <= 0) {
            return vanillaTicks;
        }
        double reduction = AdvancedAugmentTuning.current().pearlCooldownReduction(level);
        return Math.max(0, (int) Math.round(vanillaTicks * (1.0D - reduction)));
    }

    /** True only for the dry portable-Riptide substitution; natural wet use never consumes water. */
    public static boolean canUsePortableRiptide(ServerPlayer player, ItemStack trident) {
        if (player.isInWaterOrRain()
                || player.getOffhandItem().isEmpty()
                || !player.getOffhandItem().is(Items.WATER_BUCKET)
                || boundedLevel(
                        AugmentationPlayerStateService.state(player),
                        PORTABLE_RIPTIDE_ID
                ) <= 0) {
            return false;
        }
        return EnchantmentHelper.getTridentSpinAttackStrength(trident, player) > 0.0F;
    }

    public static void consumePortableRiptideWater(ServerPlayer player) {
        if (player.getAbilities().instabuild || !player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return;
        }
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
    }

    private static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || (!(killed instanceof Enemy) && !EntityCompatibilityProfileService.isBoss(killed))
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, HUNT_RECORD_ID) <= 0) {
            return;
        }
        int stacks = huntStacks(state);
        int maxStacks = AdvancedAugmentTuning.current().huntRecordMaxStacks();
        if (stacks >= maxStacks) {
            return;
        }
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (entityId == null) {
            return;
        }
        String typeKey = HUNT_TYPE_PREFIX + entityId;
        if (state.persistentInt(typeKey) > 0) {
            return;
        }
        AugmentationPlayerState next = state
                .withPersistentInt(typeKey, 1)
                .withPersistentInt(HUNT_STACK_KEY, stacks + 1);
        AugmentationPlayerStateService.replace(player, next);
    }

    private static void applyHuntRecordHealth(
            ServerPlayer player,
            AugmentationPlayerState state
    ) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        int stacks = boundedLevel(state, HUNT_RECORD_ID) > 0 ? huntStacks(state) : 0;
        if (stacks <= 0) {
            maxHealth.removeModifier(HUNT_MAX_HEALTH_MODIFIER);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            return;
        }
        double amount = stacks * AdvancedAugmentTuning.current().huntRecordMaxHealthPerStack();
        maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                HUNT_MAX_HEALTH_MODIFIER,
                amount,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void tickTreeClimb(MinecraftServer server) {
        Iterator<UUID> iterator = TREE_CLIMBERS.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int level = boundedLevel(state, TREE_CLIMB_ID);
            if (level <= 0) {
                iterator.remove();
                continue;
            }
            if (!shouldClimbTree(player)) {
                continue;
            }
            double ascent = AdvancedAugmentTuning.current().ladderAscentSpeed()
                    * AdvancedAugmentTuning.current().treeClimbSpeedScale(level);
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.y >= ascent) {
                continue;
            }
            player.setDeltaMovement(velocity.x, ascent, velocity.z);
            player.resetFallDistance();
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static boolean shouldClimbTree(ServerPlayer player) {
        if (player.isDeadOrDying()
                || player.isSpectator()
                || player.isPassenger()
                || player.isInWater()
                || player.onClimbable()
                || !player.horizontalCollision
                || player.getLastClientInput().shift()) {
            return false;
        }
        var input = player.getLastClientInput();
        if (!(input.forward() || input.backward() || input.left() || input.right() || input.jump())) {
            return false;
        }
        BlockPos origin = player.blockPosition();
        for (int yOffset = 0; yOffset <= 1; yOffset++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = origin.above(yOffset).relative(direction);
                var blockState = player.level().getBlockState(pos);
                if (blockState.is(BlockTags.LOGS) || blockState.is(BlockTags.LEAVES)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void clearPlayer(ServerPlayer player) {
        TREE_CLIMBERS.remove(player.getUUID());
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(HUNT_MAX_HEALTH_MODIFIER);
        }
    }

    private static int huntStacks(AugmentationPlayerState state) {
        return Math.min(
                state.persistentInt(HUNT_STACK_KEY),
                AdvancedAugmentTuning.current().huntRecordMaxStacks()
        );
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static float safeScale(float damage, double multiplier) {
        double scaled = damage * Math.max(0.0D, multiplier);
        if (!Double.isFinite(scaled)) {
            return damage;
        }
        return (float) Math.min(Float.MAX_VALUE, scaled);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
