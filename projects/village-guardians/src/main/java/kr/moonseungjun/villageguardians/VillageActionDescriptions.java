package kr.moonseungjun.villageguardians;

final class VillageActionDescriptions {
    private VillageActionDescriptions() {}

    static String describe(String action, String label) {
        if (action == null) return "항목을 선택하면 효과와 비용이 표시됩니다.";
        if (action.startsWith("manage:")) {
            return label + "\n회관에서 공동 보급품을 사용해 내구도와 시설 등급을 관리합니다.";
        }
        if (action.startsWith("building:")) {
            return label + "\n해당 시설의 현장 기능을 이용합니다.";
        }
        if (action.startsWith("repair:")) {
            return label + "\n공동 보급품을 사용해 시설 내구도를 최대치로 복구합니다. 보급품은 회관에서 개인 주화로 조달할 수 있습니다.";
        }
        if (action.startsWith("upgrade:")) {
            return label + "\n공동 보급품을 사용해 시설 등급·최대 내구도·고유 효과를 강화합니다.";
        }
        if (action.startsWith("select_role:")) {
            return label + "\n마을 회관에서 현재 직업을 변경합니다. 성장과 기술 관리는 기술 연구소에서 진행합니다.";
        }
        if (action.startsWith("skill_node:")) {
            return label + "\n기술 연구소에서 전술 포인트 1개를 사용해 공용 전술을 습득합니다.";
        }
        if (action.startsWith("role_node:")) {
            return label + "\n기술 연구소에서 요구 레벨과 수호 주화를 지불해 직업 성장 효과를 습득합니다.";
        }
        if (action.startsWith("role_skill_unlock:")) {
            return label + "\n요구 레벨과 수호 주화를 사용해 직업 기술을 습득합니다.";
        }
        if (action.startsWith("role_skill_equip:")) {
            return label + "\n습득한 기술을 R 또는 G 슬롯에 장착합니다.";
        }
        if (action.startsWith("gear:")) {
            return label + "\n수호 주화로 성장 장비를 구매합니다. 요구 레벨과 방어 일수를 충족해야 합니다.";
        }
        if (action.startsWith("funding:")) {
            return label + "\n개인 수호 주화를 공동 보급품으로 전환해 시설 수리와 강화에 사용합니다.";
        }
        return switch (action) {
            case "open_status" -> "레벨·직업·재화·현재 장착 기술을 확인합니다.";
            case "open_caller_menu", "open_manual" -> "호출기의 상태·통신·귀환 메뉴로 돌아갑니다.";
            case "open_skill_tree" -> "기술 연구소의 공용 전술 발전 화면을 엽니다.";
            case "open_role_progress_current" -> "현재 직업의 세 갈래 성장과 두 기술 슬롯을 관리합니다.";
            case "open_quick_chat" -> "접속 중인 수호단에게 보낼 빠른 신호를 엽니다.";
            case "open_dashboard", "open_mayor" -> "마을 회관의 직업 배치와 시설 관리 화면을 엽니다.";
            case "open_tower_control" -> "마을 회관에서 성벽 수리·강화와 네 종류의 방어탑 해금 상태를 관리합니다.";
            case "open_funding" -> "개인 수호 주화로 공동 보급품을 조달합니다.";
            case "return_village" -> "전투 중이 아닐 때 마을 중앙 광장으로 귀환합니다.";
            case "claim_bread" -> "오늘의 무료 식량을 받습니다.";
            case "buy_arrows" -> label + "\n수호 주화로 화살 묶음을 구매합니다.";
            case "buy_food" -> label + "\n수호 주화로 전투 식량을 구매합니다.";
            case "sell_loot" -> "판매 가능한 몬스터 전리품을 수호 주화로 일괄 교환합니다.";
            case "open_equipment_shop" -> "레벨과 방어 일수에 따라 해금되는 성장 장비 상점을 엽니다.";
            case "forge_upgrade" -> label + "\n수호 주화로 개인 장비 공격 보너스를 강화합니다.";
            case "skill_learn" -> label + "\n수호 주화로 연구 단계를 높여 기술 피해와 재사용 효율을 강화합니다.";
            case "train" -> label + "\n병영 훈련으로 경험치를 획득합니다.";
            case "hire_mercenary" -> label + "\n수호 주화로 영구 용병을 고용합니다. 사망 전까지 저장과 재접속 후에도 유지됩니다.";
            case "tower_status" -> "활성화된 방어탑 종류와 현재 용병 수·정원을 확인합니다.";
            case "wall_status_local" -> "현재 성벽 내구도를 확인합니다. 포탑 지휘와 수리·강화는 회관에서 진행합니다.";
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
        if (action == null) return false;
        return action.startsWith("repair:")
                || action.startsWith("upgrade:")
                || action.startsWith("select_role:")
                || action.startsWith("skill_node:")
                || action.startsWith("role_node:")
                || action.startsWith("role_skill_unlock:")
                || action.startsWith("gear:")
                || action.startsWith("funding:")
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
        if (action == null) return "선택 필요";
        if (action.startsWith("select_role:")) return "직업 배치";
        if (action.startsWith("skill_node:") || action.startsWith("role_node:")) return "노드 습득";
        if (action.startsWith("role_skill_unlock:")) return "기술 습득";
        if (action.startsWith("role_skill_equip:")) return "기술 장착";
        if (action.startsWith("repair:")) return "시설 수리";
        if (action.startsWith("upgrade:")) return "시설 강화";
        if (action.startsWith("funding:")) return "보급 조달";
        if (action.startsWith("gear:")) return "장비 구매";
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
