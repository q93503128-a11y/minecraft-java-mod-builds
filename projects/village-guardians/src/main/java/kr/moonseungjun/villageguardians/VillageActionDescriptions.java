package kr.moonseungjun.villageguardians;

final class VillageActionDescriptions {
    private VillageActionDescriptions() {
    }

    static String describe(String action, String label) {
        if (action == null) {
            return "동작을 선택하면 상세 설명이 표시됩니다.";
        }
        if (action.startsWith("manage:")) {
            return label + " 화면을 열어 시설의 현재 내구도, 수리비, 다음 강화 비용과 효과를 확인합니다.";
        }
        if (action.startsWith("building:")) {
            return label + "의 현장 기능 화면을 엽니다. 시설 관리와 실제 시설 기능은 분리되어 있습니다.";
        }
        if (action.startsWith("repair:")) {
            return label + "\n공동 보급품을 사용해 해당 시설의 내구도를 최대치로 복구합니다. 습격 중에는 실행할 수 없습니다.";
        }
        if (action.startsWith("upgrade:")) {
            return label + "\n공동 보급품을 사용해 시설 레벨을 올립니다. 최대 내구도와 시설 고유 효과가 강화됩니다.";
        }
        if (action.startsWith("role_info:")) {
            return label + "의 전투 위치, 상시 효과, 단축키 스킬과 추천 운용을 먼저 확인합니다.";
        }
        if (action.startsWith("select_role:")) {
            return label + "\n현재 역할을 이 역할로 변경합니다. 역할 효과와 R 단축키 스킬이 즉시 바뀝니다.";
        }
        if (action.startsWith("skill_node:")) {
            return label + "\n스킬 포인트 1개를 사용합니다. 앞 단계 노드와 포인트 조건을 충족해야 합니다.";
        }
        return switch (action) {
            case "open_status" -> "레벨, 역할, 개인 강화, 스킬 포인트와 해금된 전투 기술을 확인합니다.";
            case "open_skill_tree" -> "공격·방어·지원 세 갈래의 발전 과제형 스킬 트리를 엽니다.";
            case "open_quick_chat", "open_manual" -> "접속 중인 플레이어에게 보낼 빠른 신호 목록을 엽니다.";
            case "open_dashboard", "open_mayor" -> "마을 회관의 시설 관리 화면으로 돌아갑니다.";
            case "return_village" -> "전투 중이 아닐 때 마을 광장으로 귀환합니다. 성공 후 60초 재사용 대기시간이 적용됩니다.";
            case "claim_bread" -> "오늘의 무료 식량을 받습니다. 하루에 한 번만 받을 수 있습니다.";
            case "buy_arrows" -> label + "\n개인 수호 주화를 지불하고 화살을 구매합니다.";
            case "buy_food" -> label + "\n개인 수호 주화를 지불하고 전투 식량을 구매합니다.";
            case "sell_loot" -> "주 인벤토리 36칸의 판매 가능한 몬스터 전리품만 일괄 판매합니다. 갑옷과 보조 손은 건드리지 않습니다.";
            case "forge_upgrade" -> label + "\n개인 수호 주화를 사용해 영구 공격 보너스를 강화합니다.";
            case "skill_learn" -> label + "\n개인 수호 주화를 사용해 역할 스킬과 후반 전투 기술의 연구 단계를 높입니다.";
            case "train" -> label + "\n병영 훈련으로 XP를 획득합니다. 훈련 후 재사용 대기시간이 적용됩니다.";
            case "hire_mercenary" -> "철 주괴 24개를 사용해 마을 용병을 고용합니다. 병영 레벨과 공병대장이 정원을 늘립니다.";
            case "defense_status" -> "현재 방어탑 레벨, 공병 지휘 여부와 배치된 용병 수를 확인합니다.";
            case "use_infirmary" -> "의무소 레벨에 따라 즉시 체력을 회복합니다.";
            case "chat_ready" -> "모든 접속 플레이어에게 준비 완료 신호를 보냅니다.";
            case "chat_gate" -> "모든 접속 플레이어에게 북문 집결 신호를 보냅니다.";
            case "chat_repair" -> "모든 접속 플레이어에게 시설 수리 요청을 보냅니다.";
            case "chat_help" -> "모든 접속 플레이어에게 현재 위치 지원 요청을 보냅니다.";
            case "vote_yes" -> "현재 시간 진행 투표에 찬성합니다.";
            case "vote_no" -> "현재 시간 진행 투표에 반대합니다.";
            case "restart_previous" -> "현재 성장과 강화를 유지하고 이전 날 낮부터 다시 시작합니다.";
            case "restart_start" -> "마을 발전, 개인 성장과 역할을 초기화하고 첫날부터 다시 시작합니다.";
            default -> label == null || label.isBlank()
                    ? "선택한 동작의 내용을 확인한 뒤 실행하세요."
                    : label + " 동작을 실행합니다.";
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
            return "동작 선택";
        }
        if (action.startsWith("select_role:")) return "역할 선택 확인";
        if (action.startsWith("skill_node:")) return "노드 습득 확인";
        if (action.startsWith("repair:")) return "수리 확인";
        if (action.startsWith("upgrade:")) return "강화 확인";
        if (action.startsWith("manage:") || action.startsWith("building:")
                || action.startsWith("role_info:") || action.startsWith("open_")) return "상세 화면 열기";
        if (action.startsWith("buy_")) return "구매 확인";
        if (action.equals("sell_loot")) return "판매 확인";
        if (action.startsWith("restart_")) return "재시작 확인";
        return "선택한 동작 실행";
    }
}
