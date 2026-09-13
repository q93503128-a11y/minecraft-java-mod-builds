package kr.moonseungjun.turnboundre.progression;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M6EquipmentInventoryTransactionTest {
    private static final TestResource IRON = new TestResource("iron");
    private static final TestResource COPPER = new TestResource("copper");
    private static final TestResource GOLD = new TestResource("gold");

    @Test
    void uncommittedExtractionRollsBackAndCommittedExtractionConsumesExactMaterial() {
        TestHandler inventory = new TestHandler(
                new TestResource[] { IRON, COPPER, IRON, TestResource.EMPTY },
                new int[] { 4, 11, 5, 0 });

        assertEquals(9, ExactResourceTransaction.count(inventory, IRON::equals));
        assertEquals(11, ExactResourceTransaction.count(inventory, COPPER::equals));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(6, ExactResourceTransaction.extract(inventory, IRON::equals, 6, transaction));
            assertEquals(3, ExactResourceTransaction.count(inventory, IRON::equals));
        }
        assertEquals(9, ExactResourceTransaction.count(inventory, IRON::equals));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(6, ExactResourceTransaction.extract(inventory, IRON::equals, 6, transaction));
            transaction.commit();
        }
        assertEquals(3, ExactResourceTransaction.count(inventory, IRON::equals));
        assertEquals(11, ExactResourceTransaction.count(inventory, COPPER::equals));
    }

    @Test
    void partialExtractionAlsoRollsBackWhenExactCostCannotBeMet() {
        TestHandler inventory = new TestHandler(
                new TestResource[] { GOLD, TestResource.EMPTY },
                new int[] { 3, 0 });

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(3, ExactResourceTransaction.extract(inventory, GOLD::equals, 4, transaction));
            assertEquals(0, ExactResourceTransaction.count(inventory, GOLD::equals));
        }
        assertEquals(3, ExactResourceTransaction.count(inventory, GOLD::equals));
    }

    private record TestResource(String id) implements Resource {
        private static final TestResource EMPTY = new TestResource("");
        @Override public boolean isEmpty() { return id == null || id.isBlank(); }
    }

    /** Small transaction-aware handler used only to verify our exact multi-slot extraction algorithm. */
    private static final class TestHandler extends SnapshotJournal<int[]> implements ResourceHandler<TestResource> {
        private final TestResource[] resources;
        private int[] amounts;

        private TestHandler(TestResource[] resources, int[] amounts) {
            if (resources.length != amounts.length) throw new IllegalArgumentException("resource/amount length mismatch");
            this.resources = resources.clone();
            this.amounts = amounts.clone();
        }

        @Override public int size() { return resources.length; }
        @Override public TestResource getResource(int index) { return amounts[index] == 0 ? TestResource.EMPTY : resources[index]; }
        @Override public long getAmountAsLong(int index) { return amounts[index]; }
        @Override public long getCapacityAsLong(int index, TestResource resource) { return Integer.MAX_VALUE; }
        @Override public boolean isValid(int index, TestResource resource) { return resource != null && !resource.isEmpty(); }
        @Override public int insert(int index, TestResource resource, int amount, TransactionContext transaction) { return 0; }

        @Override
        public int extract(int index, TestResource resource, int amount, TransactionContext transaction) {
            if (amount < 0 || resource == null || resource.isEmpty()) throw new IllegalArgumentException("invalid extraction");
            if (!resource.equals(getResource(index))) return 0;
            int extracted = Math.min(amounts[index], amount);
            if (extracted == 0) return 0;
            updateSnapshots(transaction);
            amounts[index] -= extracted;
            return extracted;
        }

        @Override protected int[] createSnapshot() { return amounts.clone(); }
        @Override protected void revertToSnapshot(int[] snapshot) { amounts = Arrays.copyOf(snapshot, snapshot.length); }
    }
}
