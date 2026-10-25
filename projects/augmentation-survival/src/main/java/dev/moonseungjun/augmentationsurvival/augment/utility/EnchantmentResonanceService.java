package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** Effective-level boundary for the Magic Resonance augment. */
public final class EnchantmentResonanceService {
    private static final String RESONANCE_ID = AugmentationSurvival.MOD_ID + ":enchantment_resonance";
    private static final ThreadLocal<Deque<Optional<ServerPlayer>>> ACTORS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final Set<Identifier> SAFE_EXTERNAL = new HashSet<>();

    private EnchantmentResonanceService() {
    }

    public static void initialize() {
        AugmentCatalog.require(RESONANCE_ID);
    }

    /** External enchantments remain opt-in; vanilla enchantments are always eligible. */
    public static void allowExternal(Identifier enchantmentId) {
        if (!"minecraft".equals(enchantmentId.getNamespace())) {
            SAFE_EXTERNAL.add(enchantmentId);
        }
    }

    public static void pushActor(Entity entity) {
        ACTORS.get().push(Optional.ofNullable(resolvePlayer(entity)));
    }

    public static void pushRelevantActor(ServerLevel level, ItemStack stack, Entity entity) {
        ServerPlayer direct = resolvePlayer(entity);
        ACTORS.get().push(Optional.ofNullable(direct != null ? direct : findItemOwner(level, stack)));
    }

    public static void pushItemOwner(ServerLevel level, ItemStack stack) {
        ACTORS.get().push(Optional.ofNullable(findItemOwner(level, stack)));
    }

    public static void popActor() {
        Deque<Optional<ServerPlayer>> stack = ACTORS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            ACTORS.remove();
        }
    }

    public static int effectiveLevel(Holder<Enchantment> enchantment, int storedLevel) {
        Deque<Optional<ServerPlayer>> stack = ACTORS.get();
        ServerPlayer player = stack.isEmpty() ? null : stack.peek().orElse(null);
        return effectiveLevel(player, enchantment, storedLevel);
    }

    public static int effectiveLevel(
            LivingEntity entity,
            Holder<Enchantment> enchantment,
            int storedLevel
    ) {
        return effectiveLevel(entity instanceof ServerPlayer player ? player : null, enchantment, storedLevel);
    }

    private static int effectiveLevel(
            ServerPlayer player,
            Holder<Enchantment> enchantment,
            int storedLevel
    ) {
        if (player == null || storedLevel <= 0 || !isAllowed(enchantment)) {
            return storedLevel;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int resonance = Math.max(0, Math.min(
                state.level(RESONANCE_ID),
                AugmentCatalog.require(RESONANCE_ID).maxLevel()
        ));
        return resonance <= 0 ? storedLevel : storedLevel + resonance;
    }

    private static ServerPlayer resolvePlayer(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean isAllowed(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> {
            Identifier id = key.identifier();
            return "minecraft".equals(id.getNamespace()) || SAFE_EXTERNAL.contains(id);
        }).orElse(false);
    }

    private static ServerPlayer findItemOwner(ServerLevel level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) {
            return null;
        }
        for (ServerPlayer player : level.players()) {
            if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) {
                return player;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot) == stack) {
                    return player;
                }
            }
        }
        return null;
    }
}
