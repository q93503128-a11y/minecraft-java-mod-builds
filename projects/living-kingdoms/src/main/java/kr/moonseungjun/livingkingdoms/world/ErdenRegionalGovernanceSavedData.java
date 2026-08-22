package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent municipal budgets, public contracts and guard-post state for the six regional villages. */
public final class ErdenRegionalGovernanceSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record CouncilState(
            String settlementId,
            long treasuryMarks,
            long lastProcessedDay,
            long lastProductionSeen,
            long totalTaxCollected,
            long totalPublicSpent,
            long totalGuardPayroll,
            String activeContract,
            long contractStartedDay,
            long contractDueDay,
            long completedContracts,
            int safetyScore,
            long totalIncidents,
            long lastIncidentTick) {
        private static final Codec<CouncilState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("settlement_id").forGetter(CouncilState::settlementId),
                Codec.LONG.optionalFieldOf("treasury_marks", 0L).forGetter(CouncilState::treasuryMarks),
                Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(CouncilState::lastProcessedDay),
                Codec.LONG.optionalFieldOf("last_production_seen", 0L).forGetter(CouncilState::lastProductionSeen),
                Codec.LONG.optionalFieldOf("total_tax_collected", 0L).forGetter(CouncilState::totalTaxCollected),
                Codec.LONG.optionalFieldOf("total_public_spent", 0L).forGetter(CouncilState::totalPublicSpent),
                Codec.LONG.optionalFieldOf("total_guard_payroll", 0L).forGetter(CouncilState::totalGuardPayroll),
                Codec.STRING.optionalFieldOf("active_contract", "").forGetter(CouncilState::activeContract),
                Codec.LONG.optionalFieldOf("contract_started_day", -1L).forGetter(CouncilState::contractStartedDay),
                Codec.LONG.optionalFieldOf("contract_due_day", -1L).forGetter(CouncilState::contractDueDay),
                Codec.LONG.optionalFieldOf("completed_contracts", 0L).forGetter(CouncilState::completedContracts),
                Codec.INT.optionalFieldOf("safety_score", 50).forGetter(CouncilState::safetyScore),
                Codec.LONG.optionalFieldOf("total_incidents", 0L).forGetter(CouncilState::totalIncidents),
                Codec.LONG.optionalFieldOf("last_incident_tick", -1L).forGetter(CouncilState::lastIncidentTick)
        ).apply(instance, CouncilState::new));

        public CouncilState {
            settlementId = settlementId == null ? "" : settlementId;
            treasuryMarks = Math.max(0L, treasuryMarks);
            lastProductionSeen = Math.max(0L, lastProductionSeen);
            totalTaxCollected = Math.max(0L, totalTaxCollected);
            totalPublicSpent = Math.max(0L, totalPublicSpent);
            totalGuardPayroll = Math.max(0L, totalGuardPayroll);
            activeContract = activeContract == null ? "" : activeContract;
            completedContracts = Math.max(0L, completedContracts);
            safetyScore = Math.max(0, Math.min(100, safetyScore));
            totalIncidents = Math.max(0L, totalIncidents);
        }

        public CouncilState processDay(
                long day,
                long productionSeen,
                long tax,
                long payroll,
                long contractCost,
                String contract,
                long contractStart,
                long contractDue,
                boolean completed,
                int safety) {
            long income = Math.max(0L, tax);
            long wages = Math.max(0L, payroll);
            long project = Math.max(0L, contractCost);
            long spent = wages + project;
            long treasury = Math.max(0L, treasuryMarks + income - spent);
            return new CouncilState(
                    settlementId, treasury, day, Math.max(lastProductionSeen, productionSeen),
                    totalTaxCollected + income, totalPublicSpent + project,
                    totalGuardPayroll + wages, contract, contractStart, contractDue,
                    completedContracts + (completed ? 1L : 0L), safety,
                    totalIncidents, lastIncidentTick);
        }

        public CouncilState recordIncident(long tick, int safetyPenalty) {
            return new CouncilState(
                    settlementId, treasuryMarks, lastProcessedDay, lastProductionSeen,
                    totalTaxCollected, totalPublicSpent, totalGuardPayroll,
                    activeContract, contractStartedDay, contractDueDay, completedContracts,
                    Math.max(0, safetyScore - Math.max(0, safetyPenalty)),
                    totalIncidents + 1L, Math.max(lastIncidentTick, tick));
        }
    }

    public record GuardPost(
            String settlementId,
            int slot,
            int generation,
            boolean alive,
            long replacementDueDay,
            long totalPatrols) {
        private static final Codec<GuardPost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("settlement_id").forGetter(GuardPost::settlementId),
                Codec.INT.fieldOf("slot").forGetter(GuardPost::slot),
                Codec.INT.optionalFieldOf("generation", 1).forGetter(GuardPost::generation),
                Codec.BOOL.optionalFieldOf("alive", true).forGetter(GuardPost::alive),
                Codec.LONG.optionalFieldOf("replacement_due_day", -1L).forGetter(GuardPost::replacementDueDay),
                Codec.LONG.optionalFieldOf("total_patrols", 0L).forGetter(GuardPost::totalPatrols)
        ).apply(instance, GuardPost::new));

        public GuardPost {
            settlementId = settlementId == null ? "" : settlementId;
            slot = Math.max(0, slot);
            generation = Math.max(1, generation);
            totalPatrols = Math.max(0L, totalPatrols);
        }

        public String id() {
            return settlementId + "_guard_" + slot + "_g" + generation;
        }

        public GuardPost killed(long replacementDay) {
            return new GuardPost(settlementId, slot, generation, false,
                    Math.max(0L, replacementDay), totalPatrols);
        }

        public GuardPost replaced() {
            return new GuardPost(settlementId, slot, generation + 1, true, -1L, totalPatrols);
        }

        public GuardPost patrolCompleted() {
            return new GuardPost(settlementId, slot, generation, alive,
                    replacementDueDay, totalPatrols + 1L);
        }
    }

    private static final Codec<ErdenRegionalGovernanceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("governance_revision", 0).forGetter(data -> data.governanceRevision),
            CouncilState.CODEC.listOf().optionalFieldOf("councils", List.of())
                    .forGetter(data -> List.copyOf(data.councils)),
            GuardPost.CODEC.listOf().optionalFieldOf("guard_posts", List.of())
                    .forGetter(data -> List.copyOf(data.guardPosts)),
            Codec.STRING.listOf().optionalFieldOf("materialized_ledgers", List.of())
                    .forGetter(data -> List.copyOf(data.materializedLedgers))
    ).apply(instance, ErdenRegionalGovernanceSavedData::new));

    public static final SavedDataType<ErdenRegionalGovernanceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_regional_governance"),
            level -> new ErdenRegionalGovernanceSavedData(),
            level -> CODEC
    );

    private int governanceRevision;
    private final List<CouncilState> councils;
    private final List<GuardPost> guardPosts;
    private final List<String> materializedLedgers;

    public ErdenRegionalGovernanceSavedData() {
        this(0, List.of(), List.of(), List.of());
    }

    private ErdenRegionalGovernanceSavedData(
            int governanceRevision,
            List<CouncilState> councils,
            List<GuardPost> guardPosts,
            List<String> materializedLedgers) {
        this.governanceRevision = Math.max(0, governanceRevision);
        this.councils = new ArrayList<>(councils);
        this.guardPosts = new ArrayList<>(guardPosts);
        this.materializedLedgers = new ArrayList<>(materializedLedgers);
    }

    public boolean hasGovernance(int revision, int expectedCouncils, int expectedGuardPosts) {
        return governanceRevision == revision
                && councils.size() == expectedCouncils
                && guardPosts.size() == expectedGuardPosts;
    }

    public void initialize(int revision, List<CouncilState> replacementCouncils, List<GuardPost> replacementGuards) {
        governanceRevision = revision;
        councils.clear();
        councils.addAll(replacementCouncils);
        guardPosts.clear();
        guardPosts.addAll(replacementGuards);
        materializedLedgers.clear();
        setDirty();
    }

    public List<CouncilState> councils() {
        return List.copyOf(councils);
    }

    public List<GuardPost> guardPosts() {
        return List.copyOf(guardPosts);
    }

    public CouncilState council(String settlementId) {
        for (CouncilState council : councils) {
            if (council.settlementId().equals(settlementId)) return council;
        }
        return null;
    }

    public GuardPost guard(String settlementId, int slot) {
        for (GuardPost guard : guardPosts) {
            if (guard.settlementId().equals(settlementId) && guard.slot() == slot) return guard;
        }
        return null;
    }

    public void replaceCouncil(CouncilState replacement) {
        for (int index = 0; index < councils.size(); index++) {
            if (!councils.get(index).settlementId().equals(replacement.settlementId())) continue;
            if (!councils.get(index).equals(replacement)) {
                councils.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void replaceGuard(GuardPost replacement) {
        for (int index = 0; index < guardPosts.size(); index++) {
            GuardPost current = guardPosts.get(index);
            if (!current.settlementId().equals(replacement.settlementId()) || current.slot() != replacement.slot()) continue;
            if (!current.equals(replacement)) {
                guardPosts.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public boolean ledgerMaterialized(String settlementId) {
        return materializedLedgers.contains(settlementId);
    }

    public void markLedgerMaterialized(String settlementId) {
        if (!materializedLedgers.contains(settlementId)) {
            materializedLedgers.add(settlementId);
            setDirty();
        }
    }

    public int aliveGuardCount(String settlementId) {
        int count = 0;
        for (GuardPost guard : guardPosts) {
            if (guard.settlementId().equals(settlementId) && guard.alive()) count++;
        }
        return count;
    }

    public int aliveGuardCount() {
        int count = 0;
        for (GuardPost guard : guardPosts) if (guard.alive()) count++;
        return count;
    }

    public long totalTaxCollected() {
        return councils.stream().mapToLong(CouncilState::totalTaxCollected).sum();
    }

    public long totalPublicSpent() {
        return councils.stream().mapToLong(CouncilState::totalPublicSpent).sum();
    }

    public long totalGuardPayroll() {
        return councils.stream().mapToLong(CouncilState::totalGuardPayroll).sum();
    }

    public long completedContractCount() {
        return councils.stream().mapToLong(CouncilState::completedContracts).sum();
    }

    public long totalIncidents() {
        return councils.stream().mapToLong(CouncilState::totalIncidents).sum();
    }
}
