package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Three equipment lines with three efficiency tiers and a two-slot robe runtime. */
public final class MageGearService {
    private static final String TWO_SLOT_ROBE_RUNTIME = "mage_robe_hem|sage_robe_hem|archmage_robe_hem";
    private static final Set<UUID> ROBE_SLOT_WARNED = new HashSet<>();

    private MageGearService() {}

    public static void tick(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        int robeTier = robeTier(chest);
        int hemTier = hemTier(legs);

        if (robeTier > 0 && legs.isEmpty()) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(hemForTier(robeTier)));
            hemTier = robeTier;
            ROBE_SLOT_WARNED.remove(player.getUUID());
        } else if (robeTier == 0 && hemTier > 0) {
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            ROBE_SLOT_WARNED.remove(player.getUUID());
            hemTier = 0;
        } else if (robeTier > 0 && hemTier > 0 && robeTier != hemTier) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(hemForTier(robeTier)));
            hemTier = robeTier;
            ROBE_SLOT_WARNED.remove(player.getUUID());
        } else if (robeTier > 0 && hemTier == 0 && !legs.isEmpty()
                && ROBE_SLOT_WARNED.add(player.getUUID())) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§c[로브 비활성] §f로브는 몸·바지 두 슬롯을 사용합니다. 현재 바지 장비를 먼저 빼세요."), 110);
        } else if (robeTier == 0) {
            ROBE_SLOT_WARNED.remove(player.getUUID());
        }

        GearStats stats = stats(player);
        if (stats.boots()) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, stats.bootsTier() - 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 30, stats.bootsTier() - 1, true, false));
            if (stats.bootsTier() >= 2 && !player.onGround()) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30,
                        stats.bootsTier() >= 3 ? 1 : 0, true, false));
            }
            if (stats.bootsTier() >= 3) freezeWater(player);
        }
        if (stats.robe()) {
            int healthAmplifier = switch (stats.robeTier()) {
                case 2 -> 3;
                case 3 -> 7;
                default -> 1;
            };
            int resistance = stats.robeTier() >= 3 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 30, healthAmplifier, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, resistance, true, false));
            if (stats.robeTier() >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false));
            }
        }
    }

    public static GearStats stats(Player player) {
        int hatTier = hatTier(player.getItemBySlot(EquipmentSlot.HEAD));
        int chestTier = robeTier(player.getItemBySlot(EquipmentSlot.CHEST));
        int legsTier = hemTier(player.getItemBySlot(EquipmentSlot.LEGS));
        int robeTier = chestTier > 0 && chestTier == legsTier ? chestTier : 0;
        int bootsTier = bootsTier(player.getItemBySlot(EquipmentSlot.FEET));

        Piece hat = hat(hatTier);
        Piece robe = robe(robeTier);
        Piece boots = boots(bootsTier);
        return new GearStats(hatTier, robeTier, bootsTier,
                hat.mana + robe.mana + boots.mana,
                hat.regen * robe.regen * boots.regen,
                hat.manaCost * robe.manaCost * boots.manaCost,
                hat.power * robe.power * boots.power,
                hat.range * robe.range * boots.range,
                hat.cooldown * robe.cooldown * boots.cooldown);
    }

    public static String hatName(Player player) {
        return switch (hatTier(player.getItemBySlot(EquipmentSlot.HEAD))) {
            case 1 -> "비전 모자";
            case 2 -> "현자의 모자";
            case 3 -> "대마도사 관";
            default -> "모자 없음";
        };
    }

    public static String robeName(Player player) {
        int chestTier = robeTier(player.getItemBySlot(EquipmentSlot.CHEST));
        if (chestTier == 0) return "로브 없음";
        String name = switch (chestTier) {
            case 2 -> "현자의 로브";
            case 3 -> "대마도사 예복";
            default -> "중층 마도 로브";
        };
        return stats(player).robeTier() == chestTier ? name : name + " · 바지 슬롯 필요";
    }

    public static String bootsName(Player player) {
        return switch (bootsTier(player.getItemBySlot(EquipmentSlot.FEET))) {
            case 1 -> "유랑 마도화";
            case 2 -> "천공 마도화";
            case 3 -> "빙결 보행화";
            default -> "마도화 없음";
        };
    }

    private static int hatTier(ItemStack stack) {
        if (stack.getItem() == ModItems.ARCHMAGE_CROWN.get()) return 3;
        if (stack.getItem() == ModItems.SAGE_HAT.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_HAT.get()) return 1;
        return 0;
    }

    private static int robeTier(ItemStack stack) {
        if (stack.getItem() == ModItems.ARCHMAGE_ROBE.get()) return 3;
        if (stack.getItem() == ModItems.SAGE_ROBE.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_ROBE.get()) return 1;
        return 0;
    }

    private static int hemTier(ItemStack stack) {
        if (stack.getItem() == ModItems.ARCHMAGE_ROBE_HEM.get()) return 3;
        if (stack.getItem() == ModItems.SAGE_ROBE_HEM.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_ROBE_HEM.get()) return 1;
        return 0;
    }

    private static int bootsTier(ItemStack stack) {
        if (stack.getItem() == ModItems.FROSTSTEP_BOOTS.get()) return 3;
        if (stack.getItem() == ModItems.SKYWALKER_BOOTS.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_BOOTS.get()) return 1;
        return 0;
    }

    private static net.minecraft.world.item.Item hemForTier(int tier) {
        return switch (tier) {
            case 3 -> ModItems.ARCHMAGE_ROBE_HEM.get();
            case 2 -> ModItems.SAGE_ROBE_HEM.get();
            default -> ModItems.MAGE_ROBE_HEM.get();
        };
    }

    private static Piece hat(int tier) {
        return switch (tier) {
            case 1 -> new Piece(90, 1.20, 0.92, 1.03, 1.01, 0.97);
            case 2 -> new Piece(360, 1.55, 0.78, 1.10, 1.08, 0.84);
            case 3 -> new Piece(1200, 2.30, 0.52, 1.28, 1.20, 0.58);
            default -> Piece.NONE;
        };
    }

    private static Piece robe(int tier) {
        return switch (tier) {
            case 1 -> new Piece(45, 1.08, 0.97, 1.09, 1.03, 0.95);
            case 2 -> new Piece(260, 1.25, 0.90, 1.28, 1.12, 0.82);
            case 3 -> new Piece(900, 1.70, 0.75, 1.65, 1.30, 0.60);
            default -> Piece.NONE;
        };
    }

    private static Piece boots(int tier) {
        return switch (tier) {
            case 1 -> new Piece(10, 1.03, 0.99, 1.02, 1.07, 0.94);
            case 2 -> new Piece(90, 1.10, 0.96, 1.08, 1.25, 0.75);
            case 3 -> new Piece(300, 1.25, 0.90, 1.18, 1.55, 0.48);
            default -> Piece.NONE;
        };
    }

    private static void freezeWater(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos center = player.blockPosition().below();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x * x + z * z > 6) continue;
                BlockPos pos = center.offset(x, 0, z);
                if (level.getBlockState(pos).is(Blocks.WATER)
                        && level.getBlockState(pos.above()).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }
    }

    public static void clear(UUID playerId) {
        ROBE_SLOT_WARNED.remove(playerId);
    }

    private record Piece(int mana, double regen, double manaCost, double power, double range, double cooldown) {
        private static final Piece NONE = new Piece(0, 1.0, 1.0, 1.0, 1.0, 1.0);
    }

    public record GearStats(
            int hatTier,
            int robeTier,
            int bootsTier,
            int maxManaBonus,
            double regenMultiplier,
            double manaCostMultiplier,
            double powerMultiplier,
            double rangeMultiplier,
            double cooldownMultiplier
    ) {
        public boolean hat() { return hatTier > 0; }
        public boolean robe() { return robeTier > 0; }
        public boolean boots() { return bootsTier > 0; }
    }
}
