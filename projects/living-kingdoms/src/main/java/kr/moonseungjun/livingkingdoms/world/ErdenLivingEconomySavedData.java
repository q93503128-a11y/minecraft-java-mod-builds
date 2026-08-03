package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent shop schedules, household errands and purchase failures for Erden's living economy. */
public final class ErdenLivingEconomySavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record HouseholdMarketState(
            String householdId,
            long day,
            String status,
            String shopId,
            long price,
            int attemptTick,
            int consecutiveFailures,
            boolean fallbackShop) {
        private static final Codec<HouseholdMarketState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("household_id").forGetter(HouseholdMarketState::householdId),
                Codec.LONG.fieldOf("day").forGetter(HouseholdMarketState::day),
                Codec.STRING.fieldOf("status").forGetter(HouseholdMarketState::status),
                Codec.STRING.optionalFieldOf("shop_id", "").forGetter(HouseholdMarketState::shopId),
                Codec.LONG.optionalFieldOf("price", 0L).forGetter(HouseholdMarketState::price),
                Codec.INT.optionalFieldOf("attempt_tick", 6_000).forGetter(HouseholdMarketState::attemptTick),
                Codec.INT.optionalFieldOf("consecutive_failures", 0).forGetter(HouseholdMarketState::consecutiveFailures),
                Codec.BOOL.optionalFieldOf("fallback_shop", false).forGetter(HouseholdMarketState::fallbackShop)
        ).apply(instance, HouseholdMarketState::new));

        public HouseholdMarketState {
            status = status == null || status.isBlank() ? "unknown" : status;
            shopId = shopId == null ? "" : shopId;
            price = Math.max(0L, price);
            attemptTick = Math.clamp(attemptTick, 0, 23_999);
            consecutiveFailures = Math.max(0, consecutiveFailures);
        }

        public boolean success() {
            return status.equals(ErdenLivingEconomyManager.STATUS_SUCCESS);
        }
    }

    private static final Codec<ErdenLivingEconomySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("living_economy_revision", 0)
                    .forGetter(data -> data.livingEconomyRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L)
                    .forGetter(data -> data.lastProcessedDay),
            HouseholdMarketState.CODEC.listOf().optionalFieldOf("household_market", List.of())
                    .forGetter(data -> List.copyOf(data.householdMarket)),
            Codec.LONG.optionalFieldOf("total_successes", 0L)
                    .forGetter(data -> data.totalSuccesses),
            Codec.LONG.optionalFieldOf("total_failures", 0L)
                    .forGetter(data -> data.totalFailures),
            Codec.LONG.optionalFieldOf("closed_failures", 0L)
                    .forGetter(data -> data.closedFailures),
            Codec.LONG.optionalFieldOf("stockout_failures", 0L)
                    .forGetter(data -> data.stockoutFailures),
            Codec.LONG.optionalFieldOf("unaffordable_failures", 0L)
                    .forGetter(data -> data.unaffordableFailures),
            Codec.LONG.optionalFieldOf("total_spent", 0L)
                    .forGetter(data -> data.totalSpent)
    ).apply(instance, ErdenLivingEconomySavedData::new));

    public static final SavedDataType<ErdenLivingEconomySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_living_economy"),
            level -> new ErdenLivingEconomySavedData(),
            level -> CODEC
    );

    private int livingEconomyRevision;
    private long lastProcessedDay;
    private final List<HouseholdMarketState> householdMarket;
    private long totalSuccesses;
    private long totalFailures;
    private long closedFailures;
    private long stockoutFailures;
    private long unaffordableFailures;
    private long totalSpent;

    public ErdenLivingEconomySavedData() {
        this(0, -1L, List.of(), 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private ErdenLivingEconomySavedData(
            int livingEconomyRevision,
            long lastProcessedDay,
            List<HouseholdMarketState> householdMarket,
            long totalSuccesses,
            long totalFailures,
            long closedFailures,
            long stockoutFailures,
            long unaffordableFailures,
            long totalSpent) {
        this.livingEconomyRevision = Math.max(0, livingEconomyRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.householdMarket = new ArrayList<>(householdMarket);
        this.totalSuccesses = Math.max(0L, totalSuccesses);
        this.totalFailures = Math.max(0L, totalFailures);
        this.closedFailures = Math.max(0L, closedFailures);
        this.stockoutFailures = Math.max(0L, stockoutFailures);
        this.unaffordableFailures = Math.max(0L, unaffordableFailures);
        this.totalSpent = Math.max(0L, totalSpent);
    }

    public void applyDay(
            int revision,
            long day,
            List<HouseholdMarketState> states,
            long successes,
            long failures,
            long closed,
            long stockout,
            long unaffordable,
            long spent) {
        if (day <= lastProcessedDay) return;
        livingEconomyRevision = Math.max(0, revision);
        lastProcessedDay = day;
        householdMarket.clear();
        householdMarket.addAll(states);
        totalSuccesses += Math.max(0L, successes);
        totalFailures += Math.max(0L, failures);
        closedFailures += Math.max(0L, closed);
        stockoutFailures += Math.max(0L, stockout);
        unaffordableFailures += Math.max(0L, unaffordable);
        totalSpent += Math.max(0L, spent);
        setDirty();
    }

    public boolean hasCurrentDay(int revision, long day, int expectedHouseholds) {
        return livingEconomyRevision == revision
                && lastProcessedDay == day
                && householdMarket.size() == expectedHouseholds;
    }

    public HouseholdMarketState outcome(String householdId) {
        for (HouseholdMarketState state : householdMarket) {
            if (state.householdId().equals(householdId)) return state;
        }
        return null;
    }

    public List<HouseholdMarketState> outcomes() {
        return List.copyOf(householdMarket);
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public long totalSuccesses() {
        return totalSuccesses;
    }

    public long totalFailures() {
        return totalFailures;
    }

    public long closedFailures() {
        return closedFailures;
    }

    public long stockoutFailures() {
        return stockoutFailures;
    }

    public long unaffordableFailures() {
        return unaffordableFailures;
    }

    public long totalSpent() {
        return totalSpent;
    }
}
