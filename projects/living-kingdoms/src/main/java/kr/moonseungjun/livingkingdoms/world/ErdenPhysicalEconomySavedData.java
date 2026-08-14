package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent worksite inventories, household wallets and delivery totals for Erden. */
public final class ErdenPhysicalEconomySavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record StockEntry(String resource, long amount) {
        private static final Codec<StockEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("resource").forGetter(StockEntry::resource),
                Codec.LONG.fieldOf("amount").forGetter(StockEntry::amount)
        ).apply(instance, StockEntry::new));

        public StockEntry {
            amount = Math.max(0L, amount);
        }
    }

    public record MetricEntry(String metric, long value) {
        private static final Codec<MetricEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("metric").forGetter(MetricEntry::metric),
                Codec.LONG.fieldOf("value").forGetter(MetricEntry::value)
        ).apply(instance, MetricEntry::new));
    }

    public record SiteState(
            String id,
            int x,
            int z,
            String role,
            List<StockEntry> stocks,
            List<MetricEntry> metrics,
            boolean materialized) {
        private static final Codec<SiteState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(SiteState::id),
                Codec.INT.fieldOf("x").forGetter(SiteState::x),
                Codec.INT.fieldOf("z").forGetter(SiteState::z),
                Codec.STRING.fieldOf("role").forGetter(SiteState::role),
                StockEntry.CODEC.listOf().optionalFieldOf("stocks", List.of()).forGetter(SiteState::stocks),
                MetricEntry.CODEC.listOf().optionalFieldOf("metrics", List.of()).forGetter(SiteState::metrics),
                Codec.BOOL.optionalFieldOf("materialized", false).forGetter(SiteState::materialized)
        ).apply(instance, SiteState::new));

        public SiteState {
            stocks = List.copyOf(stocks);
            metrics = List.copyOf(metrics);
        }

        public long stock(String resource) {
            for (StockEntry entry : stocks) {
                if (entry.resource().equals(resource)) return entry.amount();
            }
            return 0L;
        }

        public long metric(String name) {
            for (MetricEntry entry : metrics) {
                if (entry.metric().equals(name)) return entry.value();
            }
            return 0L;
        }

        public SiteState withStock(String resource, long amount) {
            Map<String, Long> values = new LinkedHashMap<>();
            for (StockEntry entry : stocks) values.put(entry.resource(), entry.amount());
            if (amount <= 0L) values.remove(resource);
            else values.put(resource, amount);
            List<StockEntry> replacement = values.entrySet().stream()
                    .map(entry -> new StockEntry(entry.getKey(), entry.getValue()))
                    .toList();
            return new SiteState(id, x, z, role, replacement, metrics, materialized);
        }

        public SiteState addStock(String resource, long delta) {
            return withStock(resource, Math.max(0L, stock(resource) + delta));
        }

        public SiteState withMetric(String name, long value) {
            Map<String, Long> values = new LinkedHashMap<>();
            for (MetricEntry entry : metrics) values.put(entry.metric(), entry.value());
            if (value == 0L) values.remove(name);
            else values.put(name, value);
            List<MetricEntry> replacement = values.entrySet().stream()
                    .map(entry -> new MetricEntry(entry.getKey(), entry.getValue()))
                    .toList();
            return new SiteState(id, x, z, role, stocks, replacement, materialized);
        }

        public SiteState addMetric(String name, long delta) {
            return withMetric(name, metric(name) + delta);
        }

        public SiteState withMaterialized(boolean value) {
            if (materialized == value) return this;
            return new SiteState(id, x, z, role, stocks, metrics, value);
        }
    }

    public record WalletState(String householdId, long coins, long earned, long spent) {
        private static final Codec<WalletState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("household_id").forGetter(WalletState::householdId),
                Codec.LONG.fieldOf("coins").forGetter(WalletState::coins),
                Codec.LONG.fieldOf("earned").forGetter(WalletState::earned),
                Codec.LONG.fieldOf("spent").forGetter(WalletState::spent)
        ).apply(instance, WalletState::new));

        public WalletState {
            coins = Math.max(0L, coins);
            earned = Math.max(0L, earned);
            spent = Math.max(0L, spent);
        }

        public WalletState earn(long amount) {
            long safe = Math.max(0L, amount);
            return new WalletState(householdId, coins + safe, earned + safe, spent);
        }

        public WalletState spend(long amount) {
            long safe = Math.min(coins, Math.max(0L, amount));
            return new WalletState(householdId, coins - safe, earned, spent + safe);
        }
    }

    private static final Codec<ErdenPhysicalEconomySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("economy_revision", 0).forGetter(data -> data.economyRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(data -> data.lastProcessedDay),
            SiteState.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(data -> List.copyOf(data.sites)),
            WalletState.CODEC.listOf().optionalFieldOf("wallets", List.of()).forGetter(data -> List.copyOf(data.wallets)),
            Codec.LONG.optionalFieldOf("total_deliveries", 0L).forGetter(data -> data.totalDeliveries),
            Codec.LONG.optionalFieldOf("total_crafted", 0L).forGetter(data -> data.totalCrafted),
            Codec.LONG.optionalFieldOf("total_sales", 0L).forGetter(data -> data.totalSales),
            Codec.LONG.optionalFieldOf("total_wages", 0L).forGetter(data -> data.totalWages)
    ).apply(instance, ErdenPhysicalEconomySavedData::new));

    public static final SavedDataType<ErdenPhysicalEconomySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_physical_economy"),
            level -> new ErdenPhysicalEconomySavedData(),
            level -> CODEC
    );

    private int economyRevision;
    private long lastProcessedDay;
    private final List<SiteState> sites;
    private final List<WalletState> wallets;
    private long totalDeliveries;
    private long totalCrafted;
    private long totalSales;
    private long totalWages;

    public ErdenPhysicalEconomySavedData() {
        this(0, -1L, List.of(), List.of(), 0L, 0L, 0L, 0L);
    }

    private ErdenPhysicalEconomySavedData(
            int economyRevision,
            long lastProcessedDay,
            List<SiteState> sites,
            List<WalletState> wallets,
            long totalDeliveries,
            long totalCrafted,
            long totalSales,
            long totalWages) {
        this.economyRevision = Math.max(0, economyRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.sites = new ArrayList<>(sites);
        this.wallets = new ArrayList<>(wallets);
        this.totalDeliveries = Math.max(0L, totalDeliveries);
        this.totalCrafted = Math.max(0L, totalCrafted);
        this.totalSales = Math.max(0L, totalSales);
        this.totalWages = Math.max(0L, totalWages);
    }

    public boolean hasEconomy(int revision, int expectedSites, int expectedWallets) {
        return economyRevision == revision
                && sites.size() == expectedSites
                && wallets.size() == expectedWallets;
    }

    public List<SiteState> sites() {
        return List.copyOf(sites);
    }

    public List<WalletState> wallets() {
        return List.copyOf(wallets);
    }

    public WalletState wallet(String householdId) {
        if (householdId == null || householdId.isBlank()) return null;
        for (WalletState wallet : wallets) {
            if (wallet.householdId().equals(householdId)) return wallet;
        }
        return null;
    }

    public boolean replaceWallet(WalletState replacement) {
        if (replacement == null || replacement.householdId().isBlank()) return false;
        for (int index = 0; index < wallets.size(); index++) {
            WalletState current = wallets.get(index);
            if (!current.householdId().equals(replacement.householdId())) continue;
            if (!current.equals(replacement)) {
                wallets.set(index, replacement);
                setDirty();
            }
            return true;
        }
        return false;
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public long totalDeliveries() {
        return totalDeliveries;
    }

    public long totalCrafted() {
        return totalCrafted;
    }

    public long totalSales() {
        return totalSales;
    }

    public long totalWages() {
        return totalWages;
    }

    public void replaceEconomy(int revision, List<SiteState> replacementSites, List<WalletState> replacementWallets) {
        economyRevision = revision;
        lastProcessedDay = -1L;
        sites.clear();
        sites.addAll(replacementSites);
        wallets.clear();
        wallets.addAll(replacementWallets);
        totalDeliveries = 0L;
        totalCrafted = 0L;
        totalSales = 0L;
        totalWages = 0L;
        setDirty();
    }

    public void replaceSite(SiteState replacement) {
        for (int index = 0; index < sites.size(); index++) {
            if (!sites.get(index).id().equals(replacement.id())) continue;
            if (!sites.get(index).equals(replacement)) {
                sites.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void applyDay(
            long day,
            List<SiteState> replacementSites,
            List<WalletState> replacementWallets,
            long deliveries,
            long crafted,
            long sales,
            long wages) {
        if (day <= lastProcessedDay) return;
        sites.clear();
        sites.addAll(replacementSites);
        wallets.clear();
        wallets.addAll(replacementWallets);
        lastProcessedDay = day;
        totalDeliveries += Math.max(0L, deliveries);
        totalCrafted += Math.max(0L, crafted);
        totalSales += Math.max(0L, sales);
        totalWages += Math.max(0L, wages);
        setDirty();
    }

    public long totalWalletCoins() {
        long total = 0L;
        for (WalletState wallet : wallets) total += wallet.coins();
        return total;
    }

    public int materializedSiteCount() {
        int count = 0;
        for (SiteState site : sites) {
            if (site.materialized()) count++;
        }
        return count;
    }
}
