#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
PATH = JAVA / "VillageMercenaryDeploymentSystem.java"

text = PATH.read_text(encoding="utf-8")
old = '''    public static void reset() { ticks = 0; }

    public static void openCommand(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)
                && !VillageLocationRules.isNearTownHall(player)) {
'''
new = '''    public static void reset() { ticks = 0; }

    public static boolean canOpenAt(ServerPlayer player) {
        return player != null
                && (VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)
                || VillageLocationRules.isNearTownHall(player));
    }

    public static void openCommand(ServerPlayer player) {
        if (!canOpenAt(player)) {
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f"mercenary authorization helper: expected one match, found {count}")
PATH.write_text(text.replace(old, new, 1), encoding="utf-8")
print("[PATCH] mercenary command/deployment authorization now shares one server-side helper")
