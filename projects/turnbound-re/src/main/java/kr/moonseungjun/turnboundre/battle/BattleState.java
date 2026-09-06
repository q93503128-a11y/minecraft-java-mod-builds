package kr.moonseungjun.turnboundre.battle;

public enum BattleState {
    NOT_IN_BATTLE,
    ENCOUNTER_OPEN,
    INTRO,
    ACTOR_READY,
    AWAIT_COMMAND,
    RESOLVING,
    CHECK_END,
    VICTORY,
    DEFEAT,
    REWARD,
    CLEANUP
}
