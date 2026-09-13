package kr.moonseungjun.turnboundre.progression;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Predicate;

/** Generic exact-count helper so inventory transaction semantics stay testable without Minecraft registry bootstrap. */
final class ExactResourceTransaction {
    private ExactResourceTransaction() {}

    static <T extends Resource> int count(ResourceHandler<T> handler, Predicate<T> matches) {
        if (handler == null || matches == null) throw new IllegalArgumentException("handler/matches required");
        long total = 0L;
        for (int index = 0; index < handler.size(); index++) {
            T resource = handler.getResource(index);
            if (resource != null && !resource.isEmpty() && matches.test(resource)) {
                total += handler.getAmountAsLong(index);
                if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    static <T extends Resource> int extract(
            ResourceHandler<T> handler,
            Predicate<T> matches,
            int amount,
            TransactionContext transaction
    ) {
        if (handler == null || matches == null || transaction == null) {
            throw new IllegalArgumentException("handler/matches/transaction required");
        }
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        int extracted = 0;
        for (int index = 0; index < handler.size() && extracted < amount; index++) {
            T resource = handler.getResource(index);
            if (resource == null || resource.isEmpty() || !matches.test(resource)) continue;
            extracted += handler.extract(index, resource, amount - extracted, transaction);
        }
        return extracted;
    }
}
