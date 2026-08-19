package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Stackable tactical supplies sold by the storehouse. Buff state is intentionally transient. */
public final class VillageConsumableSystem {
    private static final Map<String, Long> READY_AT = new LinkedHashMap<>();
    private static final Map<UUID, Long> ARCANE_SURGE_UNTIL = new LinkedHashMap<>();

    private VillageConsumableSystem() {}

    public static void resetTransientState() {
        READY_AT.clear();
        ARCANE_SURGE_UNTIL.clear();
    }

    public static List<Consumable> catalog() {
        return List.of(Consumable.values());
    }

    public static boolean unlocked(Consumable consumable) {
        return consumable != null && VillageCouncilState.currentDay() >= consumable.requiredDay();
    }

    public static int effectiveCost(Consumable consumable) {
        if (consumable == null) return 0;
        return Math.max(12, consumable.baseCost() - VillageProgressionSystem.storehouseLevel() * 2);
    }

    public static int bundleCount(Consumable consumable) {
        if (consumable == null) return 0;
        int logisticsBonus = consumable == Consumable.BANDAGE
                ? VillageProgressionSystem.storehouseLevel() / 2 : 0;
        return consumable.baseBundle() + logisticsBonus;
    }

    public static String status(Consumable consumable) {
        if (consumable == null) return "알 수 없음";
        if (!unlocked(consumable)) return "제 " + consumable.requiredDay() + "일 해금";
        return "구매 가능";
    }

    public static String purchase(ServerPlayer player, String id) {
        Consumable consumable = Consumable.fromId(id);
        if (consumable == null) return "알 수 없는 전투 소모품입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 전투 소모품을 구매할 수 없습니다.";
        }
        if (!unlocked(consumable)) return "제 " + consumable.requiredDay() + "일부터 입고됩니다.";
        int cost = effectiveCost(consumable);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        ItemStack stack = consumable.item().getDefaultInstance();
        stack.setCount(bundleCount(consumable));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(consumable.displayName()).withStyle(consumable.color()));
        VillageConsumableIdentity.stamp(stack, consumable.id());
        if (!player.addItem(stack)) player.drop(stack, false);
        return consumable.displayName() + " ×" + stack.getCount() + " 구매 완료 | 남은 주화 "
                + VillageProgressionSystem.coins(player);
    }

    public static boolean handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return false;
        ItemStack stack = player.getItemInHand(event.getHand());
        Consumable consumable = match(stack);
        if (consumable == null) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (VillageRespawnSystem.isDowned(player)) {
            player.sendSystemMessage(Component.literal("§c부활 대기 중에는 전투 소모품을 사용할 수 없습니다."));
            return true;
        }
        long now = player.level().getGameTime();
        String key = player.getUUID() + "|" + consumable.id();
        long readyAt = READY_AT.getOrDefault(key, 0L);
        if (readyAt > now) {
            player.sendSystemMessage(Component.literal("§e" + consumable.displayName() + " 재사용까지 "
                    + Math.max(1L, (readyAt - now + 19L) / 20L) + "초"));
            return true;
        }
        String result = apply(player, consumable, now);
        if (result == null || result.isBlank()) return true;
        stack.shrink(1);
        player.getInventory().setChanged();
        READY_AT.put(key, now + consumable.cooldownTicks());
        player.sendSystemMessage(Component.literal("§a[전투 소모품] §f" + result));
        return true;
    }

    public static float skillMultiplier(ServerPlayer player) {
        if (player == null) return 1.0f;
        long until = ARCANE_SURGE_UNTIL.getOrDefault(player.getUUID(), 0L);
        if (until <= player.level().getGameTime()) {
            ARCANE_SURGE_UNTIL.remove(player.getUUID());
            return 1.0f;
        }
        return 1.20f;
    }

    private static String apply(ServerPlayer player, Consumable consumable, long now) {
        return switch (consumable) {
            case BANDAGE -> {
                if (player.getHealth() >= player.getMaxHealth()) {
                    player.sendSystemMessage(Component.literal("§e체력이 가득 차 있어 응급 붕대를 사용하지 않았습니다."));
                    yield "";
                }
                float heal = 6.0f + VillageProgressionSystem.storehouseLevel() * 0.8f;
                player.heal(heal);
                yield "응급 붕대 사용 · 체력 " + String.format(Locale.ROOT, "%.1f", heal / 2.0f) + "칸 회복";
            }
            case CLEANSER -> {
                boolean affected = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)
                        || player.hasEffect(MobEffects.WEAKNESS) || player.hasEffect(MobEffects.SLOWNESS)
                        || player.hasEffect(MobEffects.DARKNESS);
                if (!affected) {
                    player.sendSystemMessage(Component.literal("§e정화할 약화 효과가 없어 정화 약제를 사용하지 않았습니다."));
                    yield "";
                }
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.DARKNESS);
                player.heal(2.0f);
                yield "정화 약제 사용 · 독/위더/약화/둔화/암흑 제거";
            }
            case STIMULANT -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 15, 1));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 15, 0));
                yield "전투 자극제 사용 · 15초간 신속 II + 힘 I";
            }
            case AEGIS_TONIC -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 1));
                yield "수호 비약 사용 · 20초간 피해 저항 + 흡수 보호막";
            }
            case ARCANE_CATALYST -> {
                ARCANE_SURGE_UNTIL.put(player.getUUID(), now + 20 * 20L);
                yield "비전 촉진제 사용 · 20초간 직업 기술 피해·치유 +20%";
            }
            case FIELD_REPAIR_KIT -> {
                String repaired = VillagePlacedTurretSystem.fieldRepairNearest(player,
                        70 + VillageProgressionSystem.storehouseLevel() * 15);
                if (repaired.isBlank()) {
                    player.sendSystemMessage(Component.literal("§e12블록 안에 수리가 필요한 가동 포탑이 없어 키트를 사용하지 않았습니다."));
                    yield "";
                }
                yield repaired;
            }
        };
    }

    private static Consumable match(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Consumable consumable = Consumable.fromId(VillageConsumableIdentity.id(stack));
        return consumable != null && stack.getItem() == consumable.item() ? consumable : null;
    }

    public enum Consumable {
        BANDAGE("bandage", "응급 붕대", Items.PAPER, 1, 28, 2, 20 * 6,
                "즉시 체력 회복 · 보급소 레벨이 높을수록 회복량/묶음 증가", ChatFormatting.WHITE),
        CLEANSER("cleanser", "정화 약제", Items.HONEYCOMB, 2, 36, 1, 20 * 12,
                "독·위더·약화·둔화·암흑 제거 + 소량 회복", ChatFormatting.YELLOW),
        STIMULANT("stimulant", "전투 자극제", Items.SUGAR, 3, 48, 1, 20 * 40,
                "15초간 신속 II + 힘 I", ChatFormatting.RED),
        AEGIS_TONIC("aegis_tonic", "수호 비약", Items.PRISMARINE_CRYSTALS, 5, 62, 1, 20 * 45,
                "20초간 피해 저항 + 흡수 보호막", ChatFormatting.AQUA),
        ARCANE_CATALYST("arcane_catalyst", "비전 촉진제", Items.AMETHYST_SHARD, 6, 72, 1, 20 * 50,
                "20초간 직업 기술 피해·치유 +20%", ChatFormatting.LIGHT_PURPLE),
        FIELD_REPAIR_KIT("field_repair_kit", "응급 포탑 수리 키트", Items.IRON_INGOT, 7, 84, 1, 20 * 18,
                "12블록 안의 손상된 가동 포탑을 전투 중에도 즉시 수리", ChatFormatting.GOLD);

        private final String id, displayName, description;
        private final Item item;
        private final int requiredDay, baseCost, baseBundle, cooldownTicks;
        private final ChatFormatting color;

        Consumable(String id, String displayName, Item item, int requiredDay, int baseCost,
                   int baseBundle, int cooldownTicks, String description, ChatFormatting color) {
            this.id = id; this.displayName = displayName; this.item = item; this.requiredDay = requiredDay;
            this.baseCost = baseCost; this.baseBundle = baseBundle; this.cooldownTicks = cooldownTicks;
            this.description = description; this.color = color;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Item item() { return item; }
        public int requiredDay() { return requiredDay; }
        public int baseCost() { return baseCost; }
        public int baseBundle() { return baseBundle; }
        public int cooldownTicks() { return cooldownTicks; }
        public String description() { return description; }
        public ChatFormatting color() { return color; }

        public static Consumable fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Consumable value : values()) if (value.id.equals(normalized)) return value;
            return null;
        }
    }
}
