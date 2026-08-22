package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record RoadSegment(int startX, int startY, int startZ,
                          int directionX, int directionZ, int length,
                          List<Integer> path) {
    public static final Codec<RoadSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("start_x").forGetter(RoadSegment::startX),
            Codec.INT.fieldOf("start_y").forGetter(RoadSegment::startY),
            Codec.INT.fieldOf("start_z").forGetter(RoadSegment::startZ),
            Codec.INT.fieldOf("direction_x").forGetter(RoadSegment::directionX),
            Codec.INT.fieldOf("direction_z").forGetter(RoadSegment::directionZ),
            Codec.INT.fieldOf("length").forGetter(RoadSegment::length),
            Codec.INT.listOf().optionalFieldOf("path", List.of()).forGetter(RoadSegment::path)
    ).apply(instance, RoadSegment::new));

    public RoadSegment(int startX, int startY, int startZ,
                       int directionX, int directionZ, int length) {
        this(startX, startY, startZ, directionX, directionZ, length, List.of());
    }

    public static RoadSegment fromPath(List<BlockPos> centers) {
        if (centers == null || centers.size() < 2) {
            return new RoadSegment(0, 0, 0, 0, 0, 0, List.of());
        }
        BlockPos first = centers.get(0);
        BlockPos last = centers.get(centers.size() - 1);
        BlockPos beforeLast = centers.get(centers.size() - 2);
        int directionX = Integer.signum(last.getX() - beforeLast.getX());
        int directionZ = Integer.signum(last.getZ() - beforeLast.getZ());
        List<Integer> encoded = new ArrayList<>(centers.size() * 3);
        for (BlockPos center : centers) {
            encoded.add(center.getX());
            encoded.add(center.getY());
            encoded.add(center.getZ());
        }
        return new RoadSegment(first.getX(), first.getY(), first.getZ(),
                directionX, directionZ, centers.size(), List.copyOf(encoded));
    }

    public boolean hasPath() {
        return path != null && path.size() >= 6 && path.size() % 3 == 0;
    }

    public List<BlockPos> centers() {
        if (hasPath()) {
            List<BlockPos> centers = new ArrayList<>(path.size() / 3);
            for (int i = 0; i + 2 < path.size(); i += 3) {
                centers.add(new BlockPos(path.get(i), path.get(i + 1), path.get(i + 2)));
            }
            return List.copyOf(centers);
        }
        if (length <= 0 || Math.abs(directionX) + Math.abs(directionZ) != 1) return List.of();
        List<BlockPos> legacy = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            legacy.add(new BlockPos(startX + directionX * i, startY, startZ + directionZ * i));
        }
        return List.copyOf(legacy);
    }

    public BlockPos start() {
        List<BlockPos> centers = centers();
        return centers.isEmpty() ? new BlockPos(startX, startY, startZ) : centers.get(0);
    }

    public BlockPos end() {
        List<BlockPos> centers = centers();
        if (!centers.isEmpty()) return centers.get(centers.size() - 1);
        int last = Math.max(0, length - 1);
        return new BlockPos(startX + directionX * last, startY, startZ + directionZ * last);
    }

    public boolean containsXZ(BlockPos pos) {
        for (BlockPos center : centers()) {
            if (Math.abs(pos.getX() - center.getX()) <= 1 && Math.abs(pos.getZ() - center.getZ()) <= 1) return true;
        }
        return false;
    }
}
