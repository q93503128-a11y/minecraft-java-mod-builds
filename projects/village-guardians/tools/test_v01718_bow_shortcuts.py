#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def check(label: str, condition: bool) -> None:
    if not condition:
        raise AssertionError(label)
    print(f"[PASS] {label}")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    ability = read("VillageRoleAbilitySystem.java")
    guard = read("VillageGuardians.java")
    keys = read("VillageClientKeys.java")
    inventory = read("VillageInventoryPanel.java")
    quick = read("VillageQuickChatScreen.java")
    starter = read("VillageStarterKit.java")
    controller = read("VillageUiController.java")
    skills = read("VillageRoleSkillSystem.java")
    lang = (ROOT / "src/main/resources/assets/villageguardians/lang/ko_kr.json").read_text(encoding="utf-8")

    check("v0.18.2-alpha.1 version is active", "mod_version=" in props)
    check(
        "신속 삼연사는 실제 활 사용을 조기에 끝내고 완충 발사를 보장합니다",
        all(token in ability for token in [
            "RAPID_DRAW_TICKS", "tickRapidBow", "player.releaseUsingItem()",
            "event.setCharge(20)", "completeAt = Math.max(5, 9 - Math.min(4, specialRank))",
        ])
        and "LivingEntityUseItemEvent" not in ability
        and "LivingEntityUseItemEvent" not in guard,
    )
    check(
        "홍염탄 접촉 판정은 폭발 반경과 분리되어 작은 값만 사용합니다",
        "fireOrbContacts" in ability
        and "fireOrbContactPadding" in ability
        and "target.getBoundingBox().inflate(padding).contains(position)" in ability
        and "targetsNear(level, owner, position, moving.radius(), 40)" in ability,
    )
    check(
        "폐지된 호출기 U 중복 단축키가 등록·전송·언어 파일에서 제거되었습니다",
        "CALLER" not in keys
        and "GLFW.GLFW_KEY_U" not in keys
        and "key.villageguardians.caller" not in lang,
    )
    check(
        "클라이언트 화면은 하드코딩된 글자 대신 현재 지정된 키 이름을 표시합니다",
        all(token in keys for token in [
            "quickCommunicationKeyName", "statusKeyName", "growthKeyName",
            "roleProgressKeyName", "getTranslatedKeyMessage",
        ])
        and "VillageClientKeys.compactSummary()" in inventory
        and "VillageClientKeys.statusKeyName()" in inventory
        and "VillageClientKeys.growthKeyName()" in inventory
        and "VillageClientKeys.roleProgressKeyName()" in inventory
        and "VillageClientKeys.quickCommunicationKeyName()" in inventory
        and "VillageClientKeys.quickCommunicationKeyName()" in quick,
    )
    check(
        "서버 안내는 알 수 없는 클라이언트 키를 단정하지 않고 조작 설정을 안내합니다",
        "설정 > 조작 > 마을 지키기" in starter
        and "설정 > 조작 > 마을 지키기" in controller
        and "B/U" not in starter
        and "기본키 Z" not in starter
        and "U 빠른 통신" not in controller,
    )
    check(
        "단축키 기능 의미가 V 통신·H 상태·J 공통 성장·K 현재 직업 성장으로 일치합니다",
        'QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_V)' in keys
        and 'STATUS = key("status", GLFW.GLFW_KEY_H)' in keys
        and 'GROWTH = key("personal_progress", GLFW.GLFW_KEY_J)' in keys
        and 'ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K)' in keys
        and 'consume(QUICK_COMMUNICATION, "open_quick_chat")' in keys
        and 'consume(STATUS, "open_status")' in keys
        and 'consume(GROWTH, "open_skill_tree")' in keys
        and 'consume(ROLE_PROGRESS, "open_role_progress_current")' in keys
        and '"key.villageguardians.personal_progress": "공통 성장 열기"' in lang
        and '"key.villageguardians.role_progress": "현재 직업 성장 열기"' in lang,
    )
    check(
        "신속 삼연사 설명이 실제 자동 완충·발사 동작과 일치합니다",
        "다음 활은 빠르게 자동 완충·발사" in skills,
    )


if __name__ == "__main__":
    main()
