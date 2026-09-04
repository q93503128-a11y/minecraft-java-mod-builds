package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * Physical Signature/Awakening hall. Exact trial enemy rosters remain governed by the existing canon-safe authoring
 * layer; this class only presents player readiness and sealed/cleared states without exposing authoring diagnostics.
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
            UUID existing = actorFor(actors, seal);
            ArmorStand stand = existing == null ? null : armor(level.getEntity(existing));
            if (stand == null) {
                stand = new ArmorStand(level, seal.pos().x, seal.pos().y, seal.pos().z);
                stand.setInvulnerable(true);
                stand.setNoGravity(true);
                stand.setShowArms(true);
                stand.setItemSlot(EquipmentSlot.MAINHAND, seal.item().getDefaultInstance());
                level.addFreshEntity(stand);
                actors.put(stand.getUUID(), seal);
            }
            SignatureTrialProgressService.Status status = SignatureTrialProgressService.status(player.getUUID(), seal.characterId());
            applyName(stand, seal, status);
            if (player.tickCount % 20 == 0 && player.position().distanceToSqr(seal.pos()) <= 24.0 * 24.0) {
                pulse(level, seal, status);
            }
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
        player.sendSystemMessage(Component.literal("목표 · " + playerObjective(status.objective())).withStyle(ChatFormatting.WHITE));

        if (status.firstClearClaimed()) {
            player.sendSystemMessage(Component.literal("첫 클리어 완료 · 전용 장비와 각성 Core를 획득했습니다.")
                    .withStyle(ChatFormatting.GREEN));
        } else if (!status.progressionReady()) {
            player.sendSystemMessage(Component.literal(status.blockReason()).withStyle(ChatFormatting.GRAY));
        } else if (!status.encounterCanonReady()) {
            // Keep internal authoring diagnostics out of the game surface. The sealed door can remain as a stable future entry point.
            player.sendSystemMessage(Component.literal("시련의 봉인이 아직 열리지 않는다.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        } else if (status.canEnter()) {
            player.sendSystemMessage(Component.literal("입장 가능 · 시련을 시작할 수 있습니다.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        return true;
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, Seal> actors = ACTORS.remove(player.getUUID());
        if (actors != null) despawn(level, actors);
    }

    private static String playerObjective(String objective) {
        if (objective == null) return "-";
        return objective
                .replace("P01", "카이렌")
                .replace("P06", "모르웬")
                .replace("P07", "마리온")
                .replace("Toto", "토토")
                .replace("Marion", "마리온")
                .replace("Trial Boss", "시련 보스");
    }

    private static void applyName(ArmorStand stand, Seal seal, SignatureTrialProgressService.Status status) {
        String suffix;
        ChatFormatting stateColor;
        if (status.firstClearClaimed()) {
            suffix = " · 완료";
            stateColor = ChatFormatting.GREEN;
        } else if (status.canEnter()) {
            suffix = " · 입장 가능";
            stateColor = ChatFormatting.GREEN;
        } else if (status.progressionReady()) {
            suffix = " · 봉인됨";
            stateColor = ChatFormatting.DARK_PURPLE;
        } else {
            suffix = " · 조건 미충족";
            stateColor = ChatFormatting.GRAY;
        }
        stand.setCustomName(Component.literal(status.title() + suffix).withStyle(stateColor, ChatFormatting.BOLD));
        stand.setCustomNameVisible(true);
    }

    private static void pulse(ServerLevel level, Seal seal, SignatureTrialProgressService.Status status) {
        Vec3 p = seal.pos().add(0, 1.1, 0);
        ParticleOptions particle = status.firstClearClaimed() ? ParticleTypes.END_ROD
                : status.canEnter() ? ParticleTypes.ENCHANT
                : status.progressionReady() ? ParticleTypes.REVERSE_PORTAL
                : ParticleTypes.SMOKE;
        int count = status.firstClearClaimed() ? 4 : status.canEnter() ? 5 : 2;
        level.sendParticles(particle, p.x, p.y, p.z, count, 0.55, 0.65, 0.55, 0.01);
    }

    private static UUID actorFor(Map<UUID, Seal> actors, Seal seal) {
        for (var entry : actors.entrySet()) if (entry.getValue().equals(seal)) return entry.getKey();
        return null;
    }

    private static ArmorStand armor(Entity entity) {
        return entity instanceof ArmorStand stand ? stand : null;
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
