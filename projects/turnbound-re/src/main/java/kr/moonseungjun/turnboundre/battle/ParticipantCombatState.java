package kr.moonseungjun.turnboundre.battle;

/** Battle-owned mutable state. Static stats remain in BattleParticipant. */
public final class ParticipantCombatState {
    private final int maxHp;
    private final int poiseMax;
    private int hp;
    private int poise;
    private int energy;
    private boolean exposed;
    private boolean poiseGuard;
    private boolean guard;

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
    public boolean exposed() { return exposed; }
    public boolean poiseGuard() { return poiseGuard; }
    public boolean guard() { return guard; }
    public boolean alive() { return hp > 0; }

    public void applyHpDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("damage must be >= 0");
        hp = Math.max(0, hp - damage);
    }

    /** Returns true only when this hit newly breaks Poise. */
    public boolean applyPoiseDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("damage must be >= 0");
        if (exposed || damage == 0) return false;
        int effective = poiseGuard ? (int) Math.floor(damage * 0.5D) : damage;
        poise = Math.max(0, poise - effective);
        if (poise == 0) {
            exposed = true;
            poiseGuard = false;
            return true;
        }
        return false;
    }

    /** Canonical target-turn recovery: EXPOSED ends, Poise refills, then one-turn POISE_GUARD begins. */
    public boolean recoverAtTurnStartIfExposed() {
        if (!exposed) return false;
        exposed = false;
        poise = poiseMax;
        poiseGuard = true;
        return true;
    }

    /** POISE_GUARD lasts through the recovered actor's turn and expires at its next turn start. */
    public void expirePoiseGuardAtTurnStart() {
        if (!exposed) poiseGuard = false;
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

    public void setGuard(boolean guard) { this.guard = guard; }
}
