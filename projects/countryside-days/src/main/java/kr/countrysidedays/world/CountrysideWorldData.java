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

/**
 * World-global state shared by every player in the same countryside world.
 */
public final class CountrysideWorldData extends SavedData {
    public static final SavedDataType<CountrysideWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CountrysideDays.MOD_ID, "world"),
            CountrysideWorldData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.optionalFieldOf("restaurant_anchor").forGetter(data -> data.restaurantAnchor),
                    Codec.LONG.listOf().optionalFieldOf("herb_preparations", List.of())
                            .forGetter(data -> List.copyOf(data.herbPreparations)),
                    Codec.INT.optionalFieldOf("meals_prepared", 0).forGetter(CountrysideWorldData::mealsPrepared),
                    Codec.BOOL.optionalFieldOf("starter_kit_issued", false).forGetter(data -> data.starterKitIssued)
            ).apply(instance, CountrysideWorldData::new))
    );

    private Optional<Long> restaurantAnchor;
    private final Set<Long> herbPreparations;
    private int mealsPrepared;
    private boolean starterKitIssued;

    public CountrysideWorldData() {
        this(Optional.empty(), List.of(), 0, false);
    }

    private CountrysideWorldData(
            Optional<Long> restaurantAnchor,
            List<Long> herbPreparations,
            int mealsPrepared,
            boolean starterKitIssued
    ) {
        this.restaurantAnchor = restaurantAnchor;
        this.herbPreparations = new HashSet<>(herbPreparations);
        this.mealsPrepared = Math.max(0, mealsPrepared);
        this.starterKitIssued = starterKitIssued;
    }

    public static CountrysideWorldData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<BlockPos> restaurantAnchor() {
        return restaurantAnchor.map(BlockPos::of);
    }

    public boolean claimRestaurantAnchor(BlockPos pos) {
        if (restaurantAnchor.isPresent()) {
            return false;
        }
        restaurantAnchor = Optional.of(pos.asLong());
        setDirty();
        return true;
    }

    public boolean claimStarterKit() {
        if (starterKitIssued) {
            return false;
        }
        starterKitIssued = true;
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

    /**
     * Removes temporary cooking state when a kitchen counter is destroyed.
     * If the destroyed counter was the restaurant anchor, a future counter can claim the anchor.
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
}
