from pathlib import Path

main_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java")
main = main_path.read_text(encoding="utf-8")

import_needle = "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorLifecycleManager;\n"
import_line = import_needle + "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorEstateManager;\n"
if "import kr.moonseungjun.livingkingdoms.world.ErdenExteriorEstateManager;" not in main:
    if main.count(import_needle) != 1:
        raise SystemExit("estate manager import insertion point missing")
    main = main.replace(import_needle, import_line, 1)

tick_needle = "        ErdenExteriorWorkforceManager.onServerTick(event);\n"
tick_block = tick_needle + "        ErdenExteriorEstateManager.onServerTick(event);\n"
if "ErdenExteriorEstateManager.onServerTick(event);" not in main:
    if main.count(tick_needle) != 1:
        raise SystemExit("estate manager tick insertion point missing")
    main = main.replace(tick_needle, tick_block, 1)
main_path.write_text(main, encoding="utf-8")

manager_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleManager.java")
manager = manager_path.read_text(encoding="utf-8")

old_call = """        for (long day = first; day <= currentDay; day++) {
            processDay(lifecycle, workforce, day);
        }
"""
new_call = """        for (long day = first; day <= currentDay; day++) {
            processDay(level, lifecycle, workforce, day);
        }
"""
if old_call in manager:
    manager = manager.replace(old_call, new_call, 1)
elif new_call not in manager:
    raise SystemExit("processDay level call pattern missing")

old_signature = """    private static void processDay(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
"""
new_signature = """    private static void processDay(
            ServerLevel level,
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
"""
if old_signature in manager:
    manager = manager.replace(old_signature, new_signature, 1)
elif new_signature not in manager:
    raise SystemExit("processDay signature pattern missing")

old_birth_call = """            BirthResult births = processBirths(
                    people, lines, workforce, day, year, nextSequence);
"""
new_birth_call = """            BirthResult births = processBirths(
                    level, people, lines, workforce, day, year, nextSequence);
"""
if old_birth_call in manager:
    manager = manager.replace(old_birth_call, new_birth_call, 1)
elif new_birth_call not in manager:
    raise SystemExit("real birth call pattern missing")

signature = """    private static BirthResult processBirths(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            ErdenExteriorWorkforceSavedData workforce,
            long day,
            int year,
            int nextSequence) {
"""
overloads = """    private static BirthResult processBirths(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            ErdenExteriorWorkforceSavedData workforce,
            long day,
            int year,
            int nextSequence) {
        return processBirths(null, people, lines, workforce, day, year, nextSequence);
    }

    private static BirthResult processBirths(
            ServerLevel level,
            List<ErdenExteriorLifecycleSavedData.Person> people,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines,
            ErdenExteriorWorkforceSavedData workforce,
            long day,
            int year,
            int nextSequence) {
"""
if "return processBirths(null, people, lines, workforce, day, year, nextSequence);" not in manager:
    if manager.count(signature) != 1:
        raise SystemExit("processBirths signature insertion point missing")
    manager = manager.replace(signature, overloads, 1)

household_gate = """            if (household == null
                    || (line.lastBirthYear() != Integer.MIN_VALUE
                    && year - line.lastBirthYear() < 2)) continue;
"""
household_gate_new = household_gate + """            if (level != null
                    && !ErdenExteriorEstateManager.birthAllowed(level, household.id())) continue;
"""
if "ErdenExteriorEstateManager.birthAllowed(level, household.id())" not in manager:
    if manager.count(household_gate) != 1:
        raise SystemExit("estate birth gate insertion point missing")
    manager = manager.replace(household_gate, household_gate_new, 1)

old_message = """        player.sendSystemMessage(Component.literal(
                "§6[" + person.name() + "] §f" + person.generation() + "세대 " + standing
                        + ", " + ageYears(person, day) + "세, " + work + "입니다."));
"""
new_message = """        String estate = ErdenExteriorEstateManager.describeHousehold(level, person.householdId());
        player.sendSystemMessage(Component.literal(
                "§6[" + person.name() + "] §f" + person.generation() + "세대 " + standing
                        + ", " + ageYears(person, day) + "세, " + work + "입니다. " + estate));
"""
if "String estate = ErdenExteriorEstateManager.describeHousehold" not in manager:
    if manager.count(old_message) != 1:
        raise SystemExit("estate interaction insertion point missing")
    manager = manager.replace(old_message, new_message, 1)

manager_path.write_text(manager, encoding="utf-8")
