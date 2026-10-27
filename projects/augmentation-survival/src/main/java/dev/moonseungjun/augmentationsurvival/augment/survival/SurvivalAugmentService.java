package dev.moonseungjun.augmentationsurvival.augment.survival;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Shared server-authoritative runtime for survival, mining and utility augments. */
public final class SurvivalAugmentService {
    private static final String MINERS_HAND_ID = id("miners_hand");
    private static final String LOGGING_KNACK_ID = id("logging_knack");
    private static final String EFFICIENT_METABOLISM_ID = id("efficient_metabolism");
    private static final String LONG_BREATH_ID = id("long_breath");
    private static final String COLD_ADAPTATION_ID = id("cold_adaptation");
    private static final String NOCTURNAL_ID = id("nocturnal");
    private static final String CONTINUOUS_MINING_ID = id("continuous_mining");
    private static final String TOOL_CARE_ID = id("tool_care");
    private static final String ARMOR_CARE_ID = id("armor_care");
    private static final String RECOVERY_ID = id("recovery");
    private static final String SUN_AFFINITY_ID = id("sun_affinity");
    private static final String NIGHT_VISION_ID = id("night_vision");
    private static final String AUTO_REFILL_ID = id("auto_refill");

    private static final Identifier OXYGEN_BONUS_ID = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_survival/oxygen_bonus"
    );

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private SurvivalAugmentService() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(player -> {
            clearRuntime(player);
            reconcile(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearRuntime(newPlayer);
            reconcile(newPlayer);
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                onBlockBroken(serverPlayer);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(SurvivalAugmentService::tickRuntime);
    }

    public static void reconcile(ServerPlayer player) {
        reconcile(player, AugmentationPlayerStateService.state(player));
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        applyOxygenBonus(player, boundedLevel(state, LONG_BREATH_ID));
        if (!needsRuntime(state)) {
            RUNTIME.remove(player.getUUID());
            return;
        }
        RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));
    }

    /** Applies all augment mining-speed percentages additively before one final scale. */
    public static float modifyBlockBreakSpeed(ServerPlayer player, BlockState blockState, float original) {
        if (!(original > 0.0F) || !Float.isFinite(original)) {
            return original;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
        double bonus = 0.0D;

        int minersHand = boundedLevel(state, MINERS_HAND_ID);
        bonus += 0.04D * minersHand;

        int logging = boundedLevel(state, LOGGING_KNACK_ID);
        if (logging > 0 && blockState.is(BlockTags.LOGS)) {
            bonus += tuning.loggingBreakSpeedPerLevel() * logging;
        }

        int nocturnal = boundedLevel(state, NOCTURNAL_ID);
        if (nocturnal > 0 && !isDaytime(player)) {
            bonus += tuning.nocturnalMiningSpeedPerLevel() * nocturnal;
        }

        int continuous = boundedLevel(state, CONTINUOUS_MINING_ID);
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        long now = player.level().getGameTime();
        if (continuous > 0 && runtime != null && runtime.miningUntilTick >= now) {
            bonus += tuning.continuousMiningSpeedPerStackPerLevel()
                    * continuous
                    * runtime.miningStacks;
        }

        double scaled = original * (1.0D + bonus);
        return Double.isFinite(scaled) ? (float) Math.min(Float.MAX_VALUE, scaled) : original;
    }

    public static float modifyFoodExhaustion(ServerPlayer player, float original) {
        if (!(original > 0.0F) || !Float.isFinite(original)) {
            return original;
        }
        int level = boundedLevel(
                AugmentationPlayerStateService.state(player),
                EFFICIENT_METABOLISM_ID
        );
        double reduction = SurvivalTuning.current().metabolismExhaustionReductionPerLevel() * level;
        return (float) (original * Math.max(0.0D, 1.0D - reduction));
    }

    /** Called by the FoodData mixin before vanilla advances its natural-regeneration timer. */
    public static int consumeExtraNaturalRegenTimerTicks(ServerPlayer player, int foodLevel) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int recovery = boundedLevel(state, RECOVERY_ID);
        int sunAffinity = boundedLevel(state, SUN_AFFINITY_ID);
        if ((recovery <= 0 && sunAffinity <= 0)
                || foodLevel < 18
                || player.getHealth() >= player.getMaxHealth()) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (runtime != null) {
                runtime.regenTickCredit = 0.0D;
            }
            return 0;
        }

        double reduction = SurvivalTuning.current().recoveryIntervalReductionPerLevel() * recovery;
        if (isDaytime(player)) {
            reduction += SurvivalTuning.current().sunAffinityRegenReductionPerLevel() * sunAffinity;
        }
        reduction = Math.min(0.90D, Math.max(0.0D, reduction));
        if (!(reduction > 0.0D)) {
            return 0;
        }

        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(player));
        runtime.regenTickCredit += 1.0D / (1.0D - reduction) - 1.0D;
        int extra = (int) Math.floor(runtime.regenTickCredit);
        if (extra > 0) {
            runtime.regenTickCredit -= extra;
        }
        return extra;
    }

    /** Cancels the whole durability event on a successful care roll, before vanilla Unbreaking. */
    public static int modifyDurabilityDamage(ServerPlayer player, ItemStack stack, int amount) {
        if (amount <= 0 || stack.isEmpty() || !stack.isDamageableItem()) {
            return amount;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        double chance = 0.0D;
        if (isEquippedArmor(player, stack)) {
            int level = boundedLevel(state, ARMOR_CARE_ID);
            chance = SurvivalTuning.current().armorCareAvoidPerLevel() * level;
        } else if (stack.has(DataComponents.TOOL)) {
            int level = boundedLevel(state, TOOL_CARE_ID);
            chance = SurvivalTuning.current().toolCareAvoidPerLevel() * level;
        }
        if (chance > 0.0D && player.getRandom().nextDouble() < Math.min(1.0D, chance)) {
            return 0;
        }
        return amount;
    }

    private static void onBlockBroken(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, CONTINUOUS_MINING_ID) <= 0) {
            return;
        }
        reconcile(player, state);
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) {
            return;
        }
        long now = player.level().getGameTime();
        SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
        if (runtime.miningUntilTick >= now) {
            runtime.miningStacks = Math.min(tuning.continuousMiningMaxStacks(), runtime.miningStacks + 1);
        } else {
            runtime.miningStacks = 1;
        }
        runtime.miningUntilTick = now + tuning.continuousMiningWindowTicks();
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> iterator = RUNTIME.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            if (!needsRuntime(state)) {
                iterator.remove();
                continue;
            }
            RuntimeState runtime = entry.getValue();
            tickNightVision(player, state);
            tickColdAdaptation(player, state, runtime);
            tickAutoRefill(player, state, runtime);
            if (runtime.miningUntilTick < player.level().getGameTime()) {
                runtime.miningStacks = 0;
            }
        }
    }

    private static void tickNightVision(ServerPlayer player, AugmentationPlayerState state) {
        if (boundedLevel(state, NIGHT_VISION_ID) <= 0) {
            return;
        }
        SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() <= tuning.nightVisionRefreshThresholdTicks()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    tuning.nightVisionRefreshDurationTicks(),
                    0,
                    true,
                    false,
                    false
            ));
        }
    }

    private static void tickColdAdaptation(
            ServerPlayer player,
            AugmentationPlayerState state,
            RuntimeState runtime
    ) {
        int level = boundedLevel(state, COLD_ADAPTATION_ID);
        int frozen = player.getTicksFrozen();
        if (level <= 0) {
            runtime.lastFrozenTicks = frozen;
            runtime.freezeCredit = 0.0D;
            return;
        }
        if (runtime.lastFrozenTicks < 0) {
            runtime.lastFrozenTicks = frozen;
            return;
        }
        if (frozen > runtime.lastFrozenTicks) {
            int increase = frozen - runtime.lastFrozenTicks;
            runtime.freezeCredit += increase * SurvivalTuning.current().freezeReduction(level);
            int remove = (int) Math.floor(runtime.freezeCredit);
            if (remove > 0) {
                frozen = Math.max(runtime.lastFrozenTicks, frozen - remove);
                player.setTicksFrozen(frozen);
                runtime.freezeCredit -= remove;
            }
        } else if (frozen < runtime.lastFrozenTicks) {
            runtime.freezeCredit = 0.0D;
        }
        runtime.lastFrozenTicks = frozen;
    }

    private static void tickAutoRefill(
            ServerPlayer player,
            AugmentationPlayerState state,
            RuntimeState runtime
    ) {
        if (boundedLevel(state, AUTO_REFILL_ID) <= 0) {
            runtime.lastSelectedTemplate = ItemStack.EMPTY;
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack selected = inventory.getSelectedItem();
        if (!selected.isEmpty()) {
            runtime.lastSelectedTemplate = selected.copy();
            runtime.lastSelectedTemplate.setCount(1);
            return;
        }
        if (runtime.lastSelectedTemplate.isEmpty()) {
            return;
        }

        int selectedSlot = inventory.getSelectedSlot();
        for (int slot = Inventory.getSelectionSize(); slot < 36; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty()
                    || !ItemStack.isSameItemSameComponents(candidate, runtime.lastSelectedTemplate)) {
                continue;
            }
            inventory.setItem(selectedSlot, candidate.copy());
            inventory.setItem(slot, ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
            ItemStack replacement = inventory.getSelectedItem();
            runtime.lastSelectedTemplate = replacement.copy();
            runtime.lastSelectedTemplate.setCount(1);
            PassiveProcReadabilityService.presentAutoRefill(player);
            return;
        }
    }

    private static void applyOxygenBonus(ServerPlayer player, int level) {
        AttributeInstance attribute = player.getAttribute(Attributes.OXYGEN_BONUS);
        if (attribute == null) {
            throw new IllegalStateException("Player is missing oxygen bonus attribute");
        }
        if (level <= 0) {
            attribute.removeModifier(OXYGEN_BONUS_ID);
            return;
        }
        attribute.addOrUpdateTransientModifier(new AttributeModifier(
                OXYGEN_BONUS_ID,
                SurvivalTuning.current().oxygenBonus(level),
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static boolean isEquippedArmor(ServerPlayer player, ItemStack stack) {
        return player.getItemBySlot(EquipmentSlot.HEAD) == stack
                || player.getItemBySlot(EquipmentSlot.CHEST) == stack
                || player.getItemBySlot(EquipmentSlot.LEGS) == stack
                || player.getItemBySlot(EquipmentSlot.FEET) == stack;
    }

    private static boolean isDaytime(ServerPlayer player) {
        long dayTime = Math.floorMod(player.level().getOverworldClockTime(), 24_000L);
        return dayTime < 13_000L;
    }

    private static boolean needsRuntime(AugmentationPlayerState state) {
        return boundedLevel(state, COLD_ADAPTATION_ID) > 0
                || boundedLevel(state, CONTINUOUS_MINING_ID) > 0
                || boundedLevel(state, RECOVERY_ID) > 0
                || boundedLevel(state, SUN_AFFINITY_ID) > 0
                || boundedLevel(state, NIGHT_VISION_ID) > 0
                || boundedLevel(state, AUTO_REFILL_ID) > 0;
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private int miningStacks;
        private long miningUntilTick;
        private int lastFrozenTicks = -1;
        private double freezeCredit;
        private double regenTickCredit;
        private ItemStack lastSelectedTemplate = ItemStack.EMPTY;

        private RuntimeState(ServerPlayer player) {
            ItemStack selected = player.getInventory().getSelectedItem();
            if (!selected.isEmpty()) {
                lastSelectedTemplate = selected.copy();
                lastSelectedTemplate.setCount(1);
            }
            lastFrozenTicks = player.getTicksFrozen();
        }
    }
}
