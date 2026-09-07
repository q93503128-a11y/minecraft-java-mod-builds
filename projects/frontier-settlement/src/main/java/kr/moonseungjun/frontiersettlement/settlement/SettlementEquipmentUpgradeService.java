package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic blacksmith reinforcement for normal and companion equipment.
 *
 * The stack stores only one bounded reinforcement level in CUSTOM_DATA. Gameplay attributes
 * are added through NeoForge's ItemAttributeModifierEvent, so the original/default component,
 * enchantments, custom name/lore and every unrelated component remain authoritative and intact.
 * Reinforcement consumes only real copper/iron items from loaded settlement storage; gold,
 * diamonds and arbitrary modded high-value metals are never implicit blacksmith payment.
 */
public final class SettlementEquipmentUpgradeService {
    public static final int MAX_REINFORCEMENT_LEVEL = 5;
    public static final double ATTACK_DAMAGE_PER_LEVEL = 0.5D;
    public static final double ARMOR_PER_LEVEL = 0.25D;

    private static final String LEVEL_TAG = "frontier_settlement_blacksmith_reinforcement";
    private static final Identifier ATTACK_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            FrontierSettlement.MOD_ID, "blacksmith_attack_reinforcement");
    private static final Identifier ARMOR_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            FrontierSettlement.MOD_ID, "blacksmith_armor_reinforcement");

    private SettlementEquipmentUpgradeService() {}

    public static int reinforcementCap(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET, VILLAGE -> 1;
            case FRONTIER_TOWN -> 2;
            case DOMAIN -> 4;
            case FRONTIER_CAPITAL -> 5;
        };
    }

    public static int nextLevelCost(int nextLevel) {
        return switch (Math.max(1, Math.min(MAX_REINFORCEMENT_LEVEL, nextLevel))) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 12;
            case 4 -> 18;
            default -> 26;
        };
    }

    public static int reinforcementLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(MAX_REINFORCEMENT_LEVEL, tag.getIntOr(LEVEL_TAG, 0)));
    }

    /**
     * @return true when this stack is reinforcement-capable and the blacksmith interaction
     *         was handled, including cap/resource failure messages. Unsupported items return false
     *         so ordinary anvil interaction is not hijacked.
     */
    public static boolean tryReinforce(ServerPlayer player, ServerLevel level, MinecraftServer server,
                                       SettlementData data, ItemStack stack) {
        ItemAttributeModifiers base = baseModifiers(stack);
        boolean attack = hasAttribute(base, Attributes.ATTACK_DAMAGE);
        boolean armor = hasAttribute(base, Attributes.ARMOR);
        if (!attack && !armor) return false;

        if (stack.getCount() != 1) {
            player.sendSystemMessage(Component.literal("§6[마을] §f강화할 장비는 한 번에 1개만 들고 있어야 합니다."));
            return true;
        }

        int current = reinforcementLevel(stack);
        int cap = reinforcementCap(data);
        if (current >= cap) {
            player.sendSystemMessage(Component.literal("§6[마을] §f현재 마을 단계의 대장간 강화 한도는 +" + cap + "입니다."));
            return true;
        }

        int next = current + 1;
        int cost = nextLevelCost(next);
        if (!SettlementStorageService.storageAvailable(level, data)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f공동 저장소가 모두 로드되어야 대장간 강화를 사용할 수 있습니다."));
            return true;
        }
        long available = countBlacksmithMetalItems(level, data);
        if (available < cost) {
            player.sendSystemMessage(Component.literal("§6[마을] §f강화 재료가 부족합니다. 필요: 구리/철 " + cost
                    + "개 · 현재 " + Math.max(0L, available) + "개"));
            return true;
        }
        if (!consumeBlacksmithMetalItems(level, data, cost)) {
            player.sendSystemMessage(Component.literal("§6[마을] §f강화 재료를 안전하게 인출하지 못했습니다. 자원 상태를 다시 확인해 주세요."));
            return true;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(LEVEL_TAG, next));
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        String gain = attack && armor
                ? "공격력 +" + (next * ATTACK_DAMAGE_PER_LEVEL) + " · 방어력 +" + (next * ARMOR_PER_LEVEL)
                : attack
                        ? "공격력 +" + (next * ATTACK_DAMAGE_PER_LEVEL)
                        : "방어력 +" + (next * ARMOR_PER_LEVEL);
        player.sendSystemMessage(Component.literal("§6[마을] §f대장간 강화 +" + next + " 완료 · " + gain
                + " · 구리/철 " + cost + "개 소비"));
        return true;
    }

    /** Blacksmith maintenance and reinforcement intentionally accept only common copper/iron. */
    public static boolean isBlacksmithMetal(ItemStack stack) {
        return stack.is(Items.COPPER_INGOT) || stack.is(Items.RAW_COPPER)
                || stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON);
    }

    public static long countBlacksmithMetalItems(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return -1L;
        long count = 0L;
        for (BlockPos pos : blacksmithStoragePositions(level, data)) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (isBlacksmithMetal(stack)) count += stack.getCount();
            }
        }
        return count;
    }

    /** Exact physical-item consumption after a full availability check; no partial payment. */
    public static boolean consumeBlacksmithMetalItems(ServerLevel level, SettlementData data, long amount) {
        if (amount <= 0L) return true;
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        if (countBlacksmithMetalItems(level, data) < amount) return false;

        long left = amount;
        // Preserve iron where possible: ordinary copper is spent before raw copper, then iron.
        for (int pass = 0; pass < 4 && left > 0L; pass++) {
            for (BlockPos pos : blacksmithStoragePositions(level, data)) {
                if (left <= 0L) break;
                if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
                boolean changed = false;
                for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!matchesPass(stack, pass)) continue;
                    int take = (int) Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                    changed = true;
                }
                if (changed) container.setChanged();
            }
        }
        return left == 0L;
    }

    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        int level = reinforcementLevel(event.getItemStack());
        if (level <= 0) return;

        EquipmentSlotGroup attackSlot = firstAttributeSlot(event.getDefaultModifiers(), Attributes.ATTACK_DAMAGE);
        if (attackSlot != null) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ATTACK_MODIFIER_ID, level * ATTACK_DAMAGE_PER_LEVEL,
                            AttributeModifier.Operation.ADD_VALUE),
                    attackSlot);
        }
        EquipmentSlotGroup armorSlot = firstAttributeSlot(event.getDefaultModifiers(), Attributes.ARMOR);
        if (armorSlot != null) {
            event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(ARMOR_MODIFIER_ID, level * ARMOR_PER_LEVEL,
                            AttributeModifier.Operation.ADD_VALUE),
                    armorSlot);
        }
    }

    private static ItemAttributeModifiers baseModifiers(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (modifiers.modifiers().isEmpty()) {
            modifiers = stack.getItem().getDefaultAttributeModifiers(stack);
        }
        return modifiers;
    }

    private static boolean hasAttribute(ItemAttributeModifiers modifiers, Holder<Attribute> attribute) {
        return firstAttributeSlot(modifiers, attribute) != null;
    }

    private static EquipmentSlotGroup firstAttributeSlot(ItemAttributeModifiers modifiers,
                                                          Holder<Attribute> attribute) {
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(attribute)) return entry.slot();
        }
        return null;
    }

    private static List<BlockPos> blacksmithStoragePositions(ServerLevel level, SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(SettlementStorageService.storagePositions(data));
        return new ArrayList<>(positions);
    }

    private static boolean matchesPass(ItemStack stack, int pass) {
        return switch (pass) {
            case 0 -> stack.is(Items.COPPER_INGOT);
            case 1 -> stack.is(Items.RAW_COPPER);
            case 2 -> stack.is(Items.IRON_INGOT);
            default -> stack.is(Items.RAW_IRON);
        };
    }
}
