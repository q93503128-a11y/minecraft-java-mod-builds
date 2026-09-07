package kr.moonseungjun.turnboundre.battle;

/** Battle-owned mutable state. Static stats remain in BattleParticipant. */
public final class ParticipantCombatState {
    private final int maxHp;
    private final int poiseMax;
    private final StatusRuntime statuses = new StatusRuntime();
    private int hp;
    private int poise;
    private int energy;

    public ParticipantCombatState(BattleParticipant participant) {
        if (participant == null) throw new IllegalArgumentException("participant must not be null");
        this.maxHp = participant.maxHp();
        this.poiseMax = participant.poiseMax();
        this.hp = maxHp;
        this.poise = poiseMax;
    }

    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public int poise() { return poise; }
    public int poiseMax() { return poiseMax; }
    public int energy() { return energy; }
    public boolean exposed() { return statuses.has(StatusService.EXPOSED); }
    public boolean poiseGuard() { return statuses.has(StatusService.POISE_GUARD); }
    public boolean guard() { return statuses.has(StatusService.GUARD); }
    public boolean alive() { return hp > 0; }
    public StatusRuntime statuses() { return statuses; }

    public void applyHpDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("damage must be >= 0");
        hp = Math.max(0, hp - damage);
    }

    /** Returns actual HP restored. */
    public int heal(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        int before = hp;
        hp = Math.min(maxHp, hp + amount);
        return hp - before;
    }

    /** Returns true only when this hit newly breaks Poise. */
    public boolean applyPoiseDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("damage must be >= 0");
        if (exposed() || damage == 0) return false;
        int effective = poiseGuard() ? (int) Math.floor(damage * 0.5D) : damage;
        poise = Math.max(0, poise - effective);
        if (poise == 0) {
            StatusService.applySingle(statuses, StatusService.EXPOSED);
            StatusService.remove(statuses, StatusService.POISE_GUARD);
            return true;
        }
        return false;
    }

    /** Canonical target-turn recovery: EXPOSED ends, Poise refills, then one-turn POISE_GUARD begins. */
    public boolean recoverAtTurnStartIfExposed() {
        if (!exposed()) return false;
        StatusService.remove(statuses, StatusService.EXPOSED);
        poise = poiseMax;
        StatusService.applySingle(statuses, StatusService.POISE_GUARD);
        return true;
    }

    /** POISE_GUARD lasts through the recovered actor's turn and expires at its next turn start. */
    public boolean expirePoiseGuardAtTurnStart() {
        if (exposed()) return false;
        return StatusService.remove(statuses, StatusService.POISE_GUARD);
    }

    public void gainEnergy(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        energy = Math.min(100, energy + amount);
    }

    public boolean spendEnergy(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }

    /** Effect-level Energy changes clamp to the canonical 0..100 range. */
    public void adjustEnergy(int delta) {
        energy = Math.max(0, Math.min(100, energy + delta));
    }

    public void setGuard(boolean guard) {
        if (guard) StatusService.applySingle(statuses, StatusService.GUARD);
        else StatusService.remove(statuses, StatusService.GUARD);
    }

    /** End-of-battle cleanup contract: Energy and transient battle statuses never leak into the next battle. */
    public void resetBattleResources() {
        energy = 0;
        statuses.clear();
    }
}
