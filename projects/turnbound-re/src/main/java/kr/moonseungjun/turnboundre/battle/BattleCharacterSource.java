package kr.moonseungjun.turnboundre.battle;

import java.util.UUID;

/** Resolves the immutable CharacterDefinition id assigned to one battle participant. */
@FunctionalInterface
public interface BattleCharacterSource {
    String characterId(UUID battleId, String participantId);
}
