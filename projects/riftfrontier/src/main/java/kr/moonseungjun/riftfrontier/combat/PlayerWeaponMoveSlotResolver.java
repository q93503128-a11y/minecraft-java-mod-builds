package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps a small input-action slot to one of a validated weapon family's authored moves.
 *
 * <p>The slot is only an input indirection. It does not own timing, damage, target, hit or cooldown
 * authority. Stable content-id ordering makes the mapping deterministic until the authored weapon
 * schema grows an explicit presentation/input-order field.</p>
 */
public final class PlayerWeaponMoveSlotResolver {
    private PlayerWeaponMoveSlotResolver() {}

    public static Optional<ContentId> resolve(CoreDefinition.WeaponFamily family, int zeroBasedSlot) {
        Objects.requireNonNull(family, "family");
        if (zeroBasedSlot < 0) return Optional.empty();

        List<ContentId> moves = family.moves().stream().sorted().toList();
        if (zeroBasedSlot >= moves.size()) return Optional.empty();
        return Optional.of(moves.get(zeroBasedSlot));
    }
}
