package kr.moonseungjun.anomalousclasses.item;

import java.util.Map;
import java.util.Optional;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetEconomy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

/** A physical reusable module; replacing one never converts it into free raw materials. */
public final class PuppetPartItem extends Item {
    public enum Kind { FRAME, DEVICE, CORE }
    public record Module(Kind kind, int variant, PuppetBlueprint.Size size,
                         double condition, Map<String,Integer> paid) {
        public Module {
            if (kind == null || size == null || !Double.isFinite(condition)
                    || condition < 0 || condition > 1 || variant < 0
                    || variant >= (kind == Kind.FRAME ? 3 : kind == Kind.DEVICE ? 3 : 4)
                    || (kind != Kind.FRAME && size != PuppetBlueprint.Size.NORMAL)
                    || !PuppetEconomy.partMaterials(kind, variant, size).equals(paid))
                throw new IllegalArgumentException("Invalid physical puppet component");
            paid = Map.copyOf(paid);
        }
    }
    public PuppetPartItem(Properties properties) { super(properties); }

    public static ItemStack create(Kind kind, int variant, PuppetBlueprint.Size size,
                                   double condition) {
        PuppetBlueprint.Size tagSize = kind == Kind.FRAME ? size : PuppetBlueprint.Size.NORMAL;
        Module module = new Module(kind, variant, tagSize, condition,
                PuppetEconomy.partMaterials(kind, variant, tagSize));
        ItemStack result = new ItemStack(PuppetItems.PART.get());
        CompoundTag part = new CompoundTag();
        part.putString("kind", module.kind().name());
        part.putInt("variant", module.variant());
        part.putString("size", module.size().name());
        part.putDouble("condition", module.condition());
        CompoundTag root = new CompoundTag();
        root.put("marionette_module", part);
        CustomData.set(DataComponents.CUSTOM_DATA, result, root);
        return result;
    }

    public static Optional<Module> read(ItemStack item) {
        if (item == null || item.isEmpty() || !item.is(PuppetItems.PART.get())) return Optional.empty();
        CompoundTag part = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound("marionette_module");
        try {
            if (!part.contains("kind") || !part.contains("size") ||
                    !part.contains("variant") || !part.contains("condition"))
                return Optional.empty();
            Kind kind = Kind.valueOf(part.getString("kind"));
            int variant = part.getInt("variant");
            PuppetBlueprint.Size size = PuppetBlueprint.Size.valueOf(part.getString("size"));
            double condition = part.getDouble("condition");
            return Optional.of(new Module(kind, variant, size, condition,
                    PuppetEconomy.partMaterials(kind, variant, size)));
        } catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        read(stack).ifPresent(part -> {
            tooltip.add(Component.translatable("tooltip.anomalousclasses.puppet.part",
                    Component.translatable("puppet.anomalousclasses.part." +
                            part.kind().name().toLowerCase()),
                    Component.translatable("puppet.anomalousclasses." +
                            part.kind().name().toLowerCase() + "." +
                            (part.kind() == Kind.FRAME
                                    ? PuppetBlueprint.Frame.values()[part.variant()].name().toLowerCase()
                                    : part.kind() == Kind.DEVICE
                                    ? PuppetBlueprint.Device.values()[part.variant()].name().toLowerCase()
                                    : PuppetBlueprint.Core.values()[part.variant()].name().toLowerCase())))
                    .withStyle(ChatFormatting.GRAY));
            if (part.kind() == Kind.FRAME)
                tooltip.add(Component.translatable("tooltip.anomalousclasses.puppet.part_size",
                        part.size().height()).withStyle(ChatFormatting.GOLD));
            if (part.condition() < .99999)
                tooltip.add(Component.translatable("tooltip.anomalousclasses.puppet.part_condition",
                        (int) Math.round(part.condition() * 100)).withStyle(ChatFormatting.GRAY));
        });
    }
}
