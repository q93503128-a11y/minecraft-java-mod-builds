from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenCitadelInteriorManager.java')
text = path.read_text(encoding='utf-8')
old = '''    private static int requiredFixtures(Zone zone) {\n        // The verified guard-command room is deliberately compact. Its first three fixtures are\n        // the role-defining target, anvil and grindstone; storage is optional rather than a reason\n        // to reject an otherwise complete command room.\n        return "guard_command".equals(zone.id) ? 3 : MIN_ZONE_FIXTURES;\n    }\n'''
new = '''    private static int requiredFixtures(Zone zone) {\n        // The imported citadel has two deliberately compact working rooms. Their first three\n        // fixtures are the role-defining workstations; storage/utility furniture is optional and\n        // must not justify carving or spilling furniture through authored walls just to reach a\n        // generic four-fixture quota.\n        return switch (zone.id) {\n            case "guard_command", "service_quarter" -> 3;\n            default -> MIN_ZONE_FIXTURES;\n        };\n    }\n'''
if new not in text:
    if old not in text:
        raise SystemExit('citadel requiredFixtures source block not found')
    text = text.replace(old, new, 1)
path.write_text(text, encoding='utf-8')
print('Living Kingdoms compact citadel service quarter now accepts its three role-defining workstations')
