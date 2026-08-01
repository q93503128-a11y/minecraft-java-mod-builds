package kr.countrysidedays.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.countrysidedays.CountrysideDays;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** World state. Public village data is shared; homes, restaurants, farms and ranches are per player. */
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
                    Codec.STRING.optionalFieldOf("homestead_owner_uuid").forGetter(data -> data.legacyOwnerUuid),
                    Codec.STRING.optionalFieldOf("homestead_owner_name", "").forGetter(data -> data.legacyOwnerName),
                    Codec.STRING.optionalFieldOf("restaurant_name", DEFAULT_RESTAURANT_NAME)
                            .forGetter(data -> data.legacyRestaurantName),
                    PlayerEstate.CODEC.listOf().optionalFieldOf("player_estates", List.of())
                            .forGetter(data -> List.copyOf(data.playerEstates))
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
    private Optional<String> legacyOwnerUuid;
    private String legacyOwnerName;
    private String legacyRestaurantName;
    private final List<PlayerEstate> playerEstates;

    public CountrysideWorldData() {
        this(Optional.empty(), Optional.empty(), List.of(), 0, 0, 0, -1L, List.of(),
                Optional.empty(), "", DEFAULT_RESTAURANT_NAME, List.of());
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
            Optional<String> legacyOwnerUuid,
            String legacyOwnerName,
            String legacyRestaurantName,
            List<PlayerEstate> playerEstates
    ) {
        this.restaurantAnchor = restaurantAnchor;
        this.homesteadOrigin = homesteadOrigin;
        this.herbPreparations = new HashSet<>(herbPreparations);
        this.terrainChunks = new HashSet<>(terrainChunks);
        this.mealsPrepared = Math.max(0, mealsPrepared);
        this.customersServed = Math.max(0, customersServed);
        this.villageCoinsEarned = Math.max(0, villageCoinsEarned);
        this.lastCustomerServiceDay = lastCustomerServiceDay;
        this.legacyOwnerUuid = legacyOwnerUuid;
        this.legacyOwnerName = legacyOwnerName == null ? "" : legacyOwnerName;
        this.legacyRestaurantName = normalizeRestaurantName(legacyRestaurantName);
        this.playerEstates = new ArrayList<>(playerEstates);
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

    public Optional<PlayerEstate> estate(UUID owner) {
        String id = owner.toString();
        return playerEstates.stream().filter(estate -> estate.ownerUuid().equals(id)).findFirst();
    }

    public Optional<PlayerEstate> estateAt(BlockPos pos) {
        return playerEstates.stream().filter(estate -> PlayerEstateLayout.contains(estate.originPos(), pos)).findFirst();
    }

    public List<PlayerEstate> estates() {
        return List.copyOf(playerEstates);
    }

    public EstateAllocation ensureEstate(UUID owner, String ownerName, BlockPos villageOrigin) {
        Optional<PlayerEstate> existing = estate(owner);
        if (existing.isPresent()) return new EstateAllocation(existing.get(), false);

        BlockPos origin = PlayerEstateLayout.originForIndex(villageOrigin, playerEstates.size());
        String safeOwnerName = ownerName == null || ownerName.isBlank() ? "새 주민" : ownerName;
        PlayerEstate estate = new PlayerEstate(
                owner.toString(),
                safeOwnerName,
                origin.asLong(),
                safeOwnerName + "의 시골식당",
                0,
                0,
                0,
                -1L
        );
        playerEstates.add(estate);
        setDirty();
        return new EstateAllocation(estate, true);
    }

    public boolean renameRestaurant(UUID requester, String requestedName) {
        Optional<PlayerEstate> found = estate(requester);
        if (found.isPresent()) {
            PlayerEstate current = found.get();
            String normalized = normalizeRestaurantName(requestedName);
            if (normalized.equals(current.restaurantName())) return false;
            replaceEstate(current, current.withRestaurantName(normalized));
            return true;
        }

        if (!legacyOwnerUuid.map(value -> value.equals(requester.toString())).orElse(false)) return false;
        String normalized = normalizeRestaurantName(requestedName);
        if (normalized.equals(legacyRestaurantName)) return false;
        legacyRestaurantName = normalized;
        setDirty();
        return true;
    }

    public void recordPreparedMeal(UUID owner) {
        estate(owner).ifPresent(current -> replaceEstate(current, current.withPreparedMeal()));
        mealsPrepared++;
        setDirty();
    }

    public boolean recordCustomerService(UUID owner, long day, int rewardCoins) {
        Optional<PlayerEstate> found = estate(owner);
        if (found.isEmpty() || day <= found.get().lastCustomerServiceDay()) return false;
        PlayerEstate current = found.get();
        replaceEstate(current, current.withCustomerService(day, rewardCoins));
        customersServed++;
        villageCoinsEarned += Math.max(0, rewardCoins);
        lastCustomerServiceDay = Math.max(lastCustomerServiceDay, day);
        setDirty();
        return true;
    }

    private void replaceEstate(PlayerEstate oldEstate, PlayerEstate newEstate) {
        int index = playerEstates.indexOf(oldEstate);
        if (index >= 0) {
            playerEstates.set(index, newEstate);
            setDirty();
        }
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

    public boolean claimHomesteadOwner(UUID uuid, String playerName) {
        if (legacyOwnerUuid.isPresent()) return false;
        legacyOwnerUuid = Optional.of(uuid.toString());
        legacyOwnerName = playerName == null ? "" : playerName;
        if (DEFAULT_RESTAURANT_NAME.equals(legacyRestaurantName) && !legacyOwnerName.isBlank()) {
            legacyRestaurantName = legacyOwnerName + "의 시골식당";
        }
        setDirty();
        return true;
    }

    public boolean isHomesteadOwner(UUID uuid) {
        return estate(uuid).isPresent() || legacyOwnerUuid.map(value -> value.equals(uuid.toString())).orElse(false);
    }

    public String ownerName() {
        return legacyOwnerName;
    }

    public String restaurantName() {
        return legacyRestaurantName;
    }

    private static String normalizeRestaurantName(String value) {
        if (value == null) return DEFAULT_RESTAURANT_NAME;
        String stripped = value.strip();
        if (stripped.isEmpty()) return DEFAULT_RESTAURANT_NAME;
        return stripped.length() > 24 ? stripped.substring(0, 24) : stripped;
    }

    public record EstateAllocation(PlayerEstate estate, boolean created) {
    }

    public record PlayerEstate(
            String ownerUuid,
            String ownerName,
            long origin,
            String restaurantName,
            int mealsPrepared,
            int customersServed,
            int coinsEarned,
            long lastCustomerServiceDay
    ) {
        public static final Codec<PlayerEstate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner_uuid").forGetter(PlayerEstate::ownerUuid),
                Codec.STRING.fieldOf("owner_name").forGetter(PlayerEstate::ownerName),
                Codec.LONG.fieldOf("origin").forGetter(PlayerEstate::origin),
                Codec.STRING.optionalFieldOf("restaurant_name", DEFAULT_RESTAURANT_NAME)
                        .forGetter(PlayerEstate::restaurantName),
                Codec.INT.optionalFieldOf("meals_prepared", 0).forGetter(PlayerEstate::mealsPrepared),
                Codec.INT.optionalFieldOf("customers_served", 0).forGetter(PlayerEstate::customersServed),
                Codec.INT.optionalFieldOf("coins_earned", 0).forGetter(PlayerEstate::coinsEarned),
                Codec.LONG.optionalFieldOf("last_customer_service_day", -1L)
                        .forGetter(PlayerEstate::lastCustomerServiceDay)
        ).apply(instance, PlayerEstate::new));

        public BlockPos originPos() {
            return BlockPos.of(origin);
        }

        public boolean isOwner(UUID uuid) {
            return ownerUuid.equals(uuid.toString());
        }

        public PlayerEstate withRestaurantName(String name) {
            return new PlayerEstate(ownerUuid, ownerName, origin, name, mealsPrepared, customersServed,
                    coinsEarned, lastCustomerServiceDay);
        }

        public PlayerEstate withPreparedMeal() {
            return new PlayerEstate(ownerUuid, ownerName, origin, restaurantName, mealsPrepared + 1,
                    customersServed, coinsEarned, lastCustomerServiceDay);
        }

        public PlayerEstate withCustomerService(long day, int rewardCoins) {
            return new PlayerEstate(ownerUuid, ownerName, origin, restaurantName, mealsPrepared,
                    customersServed + 1, coinsEarned + Math.max(0, rewardCoins), day);
        }
    }
}
