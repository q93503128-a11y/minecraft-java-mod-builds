package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Final ordinary-drop multiplier for the Mythic abundance augment.
 *
 * <p>Copies are delayed until compatible vanilla/mod and augmentation-added drops have entered the
 * world, then each eligible fresh stack is claimed exactly once. This preserves Fortune/Looting and
 * the harvest runtime's extra-output ordering without re-running loot tables.</p>
 */
public final class AbundanceService {
    private static final String ABUNDANCE_ID = id("abundance");
    private static final List<PendingCapture> PENDING = new ArrayList<>();
    private static final Map<UUID, Long> GENERATED = new HashMap<>();
    private static final Map<UUID, Long> CLAIMED = new HashMap<>();

    private AbundanceService() {
    }

    public static void initialize() {
        AugmentCatalog.require(ABUNDANCE_ID);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel level
                    && player instanceof ServerPlayer serverPlayer
                    && hasAbundance(serverPlayer)) {
                schedule(
                        level,
                        serverPlayer,
                        Vec3.atCenterOf(pos),
                        ConsumptionAugmentTuning.current().abundanceBlockDelayTicks()
                );
            }
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AbundanceService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(AbundanceService::tick);
    }

    public static void scheduleFishing(ServerPlayer player, Vec3 hookPosition) {
        if (!(player.level() instanceof ServerLevel level) || !hasAbundance(player)) {
            return;
        }
        schedule(
                level,
                player,
                hookPosition,
                ConsumptionAugmentTuning.current().abundanceFishingDelayTicks()
        );
    }

    private static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || killed instanceof Player
                || CombatExpansionService.isConfiguredBoss(killed)
                || !hasAbundance(player)) {
            return;
        }
        schedule(
                level,
                player,
                killed.position(),
                ConsumptionAugmentTuning.current().abundanceMobDelayTicks()
        );
    }

    private static void schedule(ServerLevel level, ServerPlayer player, Vec3 position, int delay) {
        PENDING.add(new PendingCapture(
                level.dimension(),
                position,
                player.getUUID(),
                level.getGameTime() + Math.max(1, delay)
        ));
    }

    private static void tick(MinecraftServer server) {
        Iterator<PendingCapture> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingCapture pending = iterator.next();
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null || level.getGameTime() < pending.readyTick()) {
                continue;
            }
            iterator.remove();
            capture(level, pending);
        }

        long now = server.overworld().getGameTime();
        GENERATED.entrySet().removeIf(entry -> entry.getValue() < now);
        CLAIMED.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private static void capture(ServerLevel level, PendingCapture pending) {
        double radius = ConsumptionAugmentTuning.current().abundanceCaptureRadius();
        AABB box = new AABB(
                pending.position().x - radius,
                pending.position().y - radius,
                pending.position().z - radius,
                pending.position().x + radius,
                pending.position().y + radius,
                pending.position().z + radius
        );
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                item -> item.tickCount <= ConsumptionAugmentTuning.current().abundanceMaxItemAgeTicks()
                        && !GENERATED.containsKey(item.getUUID())
                        && !CLAIMED.containsKey(item.getUUID())
                        && eligible(item.getItem())
        );
        Set<UUID> capturedNow = new HashSet<>();
        for (ItemEntity item : items) {
            if (!capturedNow.add(item.getUUID())) {
                continue;
            }
            ItemStack stack = item.getItem();
            if (!eligible(stack)) {
                continue;
            }
            CLAIMED.put(item.getUUID(), level.getGameTime() + 8L);
            ItemEntity duplicate = new ItemEntity(
                    level,
                    item.getX(),
                    item.getY(),
                    item.getZ(),
                    stack.copy()
            );
            duplicate.setDeltaMovement(item.getDeltaMovement());
            if (level.addFreshEntity(duplicate)) {
                GENERATED.put(duplicate.getUUID(), level.getGameTime() + 8L);
            }
        }
    }

    private static boolean eligible(ItemStack stack) {
        if (stack.isEmpty()
                || stack.getMaxStackSize() <= 1
                || stack.isDamageableItem()
                || stack.get(DataComponents.CUSTOM_DATA) != null
                || isCore(stack)) {
            return false;
        }
        return true;
    }

    private static boolean isCore(ItemStack stack) {
        for (AugmentTier tier : AugmentTier.values()) {
            if (stack.is(AugmentCoreItems.item(tier))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAbundance(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return state.level(ABUNDANCE_ID) > 0;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record PendingCapture(
            ResourceKey<Level> dimension,
            Vec3 position,
            UUID playerId,
            long readyTick
    ) {
    }
}
