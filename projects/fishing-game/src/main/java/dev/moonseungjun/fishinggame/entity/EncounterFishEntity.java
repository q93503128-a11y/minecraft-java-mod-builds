package dev.moonseungjun.fishinggame.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.level.Level;

public final class EncounterFishEntity extends Salmon {
    private static final EntityDataAccessor<String> SPECIES_ID =
            SynchedEntityData.defineId(EncounterFishEntity.class, EntityDataSerializers.STRING);

    public EncounterFishEntity(EntityType<? extends EncounterFishEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(SPECIES_ID, "bluegill");
    }

    public String speciesId() {
        return entityData.get(SPECIES_ID);
    }

    public void setSpeciesId(String speciesId) {
        entityData.set(SPECIES_ID, speciesId == null || speciesId.isBlank() ? "bluegill" : speciesId);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
