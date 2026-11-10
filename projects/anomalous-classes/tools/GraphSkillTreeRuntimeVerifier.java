import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.ClassProfileSavedData;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeCatalog;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeMigration;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import kr.moonseungjun.anomalousclasses.state.SkillTreeCatalog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

/** Runtime verification fixture; launched in source-file mode and excluded from the mod jar. */
public final class GraphSkillTreeRuntimeVerifier {
    private GraphSkillTreeRuntimeVerifier() {}

    public static void main(String[] args) throws Exception {
        verifyCatalog();
        verifyPurchaseReachability();
        verifyNewWorld();
        verifyMigrationBoundaries();
        verifyMultiClassAndReset();
        verifyAtomicFailure();
        verifyNbtPersistence();
        System.out.println("PASS: runtime graph catalog, legacy migration, SP conservation, atomic failure, reset, and NBT round trip");
    }

    private static void verifyPurchaseReachability() {
        for (ClassId classId : ClassId.values()) {
            var tree = GraphSkillTreeCatalog.tree(classId);
            Set<String> owned = new HashSet<>();
            int remaining = 900;
            while (owned.size() < tree.purchaseNodes().size()) {
                GraphSkillTreeCatalog.Node next = null;
                for (GraphSkillTreeCatalog.Node node : tree.purchaseNodes()) {
                    if (GraphSkillTreeCatalog.canPurchase(classId, owned, remaining, node.id())
                            == GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE
                            && (next == null || node.cost() < next.cost())) next = node;
                }
                require(next != null, classId + " path reaches all nodes with 900 SP");
                owned.add(next.id());
                remaining -= next.cost();
                require(GraphSkillTreeCatalog.canPurchase(classId, owned, remaining, next.id())
                        == GraphSkillTreeCatalog.PurchaseCheck.ALREADY_OWNED, classId + " duplicate purchase rejected");
            }
            require(remaining == 0, classId + " purchase costs sum exactly to the available SP");
            GraphSkillTreeCatalog.Node first = tree.purchaseNodes().stream()
                    .filter(node -> GraphSkillTreeCatalog.canPurchase(classId, Set.of(), 900, node.id())
                            == GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE).findFirst().orElseThrow();
            require(GraphSkillTreeCatalog.canPurchase(classId, Set.of(), first.cost() - 1, first.id())
                    == GraphSkillTreeCatalog.PurchaseCheck.INSUFFICIENT_SP, classId + " insufficient SP rejected");
        }
    }

    private static void verifyCatalog() {
        int allNodes = 0;
        for (ClassId classId : ClassId.values()) {
            GraphSkillTreeCatalog.Tree tree = GraphSkillTreeCatalog.tree(classId);
            require(tree.purchaseNodes().size() == expectedCount(classId), classId + " node count");
            require(tree.totalCost() == 900, classId + " total cost");
            allNodes += tree.purchaseNodes().size();
            for (GraphSkillTreeCatalog.Skill skill : tree.skills().values()) {
                for (GraphSkillTreeCatalog.Segment stage : skill.segments()) {
                    int migratedPrice = stage.majorCost() + stage.minorCosts().stream().mapToInt(Integer::intValue).sum();
                    require(migratedPrice == stage.oldSegmentSp(), classId + " stage cost conservation");
                }
            }
        }
        require(allNodes == 581, "total node count");
    }

    private static void verifyNewWorld() {
        PlayerClassProfile fresh = new PlayerClassProfile();
        int revision = fresh.revision();
        require(!GraphSkillTreeMigration.migrateAll(fresh), "fresh profile is already on graph schema");
        require(fresh.revision() == revision, "new world is not rewritten on login");
    }

    private static void verifyMigrationBoundaries() {
        int[] ranksToTest = {0, 9, 10, 11, 19, 20, 29, 30};
        for (ClassId classId : ClassId.values()) {
            var legacyNodes = SkillTreeCatalog.nodes(classId);
            for (int requestedRank : ranksToTest) {
                int firstMax = legacyNodes.get(0).maxRank();
                int rank = Math.min(requestedRank, firstMax);
                int[] ranks = new int[legacyNodes.size()];
                ranks[0] = rank;
                int startingPoints = 41;
                int oldSpend = SkillTreeCatalog.spent(classId, ranks);
                PlayerClassProfile profile = new PlayerClassProfile();
                profile.updateProgress(classId, oldProgress(startingPoints, ranks, 12, 9876L));
                require(GraphSkillTreeMigration.migrateAll(profile), classId + " rank " + rank + " migrates once");
                ClassProgress moved = profile.progress(classId);
                int newSpend = GraphSkillTreeCatalog.spent(classId, moved.purchasedNodes());
                require((long) moved.skillPoints() + newSpend == startingPoints + oldSpend,
                        classId + " rank " + rank + " SP conservation");
                require(moved.skillTreeVersion() == GraphSkillTreeCatalog.CURRENT_VERSION,
                        classId + " migration version");
                require(moved.level() == 12 && moved.experience() == 9876L, classId + " XP preservation");
                require(moved.skillRanks().length == 0, classId + " clears converted legacy array");
                boolean firstStageOwned = moved.purchasedNodes().contains(legacyNodes.get(0).id() + ".s1.major");
                require(firstStageOwned == (rank >= 10), classId + " stage-one boundary " + rank);
                int revisionAfter = profile.revision();
                require(!GraphSkillTreeMigration.migrateAll(profile), classId + " does not migrate twice");
                require(profile.revision() == revisionAfter, classId + " duplicate migration does not revise profile");
            }
        }

        for (ClassId classId : ClassId.values()) {
            var legacyNodes = SkillTreeCatalog.nodes(classId);
            int[] fullRanks = legacyNodes.stream().mapToInt(SkillTreeCatalog.Node::maxRank).toArray();
            PlayerClassProfile full = new PlayerClassProfile();
            full.updateProgress(classId, oldProgress(0, fullRanks, 301, 42_000L));
            GraphSkillTreeMigration.migrateAll(full);
            ClassProgress complete = full.progress(classId);
            require(GraphSkillTreeCatalog.spent(classId, complete.purchasedNodes()) == 900,
                    classId + " fully completed legacy skills map to all 900 SP");
            require(complete.purchasedNodes().size() == GraphSkillTreeCatalog.tree(classId).purchaseNodes().size(),
                    classId + " full legacy ranks map to every graph node");
        }
    }

    private static void verifyMultiClassAndReset() {
        PlayerClassProfile profile = new PlayerClassProfile();
        EnumMap<ClassId, Integer> before = new EnumMap<>(ClassId.class);
        for (ClassId classId : ClassId.values()) {
            var nodes = SkillTreeCatalog.nodes(classId);
            int[] ranks = new int[nodes.size()];
            ranks[0] = Math.min(5 + classId.ordinal(), nodes.get(0).maxRank());
            ranks[1] = 10;
            int points = 100 + classId.ordinal();
            before.put(classId, points + SkillTreeCatalog.spent(classId, ranks));
            profile.updateProgress(classId, oldProgress(points, ranks, 20 + classId.ordinal(), 5000L + classId.ordinal()));
        }
        int revision = profile.revision();
        require(GraphSkillTreeMigration.migrateAll(profile), "six class migration");
        require(profile.revision() == revision + 1, "all class migrations commit in one profile revision");
        for (ClassId classId : ClassId.values()) {
            ClassProgress progress = profile.progress(classId);
            int spent = GraphSkillTreeCatalog.spent(classId, progress.purchasedNodes());
            require(progress.skillPoints() + spent == before.get(classId), classId + " independent migration balance");
            require(progress.level() == 20 + classId.ordinal(), classId + " level is preserved");
            require(progress.permanentData().getInt("retained") == classId.ordinal() + 7,
                    classId + " non-SP permanent data is preserved");

            int refunded = progress.skillPoints() + spent;
            ClassProgress reset = progress.withGraphTree(refunded, Set.of(), GraphSkillTreeCatalog.CURRENT_VERSION);
            require(reset.skillPoints() == refunded && reset.purchasedNodes().isEmpty(), classId + " free reset refund");
            require(GraphSkillTreeCatalog.spent(classId, reset.purchasedNodes()) == 0, classId + " reset clears graph nodes");
        }
    }

    private static void verifyAtomicFailure() {
        PlayerClassProfile invalid = new PlayerClassProfile();
        for (ClassId classId : ClassId.values()) {
            int[] ranks = new int[SkillTreeCatalog.nodes(classId).size()];
            if (classId == ClassId.SWARM_CASTER) ranks = new int[] {1};
            invalid.updateProgress(classId, oldProgress(0, ranks, 1, 10L));
        }
        int originalRevision = invalid.revision();
        boolean failed = false;
        try {
            GraphSkillTreeMigration.migrateAll(invalid);
        } catch (IllegalStateException expected) {
            failed = true;
        }
        require(failed, "malformed old data is rejected safely");
        require(invalid.revision() == originalRevision, "failed conversion does not partially commit");
        for (ClassId classId : ClassId.values()) {
            require(invalid.progress(classId).skillTreeVersion() == 0, classId + " remains legacy after failed batch");
        }
    }

    private static void verifyNbtPersistence() throws Exception {
        ClassProfileSavedData data = new ClassProfileSavedData();
        UUID playerId = UUID.fromString("97e31e62-a0ad-4372-b799-954fd5689280");
        PlayerClassProfile profile = data.profile(playerId);
        ClassId classId = ClassId.PACT_CASTER;
        GraphSkillTreeCatalog.Tree tree = GraphSkillTreeCatalog.tree(classId);
        Set<String> nodes = new HashSet<>();
        nodes.add(tree.purchaseNodes().get(0).id());
        profile.updateProgress(classId, ClassProgress.fresh().withGraphTree(29, nodes, GraphSkillTreeCatalog.CURRENT_VERSION));
        profile.select(classId);
        CompoundTag encoded = data.save(new CompoundTag(), RegistryAccess.EMPTY);

        Method load = ClassProfileSavedData.class.getDeclaredMethod("load", CompoundTag.class,
                net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        ClassProfileSavedData decoded = (ClassProfileSavedData) load.invoke(null, encoded, RegistryAccess.EMPTY);
        ClassProgress restored = decoded.profile(playerId).progress(classId);
        require(restored.skillTreeVersion() == GraphSkillTreeCatalog.CURRENT_VERSION, "saved graph version round trip");
        require(restored.purchasedNodes().equals(nodes), "purchased node IDs round trip");
        require(restored.skillPoints() == 29, "remaining SP round trip");
        require(decoded.profile(playerId).currentClass().orElseThrow() == classId, "selected class round trip");
    }

    private static ClassProgress oldProgress(int points, int[] ranks, int level, long experience) {
        CompoundTag permanent = new CompoundTag();
        permanent.putInt("retained", Math.max(0, level - 13));
        return new ClassProgress(true, level, experience, points, permanent, ranks);
    }

    private static int expectedCount(ClassId id) {
        return switch (id) {
            case MENTAL_DOMINATOR -> 94;
            case PUPPETEER -> 101;
            case PARASITE_MORPHER -> 108;
            case BARRIER_CASTER -> 102;
            case PACT_CASTER -> 86;
            case SWARM_CASTER -> 90;
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Graph skill-tree runtime check failed: " + message);
    }
}
