from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredGroundPlanCatalog.java')
text = path.read_text(encoding='utf-8')

needle = '''        Set<Long> reserved = new HashSet<>();\n        List<LocalBed> beds = new ArrayList<>();\n'''
replacement = '''        Set<Long> reserved = new HashSet<>();\n        ErdenUrbanSourceAirRoutePlanner.RoutePlan upperAccessRoute =\n                ErdenUrbanSourceAirRoutePlanner.plan(placement.fragmentKey());\n        if (upperAccessRoute == null\n                || upperAccessRoute.classification()\n                != ErdenUrbanSourceAirRoutePlanner.RouteClassification.ZERO_CUT_ROUTE\n                || upperAccessRoute.path().isEmpty()) {\n            throw new IllegalStateException(\n                    "Authored-ground plan has no zero-cut upper access route fragment="\n                            + placement.fragmentKey());\n        }\n        int reservedRouteColumns = 0;\n        for (ErdenUrbanSourceAirRoutePlanner.Node node : upperAccessRoute.path()) {\n            // Ground furniture is one block tall. Reserve every route column whose feet/head body\n            // can intersect that furniture at the authored ground band. This keeps beds, barrels,\n            // work targets and resident targets out of the staircase's immutable source-air lane.\n            if (Math.abs(node.y() - groundY) <= 1\n                    && reserved.add(columnKey(node.x(), node.z()))) {\n                reservedRouteColumns++;\n            }\n        }\n        if (reservedRouteColumns == 0) {\n            throw new IllegalStateException(\n                    "Authored-ground plan reserved no upper-route ground columns fragment="\n                            + placement.fragmentKey() + " ground_y=" + groundY);\n        }\n        List<LocalBed> beds = new ArrayList<>();\n'''

if replacement not in text:
    if needle not in text:
        raise SystemExit('ground-plan route reservation insertion point not found')
    text = text.replace(needle, replacement, 1)

if 'reservedRouteColumns' not in text or 'ErdenUrbanSourceAirRoutePlanner.plan(placement.fragmentKey())' not in text:
    raise SystemExit('ground-route reservation patch did not install')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms authored-ground furniture now reserves source-air upper-route columns')
