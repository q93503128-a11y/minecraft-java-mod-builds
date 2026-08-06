from pathlib import Path

manager_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorLifecycleManager.java")
manager = manager_path.read_text(encoding="utf-8")

old_gate = """        if (futureAdults <= 0 || futureRetirements <= 0
                || successionHouseholds <= 0 || parentReadyHouseholds <= 0) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                \"LK_ERDEN_EXTERIOR_LIFECYCLE_PASS revision={} founders={} households={} future_adults={} scheduled_retirements={} succession_households={} parent_ready_households={} year_days={} adulthood={} retirement={} births=true inheritance=true replacement_labor=true permanent_deaths=true save_overlay=true\",
                LIFECYCLE_REVISION, lifecycle.founderCount(), lifecycle.householdLines().size(),
                futureAdults, futureRetirements, successionHouseholds, parentReadyHouseholds,
                DAYS_PER_YEAR, ADULT_AGE, RETIREMENT_AGE);
"""
new_gate = """        CiProjection projection = projectFutureLifecycle(lifecycle, workforce);
        if (futureAdults <= 0 || futureRetirements <= 0
                || successionHouseholds <= 0 || parentReadyHouseholds <= 0
                || projection.births() <= 0
                || projection.successions() <= 0
                || projection.replacementWorkers() <= 0) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                \"LK_ERDEN_EXTERIOR_LIFECYCLE_PASS revision={} founders={} households={} future_adults={} scheduled_retirements={} succession_households={} parent_ready_households={} year_days={} adulthood={} retirement={} births=true inheritance=true replacement_labor=true permanent_deaths=true save_overlay=true projected_births={} projected_successions={} projected_replacement_workers={} projected_years={} ci_projection_non_persistent=true\",
                LIFECYCLE_REVISION, lifecycle.founderCount(), lifecycle.householdLines().size(),
                futureAdults, futureRetirements, successionHouseholds, parentReadyHouseholds,
                DAYS_PER_YEAR, ADULT_AGE, RETIREMENT_AGE,
                projection.births(), projection.successions(), projection.replacementWorkers(),
                projection.years());
"""
if old_gate not in manager:
    raise SystemExit("lifecycle CI gate pattern missing")
manager = manager.replace(old_gate, new_gate, 1)

insertion = """    private static int founderAge(ErdenExteriorWorkforceSavedData.Resident resident) {
"""
projection_methods = """    private static CiProjection projectFutureLifecycle(
            ErdenExteriorLifecycleSavedData lifecycle,
            ErdenExteriorWorkforceSavedData workforce) {
        final int projectedYears = 20;
        long establishedDay = lifecycle.establishedDay();
        long projectedDay = establishedDay + (long) projectedYears * DAYS_PER_YEAR;
        List<ErdenExteriorLifecycleSavedData.Person> people =
                new ArrayList<>(lifecycle.persons());
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines =
                new ArrayList<>(lifecycle.householdLines());
        int nextSequence = lifecycle.nextBirthSequence();
        int initialPopulation = people.size();

        for (int year = 1; year <= projectedYears; year++) {
            long birthDay = establishedDay + (long) year * DAYS_PER_YEAR;
            BirthResult births = processBirths(
                    people, lines, workforce, birthDay, year, nextSequence);
            people = births.people();
            lines = births.lines();
            nextSequence = births.nextSequence();
        }

        int projectedBirths = people.size() - initialPopulation;
        int projectedSuccessions = projectSuccession(people, lines, projectedDay);
        int projectedReplacementWorkers =
                projectReplacementLabor(people, workforce, projectedDay);
        return new CiProjection(
                projectedBirths,
                projectedSuccessions,
                projectedReplacementWorkers,
                projectedYears);
    }

    private static int projectSuccession(
            List<ErdenExteriorLifecycleSavedData.Person> sourcePeople,
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> sourceLines,
            long day) {
        List<ErdenExteriorLifecycleSavedData.Person> people =
                new ArrayList<>(sourcePeople);
        List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines =
                new ArrayList<>(sourceLines);
        Map<String, ErdenExteriorLifecycleSavedData.Person> byId = new HashMap<>();
        for (ErdenExteriorLifecycleSavedData.Person person : people) {
            byId.put(person.id(), person);
        }
        int before = successionTotal(lines);
        for (ErdenExteriorLifecycleSavedData.HouseholdLine line : lines) {
            ErdenExteriorLifecycleSavedData.Person steward = byId.get(line.stewardId());
            if (steward == null || !steward.aliveOn(day)
                    || ageYears(steward, day) < ADULT_AGE) continue;
            boolean alternateAdult = false;
            for (ErdenExteriorLifecycleSavedData.Person member : people) {
                if (member.householdId().equals(line.householdId())
                        && !member.id().equals(steward.id())
                        && member.aliveOn(day)
                        && ageYears(member, day) >= ADULT_AGE) {
                    alternateAdult = true;
                    break;
                }
            }
            if (!alternateAdult) continue;
            replacePerson(people, steward.withDeath(day));
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> projected =
                    processSuccession(people, lines, day);
            return Math.max(0, successionTotal(projected) - before);
        }
        return 0;
    }

    private static int projectReplacementLabor(
            List<ErdenExteriorLifecycleSavedData.Person> sourcePeople,
            ErdenExteriorWorkforceSavedData workforce,
            long day) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            List<ErdenExteriorLifecycleSavedData.Person> people =
                    new ArrayList<>(sourcePeople);
            ErdenExteriorLifecycleSavedData.Person vacancyWorker = null;
            boolean eligibleReplacement = false;
            for (ErdenExteriorLifecycleSavedData.Person person : people) {
                if (!person.nodeId().equals(node.id) || !person.aliveOn(day)) continue;
                if (vacancyWorker == null
                        && person.assignedWorker()
                        && !person.retiredOn(day)
                        && ageYears(person, day) >= ADULT_AGE) {
                    vacancyWorker = person;
                }
                if (!person.foundingWorker()
                        && !person.assignedWorker()
                        && ageYears(person, day) >= ADULT_AGE
                        && ageYears(person, day) < RETIREMENT_AGE) {
                    eligibleReplacement = true;
                }
            }
            if (vacancyWorker == null || !eligibleReplacement) continue;
            int before = replacementWorkerCount(people, node.id);
            replacePerson(people, vacancyWorker.withDeath(day));
            List<ErdenExteriorLifecycleSavedData.Person> projected =
                    fillVacancies(people, workforce, day);
            int after = replacementWorkerCount(projected, node.id);
            if (after > before) return after - before;
        }
        return 0;
    }

    private static int replacementWorkerCount(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            String nodeId) {
        int count = 0;
        for (ErdenExteriorLifecycleSavedData.Person person : people) {
            if (person.nodeId().equals(nodeId)
                    && !person.foundingWorker()
                    && person.assignedWorker()) count++;
        }
        return count;
    }

    private static int successionTotal(
            List<ErdenExteriorLifecycleSavedData.HouseholdLine> lines) {
        int total = 0;
        for (ErdenExteriorLifecycleSavedData.HouseholdLine line : lines) {
            total += line.successionCount();
        }
        return total;
    }

    private static void replacePerson(
            List<ErdenExteriorLifecycleSavedData.Person> people,
            ErdenExteriorLifecycleSavedData.Person replacement) {
        for (int i = 0; i < people.size(); i++) {
            if (!people.get(i).id().equals(replacement.id())) continue;
            people.set(i, replacement);
            return;
        }
        throw new IllegalStateException(
                \"Missing Erden lifecycle projection person \" + replacement.id());
    }

""" + insertion
if manager.count(insertion) != 1:
    raise SystemExit("lifecycle projection insertion point missing")
manager = manager.replace(insertion, projection_methods, 1)

record_insertion = """    private record BirthResult(
"""
projection_record = """    private record CiProjection(
            int births,
            int successions,
            int replacementWorkers,
            int years) {
    }

""" + record_insertion
if manager.count(record_insertion) != 1:
    raise SystemExit("lifecycle projection record insertion point missing")
manager = manager.replace(record_insertion, projection_record, 1)
manager_path.write_text(manager, encoding="utf-8")

workflow_path = Path(".github/workflows/audit-living-kingdoms-exterior-lifecycle.yml")
workflow = workflow_path.read_text(encoding="utf-8")
if "projectFutureLifecycle" not in workflow:
    needle = "          grep -F 'fillVacancies' \"$manager\"\n"
    if needle not in workflow:
        raise SystemExit("lifecycle workflow source insertion point missing")
    workflow = workflow.replace(
        needle,
        needle + "          grep -F 'projectFutureLifecycle' \"$manager\"\n"
        + "          grep -F 'ci_projection_non_persistent=true' \"$manager\"\n",
        1,
    )
if "projected_births=" not in workflow:
    needle = "          grep -F 'year_days=112 adulthood=18 retirement=58 births=true inheritance=true replacement_labor=true permanent_deaths=true save_overlay=true' ../../logs/exterior-lifecycle-server.log\n"
    if needle not in workflow:
        raise SystemExit("lifecycle workflow runtime insertion point missing")
    workflow = workflow.replace(
        needle,
        needle
        + "          grep -E 'projected_births=[1-9][0-9]* projected_successions=[1-9][0-9]* projected_replacement_workers=[1-9][0-9]* projected_years=20 ci_projection_non_persistent=true' ../../logs/exterior-lifecycle-server.log\n",
        1,
    )
workflow_path.write_text(workflow, encoding="utf-8")

doc_path = Path("projects/living-kingdoms/docs/ERDEN_EXTERIOR_LIFECYCLE_VALIDATION.md")
doc = doc_path.read_text(encoding="utf-8")
section = """
## Non-persistent future projection

The CI proof does not advance or rewrite the actual world save. It clones the current people and
household-line lists in memory, then runs the production methods used by normal gameplay over a
20-year projection. The projection must create at least one real birth through `processBirths`,
complete at least one steward succession through `processSuccession`, and assign at least one adult
dependent or descendant to a forced labour vacancy through `fillVacancies`. Failure of any actual
transition suppresses the lifecycle pass marker. The projected people and household lines are then
discarded, leaving the live save on its original day.
"""
if "## Non-persistent future projection" not in doc:
    marker = "\n## Required regression proof\n"
    if marker not in doc:
        raise SystemExit("lifecycle document insertion point missing")
    doc = doc.replace(marker, section + marker, 1)
doc_path.write_text(doc, encoding="utf-8")
