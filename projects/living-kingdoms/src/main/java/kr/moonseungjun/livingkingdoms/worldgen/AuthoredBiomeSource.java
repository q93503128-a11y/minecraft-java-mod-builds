package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Permanent coordinate-authored biome geography for the Living Realm.
 *
 * <p>The world seed may vary decoration details, but it cannot move kingdoms, climate zones,
 * mountain biomes, the western archipelago or the principal river corridors.</p>
 */
public final class AuthoredBiomeSource extends BiomeSource {
    public static final MapCodec<AuthoredBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("ocean").forGetter(source -> source.ocean),
            Biome.CODEC.fieldOf("beach").forGetter(source -> source.beach),
            Biome.CODEC.fieldOf("river").forGetter(source -> source.river),
            Biome.CODEC.fieldOf("plains").forGetter(source -> source.plains),
            Biome.CODEC.fieldOf("forest").forGetter(source -> source.forest),
            Biome.CODEC.fieldOf("silvana").forGetter(source -> source.silvana),
            Biome.CODEC.fieldOf("kardum").forGetter(source -> source.kardum),
            Biome.CODEC.fieldOf("dragonlands").forGetter(source -> source.dragonlands),
            Biome.CODEC.fieldOf("steppe").forGetter(source -> source.steppe),
            Biome.CODEC.fieldOf("sahar").forGetter(source -> source.sahar),
            Biome.CODEC.fieldOf("ruins").forGetter(source -> source.ruins),
            Biome.CODEC.fieldOf("archipelago").forGetter(source -> source.archipelago)
    ).apply(instance, AuthoredBiomeSource::new));

    private final Holder<Biome> ocean;
    private final Holder<Biome> beach;
    private final Holder<Biome> river;
    private final Holder<Biome> plains;
    private final Holder<Biome> forest;
    private final Holder<Biome> silvana;
    private final Holder<Biome> kardum;
    private final Holder<Biome> dragonlands;
    private final Holder<Biome> steppe;
    private final Holder<Biome> sahar;
    private final Holder<Biome> ruins;
    private final Holder<Biome> archipelago;

    public AuthoredBiomeSource(Holder<Biome> ocean,
                               Holder<Biome> beach,
                               Holder<Biome> river,
                               Holder<Biome> plains,
                               Holder<Biome> forest,
                               Holder<Biome> silvana,
                               Holder<Biome> kardum,
                               Holder<Biome> dragonlands,
                               Holder<Biome> steppe,
                               Holder<Biome> sahar,
                               Holder<Biome> ruins,
                               Holder<Biome> archipelago) {
        this.ocean = ocean;
        this.beach = beach;
        this.river = river;
        this.plains = plains;
        this.forest = forest;
        this.silvana = silvana;
        this.kardum = kardum;
        this.dragonlands = dragonlands;
        this.steppe = steppe;
        this.sahar = sahar;
        this.ruins = ruins;
        this.archipelago = archipelago;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
                ocean, beach, river, plains, forest, silvana,
                kardum, dragonlands, steppe, sahar, ruins, archipelago
        ).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ,
                                       Climate.Sampler sampler) {
        double x = quartX * 4.0;
        double z = quartZ * 4.0;
        double surface = AuthoredContinentDensity.surfaceHeight(x, z);

        if (surface < 63.0) return ocean;
        if (surface < 67.0) return beach;
        if (riverStrength(x, z) > 0.52 && surface < 77.0) return river;

        double silvanaWeight = radial(x, z, -2_400.0, -1_200.0, 1_350.0);
        if (silvanaWeight > 0.08) return silvana;

        double dragonWeight = radial(x, z, 0.0, -4_200.0, 1_800.0);
        if (dragonWeight > 0.05 || (z < -3_050.0 && surface > 88.0)) return dragonlands;

        double kardumWeight = radial(x, z, 2_200.0, -1_500.0, 1_500.0);
        if (kardumWeight > 0.05 || surface > 105.0) return kardum;

        double ruinsWeight = radial(x, z, 3_800.0, -2_800.0, 1_050.0);
        if (ruinsWeight > 0.08) return ruins;

        double saharWeight = radial(x, z, 3_200.0, 2_600.0, 1_550.0);
        if (saharWeight > 0.06) return sahar;

        double steppeWeight = radial(x, z, 3_400.0, 300.0, 1_450.0);
        if (steppeWeight > 0.06) return steppe;

        if (x < -3_050.0 && z > 250.0) return archipelago;

        // Central and Velas lowlands alternate in broad, stable belts instead of noisy biome specks.
        double belt = stableNoise(x * 0.00115, z * 0.00115, 0xC13FA9A902A6328FL);
        return belt > -0.04 ? forest : plains;
    }

    private static double riverStrength(double x, double z) {
        double first = riverBand(x, z, 0.0, 520.0, 680.0, 48.0);
        double second = riverBand(z, x, -450.0, 760.0, 910.0, 42.0);
        double third = riverBand(x + z * 0.24, z, 1_350.0, 930.0, 1_250.0, 38.0);
        return Math.max(first, Math.max(second, third));
    }

    private static double riverBand(double primary, double secondary, double phase,
                                    double wavelength, double bendScale, double halfWidth) {
        double center = phase + Math.sin(secondary / wavelength) * bendScale
                + Math.sin(secondary / (wavelength * 0.37)) * bendScale * 0.19;
        double distance = Math.abs(primary - center);
        return 1.0 - smoothstep(halfWidth, halfWidth * 3.2, distance);
    }

    private static double radial(double x, double z, double cx, double cz, double radius) {
        return 1.0 - smoothstep(0.0, radius, Math.hypot(x - cx, z - cz));
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
