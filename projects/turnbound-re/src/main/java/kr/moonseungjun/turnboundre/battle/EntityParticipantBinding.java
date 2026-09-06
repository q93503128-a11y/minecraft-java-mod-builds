package kr.moonseungjun.turnboundre.battle;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Thin Minecraft adapter identity. The deterministic battle core stores participant ids;
 * this binding is the only identity bridge needed by BattleManager.
 */
public record EntityParticipantBinding(String participantId, UUID entityId) {
    public EntityParticipantBinding {
        if (participantId == null || participantId.isBlank()) {
            throw new IllegalArgumentException("participantId must not be blank");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
    }

    public static EntityParticipantBinding fromEntity(String participantId, Entity entity) {
        if (entity == null) throw new IllegalArgumentException("entity must not be null");
        return new EntityParticipantBinding(participantId, entity.getUUID());
    }
}
