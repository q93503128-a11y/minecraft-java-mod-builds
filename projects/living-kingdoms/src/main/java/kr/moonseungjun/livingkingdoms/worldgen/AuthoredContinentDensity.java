package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * One-block-per-metre authored terrain for the complete Erden kingdom footprint.
 *
 * <p>The active landmass is approximately 48 km east-west by 40 km north-south. It contains a
 * navigable river basin, western mineral hills, northern forest uplands, eastern marshes and a
 * southern grain belt. No legacy test capitals remain in this density function.</p>
 */
public enum AuthoredContinentDensity implements DensityFunction.SimpleFunction {
    INSTANCE;

    public static final KeyDispatchDataCodec<AuthoredContinentDensity> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    private static final double SEA_LEVEL = 63.0;

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();
        double surface = surfaceHeight(x, z);
        double density = (surface - y) / 18.0;

        // Settlement floors and roads keep a solid ten-metre crust. Deeper caves remain part of the
        // natural geology and can later be assigned to mines, aquifers and dungeons deliberately.
        if (y < surface - 10.0 && y > -48) {
            double cave = Math.abs(fractal3(x * 0.020, y * 0.028, z * 0.020,
                    0x61C8864680B583EBL));
            double limit = y < 5 ? 0.69 : 0.75;
            if (cave > limit) density -= (cave - limit) * 7.5;
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
        double continental = fractal2(x * 0.00010, z * 0.00010, 0x9E3779B97F4A7C15L);
        double regional = fractal2(x * 0.00034, z * 0.00034, 0xD1B54A32D192ED03L);
        double hills = fractal2(x * 0.00115, z * 0.00115, 0x94D049BB133111EBL);
        double detail = fractal2(x * 0.0042, z * 0.0042, 0xDB4F0B9175AE2165L);

        // The kingdom occupies a large, irregular peninsula rather than the old miniature island.
        double erden = ellipse(x + 250.0, z - 150.0, 24_000.0, 20_000.0);
        erden += continental * 0.12 + regional * 0.05;

        double surface;
        if (erden <= -0.08) {
            surface = 42.0 + continental * 8.0 + hills * 2.0;
        } else if (erden < 0.035) {
            double coast = smoothstep(-0.08, 0.035, erden);
            surface = lerp(53.0 + regional * 3.0, 67.0 + hills * 2.5, coast);
        } else {
            surface = 69.0 + Math.min(erden, 0.85) * 12.0
                    + continental * 7.0 + regional * 5.0 + hills * 2.8 + detail * 0.8;
        }

        // Western mineral hills and north-western ridges form the kingdom's stone and iron belt.
        double west = smoothstep(3_000.0, 19_000.0, -x);
        double westRidge = Math.abs(fractal2(x * 0.00078, z * 0.00078,
                0xBF58476D1CE4E5B9L));
        surface += west * (5.0 + westRidge * 21.0);

        // Northern forest uplands are high enough to change climate without becoming a continuous
        // impassable wall. Local passes remain available for roads and settlements.
        double north = smoothstep(4_000.0, 17_500.0, -z);
        double northRidge = Math.abs(fractal2(x * 0.00092, z * 0.00092,
                0xC13FA9A902A6328FL));
        surface += north * (4.0 + northRidge * 14.0);

        // Eastern marsh country is a shallow, poorly drained basin with raised natural levees.
        double east = smoothstep(6_000.0, 19_000.0, x);
        double marshBand = 1.0 - smoothstep(0.0, 14_000.0, Math.abs(z - 1_200.0));
        surface -= east * marshBand * (4.0 + Math.max(0.0, regional) * 2.0);

        // The southern grain belt is broad and gently rolling, not a mathematically flat platform.
        double south = smoothstep(5_000.0, 17_000.0, z);
        double farmTarget = 70.0 + continental * 3.0 + regional * 2.2 + detail * 0.5;
        surface = lerp(surface, farmTarget, south * 0.58);

        // A western tributary creates the crossing that originally justified the royal capital.
        double tributaryCenterZ = 2_650.0 + Math.sin(x / 2_700.0) * 520.0;
        double tributaryDistance = Math.abs(z - tributaryCenterZ);
        double tributary = 1.0 - smoothstep(45.0, 185.0, tributaryDistance);
        if (x < 1_500.0 && x > -12_000.0) surface -= tributary * 5.5;

        // The 6.2 x 5 km metropolitan basin follows natural relief. Only the inner citadel terrace is
        // strongly stabilised; residential districts keep several metres of elevation change.
        double capitalDistance = Math.hypot(x, z);
        if (capitalDistance < 3_300.0) {
            double urbanBlend = 1.0 - smoothstep(1_600.0, 3_300.0, capitalDistance);
            double urbanRoll = fractal2(x * 0.00115, z * 0.00115,
                    0x8CB92BA72F3D8DD7L) * 3.1;
            surface = lerp(surface, 72.0 + urbanRoll, urbanBlend * 0.62);
        }
        double citadelDistance = Math.hypot(x, z + 220.0);
        if (citadelDistance < 310.0) {
            double terrace = 1.0 - smoothstep(190.0, 310.0, citadelDistance);
            double local = fractal2(x * 0.004, (z + 220.0) * 0.004,
                    0xA24BAED4963EE407L) * 0.7;
            surface = lerp(surface, 74.0 + local, terrace * 0.88);
        }

        // Silver River is applied after metropolitan grading so the promised navigable channel
        // cannot be lifted back above sea level by the capital terrain blend. Through the capital
        // latitude it bends outside the western wall, keeping dense urban lots dry while allowing
        // the authored west wharf to project directly into the main channel.
        double riverCenterX = silverRiverCenterX(z);
        double riverDistance = Math.abs(x - riverCenterX);
        double floodplain = 1.0 - smoothstep(95.0, 310.0, riverDistance);
        double channel = 1.0 - smoothstep(0.0, 72.0, riverDistance);
        surface -= floodplain * 4.0 + channel * 7.5;

        return surface;
    }

    public static double silverRiverStrength(double x, double z) {
        double distance = Math.abs(x - silverRiverCenterX(z));
        return 1.0 - smoothstep(72.0, 310.0, distance);
    }

    /** Exact authored centre line used by terrain, drainage diagnostics and river-port construction. */
    public static double silverRiverCenterX(double z) {
        double natural = -820.0 + Math.sin(z / 2_900.0) * 470.0
                + Math.sin(z / 930.0) * 105.0;
        double capitalBypass = 1.0 - smoothstep(900.0, 1_800.0, Math.abs(z));
        double wallSideChannel = -1_310.0 + Math.sin(z / 760.0) * 26.0;
        return lerp(natural, wallSideChannel, capitalBypass);
    }

    private static double ellipse(double x, double z, double radiusX, double radiusZ) {
        double distance = Math.sqrt((x * x) / (radiusX * radiusX)
                + (z * z) / (radiusZ * radiusZ));
        return 1.0 - distance;
    }

    private static double fractal2(double x, double z, long salt) {
        double total = 0.0;
        double amplitude = 0.58;
        double frequency = 1.0;
        for (int octave = 0; octave < 4; octave++) {
            total += valueNoise2(x * frequency, z * frequency,
                    salt + octave * 0x9E3779B97F4A7C15L) * amplitude;
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
}
