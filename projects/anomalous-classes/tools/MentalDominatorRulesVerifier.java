import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.moonseungjun.anomalousclasses.state.ClassFocus;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeCatalog;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorRules;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorSavedData;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import java.lang.reflect.Method;

/** Server-independent fixture for the configured candidate math and purchased passive mapping. */
public final class MentalDominatorRulesVerifier {
    private MentalDominatorRulesVerifier() {}

    public static void main(String[] args) {
        PlayerClassProfile empty = new PlayerClassProfile();
        check(close(MentalDominatorRules.maxPsyche(1, empty), 100.0), "level-1 maximum psyche");
        check(close(MentalDominatorRules.maxPsyche(301, empty), 1_600.0), "level-301 maximum psyche");
        check(close(MentalDominatorRules.regenerationPerSecond(1), 2.5), "level-1 regeneration");
        check(close(MentalDominatorRules.regenerationPerSecond(301), 20.5), "level-301 regeneration");
        check(MentalDominatorRules.maxControlled(1, empty) == 3, "base control slots");
        check(MentalDominatorRules.maxControlled(100, empty) == 6, "level-based slot growth");
        check(MentalDominatorRules.maxControlled(Integer.MAX_VALUE, empty) > 6, "unbounded-level slot growth");
        check(MentalDominatorRules.shouldTakeFollowMovement(MentalDominatorSavedData.CommandMode.FOLLOW,
                true, 49.0), "follow in the same dimension");
        check(!MentalDominatorRules.shouldTakeFollowMovement(MentalDominatorSavedData.CommandMode.FOLLOW,
                false, 10_000.0), "no cross-dimension movement takeover");
        check(MentalDominatorRules.shouldTakeFollowMovement(MentalDominatorSavedData.CommandMode.STAY,
                false, 10_000.0), "stay command holds movement selector without teleporting");

        PlayerClassProfile focus = new PlayerClassProfile();
        focus.equipFocus(new ClassFocus(ClassId.MENTAL_DOMINATOR, FocusTier.INCOMPREHENSIBLE));
        check(close(MentalDominatorRules.maxPsyche(1, focus), 200.0), "Incomprehensible focus psyche bonus");
        check(close(MentalDominatorRules.focusUpkeepReduction(focus), 0.40), "Incomprehensible focus upkeep bonus");
        check(close(MentalDominatorRules.focusIntrusionBonus(focus), 0.28), "Incomprehensible focus intrusion bonus");
        focus.equipFocus(new ClassFocus(ClassId.PUPPETEER, FocusTier.INCOMPREHENSIBLE));
        check(close(MentalDominatorRules.focusUpkeepReduction(focus), 0.0), "other-class focus isolation");

        Set<String> bonds = majorNodes("multiple_bonds");
        PlayerClassProfile invested = profileWithNodes(bonds);
        check(MentalDominatorRules.passiveRanks(invested, "multiple_bonds") == 30, "purchased rank mapping");
        check(MentalDominatorRules.maxControlled(1, invested) == 9, "multiple-bond slot milestones");
        Set<String> attunement = majorNodes("distributed_attunement");
        PlayerClassProfile distributed = profileWithNodes(attunement);
        check(close(MentalDominatorRules.groupMaintenancePerSecond(3, distributed), 0.14),
                "distributed-attunement upkeep reduction");
        check(close(MentalDominatorRules.maintenancePerSecond(1.0, 301, empty), 0.0),
                "weak creature upkeep reaches zero at high level");
        check(MentalDominatorRules.maintenancePerSecond(100.0, 1, empty) > 0.0,
                "strong creature incurs upkeep");
        verifySavedStateRoundTrip();
        System.out.println("PASS: psyche growth, configurable slots, focus isolation, passive-rank mapping, upkeep floors and reductions");
    }

    private static void verifySavedStateRoundTrip() {
        try {
            UUID owner = UUID.randomUUID();
            UUID controlledId = UUID.randomUUID();
            UUID releasedId = UUID.randomUUID();
            MentalDominatorSavedData data = new MentalDominatorSavedData();
            data.player(owner).setPsyche(37.5);
            data.player(owner).setInitialized(true);
            data.put(new MentalDominatorSavedData.ControlledRecord(controlledId, owner, "minecraft:zombie", "",
                    "minecraft:overworld", 1, 64, 2, 13, 20, 12.8,
                    MentalDominatorSavedData.CommandMode.STAY, false, 1234,
                    MentalDominatorSavedData.LinkState.ACTIVE));
            data.put(new MentalDominatorSavedData.ControlledRecord(releasedId, owner, "minecraft:cow", "",
                    "minecraft:overworld", 3, 64, 4, 10, 10, 8.0,
                    MentalDominatorSavedData.CommandMode.FOLLOW, false, 1235,
                    MentalDominatorSavedData.LinkState.RELEASE_PENDING));
            data.markReleased(releasedId);
            CompoundTag encoded = data.save(new CompoundTag(), RegistryAccess.EMPTY);
            Method loader = MentalDominatorSavedData.class.getDeclaredMethod("load", CompoundTag.class,
                    net.minecraft.core.HolderLookup.Provider.class);
            loader.setAccessible(true);
            MentalDominatorSavedData restored = (MentalDominatorSavedData) loader.invoke(null, encoded, RegistryAccess.EMPTY);
            check(restored.controlled(controlledId) != null
                    && restored.controlled(controlledId).command() == MentalDominatorSavedData.CommandMode.STAY,
                    "controlled entity index persists");
            check(restored.controlled(releasedId) != null
                    && restored.controlled(releasedId).linkState() == MentalDominatorSavedData.LinkState.RELEASE_PENDING,
                    "release pending survives restart until original entity reload");
            check(close(restored.player(owner).psyche(), 37.5), "psyche persists");
            check(restored.wasReleased(releasedId), "release tombstone persists");
            restored.clearReleased(releasedId);
            check(!restored.wasReleased(releasedId), "release tombstone can be cleared after reload");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("SavedData round-trip fixture failed", exception);
        }
    }

    private static Set<String> majorNodes(String skillId) {
        var skill = GraphSkillTreeCatalog.tree(ClassId.MENTAL_DOMINATOR).skills().get(skillId);
        check(skill != null, "skill exists: " + skillId);
        return skill.segments().stream().map(GraphSkillTreeCatalog.Segment::majorNodeId).collect(Collectors.toSet());
    }

    private static PlayerClassProfile profileWithNodes(Set<String> nodes) {
        PlayerClassProfile profile = new PlayerClassProfile();
        ClassProgress fresh = ClassProgress.fresh();
        profile.updateProgress(ClassId.MENTAL_DOMINATOR,
                fresh.withGraphTree(0, nodes, GraphSkillTreeCatalog.CURRENT_VERSION));
        return profile;
    }

    private static boolean close(double left, double right) { return Math.abs(left - right) < 1.0e-6; }

    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Failed: " + label);
    }
}
