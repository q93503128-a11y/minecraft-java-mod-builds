package kr.moonseungjun.anomalousclasses.state;

/**
 * Deterministic swarm-count arithmetic. Numbers are first-playtest candidates,
 * not finalized balance. A logical organism is not a server AI entity.
 */
public final class SwarmRules {
    private SwarmRules() {}

    public record Split(int attack, int guard, int lift, int regroup) {
        public int total() { return attack + guard + lift + regroup; }
    }

    public static int maximum(int level) {
        int safe = Math.max(1, level);
        if (safe <= 25) return 16 + (safe - 1) * 32 / 24;
        if (safe <= 100) return 48 + (safe - 25) * 32 / 75;
        if (safe <= 500) return 80 + (safe - 100) * 48 / 400;
        return 128;
    }

    public static Split distribute(int alive, int lift, int regroup, int attackPercent) {
        if (alive < 0 || lift < 0 || regroup < 0 || lift + regroup > alive
                || attackPercent < 0 || attackPercent > 100)
            throw new IllegalArgumentException("invalid swarm allocation");
        int available = alive - lift - regroup;
        int attack = (int) ((long) available * attackPercent / 100L);
        return new Split(attack, available - attack, lift, regroup);
    }

    public static int casualties(float damage, int level, int available) {
        if (!(damage > 0.0F) || available <= 0) return 0;
        double resistance = 2.0 + Math.min(1000, Math.max(1, level)) * 0.025;
        return Math.min(available, Math.max(1, (int) Math.ceil(damage / resistance)));
    }
}
