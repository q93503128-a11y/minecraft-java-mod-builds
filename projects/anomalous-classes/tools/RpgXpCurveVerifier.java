import kr.moonseungjun.anomalousclasses.state.RpgXpCurve;

public final class RpgXpCurveVerifier {
    public static void main(String[] args) {
        expect(RpgXpCurve.cumulativeTo(25), 5649L);
        expect(RpgXpCurve.cumulativeTo(100), 72277L);
        expect(RpgXpCurve.cumulativeTo(200), 300288L);
        expect(RpgXpCurve.cumulativeTo(300), 713125L);
        var first = RpgXpCurve.advance(1, 0L, 0, RpgXpCurve.required(1) - 1);
        expect(first.level(), 1);
        expect(first.skillPoints(), 0);
        var second = RpgXpCurve.advance(first.level(), first.experience(), 0, 1L);
        expect(second.level(), 2);
        expect(second.skillPoints(), 3);
        expect(second.experience(), 0);
        var all = RpgXpCurve.advance(1, 0L, 0, RpgXpCurve.cumulativeTo(301));
        expect(all.level(), 301);
        expect(all.skillPoints(), 900);
        expect(all.experience(), 0);
        var over = RpgXpCurve.advance(300, 0L, 897, RpgXpCurve.required(300) + 33);
        expect(over.level(), 301);
        expect(over.skillPoints(), 900);
        expect(over.experience(), 33);
        var noGrant = RpgXpCurve.advance(40, 0L, 117, -500);
        expect(noGrant.level(), 40);
        expect(noGrant.experience(), 0);
        expect(noGrant.skillPoints(), 117);
        System.out.println("RPG XP P30 boundaries and 3SP multi-level grants: PASS");
    }
    private static void expect(long actual, long expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
}
