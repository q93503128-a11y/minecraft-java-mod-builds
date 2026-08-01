package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class VillageFundingSystem {
    private VillageFundingSystem() {}

    public static List<Bundle> bundles() {
        return List.of(Bundle.values());
    }

    public static String purchase(ServerPlayer player, String id) {
        Bundle bundle = Bundle.parse(id).orElse(null);
        if (bundle == null) {
            return "알 수 없는 보급 조달 항목입니다.";
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return "서버 상태를 확인할 수 없습니다.";
        }
        if (!VillageProgressionSystem.spendCoins(player, bundle.coinCost())) {
            return "수호 주화가 부족합니다. 필요 " + bundle.coinCost()
                    + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        VillageProgressionSystem.addSupplies(server, bundle.supplies(),
                player.getGameProfile().name() + "의 보급 조달");
        return "수호 주화 " + bundle.coinCost() + "으로 공동 보급품 "
                + bundle.supplies() + "을 조달했습니다.";
    }

    public enum Bundle {
        SMALL("small", "긴급 보급 상자", 60, 90),
        MEDIUM("medium", "수비대 보급 수레", 160, 220),
        LARGE("large", "대규모 재건 물자", 360, 460);

        private final String id;
        private final String displayName;
        private final int supplies;
        private final int coinCost;

        Bundle(String id, String displayName, int supplies, int coinCost) {
            this.id = id;
            this.displayName = displayName;
            this.supplies = supplies;
            this.coinCost = coinCost;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public int supplies() { return supplies; }
        public int coinCost() { return coinCost; }

        public static Optional<Bundle> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(bundle -> bundle.id.equals(normalized)).findFirst();
        }
    }
}
