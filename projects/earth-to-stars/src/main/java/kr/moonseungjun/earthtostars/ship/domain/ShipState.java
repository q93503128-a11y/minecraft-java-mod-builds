package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ShipState {
    private final ShipId shipId;
    private final UUID ownerId;
    private final Map<UUID, CrewRole> crewAssignments = new LinkedHashMap<>();
    private final Map<String, ModuleSlot> slots = new LinkedHashMap<>();
    private final Map<UUID, ModuleInstance> modules = new LinkedHashMap<>();

    private ShipState(ShipId shipId, UUID ownerId, Collection<ModuleSlot> initialSlots) {
        this.shipId = Objects.requireNonNull(shipId, "shipId");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        for (ModuleSlot slot : initialSlots) {
            Objects.requireNonNull(slot, "slot");
            if (slots.putIfAbsent(slot.id(), slot) != null) {
                throw new IllegalArgumentException("duplicate slot: " + slot.id());
            }
        }
    }

    public static ShipState create(ShipId shipId, UUID ownerId, Collection<ModuleSlot> slots) {
        return new ShipState(shipId, ownerId, slots);
    }

    public static ShipState restore(
            ShipId shipId,
            UUID ownerId,
            Collection<ModuleSlot> slots,
            Map<UUID, CrewRole> crewAssignments,
            Collection<ModuleInstance> modules,
            ModuleCatalog catalog
    ) {
        ShipState state = new ShipState(shipId, ownerId, slots);
        crewAssignments.forEach((playerId, role) -> state.restoreRole(playerId, role));
        for (ModuleInstance module : modules) {
            state.installRestored(catalog.require(module.definitionId()), module);
        }
        return state;
    }

    public ShipId shipId() {
        return shipId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public CrewRole roleOf(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (ownerId.equals(playerId)) {
            return CrewRole.OWNER;
        }
        return crewAssignments.getOrDefault(playerId, CrewRole.GUEST);
    }

    public boolean can(UUID playerId, ShipPermission permission) {
        return ShipPermissionPolicy.allows(roleOf(playerId), Objects.requireNonNull(permission, "permission"));
    }

    public void assignRole(UUID actorId, UUID playerId, CrewRole role) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
        if (!ownerId.equals(actorId)) {
            throw new SecurityException("only the ship owner may change crew roles");
        }
        if (ownerId.equals(playerId)) {
            throw new IllegalArgumentException("owner role is implicit and cannot be reassigned");
        }
        if (role == CrewRole.OWNER) {
            throw new IllegalArgumentException("OWNER cannot be assigned through crew policy");
        }
        if (role == CrewRole.GUEST) {
            crewAssignments.remove(playerId);
        } else {
            crewAssignments.put(playerId, role);
        }
    }

    public void installModule(UUID actorId, ModuleDefinition definition, ModuleInstance instance) {
        requirePermission(actorId, ShipPermission.MODULE_MANAGE);
        validateInstall(definition, instance);
        modules.put(instance.instanceId(), instance);
    }

    public ModuleInstance removeModule(UUID actorId, UUID instanceId) {
        requirePermission(actorId, ShipPermission.MODULE_MANAGE);
        ModuleInstance removed = modules.remove(instanceId);
        if (removed == null) {
            throw new IllegalArgumentException("unknown module instance: " + instanceId);
        }
        return removed;
    }

    public Optional<ModuleInstance> module(UUID instanceId) {
        return Optional.ofNullable(modules.get(instanceId));
    }

    public Map<String, ModuleSlot> slots() {
        return Collections.unmodifiableMap(slots);
    }

    public Map<UUID, ModuleInstance> modules() {
        return Collections.unmodifiableMap(modules);
    }

    public Map<UUID, CrewRole> crewAssignments() {
        return Collections.unmodifiableMap(crewAssignments);
    }

    private void installRestored(ModuleDefinition definition, ModuleInstance instance) {
        validateInstall(definition, instance);
        modules.put(instance.instanceId(), instance);
    }

    private void restoreRole(UUID playerId, CrewRole role) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
        if (ownerId.equals(playerId) || role == CrewRole.OWNER) {
            throw new IllegalArgumentException("invalid persisted owner/crew role mapping");
        }
        if (role == CrewRole.CREW) {
            crewAssignments.put(playerId, role);
        }
    }

    private void validateInstall(ModuleDefinition definition, ModuleInstance instance) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(instance, "instance");
        if (!definition.id().equals(instance.definitionId())) {
            throw new IllegalArgumentException("module instance definition does not match supplied definition");
        }
        if (modules.containsKey(instance.instanceId())) {
            throw new IllegalArgumentException("duplicate module instance: " + instance.instanceId());
        }
        ModuleSlot slot = slots.get(instance.slotId());
        if (slot == null) {
            throw new IllegalArgumentException("unknown slot: " + instance.slotId());
        }
        if (slot.type() != definition.slotType()) {
            throw new IllegalArgumentException("module " + definition.id() + " is incompatible with slot " + slot.id());
        }
        if (definition.sizeClass() > slot.sizeClass()) {
            throw new IllegalArgumentException("module " + definition.id() + " exceeds slot size class");
        }
        boolean occupied = modules.values().stream().anyMatch(existing -> existing.slotId().equals(slot.id()));
        if (occupied) {
            throw new IllegalArgumentException("slot already occupied: " + slot.id());
        }
    }

    private void requirePermission(UUID actorId, ShipPermission permission) {
        if (!can(actorId, permission)) {
            throw new SecurityException("player lacks " + permission + " permission for ship " + shipId);
        }
    }
}
