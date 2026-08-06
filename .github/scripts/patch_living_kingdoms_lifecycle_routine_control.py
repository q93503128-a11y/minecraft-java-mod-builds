from pathlib import Path

lifecycle_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleManager.java")
lifecycle = lifecycle_path.read_text(encoding="utf-8")

needle = """    public static LaborContribution additionalLabor(
"""
method = """    public static boolean controlsRoutine(
            ServerLevel level,
            String residentId,
            long day) {
        ErdenExteriorWorkforceSavedData workforce = workforce(level);
        ErdenExteriorLifecycleSavedData lifecycle = lifecycle(level);
        ensureInitialized(lifecycle, workforce, day);
        ErdenExteriorLifecycleSavedData.Person person = lifecycle.person(residentId);
        return person != null
                && (!person.aliveOn(day)
                || person.retiredOn(day)
                || (!person.foundingWorker() && person.assignedWorker()));
    }

""" + needle
if "public static boolean controlsRoutine(" not in lifecycle:
    if lifecycle.count(needle) != 1:
        raise SystemExit("controlsRoutine insertion point missing")
    lifecycle = lifecycle.replace(needle, method, 1)

old = """        if (person == null || person.founder()) return;
"""
new = """        if (person == null) return;
"""
if old in lifecycle:
    lifecycle = lifecycle.replace(old, new, 1)
elif new not in lifecycle:
    raise SystemExit("lifecycle interaction founder guard missing")

old = """        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.foundingWorker()) people.put(person.name(), person);
        }
"""
new = """        for (ErdenExteriorLifecycleSavedData.Person person : lifecycle.persons()) {
            if (!person.foundingWorker() || person.retiredOn(day)) {
                people.put(person.name(), person);
            }
        }
"""
if old in lifecycle:
    lifecycle = lifecycle.replace(old, new, 1)
elif new not in lifecycle:
    raise SystemExit("lifecycle routine ownership map missing")

lifecycle_path.write_text(lifecycle, encoding="utf-8")

workforce_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
workforce = workforce_path.read_text(encoding="utf-8")

old = """    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
"""
new = """    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce interaction cancellation guard missing")

old = """            ResidentRef reference = references.get(villager.getName().getString());
            if (reference == null || workforce.isDead(reference.resident().id())) continue;
            boolean working = reference.resident().worker()
"""
new = """            ResidentRef reference = references.get(villager.getName().getString());
            if (reference == null || workforce.isDead(reference.resident().id())) continue;
            if (ErdenExteriorLifecycleManager.controlsRoutine(
                    level, reference.resident().id(), day)) continue;
            boolean working = reference.resident().worker()
"""
if old in workforce:
    workforce = workforce.replace(old, new, 1)
elif new not in workforce:
    raise SystemExit("workforce routine ownership guard missing")

workforce_path.write_text(workforce, encoding="utf-8")
