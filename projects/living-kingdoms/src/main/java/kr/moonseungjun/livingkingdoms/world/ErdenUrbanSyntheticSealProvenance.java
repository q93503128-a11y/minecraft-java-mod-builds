package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recovers the raw-schematic coordinates behind each cropped Erden facade and proves which cut-face
 * blocks are synthetic seals placed over cells that were AIR in the downloaded source.
 *
 * <p>The urban facade builder intentionally seals crop faces after selecting original structure
 * blocks. Those seals are useful for a closed building shell, but they are not authored source
 * blocks. This audit matches retained source doors and structural blocks back to the immutable Sponge
 * schematic, then exposes only the subset of boundary cells where (a) the raw source is air and
 * (b) the synthetic seal is from the already-approved conversion-clear palette. No world chunks are
 * read and no block is mutated.</p>
 */
public final class ErdenUrbanSyntheticSealProvenance {
    private static final int SEAL_MAX_Y = 22;
    private static final int MIN_ALIGNMENT_SCORE = 400;

    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanSyntheticSealProvenance() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PROFILES.clear();

        for (ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot
                : ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics().values()) {
            Profile profile = analyze(snapshot);
            PROFILES.put(snapshot.fragmentKey(), profile);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_SYNTHETIC_SEAL_PROVENANCE fragment={} translation={},{},{} alignment_score={} source_air_seals={} clearable_source_air_seals={} source_only=true world_reads=false mutations=0",
                    snapshot.fragmentKey(), profile.rawOffsetX(), profile.rawOffsetY(),
                    profile.rawOffsetZ(), profile.alignmentScore(), profile.sourceAirSeals().size(),
                    profile.clearableSourceAirSeals().size());
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden synthetic crop-seal provenance fragments={} source_only=true world_reads=false mutations=0 source_blocks_cut=0",
                PROFILES.size());
    }

    public static boolean isSourceAirSeal(String fragmentKey, int x, int y, int z) {
        bootstrap();
        Profile profile = PROFILES.get(fragmentKey);
        return profile != null && profile.sourceAirSeals().contains(localKey(x, y, z));
    }

    public static boolean isClearableSourceAirSeal(String fragmentKey, int x, int y, int z) {
        bootstrap();
        Profile profile = PROFILES.get(fragmentKey);
        return profile != null && profile.clearableSourceAirSeals().contains(localKey(x, y, z));
    }

    public static int clearableSealCount(String fragmentKey) {
        bootstrap();
        Profile profile = PROFILES.get(fragmentKey);
        return profile == null ? 0 : profile.clearableSourceAirSeals().size();
    }

    private static Profile analyze(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        SpongeStructureTemplate raw = SpongeStructureTemplate.load(snapshot.resource());
        List<LocalDoor> localDoors = new ArrayList<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (block.state().getBlock() instanceof DoorBlock) {
                localDoors.add(new LocalDoor(
                        block.x(), block.y(), block.z(), blockId(block.state().getBlock())));
            }
        }
        if (localDoors.isEmpty()) {
            throw new IllegalStateException("Cannot align Erden crop without retained door: "
                    + snapshot.fragmentKey());
        }

        List<RawDoor> rawDoors = new ArrayList<>();
        for (int y = 0; y < raw.height(); y++) {
            for (int z = 0; z < raw.length(); z++) {
                for (int x = 0; x < raw.width(); x++) {
                    String id = rawBlockId(raw, x, y, z);
                    if (id.endsWith("_door") && !id.endsWith("_trapdoor")) {
                        rawDoors.add(new RawDoor(x, y, z, normalizeRawId(id)));
                    }
                }
            }
        }
        if (rawDoors.isEmpty()) {
            throw new IllegalStateException("Cannot align Erden crop because source has no door: "
                    + snapshot.resource());
        }

        Alignment best = null;
        for (LocalDoor local : localDoors) {
            for (RawDoor source : rawDoors) {
                if (!local.blockId().equals(source.blockId())) continue;
                Alignment candidate = scoreAlignment(
                        snapshot, raw,
                        source.x() - local.x(), source.y() - local.y(), source.z() - local.z());
                if (best == null || candidate.score() > best.score()) best = candidate;
            }
        }
        if (best == null || best.score() < MIN_ALIGNMENT_SCORE) {
            throw new IllegalStateException("Unable to prove Erden crop/source alignment fragment="
                    + snapshot.fragmentKey() + " score=" + (best == null ? -1 : best.score()));
        }

        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> localBlocks = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            localBlocks.put(localKey(block.x(), block.y(), block.z()), block);
        }
        Set<Long> sourceAirSeals = new HashSet<>();
        Set<Long> clearableSourceAirSeals = new HashSet<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (!onCropFace(snapshot, block.x(), block.z())
                    || block.y() < 1 || block.y() > Math.min(SEAL_MAX_Y, snapshot.height() - 1)) {
                continue;
            }
            int rawX = block.x() + best.offsetX();
            int rawY = block.y() + best.offsetY();
            int rawZ = block.z() + best.offsetZ();
            if (!inside(raw, rawX, rawY, rawZ)) continue;
            String rawId = normalizeRawId(rawBlockId(raw, rawX, rawY, rawZ));
            if (!isRawAir(rawId)) continue;
            long key = localKey(block.x(), block.y(), block.z());
            sourceAirSeals.add(key);
            if (isApprovedRuntimeClearBlock(block.state().getBlock())) {
                clearableSourceAirSeals.add(key);
            }
        }

        return new Profile(
                best.offsetX(), best.offsetY(), best.offsetZ(), best.score(),
                Set.copyOf(sourceAirSeals), Set.copyOf(clearableSourceAirSeals));
    }

    private static Alignment scoreAlignment(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            SpongeStructureTemplate raw,
            int offsetX, int offsetY, int offsetZ) {
        int score = 0;
        int checked = 0;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            // Skip probable synthetic cut-face seals while scoring; interior authored blocks should
            // overwhelmingly determine the translation.
            if (onCropFace(snapshot, block.x(), block.z()) && block.y() <= SEAL_MAX_Y) continue;
            int rawX = block.x() + offsetX;
            int rawY = block.y() + offsetY;
            int rawZ = block.z() + offsetZ;
            if (!inside(raw, rawX, rawY, rawZ)) continue;
            checked++;
            String localId = blockId(block.state().getBlock());
            String rawId = normalizeRawId(rawBlockId(raw, rawX, rawY, rawZ));
            if (localId.equals(rawId)) score++;
        }
        return new Alignment(offsetX, offsetY, offsetZ, score, checked);
    }

    private static boolean onCropFace(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot, int x, int z) {
        return x == 0 || x == snapshot.width() - 1
                || z == 0 || z == snapshot.length() - 1;
    }

    private static boolean inside(SpongeStructureTemplate raw, int x, int y, int z) {
        return x >= 0 && x < raw.width()
                && y >= 0 && y < raw.height()
                && z >= 0 && z < raw.length();
    }

    private static String rawBlockId(SpongeStructureTemplate raw, int x, int y, int z) {
        int palette = raw.paletteIndex(x, y, z);
        if (palette < 0 || palette >= raw.palette().size()) return "minecraft:air";
        String specification = raw.palette().get(palette);
        int bracket = specification.indexOf('[');
        return (bracket < 0 ? specification : specification.substring(0, bracket)).trim();
    }

    private static String normalizeRawId(String id) {
        return switch (id) {
            case "minecraft:chain" -> "minecraft:iron_chain";
            case "minecraft:grass" -> "minecraft:short_grass";
            default -> id;
        };
    }

    private static boolean isRawAir(String id) {
        return id.equals("minecraft:air")
                || id.equals("minecraft:cave_air")
                || id.equals("minecraft:void_air")
                || id.equals("minecraft:structure_void");
    }

    private static boolean isApprovedRuntimeClearBlock(Block block) {
        return block == Blocks.OAK_PLANKS
                || block == Blocks.SPRUCE_PLANKS
                || block == Blocks.SMOOTH_STONE
                || block == Blocks.COARSE_DIRT
                || block == Blocks.STONE_BRICKS
                || block == Blocks.OAK_SLAB
                || block == Blocks.SMOOTH_STONE_SLAB
                || block == Blocks.OAK_STAIRS
                || block == Blocks.SPRUCE_STAIRS
                || block == Blocks.STONE_BRICK_STAIRS;
    }

    private static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    public record Profile(
            int rawOffsetX,
            int rawOffsetY,
            int rawOffsetZ,
            int alignmentScore,
            Set<Long> sourceAirSeals,
            Set<Long> clearableSourceAirSeals) {
    }

    private record LocalDoor(int x, int y, int z, String blockId) {
    }

    private record RawDoor(int x, int y, int z, String blockId) {
    }

    private record Alignment(int offsetX, int offsetY, int offsetZ, int score, int checked) {
    }
}
