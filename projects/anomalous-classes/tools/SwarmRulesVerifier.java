import kr.moonseungjun.anomalousclasses.state.SwarmRules;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData;
import kr.moonseungjun.anomalousclasses.state.SwarmFlightRules;

public final class SwarmRulesVerifier {
    public static void main(String[] args) {
        require(SwarmRules.maximum(1) == 16, "level 1");
        require(SwarmRules.maximum(25) == 48, "level 25");
        require(SwarmRules.maximum(100) == 80, "level 100");
        require(SwarmRules.maximum(500) == 128, "level 500");
        require(SwarmRules.maximum(100000) == 128, "bounded entity budget");
        require(SwarmFlightRules.requiredLift(1) == 0, "no low-level flight");
        require(SwarmFlightRules.requiredLift(25) == 24, "level 25");
        require(SwarmFlightRules.requiredLift(100) == 30, "level 100");
        require(SwarmFlightRules.requiredLift(500) == 30, "max support");
        require(!SwarmFlightRules.qualified(25, 23), "flight needs actual organisms");
        require(SwarmFlightRules.qualified(25, 24), "available lift");
        require(SwarmFlightRules.flyingSpeed(500) > SwarmFlightRules.flyingSpeed(25),
                "later flight grows");
        for (int pct : new int[]{0, 40, 50, 100}) {
            var split = SwarmRules.distribute(80, 30, 0, pct);
            require(split.total() == 80, "flight conservation");
            require(split.lift() == 30, "flight reservation");
            require(split.attack() == 50 * pct / 100, "tactic exactness");
        }
        var state = new SwarmSavedData.State();
        require(state.alive() == 16 && state.regroup() == 16, "no injected tactic");
        require(state.activeName().isBlank() && state.tactics().isEmpty(), "no starter preset");
        require(state.saveTactic("방어", 0), "user tactic 0");
        for (int tick = 1; tick <= 50; tick++) state.advance(tick * 10L, 16);
        require(state.attack() == 0 && state.guard() == 16, "0:100");
        require(state.saveTactic("돌격", 100), "user tactic 100");
        for (int tick = 51; tick <= 130; tick++) state.advance(tick * 10L, 16);
        require(state.attack() == 16 && state.guard() == 0, "100:0");
        int beforeFlight = state.alive();
        for (int x = 0; x < 8; x++) {
            int moved = state.reserveLift(24, true, true);
            require(moved <= 2, "flight deployment rate limit");
        }
        require(state.lift() == 16 && state.attack() == 0, "lift cannot create creatures");
        require(state.alive() == beforeFlight, "lift N conservation");
        require(state.lose(SwarmSavedData.SwarmNodeRole.LIFT, 3) == 3, "lift can be killed");
        require(state.lift() == 13 && state.alive() == beforeFlight - 3, "lift casualties real");
        require(state.releaseLift() == 13, "landing returns survivors only");
        require(state.lift() == 0, "landing clears lift");
        state.verify();
        for (int t = 1; t <= 20; t++) state.advance(10000 + 10L * t, 16);
        require(state.attack() > 0, "attack resuming after landing");
        int casualties = Math.min(5, state.attack());
        require(state.lose(SwarmSavedData.SwarmNodeRole.ATTACK, casualties) == casualties, "real loss");
        require(state.alive() == beforeFlight - 3 - casualties, "no free refill");
        require(state.saveTactic("균등", 50), "mid tactic");
        for (int tick = 131; tick <= 160; tick++) state.advance(tick * 10L, 16);
        state.verify();
        require(!state.saveTactic("bad", 101), "reject invalid ratio");
        require(SwarmRules.sacrificeCost(0) == 0, "zero attack cannot sacrifice");
        require(SwarmRules.sacrificeCost(1) == 1, "minimum sacrifice cost");
        require(SwarmRules.sacrificeCost(16) == 4, "sacrifice based on attacking not total");
        require(SwarmRules.sacrificeDamage(0, 1) == 0F, "no damage without paid organisms");
        require(SwarmRules.rushDamage(0, 1) == 0F, "no attack when allocated zero");
        require(state.cooldownRemaining(1, 1000) == 0, "fresh rush");
        require(state.commitCooldown(1, 1000, 160), "first rush cast accepted");
        require(state.cooldownRemaining(1, 1000) == 160, "rush cooldown starts");
        require(!state.commitCooldown(1, 1001, 160), "same skill cannot spam");
        require(state.commitCooldown(2, 1002, 240), "separate sacrifice cooldown");
        require(state.cooldownRemaining(2, 1003) == 239, "sacrifice cooldown active");
        require(state.commitCooldown(1, 1160, 160), "rush ready again");
        System.out.println("Swarm rules / conservation / user tactics VERIFIED");
    }
    private static void require(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
}
