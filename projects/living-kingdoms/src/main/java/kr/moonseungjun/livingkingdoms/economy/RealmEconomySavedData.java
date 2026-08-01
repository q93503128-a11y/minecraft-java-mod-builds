package kr.moonseungjun.livingkingdoms.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent civic economy. Currency is abstract silver, not a stackable vanilla barter item. */
public final class RealmEconomySavedData extends SavedData {
    private static final long STARTING_SILVER = 36L;

    private static final Codec<Account> ACCOUNT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("silver", STARTING_SILVER).forGetter(Account::silver),
            Codec.INT.optionalFieldOf("renown", 0).forGetter(Account::renown),
            Codec.STRING.optionalFieldOf("profession", "unregistered").forGetter(Account::profession),
            Codec.LONG.optionalFieldOf("last_contract_day", -1L).forGetter(Account::lastContractDay)
    ).apply(instance, Account::new));

    private static final Codec<MarketState> MARKET_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("day", 0L).forGetter(MarketState::day),
            Codec.INT.optionalFieldOf("grain", 100).forGetter(MarketState::grain),
            Codec.INT.optionalFieldOf("metal", 100).forGetter(MarketState::metal),
            Codec.INT.optionalFieldOf("herb", 100).forGetter(MarketState::herb),
            Codec.INT.optionalFieldOf("labor", 100).forGetter(MarketState::labor)
    ).apply(instance, MarketState::new));

    private static final Codec<RealmEconomySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ACCOUNT_CODEC)
                    .optionalFieldOf("accounts", Map.of())
                    .forGetter(data -> Map.copyOf(data.accounts)),
            MARKET_CODEC.optionalFieldOf("market", new MarketState(0L, 100, 100, 100, 100))
                    .forGetter(data -> data.market)
    ).apply(instance, RealmEconomySavedData::new));

    public static final SavedDataType<RealmEconomySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "realm_economy"),
            level -> new RealmEconomySavedData(),
            level -> CODEC
    );

    private final Map<String, Account> accounts;
    private MarketState market;

    public RealmEconomySavedData() {
        this(Map.of(), new MarketState(0L, 100, 100, 100, 100));
    }

    private RealmEconomySavedData(Map<String, Account> accounts, MarketState market) {
        this.accounts = new LinkedHashMap<>(accounts);
        this.market = market;
    }

    public Account account(UUID playerId) {
        String key = playerId.toString();
        Account account = accounts.get(key);
        if (account != null) return account;
        Account created = new Account(STARTING_SILVER, 0, "unregistered", -1L);
        accounts.put(key, created);
        setDirty();
        return created;
    }

    public Account credit(UUID playerId, long amount, int renown) {
        Account current = account(playerId);
        long safeAmount = Math.max(0L, amount);
        long silver = current.silver() > Long.MAX_VALUE - safeAmount
                ? Long.MAX_VALUE : current.silver() + safeAmount;
        Account updated = new Account(silver, current.renown() + Math.max(0, renown),
                current.profession(), current.lastContractDay());
        accounts.put(playerId.toString(), updated);
        setDirty();
        return updated;
    }

    public SpendResult spend(UUID playerId, long amount) {
        Account current = account(playerId);
        long price = Math.max(0L, amount);
        if (current.silver() < price) return new SpendResult(false, current);
        Account updated = new Account(current.silver() - price, current.renown(),
                current.profession(), current.lastContractDay());
        accounts.put(playerId.toString(), updated);
        setDirty();
        return new SpendResult(true, updated);
    }

    public Account setProfession(UUID playerId, String profession) {
        Account current = account(playerId);
        String safe = profession == null || profession.isBlank() ? "unregistered" : profession;
        Account updated = new Account(current.silver(), current.renown(), safe, current.lastContractDay());
        accounts.put(playerId.toString(), updated);
        setDirty();
        return updated;
    }

    public boolean beginDailyContract(UUID playerId, long day) {
        Account current = account(playerId);
        if (current.lastContractDay() == day) return false;
        Account updated = new Account(current.silver(), current.renown(),
                current.profession(), day);
        accounts.put(playerId.toString(), updated);
        setDirty();
        return true;
    }

    public MarketState updateMarket(long day) {
        if (market.day() == day) return market;
        int seasonDay = (int) Math.floorMod(day, 112L);
        int season = seasonDay / 28;
        int grainSeason = switch (season) {
            case 0 -> 108;
            case 1 -> 96;
            case 2 -> 82;
            default -> 132;
        };
        int metalSeason = switch (season) {
            case 3 -> 108;
            case 1 -> 96;
            default -> 100;
        };
        int herbSeason = switch (season) {
            case 0 -> 86;
            case 1 -> 92;
            case 2 -> 111;
            default -> 138;
        };
        market = new MarketState(
                day,
                bounded(grainSeason + wave(day, 17, 11)),
                bounded(metalSeason + wave(day + 31, 29, 9)),
                bounded(herbSeason + wave(day + 7, 23, 13)),
                bounded(100 + wave(day + 19, 37, 10))
        );
        setDirty();
        return market;
    }

    public MarketState market() {
        return market;
    }

    public long price(long basePrice, MarketCategory category) {
        int index = switch (category) {
            case GRAIN -> market.grain();
            case METAL -> market.metal();
            case HERB -> market.herb();
            case LABOR -> market.labor();
            case STABLE -> 100;
        };
        return Math.max(1L, Math.round(basePrice * index / 100.0D));
    }

    private static int wave(long day, int period, int amplitude) {
        double angle = Math.floorMod(day, period) / (double) period * Math.PI * 2.0D;
        return (int) Math.round(Math.sin(angle) * amplitude);
    }

    private static int bounded(int index) {
        return Math.max(65, Math.min(160, index));
    }

    public enum MarketCategory {
        GRAIN,
        METAL,
        HERB,
        LABOR,
        STABLE
    }

    public record Account(long silver, int renown, String profession, long lastContractDay) {
        public Account {
            silver = Math.max(0L, silver);
            renown = Math.max(0, renown);
            profession = profession == null || profession.isBlank() ? "unregistered" : profession;
        }
    }

    public record MarketState(long day, int grain, int metal, int herb, int labor) {
        public MarketState {
            grain = bounded(grain);
            metal = bounded(metal);
            herb = bounded(herb);
            labor = bounded(labor);
        }
    }

    public record SpendResult(boolean accepted, Account account) {
    }
}
