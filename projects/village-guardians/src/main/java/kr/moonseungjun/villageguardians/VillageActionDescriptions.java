package kr.moonseungjun.villageguardians;

final class VillageActionDescriptions {
    private VillageActionDescriptions() {
    }

    static String describe(String action, String label) {
        if (action == null) {
            return "항목을 선택하면 효과와 비용이 표시됩니다.";
        }
        if (action.startsWith("manage:")) {
            return label + "의 내구도와 다음 강화 효과를 관리합니다.";
        }
        if (action.startsWith("building:")) {
            return label + "의 현장 기능을 이용합니다.";
        }
        if (action.startsWith("repair:")) {
            return label + "\n공동 보급품을 사용해 시설 내구도를 최대치로 복구합니다.";
        }
        if (action.startsWith("upgrade:")) {
            return label + "\n공동 보급품을 사용해 시설 등급과 고유 효과를 강화합니다.";
        }
        if (action.startsWith("select_role:")) {
            return label + "\n회관에서 현재 역할을 변경합니다.";
        }
        if (action.startsWith("skill_node:")) {
            return label + "\n스킬 포인트 1개를 사용합니다.";
        }
        return switch (action) {
            case "open_status" -> "수호자 상태와 두 개의 인벤토리 동작을 확인합니다.";
            case "open_skill_tree" -> "드래그 가능한 전술 발전 화면을 엽니다.";
            case "open_quick_chat", "open_manual" -> "수호단 빠른 신호를 엽니다.";
            case "open_dashboard", "open_mayor" -> "마을 회관으로 돌아갑니다.";
            case "return_village" -> "전투 중이 아닐 때 마을 광장으로 귀환합니다.";
            case "claim_bread" -> "오늘의 무료 식량을 받습니다.";
            case "buy_arrows" -> label + "\n수호 주화를 지불하고 화살을 구매합니다.";
            case "buy_food" -> label + "\n수호 주화를 지불하고 전투 식량을 구매합니다.";
            case "sell_loot" -> "판매 가능한 몬스터 전리품만 일괄 판매합니다.";
            case "forge_upgrade" -> label + "\n수호 주화를 사용해 개인 공격 보너스를 강화합니다.";
            case "skill_learn" -> label + "\n수호 주화를 사용해 연구 단계를 높입니다.";
            case "train" -> label + "\n병영 훈련으로 XP를 획득합니다.";
            case "hire_mercenary" -> "철 주괴 24개를 사용해 마을 용병을 고용합니다.";
            case "defense_status" -> "방어탑 단계와 배치된 용병 수를 확인합니다.";
            case "use_infirmary" -> "의무소 단계에 따라 즉시 체력을 회복합니다.";
            case "chat_ready" -> "모든 접속 플레이어에게 준비 완료 신호를 보냅니다.";
            case "chat_gate" -> "모든 접속 플레이어에게 북문 집결 신호를 보냅니다.";
            case "chat_repair" -> "모든 접속 플레이어에게 시설 수리 요청을 보냅니다.";
            case "chat_help" -> "모든 접속 플레이어에게 현재 위치 지원 요청을 보냅니다.";
            case "vote_yes" -> "현재 시간 진행 투표에 찬성합니다.";
            case "vote_no" -> "현재 시간 진행 투표에 반대합니다.";
            case "restart_previous" -> "성장과 강화를 유지하고 이전 날부터 다시 시작합니다.";
            case "restart_start" -> "마을 발전과 개인 성장을 초기화하고 첫날부터 다시 시작합니다.";
            default -> label == null || label.isBlank()
                    ? "선택한 동작을 실행합니다."
                    : label + "을(를) 실행합니다.";
        };
    }

    static boolean requiresConfirmation(String action) {
        if (action == null) {
            return false;
        }
        return action.startsWith("repair:")
                || action.startsWith("upgrade:")
                || action.startsWith("select_role:")
                || action.startsWith("skill_node:")
                || action.equals("buy_arrows")
                || action.equals("buy_food")
                || action.equals("sell_loot")
                || action.equals("forge_upgrade")
                || action.equals("skill_learn")
                || action.equals("train")
                || action.equals("hire_mercenary")
                || action.equals("return_village")
                || action.startsWith("restart_");
    }

    static String executeLabel(String action) {
        if (action == null) {
            return "선택 필요";
        }
        if (action.startsWith("select_role:")) return "역할 배치";
        if (action.startsWith("skill_node:")) return "노드 습득";
        if (action.startsWith("repair:")) return "시설 수리";
        if (action.startsWith("upgrade:")) return "시설 강화";
        if (action.startsWith("manage:")) return "관리";
        if (action.startsWith("building:")) return "이용";
        if (action.startsWith("open_")) return "열기";
        if (action.startsWith("buy_")) return "구매";
        if (action.equals("sell_loot")) return "판매";
        if (action.startsWith("restart_")) return "재시작";
        if (action.equals("return_village")) return "귀환";
        return "실행";
    }
}
