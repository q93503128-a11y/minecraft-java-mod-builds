from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    text = text.replace(old, new)


replace_once(
    "    private static final int[][] HOME_OFFSETS = {\n"
    "            {-18, -12}, {0, -14}, {18, -12}, {-18, 12}, {0, 14}, {18, 12}\n"
    "    };",
    "    private static final int[][] HOME_OFFSETS = {\n"
    "            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}\n"
    "    };",
    "anchor-aligned homes",
)

replace_once(
    "    public static int productionPercent(ServerLevel level, String nodeId, long day) {\n"
    "        prepareBeforeSupply(level, day);\n"
    "        return data(level).productionPercent(nodeId, day);\n"
    "    }",
    "    public static int productionPercent(ServerLevel level, String nodeId, long day) {\n"
    "        ErdenExteriorWorkforceSavedData workforce = data(level);\n"
    "        ensurePopulation(workforce);\n"
    "        ErdenKingdomSupplyCatalog.SupplyNode node = ErdenKingdomSupplyCatalog.node(nodeId);\n"
    "        return node == null ? 0 : laborState(workforce, node, day).productionPercent();\n"
    "    }",
    "historic production",
)

replace_once(
    "            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n"
    "                int required = requiredWorkers(node.role);\n"
    "                int alive = 0;\n"
    "                int attended = 0;\n"
    "                int absent = 0;\n"
    "                int dead = 0;\n"
    "                for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {\n"
    "                    if (!household.nodeId().equals(node.id)) continue;\n"
    "                    for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {\n"
    "                        if (!resident.worker()) continue;\n"
    "                        if (workforce.isDead(resident.id())) {\n"
    "                            dead++;\n"
    "                            continue;\n"
    "                        }\n"
    "                        alive++;\n"
    "                        if (absentOnDay(resident, node.role, day)) absent++;\n"
    "                        else attended++;\n"
    "                    }\n"
    "                }\n"
    "                int percent = required <= 0 ? 100\n"
    "                        : Math.clamp(attended * 100 / required, 0, 100);\n"
    "                states.add(new ErdenExteriorWorkforceSavedData.NodeLabor(\n"
    "                        node.id, day, required, alive, attended, absent, dead,\n"
    "                        percent, 0L, 0L));\n"
    "            }",
    "            for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {\n"
    "                states.add(laborState(workforce, node, day));\n"
    "            }",
    "shared daily labor",
)

replace_once(
    "    private static boolean absentOnDay(\n"
    "            ErdenExteriorWorkforceSavedData.Resident resident,",
    "    private static ErdenExteriorWorkforceSavedData.NodeLabor laborState(\n"
    "            ErdenExteriorWorkforceSavedData workforce,\n"
    "            ErdenKingdomSupplyCatalog.SupplyNode node,\n"
    "            long day) {\n"
    "        int required = requiredWorkers(node.role);\n"
    "        int alive = 0;\n"
    "        int attended = 0;\n"
    "        int absent = 0;\n"
    "        int dead = 0;\n"
    "        for (ErdenExteriorWorkforceSavedData.Household household : workforce.households()) {\n"
    "            if (!household.nodeId().equals(node.id)) continue;\n"
    "            for (ErdenExteriorWorkforceSavedData.Resident resident : household.residents()) {\n"
    "                if (!resident.worker()) continue;\n"
    "                if (workforce.isDead(resident.id())) {\n"
    "                    dead++;\n"
    "                    continue;\n"
    "                }\n"
    "                alive++;\n"
    "                if (absentOnDay(resident, node.role, day)) absent++;\n"
    "                else attended++;\n"
    "            }\n"
    "        }\n"
    "        int percent = required <= 0 ? 100\n"
    "                : Math.clamp(attended * 100 / required, 0, 100);\n"
    "        return new ErdenExteriorWorkforceSavedData.NodeLabor(\n"
    "                node.id, day, required, alive, attended, absent, dead,\n"
    "                percent, 0L, 0L);\n"
    "    }\n\n"
    "    private static boolean absentOnDay(\n"
    "            ErdenExteriorWorkforceSavedData.Resident resident,",
    "labor state helper",
)

path.write_text(text, encoding="utf-8")
