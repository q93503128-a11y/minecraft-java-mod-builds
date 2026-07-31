package kr.countrysidedays.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.countrysidedays.CountrysideDays;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * World-global state shared by every player in the same countryside world.
 */
public final class CountrysideWorldData extends SavedData {
    public static final SavedDataType<CountrysideWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CountrysideDays.MOD_ID, "world"),
            CountrysideWorldData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.optionalFieldOf("restaurant_anchor").forGetter(data -> data.restaurantAnchor),
                    Codec.LONG.optionalFieldOf("homestead_origin").forGetter(data -> data.homesteadOrigin),
                    Codec.LONG.listOf().optionalFieldOf("herb_preparations", List.of())
                            .forGetter(data -> List.copyOf(data.herbPreparations)),
                    Codec.INT.optionalFieldOf("meals_prepared", 0).forGetter(CountrysideWorldData::mealsPrepared),
                    Codec.INT.optionalFieldOf("customers_served", 0).forGetter(CountrysideWorldData::customersServed),
                    Codec.INT.optionalFieldOf("village_coins_earned", 0).forGetter(CountrysideWorldData::villageCoinsEarned),
                    Codec.LONG.optionalFieldOf("last_customer_service_day", -1L)
                            .forGetter(CountrysideWorldData::lastCustomerServiceDay),
                    Codec.LONG.listOf().optionalFieldOf("terrain_chunks", List.of())
                            .forGetter(data -> List.copyOf(data.terrainChunks))
            ).apply(instance, CountrysideWorldData::new))
    );

    private Optional<Long> restaurantAnchor;
    private Optional<Long> homesteadOrigin;
    private final Set<Long> herbPreparations;
    private final Set<Long> terrainChunks;
    private int mealsPrepared;
    private int customersServed;
    private int villageCoinsEarned;
    private long lastCustomerServiceDay;

    public CountrysideWorldData() {
        this(Optional.empty(), Optional.empty(), List.of(), 0, 0, 0, -1L, List.of());
    }

    private CountrysideWorldData(
            Optional<Long> restaurantAnchor,
            Optional<Long> homesteadOrigin,
            List<Long> herbPreparations,
            int mealsPrepared,
            int customersServed,
            int villageCoinsEarned,
            long lastCustomerServiceDay,
            List<Long> terrainChunks
    ) {
        this.restaurantAnchor = restaurantAnchor;
        this.homesteadOrigin = homesteadOrigin;
        this.herbPreparations = new HashSet<>(herbPreparations);
        this.terrainChunks = new HashSet<>(terrainChunks);
        this.mealsPrepared = Math.max(0, mealsPrepared);
        this.customersServed = Math.max(0, customersServed);
        this.villageCoinsEarned = Math.max(0, villageCoinsEarned);
        this.lastCustomerServiceDay = lastCustomerServiceDay;
    }

    public static CountrysideWorldData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<BlockPos> restaurantAnchor() {
        return restaurantAnchor.map(BlockPos::of);
    }

    public Optional<BlockPos> homesteadOrigin() {
        return homesteadOrigin.map(BlockPos::of);
    }

    public boolean claimRestaurantAnchor(BlockPos pos) {
        if (restaurantAnchor.isPresent()) {
            return false;
        }
        restaurantAnchor = Optional.of(pos.asLong());
        setDirty();
        return true;
    }

    public boolean claimHomesteadOrigin(BlockPos pos) {
        if (homesteadOrigin.isPresent()) {
            return false;
        }
        homesteadOrigin = Optional.of(pos.asLong());
        setDirty();
        return true;
    }

    public boolean hasHerbPreparation(BlockPos pos) {
        return herbPreparations.contains(pos.asLong());
    }

    public boolean addHerbPreparation(BlockPos pos) {
        boolean added = herbPreparations.add(pos.asLong());
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean consumeHerbPreparation(BlockPos pos) {
        boolean removed = herbPreparations.remove(pos.asLong());
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean isTerrainChunkPrepared(int chunkX, int chunkZ) {
        return terrainChunks.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public boolean markTerrainChunkPrepared(int chunkX, int chunkZ) {
        boolean added = terrainChunks.add(ChunkPos.asLong(chunkX, chunkZ));
        if (added) {
            setDirty();
        }
        return added;
    }

    /**
     * Removes temporary cooking state when a kitchen counter is destroyed.
     * The homestead origin remains permanent so the whole settlement is never
     * duplicated merely because a player remodelled or removed the first counter.
     */
    public boolean removeKitchenState(BlockPos pos) {
        long packedPos = pos.asLong();
        boolean changed = herbPreparations.remove(packedPos);
        if (restaurantAnchor.isPresent() && restaurantAnchor.get() == packedPos) {
            restaurantAnchor = Optional.empty();
            changed = true;
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public int mealsPrepared() {
        return mealsPrepared;
    }

    public void recordPreparedMeal() {
        mealsPrepared++;
        setDirty();
    }

    public int customersServed() {
        return customersServed;
    }

    public int villageCoinsEarned() {
        return villageCoinsEarned;
    }

    public long lastCustomerServiceDay() {
        return lastCustomerServiceDay;
    }

    public boolean recordCustomerService(long day, int rewardCoins) {
        if (day <= lastCustomerServiceDay) {
            return false;
        }
        lastCustomerServiceDay = day;
        customersServed++;
        villageCoinsEarned += Math.max(0, rewardCoins);
        setDirty();
        return true;
    }
}
