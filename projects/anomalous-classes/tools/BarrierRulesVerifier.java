import kr.moonseungjun.anomalousclasses.state.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BarrierRulesVerifier {
    private static void same(double expected, double actual) {
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 1e-5)
            throw new AssertionError("expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Barrier invariant failed");
    }
    public static void main(String[] args) {
        PlayerClassProfile profile = new PlayerClassProfile();
        same(3, BarrierRules.capacity(profile));
        same(12, BarrierRules.reach(profile));
        same(360, BarrierRules.lifetime(BarrierRules.Kind.DEFENSE, profile));
        same(280, BarrierRules.lifetime(BarrierRules.Kind.ISOLATION, profile));
        same(90, BarrierRules.lifetime(BarrierRules.Kind.COMPRESSION, profile));
        same(1.225, BarrierRules.power(100));
        same(64, BarrierRules.integrity(BarrierRules.Kind.DEFENSE, BarrierRules.Size.MEDIUM, 1, profile));
        same(73.6, BarrierRules.integrity(BarrierRules.Kind.DEFENSE, BarrierRules.Size.SMALL, 1, profile));
        same(41.6, BarrierRules.integrity(BarrierRules.Kind.ISOLATION, BarrierRules.Size.LARGE, 1, profile));
        double total = 0;
        for (int i = 0; i < 4; i++)
            total += BarrierRules.compression(i, BarrierRules.Size.MEDIUM, 1, profile, false, false);
        same(26, total);
        profile.equipFocus(new ClassFocus(ClassId.BARRIER_CASTER, FocusTier.INCOMPREHENSIBLE));
        same(89.6, BarrierRules.integrity(BarrierRules.Kind.DEFENSE, BarrierRules.Size.MEDIUM, 1, profile));
        profile.equipFocus(new ClassFocus(ClassId.PUPPETEER, FocusTier.INCOMPREHENSIBLE));
        same(64, BarrierRules.integrity(BarrierRules.Kind.DEFENSE, BarrierRules.Size.MEDIUM, 1, profile));
        Vec3 base = new Vec3(0, 64, 0);
        check(BarrierRules.inside(new Vec3(4.9, 66, 0), base, 5));
        check(!BarrierRules.inside(new Vec3(5.1, 66, 0), base, 5));
        check(!BarrierRules.inside(new Vec3(0, 68.1, 0), base, 5));
        check(BarrierRules.overlaps(new AABB(4.7, 65, -.3, 5.3, 66, .3), base, 5));
        check(!BarrierRules.overlaps(new AABB(6, 65, 6, 7, 66, 7), base, 5));
        check(BarrierRules.crossing(new Vec3(6, 66, 0), new Vec3(4, 66, 0), base, 5));
        check(BarrierRules.Kind.of(-1) == null && BarrierRules.Size.of(20) == null);
        System.out.println("Barrier geometry, rank-zero damage, capacity and focus: PASS");
    }
}