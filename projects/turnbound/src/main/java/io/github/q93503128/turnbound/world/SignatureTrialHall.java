package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Physical Signature/Awakening hall. Exact trial enemy rosters are Canon Gaps, so this hall exposes readiness,
 * requirements and locked seals without fabricating battles. Once a trial roster becomes canonical the seal is the
 * stable physical entry point that can be wired to it.
 */
public final class SignatureTrialHall {
    public record Seal(String characterId, Vec3 pos, Item item, ChatFormatting color) { }

    private static final int MARKER_X = 88;
    private static final int MARKER_Y = 55;
    private static final int MARKER_Z = 92;
    private static final Vec3 CENTER = new Vec3(88, 66, 92);
    private static final List<Seal> SEALS = List.of(
            new Seal("P01", new Vec3(72, 66, 86), Items.IRON_SWORD, ChatFormatting.AQUA),
            new Seal("P02", new Vec3(80, 66, 86), Items.CLOCK, ChatFormatting.AQUA),
            new Seal("P03", new Vec3(88, 66, 86), Items.SHIELD, ChatFormatting.GREEN),
            new Seal("P04", new Vec3(96, 66, 86), Items.GOLDEN_APPLE, ChatFormatting.GOLD),
            new Seal("P05", new Vec3(104, 66, 86), Items.CROSSBOW, ChatFormatting.YELLOW),
            new Seal("P06", new Vec3(76, 66, 98), Items.WRITABLE_BOOK, ChatFormatting.LIGHT_PURPLE),
            new Seal("P07", new Vec3(88, 66, 98), Items.NAME_TAG, ChatFormatting.BLUE),
            new Seal("P08", new Vec3(100, 66, 98), Items.IRON_AXE, ChatFormatting.RED));
    private static final Map<UUID, Map<UUID, Seal>> ACTORS = new ConcurrentHashMap<>();

    private SignatureTrialHall() { }

    public static void build(ServerLevel level) {
        if (hasMarker(level)) return;
        hall(level);
        writeMarker(level);
    }

    public static void sync(ServerLevel level, ServerPlayer player) {
        build(level);
        Map<UUID, Seal> actors = ACTORS.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        boolean active = CampaignContentUnlocks.signatureActual(player.getUUID())
                && RadiaHubSessionManager.active(player)
                && player.position().distanceToSqr(CENTER) < 5200;
        if (!active) {
            despawn(level, actors);
            return;
        }

        for (var entry : List.copyOf(actors.entrySet())) {
            if (level.getEntity(entry.getKey()) == null) actors.remove(entry.getKey());
        }
        for (Seal seal : SEALS) {
            if (actors.containsValue(seal)) continue;
            SignatureTrialCatalog.Spec spec = SignatureTrialCatalog.forCharacter(seal.characterId());
            ArmorStand stand = new ArmorStand(level, seal.pos().x, seal.pos().y, seal.pos().z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(spec.title()).withStyle(seal.color(), ChatFormatting.BOLD));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.MAINHAND, seal.item().getDefaultInstance());
            level.addFreshEntity(stand);
            actors.put(stand.getUUID(), seal);
        }
    }

    public static boolean interact(ServerPlayer player, Entity target) {
        Map<UUID, Seal> actors = ACTORS.get(player.getUUID());
        if (actors == null) return false;
        Seal seal = actors.get(target.getUUID());
        if (seal == null) return false;

        SignatureTrialProgressService.Status status = SignatureTrialProgressService.status(
                player.getUUID(), seal.characterId());
        player.sendSystemMessage(Component.literal(status.title()).withStyle(seal.color(), ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("목표 · " + status.objective()).withStyle(ChatFormatting.WHITE));

        if (status.firstClearClaimed()) {
            player.sendSystemMessage(Component.literal("첫 클리어 완료 · 전용 장비/각성 Core 획득 기록 있음")
                    .withStyle(ChatFormatting.GREEN));
        } else if (!status.progressionReady()) {
            player.sendSystemMessage(Component.literal(status.blockReason()).withStyle(ChatFormatting.GRAY));
        } else if (!status.encounterCanonReady()) {
            player.sendSystemMessage(Component.literal("CANON GAP · " + status.blockReason())
                    .withStyle(ChatFormatting.YELLOW));
        } else if (status.canEnter()) {
            player.sendSystemMessage(Component.literal("입장 가능 · Signature Trial 전투를 시작할 수 있습니다.")
                    .withStyle(ChatFormatting.GREEN));
        }
        return true;
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, Seal> actors = ACTORS.remove(player.getUUID());
        if (actors != null) despawn(level, actors);
    }

    private static void hall(ServerLevel level) {
        for (int x = 66; x <= 110; x++) {
            for (int z = 80; z <= 104; z++) {
                boolean edge = x == 66 || x == 110 || z == 80 || z == 104;
                for (int y = 62; y <= 64; y++) set(level, x, y, z, Blocks.STONE);
                set(level, x, 65, z, edge
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS
                        : (((x + z) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS));
                for (int y = 66; y <= 73; y++) set(level, x, y, z, Blocks.AIR);
                if (edge && ((x + z) & 2) == 0) {
                    for (int y = 66; y <= 70; y++) set(level, x, y, z, Blocks.STONE_BRICKS);
                }
            }
        }
        for (Seal seal : SEALS) {
            int x = (int) seal.pos().x;
            int z = (int) seal.pos().z;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) set(level, x + dx, 65, z + dz, Blocks.POLISHED_DEEPSLATE);
            }
            set(level, x, 66, z, Blocks.AMETHYST_BLOCK);
        }
        for (int x = 84; x <= 92; x++) set(level, x, 65, 104, Blocks.AIR);
        set(level, 88, 66, 81, Blocks.BEACON);
    }

    private static void despawn(ServerLevel level, Map<UUID, Seal> actors) {
        for (UUID id : List.copyOf(actors.keySet())) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
            actors.remove(id);
        }
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.BEACON)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.BEACON);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
