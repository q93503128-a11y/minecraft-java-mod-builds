package dev.moonseungjun.augmentationsurvival.augment.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Bridges the damage-source context into the exact vanilla blocking-item durability boundary. */
public final class ShieldCompressionDurabilityContext {
    private static final ThreadLocal<Deque<Attempt>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private ShieldCompressionDurabilityContext() {
    }

    public static void begin(ServerPlayer player, DamageSource source, ItemStack blockingItem) {
        if (player == null || source == null || blockingItem == null || blockingItem.isEmpty()
                || !blockingItem.isDamageableItem()) {
            return;
        }
        STACK.get().push(new Attempt(
                player,
                source,
                blockingItem,
                blockingItem.getDamageValue(),
                blockingItem.getCount()
        ));
    }

    public static void afterVanillaDurabilityStep(
            LivingEntity user,
            ItemStack blockingItem,
            float blockedDamage
    ) {
        Deque<Attempt> stack = STACK.get();
        if (stack.isEmpty()) return;
        Attempt attempt = stack.peek();
        if (attempt == null || attempt.counted || user != attempt.player || blockingItem != attempt.blockingItem) {
            return;
        }

        boolean durabilityChanged = blockingItem.getDamageValue() > attempt.damageBefore
                || blockingItem.getCount() < attempt.countBefore
                || blockingItem.isEmpty();
        if (!durabilityChanged) return;

        attempt.counted = true;
        ShieldCompressionService.onDurabilityBlocking(attempt.player, attempt.source, blockedDamage);
    }

    public static void end(ServerPlayer player) {
        Deque<Attempt> stack = STACK.get();
        if (stack.isEmpty()) return;
        Attempt attempt = stack.peek();
        if (attempt != null && attempt.player == player) stack.pop();
        else stack.removeIf(candidate -> candidate.player == player);
        if (stack.isEmpty()) STACK.remove();
    }

    private static final class Attempt {
        private final ServerPlayer player;
        private final DamageSource source;
        private final ItemStack blockingItem;
        private final int damageBefore;
        private final int countBefore;
        private boolean counted;

        private Attempt(
                ServerPlayer player,
                DamageSource source,
                ItemStack blockingItem,
                int damageBefore,
                int countBefore
        ) {
            this.player = player;
            this.source = source;
            this.blockingItem = blockingItem;
            this.damageBefore = damageBefore;
            this.countBefore = countBefore;
        }
    }
}
