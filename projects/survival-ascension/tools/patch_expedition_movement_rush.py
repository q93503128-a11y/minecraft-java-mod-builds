#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
progress = ROOT / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java"
incident = ROOT / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncident.java"

s = progress.read_text(encoding="utf-8")
old_tick = '''    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel)) return;

        ExpeditionRegion current = currentRegion(player);
        if (current != null) ensureDiscovered(player, current);
        trackOceanVoyage(player, current);
    }
'''
new_tick = '''    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 5 != 0 || !(player.level() instanceof ServerLevel)) return;

        ExpeditionRegion current = currentRegion(player);
        if (player.tickCount % 20 == 0 && current != null) ensureDiscovered(player, current);
        trackOceanVoyage(player, current);
    }
'''
if old_tick not in s:
    raise SystemExit("onPlayerTick anchor drifted")
s = s.replace(old_tick, new_tick, 1)

old_voyage = '''    private static void trackOceanVoyage(ServerPlayer player, ExpeditionRegion current) {
        UUID uuid = player.getUUID();
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        VoyageState old = OCEAN_VOYAGE.get(uuid);
        OCEAN_VOYAGE.put(uuid, new VoyageState(dimension, pos.x, pos.z));
        if (current != ExpeditionRegion.OCEAN) return;
        if (!(player.isPassenger() || player.isSwimming() || player.isInWater())) return;
        if (old == null || !old.dimension.equals(dimension)) return;
        double dx = pos.x - old.x;
        double dz = pos.z - old.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.25D || distance > 24.0D) return;
        int amount = Math.max(1, (int) Math.floor(distance));
        addObjectiveProgress(player, ExpeditionRegion.OCEAN, ExpeditionAction.OCEAN_VOYAGE, amount);
        ExpeditionIncidentSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);
        ExpeditionOperationSystem.recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);
    }
'''
new_voyage = '''    private static void trackOceanVoyage(ServerPlayer player, ExpeditionRegion current) {
        UUID uuid = player.getUUID();
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        VoyageState old = OCEAN_VOYAGE.get(uuid);
        if (old == null || !old.dimension.equals(dimension)) {
            OCEAN_VOYAGE.put(uuid, new VoyageState(dimension, pos.x, pos.y, pos.z, 0.0D));
            return;
        }

        double dx = pos.x - old.x;
        double dy = pos.y - old.y;
        double dz = pos.z - old.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean voyageMovement = current == ExpeditionRegion.OCEAN
                && (player.isPassenger() || player.isSwimming() || player.isInWater())
                && distance >= 0.01D
                && distance <= 8.0D;
        double bank = current == ExpeditionRegion.OCEAN ? old.bank : 0.0D;
        if (voyageMovement) bank += distance;

        int amount = (int) Math.floor(bank);
        if (amount > 0) {
            bank -= amount;
            recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);
        }
        OCEAN_VOYAGE.put(uuid, new VoyageState(dimension, pos.x, pos.y, pos.z, bank));
    }
'''
if old_voyage not in s:
    raise SystemExit("trackOceanVoyage anchor drifted")
s = s.replace(old_voyage, new_voyage, 1)

old_state = '    private record VoyageState(ResourceKey<Level> dimension, double x, double z) {}\n'
new_state = '    private record VoyageState(ResourceKey<Level> dimension, double x, double y, double z, double bank) {}\n'
if old_state not in s:
    raise SystemExit("VoyageState anchor drifted")
s = s.replace(old_state, new_state, 1)
progress.write_text(s, encoding="utf-8")

i = incident.read_text(encoding="utf-8")
replacements = {
    'OCEAN_RUSH(ExpeditionRegion.OCEAN, "폭풍 항해", 900, ExpeditionAction.OCEAN_VOYAGE, 180),':
        'OCEAN_RUSH(ExpeditionRegion.OCEAN, "폭풍 항해", 900, ExpeditionAction.OCEAN_VOYAGE, 80),',
    'OCEAN_FRONTIER_RUSH(ExpeditionRegion.OCEAN, "외해 돌파", 900, ExpeditionAction.OCEAN_VOYAGE, 240),':
        'OCEAN_FRONTIER_RUSH(ExpeditionRegion.OCEAN, "외해 돌파", 900, ExpeditionAction.OCEAN_VOYAGE, 110),',
    'FROZEN_RUSH(ExpeditionRegion.FROZEN, "빙설 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 180),':
        'FROZEN_RUSH(ExpeditionRegion.FROZEN, "빙설 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 120),',
    'FROZEN_FRONTIER_RUSH(ExpeditionRegion.FROZEN, "눈보라 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 220),':
        'FROZEN_FRONTIER_RUSH(ExpeditionRegion.FROZEN, "눈보라 강행군", 900, ExpeditionAction.TRAVEL_DISTANCE, 150),',
    'END_RUSH(ExpeditionRegion.END, "공허 추적", 900, ExpeditionAction.TRAVEL_DISTANCE, 180),':
        'END_RUSH(ExpeditionRegion.END, "공허 추적", 900, ExpeditionAction.TRAVEL_DISTANCE, 120),',
}
for old, new in replacements.items():
    if old not in i:
        raise SystemExit(f"incident anchor drifted: {old}")
    i = i.replace(old, new, 1)
incident.write_text(i, encoding="utf-8")

# Targeted invariants: real swim distance is sampled at 4 Hz, fractional movement is retained,
# vertical water movement counts, teleports are rejected, and timed movement rushes are human-clearable.
p = progress.read_text(encoding="utf-8")
i = incident.read_text(encoding="utf-8")
for token in [
    "player.tickCount % 5 != 0",
    "double dy = pos.y - old.y;",
    "double bank = current == ExpeditionRegion.OCEAN ? old.bank : 0.0D;",
    "distance <= 8.0D",
    "recordAction(player, ExpeditionAction.OCEAN_VOYAGE, amount);",
    "new VoyageState(dimension, pos.x, pos.y, pos.z, bank)",
]:
    if token not in p:
        raise SystemExit(f"missing progression invariant: {token}")
for token in [
    'OCEAN_VOYAGE, 80)', 'OCEAN_VOYAGE, 110)',
    'TRAVEL_DISTANCE, 120)', 'TRAVEL_DISTANCE, 150)',
]:
    if token not in i:
        raise SystemExit(f"missing incident pacing invariant: {token}")
for forbidden in ["distance < 0.25D", "distance > 24.0D", "Math.max(1, (int) Math.floor(distance))"]:
    if forbidden in p:
        raise SystemExit(f"stale voyage logic remains: {forbidden}")

print("expedition movement rush patch PASS")
