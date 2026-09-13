from pathlib import Path

root = Path('.')
project = root / 'projects/earth-to-stars'
java = project / 'src/main/java/kr/moonseungjun/earthtostars'


def replace_once(path: Path, old: str, new: str) -> None:
    body = path.read_text(encoding='utf-8')
    count = body.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one replacement, found {count}')
    path.write_text(body.replace(old, new, 1), encoding='utf-8')


# Minecraft 26.2 Entity.startRiding rejects non-serializable vehicle EntityTypes.
entities = java / 'content/EarthToStarsEntities.java'
replace_once(
    entities,
    '''                    .updateInterval(1)\n                    .noSave()\n                    .noSummon()''',
    '''                    .updateInterval(1)\n                    .noSummon()'''
)

# Keep the runtime exterior ephemeral at instance level without making its EntityType unrideable.
exterior = java / 'ship/runtime/minecraft/ShipExteriorEntity.java'
replace_once(
    exterior,
    '''    @Override\n    public boolean isPushable() {\n        return false;\n    }\n\n    @Override\n    public boolean canCollideWith(Entity other) {''',
    '''    @Override\n    public boolean isPushable() {\n        return false;\n    }\n\n    @Override\n    public boolean shouldBeSaved() {\n        return false;\n    }\n\n    @Override\n    public boolean canCollideWith(Entity other) {'''
)

runtime = java / 'ship/runtime/minecraft/ShipRuntimeManager.java'
replace_once(
    runtime,
    '''        boardAndControl(player, entry.exterior(), tick);\n        return LaunchDeploymentResult.DEPLOYED;''',
    '''        if (!boardAndControl(player, entry.exterior(), tick)) {\n            rollbackFailedDeployment(level.getServer(), entry);\n            return LaunchDeploymentResult.CONTROL_UNAVAILABLE;\n        }\n        return LaunchDeploymentResult.DEPLOYED;'''
)
replace_once(
    runtime,
    '''        if (newlyMounted && !player.startRiding(exterior)) {\n            return false;\n        }''',
    '''        // Minecraft 26.2 rejects non-serializable vehicle types before it even\n        // considers the force flag. SHIP_EXTERIOR is therefore a serializable EntityType,\n        // while ShipExteriorEntity.shouldBeSaved() keeps this runtime shell ephemeral.\n        // Authorization, distance and single-seat occupancy have already been checked here,\n        // so force mounting is safe and avoids sneak/boarding-cooldown false negatives.\n        if (newlyMounted && !player.startRiding(exterior, true, true)) {\n            return false;\n        }'''
)
replace_once(runtime, '            pilot.startRiding(entry.exterior());', '            pilot.startRiding(entry.exterior(), true, true);')
replace_once(runtime, '        if (nextPair == null || !teleported.startRiding(nextPair.exterior())) {', '        if (nextPair == null || !teleported.startRiding(nextPair.exterior(), true, true)) {')
replace_once(runtime, '        if (returned != null && returned.startRiding(entry.exterior())) {', '        if (returned != null && returned.startRiding(entry.exterior(), true, true)) {')
replace_once(
    runtime,
    '''    public static void prepareForShutdown() {''',
    '''    private static void rollbackFailedDeployment(MinecraftServer server, Entry entry) {\n        int entityId = entry.exterior().getId();\n        ShipId shipId = entry.runtime().ship().shipId();\n        ENTRIES.remove(entityId);\n        discardPair(entry);\n        REPOSITORY.remove(shipId);\n        ShipSystemsManager.removeShip(shipId);\n        ShipTurretManager.removeShip(shipId);\n        OrbitalMissionManager.removeShip(shipId);\n        LAST_READINESS_WARNING.remove(shipId);\n        ShipSavedData.get(server).remove(shipId);\n        ShipSystemsSavedData.get(server).remove(shipId);\n        InteriorSavedData.get(server).release(shipId);\n        ShipSystemsManager.flush(server);\n    }\n\n    public static void prepareForShutdown() {'''
)

probe = java / 'ship/runtime/minecraft/ShipLifecycleProbe.java'
replace_once(
    probe,
    'import kr.moonseungjun.earthtostars.EarthToStars;\n',
    'import kr.moonseungjun.earthtostars.EarthToStars;\nimport kr.moonseungjun.earthtostars.content.EarthToStarsEntities;\n'
)
replace_once(
    probe,
    '''                case "verify" -> verify(server);\n                default -> throw new IllegalArgumentException("unknown lifecycle probe mode: " + mode);''',
    '''                case "verify" -> verify(server);\n                case "boarding_contract" -> verifyBoardingEntityContract(server);\n                default -> throw new IllegalArgumentException("unknown lifecycle probe mode: " + mode);'''
)
replace_once(
    probe,
    '''    private static void assertProbeResources(ShipSystemsSnapshot snapshot) {''',
    '''    private static void verifyBoardingEntityContract(MinecraftServer server) {\n        requireSpaceLevels(server);\n        var type = EarthToStarsEntities.SHIP_EXTERIOR.get();\n        if (!type.canSerialize()) {\n            throw new IllegalStateException("ship exterior EntityType is non-serializable and vanilla riding will always reject it");\n        }\n        ShipExteriorEntity exterior = new ShipExteriorEntity(type, server.overworld());\n        if (exterior.shouldBeSaved()) {\n            throw new IllegalStateException("runtime ship exterior would be persisted alongside authoritative ShipState");\n        }\n        EarthToStars.LOGGER.info(\n                "EARTH_TO_STARS_ALPHA15_BOARDING_ENTITY_CONTRACT_PASS serializable={} instanceSaved={}",\n                type.canSerialize(),\n                exterior.shouldBeSaved()\n        );\n    }\n\n    private static void assertProbeResources(ShipSystemsSnapshot snapshot) {'''
)

replace_once(project / 'gradle.properties', 'mod_version=0.1.0-alpha.14', 'mod_version=0.1.0-alpha.15')
replace_once(
    java / 'EarthToStars.java',
    'public static final String VERSION = "0.1.0-alpha.14";',
    'public static final String VERSION = "0.1.0-alpha.15";'
)

validator = project / 'tools/validate_alpha15_acceptance.py'
validator.write_text('''#!/usr/bin/env python3\nfrom __future__ import annotations\n\nimport json\nfrom pathlib import Path\n\nimport validate_alpha12_acceptance as alpha12\nimport validate_alpha14_acceptance as alpha14\n\nPROJECT = Path(__file__).resolve().parents[1]\nJAVA = PROJECT / "src/main/java"\n\n\nclass AcceptanceError(RuntimeError):\n    pass\n\n\ndef fail(message: str) -> None:\n    raise AcceptanceError(message)\n\n\ndef text(path: Path) -> str:\n    if not path.is_file():\n        fail(f"missing required file: {path.relative_to(PROJECT)}")\n    return path.read_text(encoding="utf-8")\n\n\ndef validate_target() -> None:\n    props = text(PROJECT / "gradle.properties")\n    for required in ("minecraft_version=26.2", "neo_version=26.2.0.76", "mod_version=0.1.0-alpha.15"):\n        if required not in props:\n            fail(f"target/version contract missing: {required}")\n\n\ndef validate_visible_vehicle_and_boarding() -> None:\n    exterior = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.java")\n    if "extends Display.ItemDisplay" not in exterior:\n        fail("starter craft exterior is no longer the visible vehicle")\n    for required in ("setVisualItem", "isPickable()", "boardAndControl(serverPlayer, this)", "public boolean shouldBeSaved()", "return false;"):\n        if required not in exterior:\n            fail(f"visible/ephemeral vehicle contract missing: {required}")\n\n    entities = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsEntities.java")\n    if ".noSave()" in entities:\n        fail("ship exterior EntityType is still non-serializable; Minecraft 26.2 startRiding will always reject it")\n    for required in (".sized(5.4F, 2.3F)", "EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F"):\n        if required not in entities:\n            fail(f"starter hull/seat contract missing: {required}")\n\n    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")\n    for required in (\n        "player.startRiding(exterior, true, true)",\n        "teleported.startRiding(nextPair.exterior(), true, true)",\n        "returned.startRiding(entry.exterior(), true, true)",\n        "if (!boardAndControl(player, entry.exterior(), tick))",\n        "rollbackFailedDeployment(level.getServer(), entry)",\n        "private static void rollbackFailedDeployment",\n        "new VehiclePair(exterior, exterior)",\n    ):\n        if required not in runtime:\n            fail(f"authoritative boarding/rollback contract missing: {required}")\n    if "player.startRiding(exterior)" in runtime:\n        fail("non-forced primary boarding path remains")\n\n    probe = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipLifecycleProbe.java")\n    for required in ("boarding_contract", "type.canSerialize()", "exterior.shouldBeSaved()", "EARTH_TO_STARS_ALPHA15_BOARDING_ENTITY_CONTRACT_PASS"):\n        if required not in probe:\n            fail(f"dedicated boarding entity probe missing: {required}")\n\n\ndef main() -> None:\n    try:\n        alpha12.validate_no_runtime_proxies()\n        alpha12.validate_26_2_runtime_api_contract()\n        alpha12.validate_recipe_syntax()\n        validate_target()\n        validate_visible_vehicle_and_boarding()\n        alpha14.validate_deployment_and_service_ux()\n        alpha14.validate_runtime_models()\n    except (OSError, json.JSONDecodeError, AcceptanceError, alpha12.AcceptanceError, alpha14.AcceptanceError) as exc:\n        raise SystemExit(f"ALPHA.15 ACCEPTANCE VALIDATION FAILED: {exc}") from exc\n    print("ALPHA.15 ACCEPTANCE VALIDATION OK: rideable serializable EntityType, non-persisted runtime exterior, forced authorized mounting, failed-deployment rollback and alpha.14 visual/UX contracts verified")\n\n\nif __name__ == "__main__":\n    main()\n''', encoding='utf-8')

workflow = root / '.github/workflows/build-earth-to-stars.yml'
replace_once(
    workflow,
    'python3 tools/validate_alpha14_acceptance.py 2>&1 | tee build/logs/alpha14-acceptance.log',
    'python3 tools/validate_alpha15_acceptance.py 2>&1 | tee build/logs/alpha15-acceptance.log'
)
replace_once(workflow, '# EARTH TO STARS — alpha.14 Starter Craft Core Rescue Build Report', '# EARTH TO STARS — alpha.15 Boarding Root-Cause Rescue Build Report')
replace_once(workflow, '- alpha.14 acceptance static gate: PASS', '- alpha.15 acceptance static gate: PASS')
replace_once(
    workflow,
    '- visible hull / interaction / passenger vehicle single-entity contract: PASS',
    '- rideable serializable EntityType + non-persisted runtime exterior contract: PASS\n          - visible hull / interaction / passenger vehicle single-entity contract: PASS'
)

print('EARTH_TO_STARS_ALPHA15_PATCH_READY')
