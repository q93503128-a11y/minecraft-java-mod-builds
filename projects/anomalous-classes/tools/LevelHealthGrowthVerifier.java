import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.LevelHealthGrowth;
public final class LevelHealthGrowthVerifier {
    private static void check(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-5) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        check(0.0, LevelHealthGrowth.bonus(ClassId.BARRIER_CASTER, 1));
        check(4.455, LevelHealthGrowth.bonus(ClassId.BARRIER_CASTER, 100));
        check(11.88, LevelHealthGrowth.bonus(ClassId.MENTAL_DOMINATOR, 100));
        check(5.94, LevelHealthGrowth.bonus(ClassId.PUPPETEER, 100));
        check(0.0, LevelHealthGrowth.bonus(ClassId.PARASITE_MORPHER, 100));
        check(0.0, LevelHealthGrowth.bonus(ClassId.PACT_CASTER, 100));
        check(0.0, LevelHealthGrowth.bonus(ClassId.SWARM_CASTER, 100));
        System.out.println("Class-specific level HP growth and double-application guard: PASS");
    }
}
