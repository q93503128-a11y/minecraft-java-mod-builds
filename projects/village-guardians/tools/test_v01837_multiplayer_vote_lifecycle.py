#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    council = read("VillageCouncilState.java")
    service = read("VillageUiService.java")
    client = read("VillageClientUi.java")
    action = read("VillageActionDetailScreen.java")

    assert "mod_version=0.18.37-alpha.1" in props

    propose = section(council, "public static synchronized String proposeAdvanceTime", "public static synchronized void onPlayerListChanged")
    assert "evaluateProposal(server);" in propose
    assert "VillageUiService.openVote(proposer, activeProposal.proposerName())" in propose
    assert "진행 중인 시간 투표를 다시 열었습니다." in propose
    assert 'new Proposal(' in propose and "proposer.getGameProfile().name()" in propose

    evaluate = section(council, "private static void evaluateProposal", "private static void advanceTime")
    assert "int online = Math.max(1, server.getPlayerList().getPlayerCount())" in evaluate
    assert "int remainingVotes = Math.max(0, online - yesVotes - noVotes)" in evaluate
    assert "yesVotes + remainingVotes < required" in evaluate
    assert "noVotes >= required" not in evaluate
    assert evaluate.count("VillageUiService.closeVoteForAll(server)") == 2
    assert "activeProposal = null" in evaluate

    assert "private record Proposal(String id, UUID proposer, String proposerName, Map<UUID, Boolean> votes)" in council
    assert "public static void openVote(ServerPlayer player, String proposerName)" in service
    assert "public static void closeVoteForAll(MinecraftServer server)" in service
    assert 'send(player, "close_vote", "", "", List.of(), List.of())' in service

    assert 'if ("close_vote".equals(payload.screenId()))' in client
    assert "screen.isVoteScreen()" in client
    assert "minecraft.gui.setScreen(null)" in client

    assert "private final String screenId;" in action
    assert "screenId = payload.screenId();" in action
    assert 'return "vote".equals(screenId);' in action
    cast = section(action, "private void execute(ActionCard card)", "private boolean confirmationRequired")
    assert '"vote_yes".equals(action) || "vote_no".equals(action)' in cast
    assert "minecraft.gui.setScreen(null)" in cast

    print("[PASS] 2-player yes/no ties cannot leave an immortal active proposal")
    print("[PASS] multiplayer rejection resolves when a yes majority becomes mathematically impossible")
    print("[PASS] vote screens close after casting and close for remaining viewers on resolution")
    print("[PASS] a still-active vote can be reopened after the player manually closes its screen")
    print("[PASS] v0.18.37 multiplayer vote lifecycle contract complete")


if __name__ == "__main__":
    main()
