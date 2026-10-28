package dev.moonseungjun.augmentationsurvival.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/** Persistent escrow for the exact shield stack while Shield Throw is physically in flight. */
public record ShieldThrowEscrowState(ItemStack shield, boolean offhand) {
    public static final Codec<ShieldThrowEscrowState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("shield").forGetter(ShieldThrowEscrowState::shield),
            Codec.BOOL.optionalFieldOf("offhand", true).forGetter(ShieldThrowEscrowState::offhand)
    ).apply(instance, ShieldThrowEscrowState::new));

    public ShieldThrowEscrowState {
        shield = shield == null ? ItemStack.EMPTY : shield.copy();
    }

    public static ShieldThrowEscrowState empty() {
        return new ShieldThrowEscrowState(ItemStack.EMPTY, true);
    }

    public boolean active() {
        return !shield.isEmpty();
    }
}
