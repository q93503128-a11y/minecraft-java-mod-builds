package kr.countrysidedays.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.countrysidedays.CountrysideDays;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** World-global state shared by every player in the same countryside world. */
public final class CountrysideWorldData extends SavedData {
    private static final String DEFAULT_RESTAURANT_NAME = "나의 시골식당";

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
                            .forGetter(data -> List.copyOf(data.terrainChunks)),
                    Codec.STRING.optionalFieldOf("homestead_owner_uuid").forGetter(data -> data.homesteadOwnerUuid),
                    Codec.STRING.optionalFieldOf("homestead_owner_name", "").forGetter(CountrysideWorldData::ownerName),
                    Codec.STRING.optionalFieldOf("restaurant_name", DEFAULT_RESTAURANT_NAME)
                            .forGetter(CountrysideWorldData::restaurantName)
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
    private Optional<String> homesteadOwnerUuid;
    private String homesteadOwnerName;
    private String restaurantName;

    public CountrysideWorldData() {
        this(Optional.empty(), Optional.empty(), List.of(), 0, 0, 0, -1L, List.of(),
                Optional.empty(), "", DEFAULT_RESTAURANT_NAME);
    }

    private CountrysideWorldData(
            Optional<Long> restaurantAnchor,
            Optional<Long> homesteadOrigin,
            List<Long> herbPreparations,
            int mealsPrepared,
            int customersServed,
            int villageCoinsEarned,
            long lastCustomerServiceDay,
            List<Long> terrainChunks,
            Optional<String> homesteadOwnerUuid,
            String homesteadOwnerName,
            String restaurantName
    ) {
        this.restaurantAnchor = restaurantAnchor;
        this.homesteadOrigin = homesteadOrigin;
        this.herbPreparations = new HashSet<>(herbPreparations);
        this.terrainChunks = new HashSet<>(terrainChunks);
        this.mealsPrepared = Math.max(0, mealsPrepared);
        this.customersServed = Math.max(0, customersServed);
        this.villageCoinsEarned = Math.max(0, villageCoinsEarned);
        this.lastCustomerServiceDay = lastCustomerServiceDay;
        this.homesteadOwnerUuid = homesteadOwnerUuid;
        this.homesteadOwnerName = homesteadOwnerName == null ? "" : homesteadOwnerName;
        this.restaurantName = normalizeRestaurantName(restaurantName);
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
        if (restaurantAnchor.isPresent()) return false;
        restaurantAnchor = Optional.of(pos.asLong());
        setDirty();
        return true;
    }

    public boolean claimHomesteadOrigin(BlockPos pos) {
        if (homesteadOrigin.isPresent()) return false;
        homesteadOrigin = Optional.of(pos.asLong());
        setDirty();
        return true;
    }

    public boolean claimHomesteadOwner(UUID uuid, String playerName) {
        if (homesteadOwnerUuid.isPresent()) return false;
        homesteadOwnerUuid = Optional.of(uuid.toString());
        homesteadOwnerName = playerName == null ? "" : playerName;
        if (DEFAULT_RESTAURANT_NAME.equals(restaurantName) && !homesteadOwnerName.isBlank()) {
            restaurantName = homesteadOwnerName + "의 시골식당";
        }
        setDirty();
        return true;
    }

    public boolean isHomesteadOwner(UUID uuid) {
        return homesteadOwnerUuid.map(value -> value.equals(uuid.toString())).orElse(false);
    }

    public String ownerName() {
        return homesteadOwnerName;
    }

    public String restaurantName() {
        return restaurantName;
    }

    public boolean renameRestaurant(UUID requester, String requestedName) {
        if (!isHomesteadOwner(requester)) return false;
        String normalized = normalizeRestaurantName(requestedName);
        if (normalized.equals(restaurantName)) return false;
        restaurantName = normalized;
        setDirty();
        return true;
    }

    private static String normalizeRestaurantName(String value) {
        if (value == null) return DEFAULT_RESTAURANT_NAME;
        String stripped = value.strip();
        if (stripped.isEmpty()) return DEFAULT_RESTAURANT_NAME;
        return stripped.length() > 24 ? stripped.substring(0, 24) : stripped;
    }

    public boolean hasHerbPreparation(BlockPos pos) {
        return herbPreparations.contains(pos.asLong());
    }

    public boolean addHerbPreparation(BlockPos pos) {
        boolean added = herbPreparations.add(pos.asLong());
        if (added) setDirty();
        return added;
    }

    public boolean consumeHerbPreparation(BlockPos pos) {
        boolean removed = herbPreparations.remove(pos.asLong());
        if (removed) setDirty();
        return removed;
    }

    public boolean isTerrainChunkPrepared(int chunkX, int chunkZ) {
        return terrainChunks.contains(packChunk(chunkX, chunkZ));
    }

    public boolean markTerrainChunkPrepared(int chunkX, int chunkZ) {
        boolean added = terrainChunks.add(packChunk(chunkX, chunkZ));
        if (added) setDirty();
        return added;
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    public boolean removeKitchenState(BlockPos pos) {
        long packedPos = pos.asLong();
        boolean changed = herbPreparations.remove(packedPos);
        if (restaurantAnchor.isPresent() && restaurantAnchor.get() == packedPos) {
            restaurantAnchor = Optional.empty();
            changed = true;
        }
        if (changed) setDirty();
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
        if (day <= lastCustomerServiceDay) return false;
        lastCustomerServiceDay = day;
        customersServed++;
        villageCoinsEarned += Math.max(0, rewardCoins);
        setDirty();
        return true;
    }
}
