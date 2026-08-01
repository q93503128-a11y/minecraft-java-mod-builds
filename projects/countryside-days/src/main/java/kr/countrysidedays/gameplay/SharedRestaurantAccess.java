package kr.countrysidedays.gameplay;

import kr.countrysidedays.world.CountrysideWorldData;

import java.util.Optional;
import java.util.UUID;

/**
 * The first allocated estate owns the world's only restaurant.
 * Every later estate owner is a restaurant employee while keeping an independent home, farm and ranch.
 * This derives the role from the existing ordered estate list, so old saves need no codec migration.
 */
public final class SharedRestaurantAccess {
    private SharedRestaurantAccess() {
    }

    public static Optional<CountrysideWorldData.PlayerEstate> restaurantEstate(CountrysideWorldData data) {
        return data.estates().stream().findFirst();
    }

    public static Optional<UUID> restaurantOwner(CountrysideWorldData data) {
        return restaurantEstate(data).flatMap(estate -> parseUuid(estate.ownerUuid()));
    }

    public static boolean isOwner(CountrysideWorldData data, UUID player) {
        return restaurantOwner(data).map(player::equals).orElse(false);
    }

    public static boolean isStaff(CountrysideWorldData data, UUID player) {
        return data.estate(player).isPresent();
    }

    public static Optional<Boolean> toggleOpen(CountrysideWorldData data, UUID requester) {
        if (!isStaff(data, requester)) return Optional.empty();
        return restaurantOwner(data).flatMap(data::toggleRestaurant);
    }

    public static boolean setOpen(CountrysideWorldData data, boolean open) {
        return restaurantOwner(data).map(owner -> data.setRestaurantOpen(owner, open)).orElse(false);
    }

    public static void recordPreparedMeal(CountrysideWorldData data) {
        restaurantOwner(data).ifPresent(data::recordPreparedMeal);
    }

    public static boolean recordCustomerService(
            CountrysideWorldData data,
            long day,
            int customerSlot,
            int rewardCoins
    ) {
        return restaurantOwner(data)
                .map(owner -> data.recordCustomerService(owner, day, customerSlot, rewardCoins))
                .orElse(false);
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
