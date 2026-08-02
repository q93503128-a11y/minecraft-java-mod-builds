package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Deterministic coordinate-authored terrain for the entire Living Realm.
 *
 * <p>This function deliberately does not sample or adapt to the vanilla overworld. Political regions,
 * coastlines, mountain chains, river valleys and safe capital plateaus are part of one permanent map.
 * World seeds may still affect biome decoration, but they never move the continental geography.</p>
 */
public enum AuthoredContinentDensity implements DensityFunction.SimpleFunction {
    INSTANCE;

    public static final KeyDispatchDataCodec<AuthoredContinentDensity> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    private static final double SEA_LEVEL = 63.0;
    private static final Capital[] CAPITALS = {
            new Capital(0, 0, 72.0, 520.0),
            new Capital(-2_400, -1_200, 79.0, 470.0),
            new Capital(2_200, -1_500, 92.0, 500.0),
            new Capital(3_400, 300, 76.0, 420.0),
            new Capital(600, 2_500, 68.0, 420.0),
            new Capital(3_200, 2_600, 73.0, 430.0),
            new Capital(3_800, -2_800, 88.0, 430.0),
            new Capital(0, -4_200, 105.0, 500.0),
            new Capital(-4_200, 1_800, 67.0, 360.0)
    };

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();
        double surface = surfaceHeight(x, z);
        double density = (surface - y) / 18.0;

        // Deep caves are carved from the authored rock mass, but the upper eight blocks remain whole
        // so roads, houses and settlement floors never open into random holes.
        if (y < surface - 10.0 && y > -48) {
            double cave = Math.abs(fractal3(x * 0.021, y * 0.028, z * 0.021, 0x61C8864680B583EBL));
            double caveLimit = y < 5 ? 0.69 : 0.75;
            if (cave > caveLimit) {
                density -= (cave - caveLimit) * 7.5;
            }
        }
        return density;
    }

    @Override
    public double minValue() {
        return -24.0;
    }

    @Override
    public double maxValue() {
        return 24.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    public static double surfaceHeight(double x, double z) {
        double broad = fractal2(x * 0.00042, z * 0.00042, 0x9E3779B97F4A7C15L);
        double hills = fractal2(x * 0.00155, z * 0.00155, 0xD1B54A32D192ED03L);
        double detail = fractal2(x * 0.0065, z * 0.0065, 0x94D049BB133111EBL);

        // Three overlapping plates form the main continent. A separate western field becomes an
        // archipelago rather than an accidental vanilla ocean biome.
        double central = ellipse(x + 100.0, z + 250.0, 4_650.0, 3_650.0);
        double north = ellipse(x, z + 3_250.0, 2_350.0, 2_250.0);
        double east = ellipse(x - 2_650.0, z - 900.0, 2_550.0, 2_850.0);
        double west = ellipse(x + 3_350.0, z - 850.0, 2_000.0, 2_350.0);
        double land = Math.max(Math.max(central, north), Math.max(east, west));
        land += broad * 0.18;

        // Western archipelago: fixed large islands with noise-cut channels.
        double archipelago = ellipse(x + 4_250.0, z - 1_750.0, 1_450.0, 1_250.0)
                + hills * 0.32 - 0.12;
        land = Math.max(land, archipelago);

        double surface;
        if (land <= -0.10) {
            // Ocean floor and abyssal shelves.
            surface = 43.0 + broad * 8.0 + detail * 2.0;
        } else if (land < 0.08) {
            // Beaches and shallow coastal shelves.
            double t = smoothstep(-0.10, 0.08, land);
            surface = lerp(54.0 + hills * 3.0, 67.0 + hills * 4.0, t);
        } else {
            surface = 69.0 + Math.min(land, 0.9) * 17.0 + hills * 7.0 + detail * 2.2;
        }

        // Major mountain systems. Kardum and the northern dragonlands are intentionally dominant.
        double kardum = radial(x, z, 2_200.0, -1_500.0, 1_300.0);
        double dragon = radial(x, z, 0.0, -4_200.0, 1_550.0);
        double greyCrown = radial(x, z, 3_800.0, -2_800.0, 950.0);
        double ridge = Math.abs(fractal2(x * 0.0025, z * 0.0025, 0xBF58476D1CE4E5B9L));
        surface += kardum * (16.0 + ridge * 30.0);
        surface += dragon * (24.0 + ridge * 42.0);
        surface += greyCrown * (9.0 + ridge * 17.0);

        // Silvana is a protected forest basin with tall outer ridges and a broad inhabitable floor.
        double silvana = radial(x, z, -2_400.0, -1_200.0, 1_050.0);
        surface -= silvana * 6.0;
        surface += ring(x, z, -2_400.0, -1_200.0, 780.0, 270.0) * 14.0;

        // Sahar dunes and the red steppe stay wide and traversable instead of becoming random cliffs.
        double sahar = radial(x, z, 3_200.0, 2_600.0, 1_250.0);
        double steppe = radial(x, z, 3_400.0, 300.0, 1_100.0);
        surface += sahar * (Math.sin(x * 0.018 + z * 0.006) * 3.2 + 2.0);
        surface += steppe * (hills * 2.5 - 1.0);

        // Authored river corridors. Their beds cross the central continent and reach the sea.
        surface -= riverCut(x, z, 0.0, 520.0, 680.0, 48.0, 10.0);
        surface -= riverCut(z, x, -450.0, 760.0, 910.0, 42.0, 8.0);
        surface -= riverCut(x + z * 0.24, z, 1_350.0, 930.0, 1_250.0, 38.0, 7.0);

        // Capital districts are part of the terrain design itself. The settlement builder performs
        // only local landscaping; it no longer searches for or conquers a random vanilla hill.
        for (Capital capital : CAPITALS) {
            double distance = Math.hypot(x - capital.x, z - capital.z);
            if (distance >= capital.radius) continue;
            double blend = 1.0 - smoothstep(capital.radius * 0.52, capital.radius, distance);
            double localRoll = fractal2((x - capital.x) * 0.004, (z - capital.z) * 0.004,
                    0xDB4F0B9175AE2165L) * 2.4;
            surface = lerp(surface, capital.height + localRoll, blend);
        }
        return surface;
    }

    private static double ellipse(double x, double z, double radiusX, double radiusZ) {
        double distance = Math.sqrt((x * x) / (radiusX * radiusX) + (z * z) / (radiusZ * radiusZ));
        return 1.0 - distance;
    }

    private static double radial(double x, double z, double cx, double cz, double radius) {
        return 1.0 - smoothstep(0.0, radius, Math.hypot(x - cx, z - cz));
    }

    private static double ring(double x, double z, double cx, double cz, double radius, double width) {
        double d = Math.abs(Math.hypot(x - cx, z - cz) - radius);
        return 1.0 - smoothstep(0.0, width, d);
    }

    private static double riverCut(double primary, double secondary, double phase,
                                   double wavelength, double bendScale, double halfWidth, double depth) {
        double center = phase + Math.sin(secondary / wavelength) * bendScale
                + Math.sin(secondary / (wavelength * 0.37)) * bendScale * 0.19;
        double distance = Math.abs(primary - center);
        return (1.0 - smoothstep(halfWidth, halfWidth * 3.2, distance)) * depth;
    }

    private static double fractal2(double x, double z, long salt) {
        double total = 0.0;
        double amplitude = 0.58;
        double frequency = 1.0;
        for (int octave = 0; octave < 4; octave++) {
            total += valueNoise2(x * frequency, z * frequency, salt + octave * 0x9E3779B97F4A7C15L)
                    * amplitude;
            frequency *= 2.03;
            amplitude *= 0.5;
        }
        return total;
    }

    private static double fractal3(double x, double y, double z, long salt) {
        double total = 0.0;
        double amplitude = 0.62;
        double frequency = 1.0;
        for (int octave = 0; octave < 3; octave++) {
            total += valueNoise3(x * frequency, y * frequency, z * frequency,
                    salt + octave * 0xD1B54A32D192ED03L) * amplitude;
            frequency *= 2.07;
            amplitude *= 0.48;
        }
        return total;
    }

    private static double valueNoise2(double x, double z, long salt) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double tz = fade(z - z0);
        double a = lerp(hashUnit(x0, 0, z0, salt), hashUnit(x0 + 1, 0, z0, salt), tx);
        double b = lerp(hashUnit(x0, 0, z0 + 1, salt), hashUnit(x0 + 1, 0, z0 + 1, salt), tx);
        return lerp(a, b, tz);
    }

    private static double valueNoise3(double x, double y, double z, long salt) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double ty = fade(y - y0);
        double tz = fade(z - z0);
        double c000 = hashUnit(x0, y0, z0, salt);
        double c100 = hashUnit(x0 + 1, y0, z0, salt);
        double c010 = hashUnit(x0, y0 + 1, z0, salt);
        double c110 = hashUnit(x0 + 1, y0 + 1, z0, salt);
        double c001 = hashUnit(x0, y0, z0 + 1, salt);
        double c101 = hashUnit(x0 + 1, y0, z0 + 1, salt);
        double c011 = hashUnit(x0, y0 + 1, z0 + 1, salt);
        double c111 = hashUnit(x0 + 1, y0 + 1, z0 + 1, salt);
        double x00 = lerp(c000, c100, tx);
        double x10 = lerp(c010, c110, tx);
        double x01 = lerp(c001, c101, tx);
        double x11 = lerp(c011, c111, tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    private static double hashUnit(int x, int y, int z, long salt) {
        long value = salt;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) y * 0x9E3779B97F4A7C15L;
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
        if (edge0 == edge1) return value < edge0 ? 0.0 : 1.0;
        double t = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record Capital(double x, double z, double height, double radius) {
    }
}
