from pathlib import Path

saved_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleSavedData.java")
saved = saved_path.read_text(encoding="utf-8")
old = """        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId) || !person.aliveOn(day)) return;
            persons.set(i, person.withDeath(day));
            setDirty();
            return;
        }
"""
new = """        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId)) continue;
            if (!person.aliveOn(day)) return;
            persons.set(i, person.withDeath(day));
            setDirty();
            return;
        }
"""
if old in saved:
    saved = saved.replace(old, new, 1)
elif new not in saved:
    raise SystemExit("lifecycle markDeath pattern missing")
saved_path.write_text(saved, encoding="utf-8")

manager_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleManager.java")
manager = manager_path.read_text(encoding="utf-8")
old = """            if (household == null || year - line.lastBirthYear() < 2) continue;
"""
new = """            if (household == null
                    || (line.lastBirthYear() != Integer.MIN_VALUE
                    && year - line.lastBirthYear() < 2)) continue;
"""
if old in manager:
    manager = manager.replace(old, new, 1)
elif new not in manager:
    raise SystemExit("lifecycle birth spacing pattern missing")
manager_path.write_text(manager, encoding="utf-8")

workforce_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
workforce = workforce_path.read_text(encoding="utf-8")
if workforce.count("additionalLabor(level, node.id, node.role, day)") != 1:
    raise SystemExit("lifecycle labor contribution must occur exactly once")
old = """                \"Erden exterior resident {} of {} died permanently and no replacement labour was created\",
"""
new = """                \"Erden exterior resident {} of {} died permanently; lifecycle succession may fill the resulting vacancy\",
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce death log pattern missing")
workforce_path.write_text(workforce, encoding="utf-8")
