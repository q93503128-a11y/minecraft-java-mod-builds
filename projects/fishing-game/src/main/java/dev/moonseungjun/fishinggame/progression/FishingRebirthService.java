package dev.moonseungjun.fishinggame.progression;

import java.util.Locale;

import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import dev.moonseungjun.fishinggame.fishing.FishingStage;
import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.profile.FishingPrestige;
import dev.moonseungjun.fishinggame.profile.FishingProfiles;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.world.FishingTravelManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class FishingRebirthService {
    private FishingRebirthService() {
    }

    public static void request(ServerPlayer player) {
        PlayerFishingProfile current = FishingProfiles.get(player);

        if (player.fishing != null) {
            notice(player, "낚시 중에는 환생할 수 없습니다.");
            return;
        }
        if (current.rebirths() >= FishingPrestige.MAX_REBIRTHS) {
            notice(player, "더 이상 환생할 수 없습니다.");
            return;
        }
        if (current.rodTier() < FishingRods.maxTier()) {
            notice(player, "블루워터까지 성장한 뒤 환생할 수 있습니다.");
            return;
        }
        if (!current.catches().isEmpty()) {
            notice(player, "환생 전에 어획 가방을 모두 판매해 주세요.");
            return;
        }

        int cost = FishingPrestige.nextCost(current.rebirths());
        if (current.coins() < cost) {
            notice(player, "환생에 " + cost + " 코인이 필요합니다.");
            return;
        }

        if (!FishingTravelManager.travel(player, FishingLocation.LAKESIDE)) {
            notice(player, "환생을 시작할 낚시터로 돌아가지 못했습니다.");
            return;
        }

        PlayerFishingProfile next = current.rebirth();
        FishingProfiles.set(player, next);
        FishingSessionManager.syncProfile(player);
        notice(player, String.format(
                Locale.ROOT,
                "환생 %d회 완료 · 영구 판매 배율 x%.2f",
                next.rebirths(),
                next.saleMultiplier()
        ));
    }

    private static void notice(ServerPlayer player, String text) {
        ServerPlayNetworking.send(player, new FishingStatePayload(
                FishingStage.values().length,
                0.0f,
                0.0f,
                "",
                FishingTravelManager.locationFor(player).displayName(),
                text
        ));
    }
}
