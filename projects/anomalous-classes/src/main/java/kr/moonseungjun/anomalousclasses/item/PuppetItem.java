package kr.moonseungjun.anomalousclasses.item;

import java.util.List;
import java.util.Optional;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class PuppetItem extends Item {
    public record Identity(PuppetBlueprint blueprint, int revision, boolean broken) {}

    public PuppetItem(Properties properties) { super(properties); }

    public static ItemStack make(PuppetBlueprint blueprint, int revision, boolean broken) {
        ItemStack item = new ItemStack(PuppetItems.PUPPET.get());
        CompoundTag identity = new CompoundTag();
        identity.put("blueprint", blueprint.save());
        identity.putInt("revision", revision);
        identity.putBoolean("broken", broken);
        CompoundTag tag = new CompoundTag();
        tag.put("anomalous_puppet", identity);
        CustomData.set(DataComponents.CUSTOM_DATA, item, tag);
        return item;
    }

    public static Optional<Identity> read(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(PuppetItems.PUPPET.get())) return Optional.empty();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound("anomalous_puppet");
        if (!tag.contains("blueprint") || tag.getInt("revision") < 1) return Optional.empty();
        return PuppetBlueprint.load(tag.getCompound("blueprint"))
                .map(p -> new Identity(p, tag.getInt("revision"), tag.getBoolean("broken")));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        return PuppetService.deploy(player, context) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        read(stack).ifPresent(identity -> {
            PuppetBlueprint b = identity.blueprint();
            tooltip.add(Component.translatable("tooltip.anomalousclasses.puppet.configuration",
                    Component.translatable("puppet.anomalousclasses.frame." + b.frame().name().toLowerCase()),
                    Component.translatable("puppet.anomalousclasses.device." + b.device().name().toLowerCase()),
                    Component.translatable("puppet.anomalousclasses.core." + b.core().name().toLowerCase()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.anomalousclasses.puppet.health",
                    (int)Math.round(b.healthRatio() * 100.0), b.size().height())
                    .withStyle(ChatFormatting.AQUA));
            if (identity.broken()) tooltip.add(Component.translatable(
                    "tooltip.anomalousclasses.puppet.broken").withStyle(ChatFormatting.RED));
        });
    }
}
