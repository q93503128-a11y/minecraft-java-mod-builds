package kr.moonseungjun.villageguardians;

final class VillageActionDescriptions {
    private VillageActionDescriptions() {}

    static String describe(String action, String label) {
        if (action == null) return "항목을 선택하면 효과와 비용이 표시됩니다.";
        if (action.startsWith("manage:")) {
            return label + "\n현재 시설 단말기 또는 회관에서 공동 보급품을 사용해 내구도와 시설 등급을 관리합니다.";
        }
        if (action.startsWith("open_building:")) {
            return label + "\n현재 시설의 훈련·치료·구매 등 고유 기능 화면으로 돌아갑니다.";
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
            return label + "\n레벨당 1개씩 얻는 전술 포인트를 사용합니다. 비용은 단계에 따라 1~4P입니다.";
        }
        if (action.startsWith("role_node:")) {
            return label + "\n기술 연구소에서 요구 레벨과 수호 주화를 지불해 직업 성장 효과를 습득합니다.";
        }
        if (action.startsWith("role_skill_unlock:")) {
            return label + "\n기술 연구소에서 요구 레벨과 수호 주화를 사용해 습득합니다.";
        }
        if (action.startsWith("role_skill_equip:")) {
            return label + "\n습득한 기술을 어디서나 Z 또는 X 슬롯에 장착합니다.";
        }
        if (action.startsWith("test_role:")) {
            return label + "\n시험 전용 직업만 바꾸며 실제 직업과 저장된 성장 상태는 유지합니다.";
        }
        if (action.startsWith("test_choose:")) {
            return label + "\n이전 시험 UI 호환 경로로 Z 시험 슬롯에 장착합니다.";
        }
        if (action.startsWith("test_equip:")) {
            return label + "\n선택한 Z/X 시험 슬롯에 장착한 뒤 화면을 닫습니다. 화면이 닫힌 상태에서 Z/X를 눌러 시전합니다.";
        }
        if (action.startsWith("gear:")) {
            return label + "\n수호 주화로 장비를 구매합니다. 강한 상품은 방어 일수에 따라 입고됩니다.";
        }
        if (action.startsWith("consumable:")) {
            return label + "\n수호 주화로 전투 소모품을 구매합니다. 전투 중 우클릭해 사용하며 종류별 재사용 대기시간이 있습니다.";
        }
        if (action.startsWith("funding:")) {
            return label + "\n개인 수호 주화를 공동 보급품으로 전환해 시설 수리와 강화에 사용합니다.";
        }
        if (action.startsWith("tower_branch:")) {
            return label + "\n방어탑의 전투 방식과 외형을 선택한 전문 분기로 교체합니다.";
        }
        if (action.startsWith("tower_upgrade:")) {
            return label + "\n현재 방어탑 전문 분기의 위력·범위·특수 효과를 한 단계 강화합니다.";
        }
        return switch (action) {
            case "open_status" -> "레벨·직업·재화·현재 장착 기술을 한 화면에서 확인합니다.";
            case "open_caller_menu", "open_manual" -> "인벤토리에서 여는 수호단 메뉴로 돌아갑니다.";
            case "open_skill_tree" -> "기술 연구소의 공용 전술 발전 화면을 엽니다.";
            case "open_role_progress_current" -> "현재 직업의 세 갈래 성장과 두 기술 슬롯을 관리합니다.";
            case "open_role_skill_research" -> "기술 연구소에서 현재 직업의 기술 습득과 Z/X 장착만 관리합니다.";
            case "open_skill_test" -> "외부 시험장으로 이동해 분리된 직업·스킬 관리함에서 시험 설정을 관리합니다.";
            case "open_skill_test_roles" -> "금색 바닥 직업 관리함을 엽니다.";
            case "open_skill_test_skills" -> "청금석 바닥 스킬 관리함을 엽니다.";
            case "test_spawn" -> "외부 시험장의 고정 표적 여섯 개를 다시 배치합니다.";
            case "test_clear" -> "자신이 만든 시험 표적만 정리합니다.";
            case "test_exit" -> "시험 표적과 임시 장착을 정리하고 원래 위치로 복귀합니다.";
            case "open_fusion" -> "대장간에서 같은 종류·같은 등급 장비 세 개를 직접 선택해 합성합니다.";
            case "open_quick_chat" -> "접속 중인 수호단에게 보낼 빠른 신호를 엽니다.";
            case "open_dashboard", "open_mayor" -> "마을 회관의 직업 배치와 시설 관리 화면을 엽니다.";
            case "open_tower_control" -> "마을 회관에서 성벽과 네 종류 방어탑을 관리합니다.";
            case "open_funding" -> "마을 회관에서 개인 수호 주화로 공동 보급품을 조달합니다.";
            case "return_village" -> "전투 중이 아닐 때 마을 중앙 광장으로 귀환합니다.";
            case "claim_bread" -> "오늘의 무료 배급 식량을 받습니다. 일반 식량 구매는 이 배급으로 통합되었습니다.";
            case "buy_arrows" -> label + "\n수호 주화로 화살 묶음을 구매합니다.";
            case "sell_loot" -> "판매용으로 표시된 잡템만 안전하게 일괄 정산합니다.";
            case "open_item_sell" -> "보유한 게임 전용 장비·소모품·잡템을 하나씩 선택해 판매합니다.";
            case "open_forge_enhancement" -> "보유한 등급 장비를 선택해 개별 강화합니다.";
            case "open_equipment_shop" -> "장비·방어구·화살·전투 소모품 상점을 엽니다. 일반 식량은 일일 배급으로 통합됐고 장비 재고는 매일 바뀝니다.";
            case "forge_upgrade", "smithy_forge_upgrade" -> label + "\n강화할 등급 장비를 직접 선택합니다.";
            case "forge_combine" -> label + "\n같은 종류·같은 등급 장비 세 개를 직접 골라 상위 등급 하나로 합성합니다.";
            case "skill_learn" -> label + "\n수호 주화로 연구 단계를 높여 기술 피해와 재사용 효율을 강화합니다.";
            case "train" -> label + "\n병영 레벨에 따라 모든 경험치 획득량이 자동으로 증가합니다.";
            case "hire_mercenary" -> label + "\n수호 주화로 영구 용병을 고용합니다. 사망 전까지 저장과 재접속 후에도 유지됩니다.";
            case "tower_status" -> "활성화된 방어탑 종류와 현재 용병 수·정원을 확인합니다.";
            case "wall_status_local" -> "현재 성벽 내구도를 확인합니다. 시설 수리·강화는 회관에서 진행합니다.";
            case "use_infirmary" -> "의무소는 낮 동안 체력을 완전히 회복하고 단계별 전투 버프를 자동 제공합니다.";
            case "chat_ready" -> "모든 접속 플레이어에게 준비 완료 신호를 보냅니다.";
            case "chat_gate" -> "모든 접속 플레이어에게 북문 집결 신호를 보냅니다.";
            case "chat_repair" -> "모든 접속 플레이어에게 시설 수리 요청을 보냅니다.";
            case "chat_help" -> "모든 접속 플레이어에게 현재 위치 지원 요청을 보냅니다.";
            case "vote_yes" -> "현재 시간 진행 투표에 찬성합니다.";
            case "vote_no" -> "현재 시간 진행 투표에 반대합니다.";
            case "restart_previous" -> "패배한 밤을 취소하고 같은 날 낮 정비 시간으로 돌아갑니다.";
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
                || action.startsWith("tower_branch:")
                || action.startsWith("tower_upgrade:")
                || action.startsWith("select_role:")
                || action.startsWith("skill_node:")
                || action.startsWith("role_node:")
                || action.startsWith("role_skill_unlock:")
                || action.startsWith("gear:")
                || action.startsWith("funding:")
                || action.equals("buy_arrows")
                || action.equals("sell_loot")
                || action.equals("forge_upgrade")
                || action.equals("smithy_forge_upgrade")
                || action.equals("forge_combine")
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
        if (action.startsWith("role_skill_unlock:") || action.startsWith("research_skill_unlock:")) return "기술 습득";
        if (action.startsWith("role_skill_equip:") || action.startsWith("research_skill_equip:")) return "기술 장착";
        if (action.startsWith("test_choose:")) return "슬롯 선택";
        if (action.startsWith("test_equip:")) return "임시 장착";
        if (action.startsWith("repair:")) return "시설 수리";
        if (action.startsWith("upgrade:")) return "시설 강화";
        if (action.startsWith("tower_branch:")) return "분기 적용";
        if (action.startsWith("tower_upgrade:")) return "분기 강화";
        if (action.startsWith("funding:")) return "보급 조달";
        if (action.startsWith("gear:")) return "장비 구매";
        if (action.startsWith("forge_enhance:")) return "장비 강화";
        if (action.startsWith("test_cast:")) return "시험 시전";
        if (action.startsWith("sell_item:")) return "판매";
        if (action.startsWith("manage:")) return "수리·강화";
        if (action.startsWith("building:")) return "이용";
        if (action.equals("smithy_forge_upgrade") || action.equals("forge_upgrade")) return "장비 강화";
        if (action.equals("forge_combine") || action.equals("open_fusion")) return "장비 합성";
        if (action.startsWith("open_")) return "열기";
        if (action.startsWith("buy_") || action.startsWith("consumable:")) return "구매";
        if (action.equals("sell_loot")) return "판매";
        if (action.startsWith("restart_")) return "재시작";
        if (action.equals("return_village")) return "귀환";
        return "실행";
    }
}
