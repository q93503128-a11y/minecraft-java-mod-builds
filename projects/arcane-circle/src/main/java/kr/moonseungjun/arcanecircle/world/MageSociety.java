package kr.moonseungjun.arcanecircle.world;

/** Social identity is independent from species, spell school and moral alignment. */
public final class MageSociety {
    public enum Role {
        WANDERER("떠돌이"),
        HOUSEHOLD("생활"),
        LICENSED("공인"),
        WARDEN("수호"),
        SCHOLAR("연구"),
        VILLAIN("빌런");

        private final String displayName;
        Role(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }

        public static Role parse(String value) {
            if (value == null || value.isBlank()) return WANDERER;
            try { return valueOf(value.toUpperCase()); }
            catch (IllegalArgumentException ignored) { return WANDERER; }
        }
    }

    public enum Relation {
        ALLIED,
        FRIENDLY,
        NEUTRAL,
        HOSTILE
    }

    private MageSociety() {}

    public static Relation relation(MagicTradition left, MagicTradition right) {
        if (left == null) left = MagicTradition.UNBOUND;
        if (right == null) right = MagicTradition.UNBOUND;
        if (left == right && left != MagicTradition.UNBOUND) return Relation.ALLIED;
        if (left == MagicTradition.UNBOUND || right == MagicTradition.UNBOUND) return Relation.NEUTRAL;
        if ((left == MagicTradition.ARCANE && right == MagicTradition.DIVINE)
                || (left == MagicTradition.DIVINE && right == MagicTradition.ARCANE)) {
            return Relation.FRIENDLY;
        }
        if (left == MagicTradition.PRIMAL || right == MagicTradition.PRIMAL) {
            MagicTradition other = left == MagicTradition.PRIMAL ? right : left;
            return other == MagicTradition.OCCULT ? Relation.NEUTRAL : Relation.HOSTILE;
        }
        return Relation.NEUTRAL;
    }

    public static boolean hostile(MagicTradition left, MagicTradition right) {
        return relation(left, right) == Relation.HOSTILE;
    }

    public static boolean avoidsAutoTarget(MagicTradition left, MagicTradition right) {
        Relation relation = relation(left, right);
        return relation == Relation.ALLIED || relation == Relation.FRIENDLY;
    }
}
