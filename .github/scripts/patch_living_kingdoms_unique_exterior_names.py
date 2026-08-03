from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
text = path.read_text(encoding="utf-8")
old_worker = """                    String name = familyName + " " + GIVEN_NAMES.get(
                            Math.floorMod(globalWorker, GIVEN_NAMES.size()));
"""
new_worker = """                    String name = familyName + " " + GIVEN_NAMES.get(
                            Math.floorMod(globalHousehold * 3 + adultSlot, GIVEN_NAMES.size()));
"""
old_dependent = """                String dependentName = familyName + " " + GIVEN_NAMES.get(
                        Math.floorMod(globalWorker + globalDependent + 5, GIVEN_NAMES.size()));
"""
new_dependent = """                String dependentName = familyName + " " + GIVEN_NAMES.get(
                        Math.floorMod(globalHousehold * 3 + 2, GIVEN_NAMES.size()));
"""
if text.count(old_worker) != 1 or text.count(old_dependent) != 1:
    raise SystemExit("resident name patterns not found exactly once")
text = text.replace(old_worker, new_worker).replace(old_dependent, new_dependent)
path.write_text(text, encoding="utf-8")
