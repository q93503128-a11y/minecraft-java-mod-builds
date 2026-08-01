package kr.moonseungjun.livingkingdoms.economy;

import kr.moonseungjun.livingkingdoms.crime.CrimeSavedData;
import kr.moonseungjun.livingkingdoms.network.FantasyHudStatePayload;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side façade for wallets, market prices and HUD synchronization. */
public final class RealmEconomyManager {
    private RealmEconomyManager() {
    }

    public static RealmEconomySavedData.Account account(ServerPlayer player) {
        return data(player).account(player.getUUID());
    }

    public static RealmEconomySavedData.Account credit(ServerPlayer player, long silver, int renown) {
        RealmEconomySavedData.Account account = data(player).credit(player.getUUID(), silver, renown);
        sync(player);
        return account;
    }

    public static RealmEconomySavedData.SpendResult spend(ServerPlayer player, long silver) {
        RealmEconomySavedData.SpendResult result = data(player).spend(player.getUUID(), silver);
        sync(player);
        return result;
    }

    public static RealmEconomySavedData.Account setProfession(ServerPlayer player, String profession) {
        RealmEconomySavedData.Account account = data(player).setProfession(player.getUUID(), profession);
        sync(player);
        return account;
    }

    public static long price(ServerPlayer player, long basePrice,
                             RealmEconomySavedData.MarketCategory category) {
        RealmEconomySavedData data = data(player);
        data.updateMarket(currentDay(player));
        return data.price(basePrice, category);
    }

    public static boolean beginDailyContract(ServerPlayer player) {
        return data(player).beginDailyContract(player.getUUID(), currentDay(player));
    }

    public static void tick(ServerPlayer player) {
        if (!player.level().dimension().equals(StarterRealmManager.REALM_KEY)) return;
        RealmEconomySavedData data = data(player);
        data.updateMarket(currentDay(player));
        data.account(player.getUUID());
        if (player.level().getGameTime() % 20L == 0L) sync(player);
    }

    public static void sync(ServerPlayer player) {
        RealmEconomySavedData data = data(player);
        RealmEconomySavedData.Account account = data.account(player.getUUID());
        RealmEconomySavedData.MarketState market = data.updateMarket(currentDay(player));
        int wanted = wanted(player);
        PacketDistributor.sendToPlayer(player, new FantasyHudStatePayload(
                account.silver(), account.renown(), wanted, professionName(account.profession()),
                market.grain(), market.metal(), market.herb(), market.labor()
        ));
    }

    public static String marketSummary(ServerPlayer player) {
        RealmEconomySavedData data = data(player);
        RealmEconomySavedData.MarketState market = data.updateMarket(currentDay(player));
        return "곡물 " + market.grain() + " · 금속 " + market.metal()
                + " · 약재 " + market.herb() + " · 노임 " + market.labor();
    }

    private static int wanted(ServerPlayer player) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return 0;
        return realm.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE)
                .record(player.getUUID()).wanted();
    }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(player.level().getDayTime(), 24_000L);
    }

    private static String professionName(String id) {
        return switch (id) {
            case "warden" -> "수비대 계약자";
            case "smith" -> "금속공 길드원";
            case "herbalist" -> "약초 조합원";
            case "carter" -> "운송 조합원";
            case "scholar" -> "기록원 수습생";
            case "fisher" -> "수운·어업 조합원";
            case "laborer" -> "도시 노동 조합원";
            default -> "미등록";
        };
    }

    private static RealmEconomySavedData data(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage()
                .computeIfAbsent(RealmEconomySavedData.TYPE);
    }
}
