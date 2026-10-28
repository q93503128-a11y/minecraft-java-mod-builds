package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative bridge for the advanced passive/utility batch. */
public final class AdvancedUtilityAugmentService {
    private static final String ARMOR_PIERCE_ID = id("armor_pierce");
    private static final String HUNT_RECORD_ID = id("hunt_record");
    private static final String TREE_CLIMB_ID = id("tree_climb");
    private static final String PEARL_MASTERY_ID = id("pearl_mastery");
    private static final String PORTABLE_RIPTIDE_ID = id("portable_riptide");

    private static final String HUNT_STACK_KEY = "hunt_record/stacks";
    private static final String HUNT_TYPE_PREFIX = "hunt_record/type/";
    private static final String PORTABLE_RIPTIDE_USE_KEY =
            "augmentation_survival.portable_riptide_uses";
    private static final int PORTABLE_RIPTIDE_USES_PER_BUCKET = 10;
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

    /** Exact ender-pearl self-damage scaling, called from the pearl landing path. */
    public static float modifyPearlSelfDamage(ServerPlayer player, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }
        int level = boundedLevel(AugmentationPlayerStateService.state(player), PEARL_MASTERY_ID);
        if (level <= 0) {
            return damage;
        }
        return safeScale(
                damage,
                1.0D - AdvancedAugmentTuning.current().pearlSelfDamageReduction(level)
        );
    }

    public static int modifyPearlCooldownTicks(ServerPlayer player, int vanillaTicks) {
        int level = boundedLevel(AugmentationPlayerStateService.state(player), PEARL_MASTERY_ID);
        return modifyPearlCooldownTicksForLevel(level, vanillaTicks);
    }

    /** Shared by the server and the client cooldown mirrors. */
    public static int modifyPearlCooldownTicksForLevel(int level, int vanillaTicks) {
        if (vanillaTicks <= 0 || level <= 0) {
            return vanillaTicks;
        }
        double reduction = AdvancedAugmentTuning.current().pearlCooldownReduction(level);
        return Math.max(0, (int) Math.round(vanillaTicks * (1.0D - reduction)));
    }

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

    public static int portableRiptideUsesRemaining(ItemStack waterBucket) {
        if (!waterBucket.is(Items.WATER_BUCKET)) {
            return 0;
        }
        CustomData data = waterBucket.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int used = data.copyTag().getInt(PORTABLE_RIPTIDE_USE_KEY).orElse(0);
        used = Mth.clamp(used, 0, PORTABLE_RIPTIDE_USES_PER_BUCKET - 1);
        return PORTABLE_RIPTIDE_USES_PER_BUCKET - used;
    }

    /** One filled water bucket powers ten successful dry Riptide activations. */
    public static void consumePortableRiptideWater(ServerPlayer player) {
        if (player.getAbilities().instabuild || !player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return;
        }
        ItemStack waterBucket = player.getOffhandItem();
        int remaining = portableRiptideUsesRemaining(waterBucket);
        if (remaining <= 1) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
            return;
        }
        int nextUsed = PORTABLE_RIPTIDE_USES_PER_BUCKET - remaining + 1;
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                waterBucket,
                tag -> tag.putInt(PORTABLE_RIPTIDE_USE_KEY, nextUsed)
        );
    }

    private static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || !CompanionAugmentService.isHostileEntity(killed)
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
                || player.getLastClientInput().shift()) {
            return false;
        }
        var input = player.getLastClientInput();
        boolean climbIntent = input.forward()
                || input.jump()
                || player.horizontalCollision
                && (input.backward() || input.left() || input.right());
        return climbIntent && touchesTreeSurface(player);
    }

    /** Uses physical proximity instead of relying solely on the transient horizontalCollision flag. */
    private static boolean touchesTreeSurface(ServerPlayer player) {
        AABB box = player.getBoundingBox();
        int minX = Mth.floor(box.minX - 0.08D);
        int maxX = Mth.floor(box.maxX + 0.08D);
        int minY = Mth.floor(box.minY + 0.05D);
        int maxY = Mth.floor(box.maxY - 0.05D);
        int minZ = Mth.floor(box.minZ - 0.08D);
        int maxZ = Mth.floor(box.maxZ + 0.08D);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    var state = player.level().getBlockState(cursor);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        return true;
                    }
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
