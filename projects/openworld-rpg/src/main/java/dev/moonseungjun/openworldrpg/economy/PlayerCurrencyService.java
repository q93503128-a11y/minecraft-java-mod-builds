package dev.moonseungjun.openworldrpg.economy;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-only mutation/read boundary for project Gold. */
public final class PlayerCurrencyService {
    private PlayerCurrencyService() {
    }

    public static PlayerCurrencyState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerCurrencyAttachments.CURRENCY,
                PlayerCurrencyState.initial()
        );
    }

    public static CreditResult creditOnce(
            ServerPlayer player,
            String transactionId,
            long amount
    ) {
        PlayerCurrencyState current = state(player);
        PlayerCurrencyState next = current.creditOnce(transactionId, amount);
        if (current.equals(next)) {
            return new CreditResult(current, false);
        }

        player.setAttached(PlayerCurrencyAttachments.CURRENCY, next);
        return new CreditResult(next, true);
    }

    public static PlayerCurrencyState forgetCreditTransaction(
            ServerPlayer player,
            String transactionId
    ) {
        PlayerCurrencyState current = state(player);
        PlayerCurrencyState next = current.forgetCreditTransaction(transactionId);
        if (!current.equals(next)) {
            player.setAttached(PlayerCurrencyAttachments.CURRENCY, next);
        }
        return next;
    }

    public record CreditResult(
            PlayerCurrencyState state,
            boolean applied
    ) {
        public CreditResult {
            Objects.requireNonNull(state, "state");
        }
    }
}
