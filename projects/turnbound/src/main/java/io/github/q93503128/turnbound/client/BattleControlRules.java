package io.github.q93503128.turnbound.client;

/** Pure presentation policy for campaign-locked battle controls. */
final class BattleControlRules {
    record State(String autoLabel, boolean autoActive,
                 String speedLabel, boolean speedActive,
                 String fleeLabel, boolean fleeActive) {}

    private BattleControlRules() {}

    static State state(ClientBattleState.Snapshot snapshot) {
        if (snapshot.finished()) {
            return new State("자동", false, snapshot.speed() + "배속", false, "복귀", true);
        }
        String autoLabel = snapshot.autoAllowed()
                ? (snapshot.auto() ? "자동✓" : "자동")
                : "자동 잠금";
        String speedLabel = snapshot.speedAllowed() ? snapshot.speed() + "배속" : "2배속 잠금";
        String fleeLabel = snapshot.fleeAllowed() ? "도주" : "도주 불가";
        return new State(autoLabel, snapshot.autoAllowed(), speedLabel, snapshot.speedAllowed(), fleeLabel, snapshot.fleeAllowed());
    }
}
