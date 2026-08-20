package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** High-circle space magic with actual cross-dimension and persistent pocket-space behavior. */
public final class PlanarSpellService {
    private static final Set<String> HANDLED = Set.of("plane_shift", "demiplane");
    private static final int ROOM_HALF = 10;
    private static final int ROOM_FLOOR_Y = 224;

    private PlanarSpellService() {}

    public static boolean handles(String id) { return HANDLED.contains(id); }

    public static boolean execute(ServerPlayer player, String id) {
        return switch (id) {
            case "plane_shift" -> planeShift(player);
            case "demiplane" -> demiplane(player);
            default -> false;
        };
    }

    /** G is also an emergency exit for invited players who do not know the spell themselves. */
    public static boolean useAuthority(ServerPlayer player) {
        if (!isDemiplaneBackend(player)) return false;
        PlanarSpellData data = PlanarSpellData.get(player.serverLevel().getServer());
        if (data.anchor(player).isEmpty()) return false;
        return returnFromDemiplane(player, data);
    }

    private static boolean planeShift(ServerPlayer caster) {
        ServerLevel origin = caster.serverLevel();
        ResourceKey<Level> targetKey = attunedPlane(caster);
        if (origin.dimension().equals(targetKey)) {
            ArcaneNoticeService.push(caster, Component.literal(
                    "§c[플레인 시프트] §f이미 그 평면에 있습니다. §7위를 보면 엔드, 아래를 보면 네더, 수평은 오버월드에 조율됩니다."), 90);
            return false;
        }
        MinecraftServer server = origin.getServer();
        ServerLevel target = server.getLevel(targetKey);
        if (target == null) return false;

        List<ServerPlayer> party = consentingParty(caster, origin);
        Vec3 casterDestination = mappedDestination(caster, origin, target);
        Optional<BlockPos> safeCaster = findSafe(target, casterDestination, targetKey == Level.END ? 80 : 48);
        if (safeCaster.isEmpty()) {
            ArcaneNoticeService.push(caster, Component.literal("§c[플레인 시프트] §f도착 평면에서 안전한 착지점을 찾지 못했습니다."), 70);
            return false;
        }
        Vec3 originCaster = caster.position();
        BlockPos center = safeCaster.get();
        int moved = 0;
        for (ServerPlayer member : party) {
            Vec3 offset = member.position().subtract(originCaster);
            Vec3 desired = new Vec3(center.getX() + .5 + offset.x, center.getY(), center.getZ() + .5 + offset.z);
            Optional<BlockPos> safe = findSafe(target, desired, 12);
            if (safe.isEmpty()) continue;
            BlockPos pos = safe.get();
            if (teleport(member, target, pos.getX() + .5, pos.getY(), pos.getZ() + .5, member.getYRot(), member.getXRot())) moved++;
        }
        if (moved == 0) return false;
        target.playSound(null, center, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, .72F);
        ArcaneNoticeService.push(caster, Component.literal("§5[플레인 시프트] §f" + planeName(targetKey)
                + "으로 존재 평면을 전환했습니다. §7주변에서 웅크린 플레이어도 동행: " + Math.max(0, moved - 1) + "명"), 100);
        return true;
    }

    private static boolean demiplane(ServerPlayer caster) {
        MinecraftServer server = caster.serverLevel().getServer();
        PlanarSpellData data = PlanarSpellData.get(server);
        if (isDemiplaneBackend(caster) && data.anchor(caster).isPresent()) {
            return returnFromDemiplane(caster, data);
        }
        ServerLevel pocket = server.getLevel(Level.END);
        if (pocket == null) return false;
        BlockPos center = roomCenter(caster.getUUID());
        ensureRoom(pocket, center);

        List<ServerPlayer> party = consentingParty(caster, caster.serverLevel());
        Vec3 base = new Vec3(center.getX() + .5, ROOM_FLOOR_Y + 2.0, center.getZ() + .5);
        int moved = 0;
        for (int i = 0; i < party.size(); i++) {
            ServerPlayer member = party.get(i);
            PlanarSpellData memberData = PlanarSpellData.get(server);
            memberData.remember(member);
            double angle = i * Math.PI * 2.0 / Math.max(1, party.size());
            double radius = i == 0 ? 0.0 : 2.2;
            if (teleport(member, pocket, base.x + Math.cos(angle) * radius, base.y,
                    base.z + Math.sin(angle) * radius, member.getYRot(), member.getXRot())) moved++;
        }
        if (moved == 0) return false;
        pocket.playSound(null, center.above(2), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, .75F, 1.18F);
        ArcaneNoticeService.push(caster, Component.literal("§5[데미플레인] §f개인 주머니 공간을 열었습니다. "
                + "§7이 방에 둔 블록과 물품은 다음 시전에도 그대로 남습니다. G키 또는 재시전으로 귀환할 수 있습니다."), 120);
        return true;
    }

    private static boolean returnFromDemiplane(ServerPlayer player, PlanarSpellData data) {
        PlanarSpellData.ReturnAnchor anchor = data.anchor(player).orElse(null);
        if (anchor == null) return false;
        Identifier id = Identifier.tryParse(anchor.dimension());
        if (id == null) return false;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel target = player.serverLevel().getServer().getLevel(key);
        if (target == null) return false;
        Optional<BlockPos> safe = findSafe(target, new Vec3(anchor.x(), anchor.y(), anchor.z()), 16);
        if (safe.isEmpty()) return false;
        BlockPos pos = safe.get();
        boolean moved = teleport(player, target, pos.getX() + .5, pos.getY(), pos.getZ() + .5, anchor.yaw(), anchor.pitch());
        if (!moved) return false;
        data.clear(player);
        target.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .9F, .82F);
        ArcaneNoticeService.push(player, Component.literal("§d[데미플레인] §f기억된 귀환점으로 돌아왔습니다."), 70);
        return true;
    }

    private static ResourceKey<Level> attunedPlane(ServerPlayer player) {
        double vertical = player.getLookAngle().y;
        if (vertical > .35) return Level.END;
        if (vertical < -.35) return Level.NETHER;
        return Level.OVERWORLD;
    }

    private static String planeName(ResourceKey<Level> key) {
        if (key == Level.NETHER) return "네더 평면";
        if (key == Level.END) return "엔드 평면";
        return "오버월드 물질계";
    }

    private static Vec3 mappedDestination(ServerPlayer player, ServerLevel origin, ServerLevel target) {
        double x = player.getX();
        double z = player.getZ();
        if (origin.dimension() == Level.OVERWORLD && target.dimension() == Level.NETHER) { x /= 8.0; z /= 8.0; }
        else if (origin.dimension() == Level.NETHER && target.dimension() == Level.OVERWORLD) { x *= 8.0; z *= 8.0; }
        if (target.dimension() == Level.END) return new Vec3(ServerLevel.END_SPAWN_POINT.getX(), 80, ServerLevel.END_SPAWN_POINT.getZ());
        double y = Math.max(target.getMinY() + 8, Math.min(target.getMaxY() - 8, player.getY()));
        return new Vec3(x, y, z);
    }

    private static List<ServerPlayer> consentingParty(ServerPlayer caster, ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();
        result.add(caster);
        for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(caster.position(), caster.position()).inflate(5.5),
                p -> p != caster && p.isAlive() && !p.isSpectator() && p.isShiftKeyDown())) {
            result.add(other);
            if (result.size() >= 9) break;
        }
        return result;
    }

    private static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.stopRiding();
        return player.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, true);
    }

    private static Optional<BlockPos> findSafe(ServerLevel level, Vec3 desired, int verticalSearch) {
        int x = (int) Math.floor(desired.x);
        int z = (int) Math.floor(desired.z);
        int startY = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int d = 0; d <= verticalSearch; d++) {
            int[] ys = d == 0 ? new int[]{startY} : new int[]{startY + d, startY - d};
            for (int y : ys) {
                if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 2) continue;
                BlockPos feet = new BlockPos(x, y, z);
                if (level.getBlockState(feet.below()).blocksMotion()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return Optional.of(feet);
            }
        }
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                Optional<BlockPos> safe = findSafe(level, new Vec3(x + dx, startY, z + dz), Math.min(12, verticalSearch));
                if (safe.isPresent()) return safe;
            }
        }
        return Optional.empty();
    }

    private static BlockPos roomCenter(UUID id) {
        int gx = Math.floorMod((int) (id.getMostSignificantBits() ^ (id.getMostSignificantBits() >>> 32)), 90_000);
        int gz = Math.floorMod((int) (id.getLeastSignificantBits() ^ (id.getLeastSignificantBits() >>> 32)), 90_000);
        return new BlockPos(4_000_000 + gx * 240, ROOM_FLOOR_Y, 4_000_000 + gz * 240);
    }

    private static boolean isDemiplaneBackend(ServerPlayer player) {
        return player.level().dimension() == Level.END && player.getY() >= ROOM_FLOOR_Y - 1
                && player.getX() > 3_500_000 && player.getZ() > 3_500_000;
    }

    private static void ensureRoom(ServerLevel level, BlockPos center) {
        BlockPos marker = center.below();
        if (level.getBlockState(marker).is(Blocks.BEDROCK)) return;
        for (int dx = -ROOM_HALF; dx <= ROOM_HALF; dx++) {
            for (int dz = -ROOM_HALF; dz <= ROOM_HALF; dz++) {
                for (int dy = 0; dy <= 12; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    boolean shell = Math.abs(dx) == ROOM_HALF || Math.abs(dz) == ROOM_HALF || dy == 0 || dy == 12;
                    if (shell) level.setBlockAndUpdate(p, Blocks.BEDROCK.defaultBlockState());
                    else if (dy == 1) level.setBlockAndUpdate(p, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    else level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int sx : new int[]{-7, 7}) for (int sz : new int[]{-7, 7}) {
            level.setBlockAndUpdate(center.offset(sx, 11, sz), Blocks.SEA_LANTERN.defaultBlockState());
        }
        level.setBlockAndUpdate(marker, Blocks.BEDROCK.defaultBlockState());
    }
}
