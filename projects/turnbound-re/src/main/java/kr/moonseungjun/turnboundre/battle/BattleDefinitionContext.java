package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;

import java.util.Map;

/** Immutable definition snapshot and participant roster captured when a data-driven battle opens. */
public record BattleDefinitionContext(
        DefinitionRegistry definitions,
        String definitionHash,
        Map<String, String> characterIdsByParticipant
) {
    public BattleDefinitionContext {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        if (definitionHash == null || !definitionHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("definitionHash must be lowercase SHA-256 hex");
        }
        if (characterIdsByParticipant == null || characterIdsByParticipant.isEmpty()) {
            throw new IllegalArgumentException("characterIdsByParticipant must not be empty");
        }
        characterIdsByParticipant = Map.copyOf(characterIdsByParticipant);
        for (Map.Entry<String, String> entry : characterIdsByParticipant.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("participant id must not be blank");
            if (entry.getValue() == null || entry.getValue().isBlank()) throw new IllegalArgumentException("character id must not be blank");
            CharacterDefinition character = definitions.characters().get(entry.getValue());
            if (character == null) throw new IllegalArgumentException("missing CharacterDefinition " + entry.getValue());
        }
    }

    public String characterId(String participantId) {
        return characterIdsByParticipant.get(participantId);
    }
}
