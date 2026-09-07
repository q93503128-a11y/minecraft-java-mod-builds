from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Alpha.127 patch anchor missing: {label}")
    return text.replace(old, new, 1)


# Version authority.
props = read("gradle.properties")
props = replace_once(props, "mod_version=0.1.0-alpha.126", "mod_version=0.1.0-alpha.127", "gradle version")
props += "\n# Alpha.127 finalization: territory-network operations visibility and current completion-status reconciliation.\n"
write("gradle.properties", props)

# Operations summary: derive territory diversity only from the already-synchronized outpost context.
summary_path = "src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsSummary.java"
summary = read(summary_path)
summary = replace_once(summary,
    "import java.util.ArrayList;\nimport java.util.List;",
    "import java.util.ArrayList;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Set;",
    "operations imports")
summary = replace_once(summary,
    "        int militaryUpgradeBacklog,\n        int cityInvestmentBacklog,\n        int outposts,",
    "        int militaryUpgradeBacklog,\n        int cityInvestmentBacklog,\n        int productiveOutpostDiversity,\n        int territoryNetworkLevel,\n        int outposts,",
    "operations record fields")
summary = replace_once(summary,
    "        int militaryUpgradeBacklog = 0;\n        int cityInvestmentBacklog = 0;",
    "        int militaryUpgradeBacklog = 0;\n        int cityInvestmentBacklog = 0;\n        Set<String> productiveOutpostRoles = new HashSet<>();",
    "operations counters")
summary = replace_once(summary,
    "        for (SettlementContextTarget target : snapshot.context().targets()) {\n            if (!\"building\".equals(target.kind())) continue;",
    "        for (SettlementContextTarget target : snapshot.context().targets()) {\n            if (\"outpost\".equals(target.kind())) {\n                collectProductiveOutpostRole(productiveOutpostRoles, target.detail());\n                continue;\n            }\n            if (!\"building\".equals(target.kind())) continue;",
    "operations outpost pass")
summary = replace_once(summary,
    "        List<String> alerts = new ArrayList<>();",
    "        int productiveOutpostDiversity = productiveOutpostRoles.size();\n        int territoryNetworkLevel = territoryNetworkLevel(snapshot.tier(), productiveOutpostDiversity);\n\n        List<String> alerts = new ArrayList<>();",
    "operations territory counters")
summary = replace_once(summary,
    "        } else if (militaryUpgradeBacklog > 0) {\n            priority = \"방어망 개량 투자\";\n        } else if (snapshot.nextGoal() != null && !snapshot.nextGoal().isBlank()) {",
    "        } else if (militaryUpgradeBacklog > 0) {\n            priority = \"방어망 개량 투자\";\n        } else if (matureTerritoryTier(snapshot.tier()) && territoryNetworkLevel < 3) {\n            priority = \"생산 특화 전초 다양화 · 영지망 \" + territoryNetworkLevel + \"/3\";\n        } else if (snapshot.nextGoal() != null && !snapshot.nextGoal().isBlank()) {",
    "operations territory priority")
summary = replace_once(summary,
    "                guardPosts, watchtowers, barracks, citadels, militaryUpgradeBacklog, cityInvestmentBacklog,\n                snapshot.context().outpostCount(), priority, alerts);",
    "                guardPosts, watchtowers, barracks, citadels, militaryUpgradeBacklog, cityInvestmentBacklog,\n                productiveOutpostDiversity, territoryNetworkLevel,\n                snapshot.context().outpostCount(), priority, alerts);",
    "operations constructor")
summary = replace_once(summary,
    "    private static String buildingId(String key) {",
    "    private static void collectProductiveOutpostRole(Set<String> roles, String detail) {\n        if (detail == null) return;\n        if (detail.contains(\"역할 · 벌목\")) roles.add(\"lumber\");\n        else if (detail.contains(\"역할 · 농업\")) roles.add(\"agriculture\");\n        else if (detail.contains(\"역할 · 채석\")) roles.add(\"quarry\");\n        else if (detail.contains(\"역할 · 광업\")) roles.add(\"mining\");\n    }\n\n    private static boolean matureTerritoryTier(String tier) {\n        return \"영지\".equals(tier) || \"개척 수도\".equals(tier);\n    }\n\n    private static int territoryNetworkLevel(String tier, int diversity) {\n        if (!matureTerritoryTier(tier)) return 0;\n        return Math.min(3, Math.max(0, diversity - 1));\n    }\n\n    private static String buildingId(String key) {",
    "operations territory helpers")
write(summary_path, summary)

# Operations screen: expose the real RTS territory composition and why diverse outposts matter.
screen_path = "src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementOperationsScreen.java"
screen = read(screen_path)
screen = replace_once(screen,
    "                \"성채 \" + summary.citadels() + \" · 전초 \" + summary.outposts(),\n                \"군사 개량 가능 \" + summary.militaryUpgradeBacklog() + \"곳\"));",
    "                \"전초 \" + summary.outposts() + \" · 생산 특화 \" + summary.productiveOutpostDiversity()\n                        + \"/4 · 영지망 \" + summary.territoryNetworkLevel() + \"/3\",\n                \"군사 개량 가능 \" + summary.militaryUpgradeBacklog() + \"곳\"));",
    "operations territory card")
screen = replace_once(screen,
    "            text = \"치명적 병목 없음 · 시설 개량은 월드의 해당 작업 지점에서 직접 투자\";",
    "            text = \"치명적 병목 없음 · 서로 다른 생산 특화 전초가 영지망과 후반 경제를 강화\";",
    "operations footer")
write(screen_path, screen)

# Guide: explain territory diversity without adding another menu or management layer.
guide_path = "src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementGuideScreen.java"
guide = read(guide_path)
guide = replace_once(guide,
    "                    \"M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.\",\n                    \"창고·수레 정거장은 빈손 웅크리기+저장통 우클릭으로 물류 II~III를 확장합니다.\",\n                    \"확장된 창고는 실물 저장통을 늘리고, 수레 정거장은 전초 생산 화물 처리량을 높입니다.\",\n                    \"언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.\");",
    "                    \"M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.\",\n                    \"창고·수레 정거장은 빈손 웅크리기+저장통 우클릭으로 물류 II~III를 확장합니다.\",\n                    \"벌목·농업·채석·광업 전초를 다양화하면 영지망이 최대 III까지 성장합니다.\",\n                    \"영지망은 시장·수리·제작·생산 화물을 강화하며 언로드 지역은 강제 로드하지 않습니다.\");",
    "guide territory page")
write(guide_path, guide)

# Runtime source marker only; no new server authority is introduced.
entry_path = "src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java"
entry = read(entry_path)
entry = entry.replace("// Alpha.125 canonical/integrated validation trigger after RTS operations visibility.",
                      "// Alpha.127 finalization: territory-network operations visibility; no new simulation authority.")
write(entry_path, entry)

# README/project direction.
readme = read("README.md")
readme = replace_once(readme, "## Current version: 0.1.0-alpha.126", "## Current version: 0.1.0-alpha.127", "README version")
alpha127 = '''\n## Alpha.127 territory-network finalization\n\n- The existing M > 운영 screen now exposes productive-outpost diversity and the real Domain territory-network level (0-3).\n- Only the four physical productive roles count: lumber, agriculture, quarry and mining. Repeating one specialization never increases network level.\n- Domain/Frontier Capital settlements with unfinished diversity receive a low-priority operations recommendation only after more urgent construction, staffing, logistics and paid-upgrade bottlenecks are clear.\n- The guide now explains the already-existing payoff: territory diversity strengthens market payout, workshop repair, advanced forging and productive outpost freight.\n- This pass creates no currency, tax, research tree, virtual cargo, force-loading, second transport authority or new settlement save ledger.\n- Repository-side core implementation is now in final hardening/acceptance phase. Real-client visual/play, long survival, save/reload edge cases and two-player acceptance remain required before calling original v0.2 complete.\n\n'''
readme = replace_once(readme, "## Functional building families", alpha127 + "## Functional building families", "README Alpha.127 section")
write("README.md", readme)

project = read("PROJECT.md")
project = project.replace("Current implementation delta: **0.1.0-alpha.79**.", "Current implementation delta: **0.1.0-alpha.127**.")
project += '''\n\n### Alpha.127 final implementation-status reconciliation\n\nFrontier's repository-side core is now in final hardening/acceptance phase rather than broad feature construction. The physical settlement, six-tier growth, construction, production, paid facility upgrades, logistics, roads/outposts, exploration feedback, supplied military, civil works, city investment and RTS operations loop all have concrete implementations. The remaining release blockers are dominated by real-client visual/play acceptance, long-session pacing, save/reload/unload edge cases, two-player shared-state acceptance and full companion-stack runtime acceptance. Optional true Xaero marker sync, rare-NPC-specific value and larger monumental engineering remain non-blocking unless stable APIs or real-play evidence justify them.\n'''
write("PROJECT.md", project)

# Current completion reconciliation: retain historical audit evidence, but stop its old Alpha.79 header from misleading future chats.
gap = read("COMPLETION_GAP_AUDIT.md")
gap = gap.replace("현재 구현 기준: `0.1.0-alpha.79`", "현재 구현 기준: `0.1.0-alpha.127`")
if "## 12. Alpha.127 현재 완성도 재조정" not in gap:
    gap += '''\n\n## 12. Alpha.127 현재 완성도 재조정\n\nAlpha.79 이후의 구현으로 이 문서 중 과거의 `미구현` 서술 상당수는 역사 기록이 되었다. 현재 정본 판단은 다음과 같다.\n\n- 핵심 게임 루프 구현: **거의 완료** — 물리 정착지, 6단계 성장, 생산/물류/군사/영토, 유료 시설 개량, 도시 투자, 탐험 되먹임, 장교량/터널/선택영역 토목까지 실제 시스템이 존재한다.\n- 남은 필수 작업의 중심: **구현 추가가 아니라 acceptance** — 장시간 생존 페이싱, save/reload·청크 unload 반복, 실제 그래픽 클라이언트, 2인 동시 조작/재접속, 전체 companion stack 실런타임.\n- Alpha.127은 기존 영지망을 M > 운영에서 직접 노출해 서로 다른 벌목/농업/채석/광업 전초가 후반 경제를 강화한다는 RTS 의사결정을 명시한다.\n- true Xaero marker, rare-NPC-specific value, 더 거대한 기념비급 토목은 **완료 판정을 막는 필수 구현으로 보지 않는다**. 안정적 API 또는 실제 플레이 필요성이 확인될 때만 선택적으로 확장한다.\n- 자동 source/docs/build/JAR 검증이 성공해도 실제 Minecraft 장시간/멀티 acceptance를 대신하지 않는다.\n\n따라서 Alpha.127 이후에는 새 대형 시스템을 계속 추가하기보다 실제 플레이에서 발견되는 결함을 수정하고 최종 acceptance를 닫는 것이 기본 방향이다.\n'''
write("COMPLETION_GAP_AUDIT.md", gap)

status = '''# Frontier Settlement — Alpha.127 final implementation status\n\n## Verdict\n\nRepository-side core implementation is **nearly complete and in final hardening/acceptance phase**. This is not a claim that original v0.2 has passed real-player acceptance.\n\n## Implemented core\n\n- one shared server-authoritative settlement and six settlement tiers;\n- physical ItemStack resource authority, construction hauling and bounded terrain grading;\n- production workers, ecology-aware farm/lumber/quarry/mine loops and per-facility paid upgrades;\n- central warehouse/cart logistics upgrades and one physical long-distance outpost transporter authority;\n- roads, bridges, bounded tunnels, outposts and four productive regional specializations;\n- exploration/conquest feedback, market/workshop/advanced-forge benefits and territory-network feedback;\n- guard/watch/barracks/citadel military investment, supplied humanoid forces and external-weapon physical armament;\n- civic/trade city investment and late-game resource sinks;\n- selected-area civil works, imported real fill, retaining walls and bounded large crossings;\n- M-screen construction/operations/location/guide UX with live production/logistics/upgrade state.\n\n## Remaining release blockers\n\n1. real graphical-client acceptance across practical GUI scales and companion key/UI interaction;\n2. long survival pacing and resource-sink balance;\n3. save/reload + loaded/unloaded route/cargo/project repetition without loss or duplication;\n4. actual two-player shared-state, simultaneous confirmation and reconnect acceptance;\n5. full locked companion client/server fresh-world runtime acceptance.\n\n## Non-blocking optional breadth\n\n- true Xaero marker synchronization if a stable supported API appears;\n- rare-NPC-specific settlement value if a safe soft integration seam exists;\n- larger monumental engineering only if real play shows the current bridge/tunnel/civil envelope is insufficient.\n\n## Direction after Alpha.127\n\nDo not add a large new management layer by default. Prioritize real-play defects, pacing, compatibility and final acceptance.\n'''
write("FINAL_IMPLEMENTATION_STATUS_ALPHA127.md", status)

# Companion manifests follow the project-owned Frontier version; third-party pins/hashes are unchanged.
for rel in ("COMPANION_LOCK.json", "companion-testpack/resolved-lock.client.json", "companion-testpack/resolved-lock.server.json"):
    text = read(rel)
    text = text.replace('"frontier_settlement": "0.1.0-alpha.126"', '"frontier_settlement": "0.1.0-alpha.127"')
    write(rel, text)

# Cumulative verifier version + Alpha.127 invariants.
verify = read("tools/test_current_source.py")
verify = replace_once(verify, 'require("mod_version=0.1.0-alpha.126" in gradle, "current verifier/version drift")',
                      'require("mod_version=0.1.0-alpha.127" in gradle, "current verifier/version drift")',
                      "source verifier version")
if "# Alpha.127 territory-network finalization." not in verify:
    verify += '''\n\n# Alpha.127 territory-network finalization.\nrequire("productiveOutpostDiversity" in operations_summary and "territoryNetworkLevel" in operations_summary,\n        "operations summary does not expose productive-outpost diversity/territory network")\nrequire("collectProductiveOutpostRole" in operations_summary and "역할 · 벌목" in operations_summary\n        and "역할 · 농업" in operations_summary and "역할 · 채석" in operations_summary and "역할 · 광업" in operations_summary,\n        "territory network presentation no longer derives from the four real productive outpost roles")\nrequire("생산 특화" in operations_screen and "영지망" in operations_screen,\n        "operations screen hides the existing territory-network decision")\nrequire("벌목·농업·채석·광업 전초" in guide_screen and "영지망" in guide_screen,\n        "guide does not explain territory diversity")\nrequire((ROOT / "FINAL_IMPLEMENTATION_STATUS_ALPHA127.md").is_file(),\n        "Alpha.127 final implementation status document missing")\n'''
write("tools/test_current_source.py", verify)

print("Applied Frontier Settlement Alpha.127 finalization patch")
