package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Optional;

public enum MagicRune {
    FIRE_ESSENCE(1, Items.BLAZE_POWDER, "불꽃 정수"),
    FROST_ESSENCE(2, Items.SNOWBALL, "서리 정수"),
    VOID_ESSENCE(3, Items.ENDER_PEARL, "공허 정수"),
    RING_FORM(11, Items.REDSTONE, "고리 형상"),
    SEAL_FORM(12, Items.QUARTZ, "봉인 형상"),
    PULSE_FORM(13, Items.AMETHYST_SHARD, "파동 형상"),
    BURST_FUEL(21, Items.GUNPOWDER, "폭발 촉매"),
    CALM_FUEL(22, Items.SUGAR, "안정 촉매"),
    LIGHT_FUEL(23, Items.GLOWSTONE_DUST, "광휘 촉매");

    private final int code;
    private final Item item;
    private final String displayName;

    MagicRune(int code, Item item, String displayName) {
        this.code = code;
        this.item = item;
        this.displayName = displayName;
    }

    public int code() { return code; }
    public Item item() { return item; }
    public String displayName() { return displayName; }

    public static Optional<MagicRune> fromStack(ItemStack stack) {
        return Arrays.stream(values()).filter(rune -> stack.is(rune.item)).findFirst();
    }

    public static Optional<MagicRune> fromCode(int code) {
        return Arrays.stream(values()).filter(rune -> rune.code == code).findFirst();
    }
}
