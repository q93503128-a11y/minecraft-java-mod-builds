package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Prevents legacy facility handlers from reopening obsolete local management screens. */
public final class VillageLocalActionSystem {
    private VillageLocalActionSystem() {}

    public static boolean handle(ServerPlayer player, String action) {
        if (player == null || action == null) return false;
        switch (action) {
            case "use_infirmary" -> {
                VillageUiController.openResult(player, "의무소",
                        "의무소는 자동 버프 건물입니다. 낮에는 마을 안에서 체력이 항상 완전히 회복됩니다.",
                        "open_dashboard");
                return true;
            }
            case "train" -> {
                VillageUiController.openResult(player, "병영 훈련",
                        "전투 훈련은 패시브로 변경되었습니다. 현재 모든 경험치 획득량 +"
                                + (VillageProgressionSystem.experienceMultiplierPercent() - 100) + "%",
                        "open_dashboard");
                return true;
            }
            case "hire_mercenary" -> {
                VillageUiController.openResult(player, "용병 고용",
                        "구형 단일 용병 호출은 제거되었습니다. 병영에서 현재 4개 병과 중 하나를 선택하세요.",
                        "open_mercenary_command");
                return true;
            }
            default -> { return false; }
        }
    }
}
