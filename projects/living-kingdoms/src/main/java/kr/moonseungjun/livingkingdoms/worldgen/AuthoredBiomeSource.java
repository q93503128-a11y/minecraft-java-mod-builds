package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/** Permanent coordinate-authored ecological geography for the active Erden kingdom. */
public final class AuthoredBiomeSource extends BiomeSource {
    public static final MapCodec<AuthoredBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("sea").forGetter(source -> source.sea),
            Biome.CODEC.fieldOf("coast").forGetter(source -> source.coast),
            Biome.CODEC.fieldOf("silver_river").forGetter(source -> source.silverRiver),
            Biome.CODEC.fieldOf("central_lowlands").forGetter(source -> source.centralLowlands),
            Biome.CODEC.fieldOf("northern_forest").forGetter(source -> source.northernForest),
            Biome.CODEC.fieldOf("western_hills").forGetter(source -> source.westernHills),
            Biome.CODEC.fieldOf("eastern_marsh").forGetter(source -> source.easternMarsh),
            Biome.CODEC.fieldOf("southern_farmland").forGetter(source -> source.southernFarmland),
            Biome.CODEC.fieldOf("highlands").forGetter(source -> source.highlands)
    ).apply(instance, AuthoredBiomeSource::new));

    private final Holder<Biome> sea;
    private final Holder<Biome> coast;
    private final Holder<Biome> silverRiver;
    private final Holder<Biome> centralLowlands;
    private final Holder<Biome> northernForest;
    private final Holder<Biome> westernHills;
    private final Holder<Biome> easternMarsh;
    private final Holder<Biome> southernFarmland;
    private final Holder<Biome> highlands;

    public AuthoredBiomeSource(Holder<Biome> sea,
                               Holder<Biome> coast,
                               Holder<Biome> silverRiver,
                               Holder<Biome> centralLowlands,
                               Holder<Biome> northernForest,
                               Holder<Biome> westernHills,
                               Holder<Biome> easternMarsh,
                               Holder<Biome> southernFarmland,
                               Holder<Biome> highlands) {
        this.sea = sea;
        this.coast = coast;
        this.silverRiver = silverRiver;
        this.centralLowlands = centralLowlands;
        this.northernForest = northernForest;
        this.westernHills = westernHills;
        this.easternMarsh = easternMarsh;
        this.southernFarmland = southernFarmland;
        this.highlands = highlands;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
                sea, coast, silverRiver, centralLowlands, northernForest,
                westernHills, easternMarsh, southernFarmland, highlands
        ).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ,
                                       Climate.Sampler sampler) {
        double x = quartX * 4.0;
        double z = quartZ * 4.0;
        double surface = AuthoredContinentDensity.surfaceHeight(x, z);

        if (surface < 63.0) return sea;
        if (surface < 67.0) return coast;

        // The royal citadel terrace is dry, while Silver River remains a real ecological corridor
        // through the wider metropolitan region.
        if (inside(x, z, 0.0, -220.0, 420.0)) return centralLowlands;
        if (AuthoredContinentDensity.silverRiverStrength(x, z) > 0.45 && surface < 77.0) {
            return silverRiver;
        }

        double north = smoothstep(5_500.0, 17_500.0, -z);
        double west = smoothstep(5_500.0, 19_000.0, -x);
        double east = smoothstep(6_500.0, 19_000.0, x);
        double south = smoothstep(5_500.0, 17_000.0, z);

        if (surface > 101.0 || north > 0.72 && surface > 91.0 || west > 0.80 && surface > 96.0) {
            return highlands;
        }
        if (east > 0.35 && Math.abs(z - 1_200.0) < 13_500.0 && surface < 74.0) {
            return easternMarsh;
        }
        if (west > 0.30) return westernHills;
        if (north > 0.28) return northernForest;
        if (south > 0.28) return southernFarmland;

        // Stable kilometre-scale belts avoid the speckled appearance of ordinary biome noise.
        double belt = stableNoise(x * 0.00042, z * 0.00042, 0xC13FA9A902A6328FL);
        return belt > 0.28 ? northernForest : centralLowlands;
    }

    private static boolean inside(double x, double z, double cx, double cz, double radius) {
        double dx = x - cx;
        double dz = z - cz;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static double stableNoise(double x, double z, long salt) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double tz = fade(z - z0);
        double a = lerp(hashUnit(x0, z0, salt), hashUnit(x0 + 1, z0, salt), tx);
        double b = lerp(hashUnit(x0, z0 + 1, salt), hashUnit(x0 + 1, z0 + 1, salt), tx);
        return lerp(a, b, tz);
    }

    private static double hashUnit(int x, int z, long salt) {
        long value = salt;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) z * 0xC6BC279692B5CC83L;
        value ^= value >>> 27;
        value *= 0x3C79AC492BA7B653L;
        value ^= value >>> 33;
        value *= 0x1C69B3F74AC4AE35L;
        value ^= value >>> 27;
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
