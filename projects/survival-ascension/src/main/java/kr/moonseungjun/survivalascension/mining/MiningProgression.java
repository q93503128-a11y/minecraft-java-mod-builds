package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.progress.MiningProgressData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MiningProgression {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            SurvivalAscension.MOD_ID, "mining_speed");
    private static final TagKey<Block> VALUABLE_ORES = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "valuable_ores"));
    private static final Set<UUID> AREA_BREAK_GUARD = new HashSet<>();

    private MiningProgression() {}

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MiningProgressData data = MiningProgressData.get(player);
        data.ensureProfile(player);
        refreshMiningSpeed(player);
        if (data.markIntroduced(player)) {
            player.sendSystemMessage(Component.literal(
                    "§6[Survival Ascension] §f채굴 숙련이 시작되었습니다. 곡괭이로 직접 채굴해 레벨을 올리세요."));
            player.sendSystemMessage(Component.literal(
                    "§eLv.10 §f3×3  §6Lv.30 §f5×5  §cLv.60 §f7×7  §7| 웅크리기: 1×1 정밀 채굴"));
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refreshMiningSpeed(player);
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState centerState = event.getState();
        BlockPos center = event.getPos();
        ItemStack tool = player.getMainHandItem();
        if (!isValidPickaxeBreak(player, level, center, centerState, tool)) return;

        if (!player.isCreative() && !player.isSpectator()) {
            awardMiningXp(player, centerState, level, center);
        }

        if (AREA_BREAK_GUARD.contains(player.getUUID())) return;
        int miningLevel = MiningProgressData.get(player).miningLevel(player);
        int size = areaSize(miningLevel);
        if (size <= 1 || player.isShiftKeyDown()) return;

        float centerHardness = Math.max(0.0F, centerState.getDestroySpeed(level, center));
        AREA_BREAK_GUARD.add(player.getUUID());
        try {
            breakArea(player, level, center, size, centerHardness);
        } finally {
            AREA_BREAK_GUARD.remove(player.getUUID());
        }
    }

    private static boolean isValidPickaxeBreak(ServerPlayer player, ServerLevel level, BlockPos pos,
                                                BlockState state, ItemStack tool) {
        if (state.isAir() || tool.isEmpty() || !tool.is(ItemTags.PICKAXES)) return false;
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        return state.canHarvestBlock(level, pos, player);
    }

    private static void awardMiningXp(ServerPlayer player, BlockState state, ServerLevel level, BlockPos pos) {
        int xp = xpForBlock(state, level, pos);
        MiningProgressData data = MiningProgressData.get(player);
        MiningProgressData.AddXpResult result = data.addMiningXp(player, xp);
        if (!result.leveledUp()) return;

        refreshMiningSpeed(player);
        int newLevel = result.newLevel();
        player.sendSystemMessage(Component.literal("§6[채굴] §f레벨 §e" + newLevel
                + "§f 달성! 채굴속도 §a" + formatMultiplier(speedMultiplier(newLevel))));
        if (result.oldLevel() < 10 && newLevel >= 10) {
            player.sendSystemMessage(Component.literal("§e[채굴 해금] §f3×3 광역 채굴이 해금되었습니다."));
        }
        if (result.oldLevel() < 30 && newLevel >= 30) {
            player.sendSystemMessage(Component.literal("§6[채굴 해금] §f5×5 광역 채굴이 해금되었습니다."));
        }
        if (result.oldLevel() < 60 && newLevel >= 60) {
            player.sendSystemMessage(Component.literal("§c[채굴 해금] §f7×7 광역 채굴이 해금되었습니다."));
        }
    }

    private static int xpForBlock(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.is(VALUABLE_ORES)) return 20;
        float hardness = Math.max(0.0F, state.getDestroySpeed(level, pos));
        return Math.max(1, Math.min(6, (int) Math.ceil(hardness)));
    }

    private static void breakArea(ServerPlayer player, ServerLevel level, BlockPos center,
                                  int size, float centerHardness) {
        int radius = size / 2;
        Vec3 look = player.getLookAngle();
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);

        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) continue;
                if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;

                BlockPos target;
                if (ay >= ax && ay >= az) {
                    target = center.offset(a, 0, b);
                } else if (ax >= az) {
                    target = center.offset(0, a, b);
                } else {
                    target = center.offset(a, b, 0);
                }

                BlockState targetState = level.getBlockState(target);
                ItemStack currentTool = player.getMainHandItem();
                if (!isValidPickaxeBreak(player, level, target, targetState, currentTool)) continue;
                if (level.getBlockEntity(target) != null) continue;

                float targetHardness = targetState.getDestroySpeed(level, target);
                if (centerHardness > 0.0F && targetHardness > centerHardness * 1.5F + 1.0F) continue;

                player.gameMode.destroyBlock(target);
            }
        }
    }

    public static void refreshMiningSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (speed == null) return;
        speed.removeModifier(SPEED_MODIFIER_ID);
        int level = MiningProgressData.get(player).miningLevel(player);
        double multiplier = speedMultiplier(level);
        if (multiplier <= 1.000001D) return;
        speed.addOrUpdateTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_ID,
                multiplier - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    public static int areaSize(int level) {
        if (level >= 60) return 7;
        if (level >= 30) return 5;
        if (level >= 10) return 3;
        return 1;
    }

    public static double speedMultiplier(int level) {
        int clamped = Math.max(0, Math.min(MiningProgressData.MAX_LEVEL, level));
        return 1.0D + 0.03D * clamped + 0.0004D * clamped * clamped;
    }

    public static String formatMultiplier(double value) {
        return String.format(Locale.ROOT, "%.2f×", value);
    }
}
