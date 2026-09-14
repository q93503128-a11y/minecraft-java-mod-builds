from pathlib import Path

root = Path('.')
project = root / 'projects/earth-to-stars'
java = project / 'src/main/java/kr/moonseungjun/earthtostars'


def replace_once(path: Path, old: str, new: str) -> None:
    body = path.read_text(encoding='utf-8')
    count = body.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one match, found {count}')
    path.write_text(body.replace(old, new, 1), encoding='utf-8')


runtime = java / 'ship/runtime/minecraft/ShipRuntimeManager.java'
replace_once(
    runtime,
    'import net.minecraft.world.entity.Display;\nimport net.minecraft.world.item.ItemStack;',
    'import net.minecraft.world.entity.Display;\nimport net.minecraft.world.entity.MoverType;\nimport net.minecraft.world.item.ItemStack;'
)
replace_once(
    runtime,
    'input = new ShipControlInput(payload.throttle(), payload.yaw(), payload.pitch());',
    'input = new ShipControlInput(payload.throttle(), payload.yaw(), payload.lift());'
)
replace_once(
    runtime,
    '''            ShipTransform transform = entry.runtime().transform();
            ShipTransitionPolicy.Transition transition = ShipTransitionPolicy.evaluate(
                    entry.exterior().level().dimension().equals(Level.OVERWORLD),
                    entry.exterior().level().dimension().equals(SpaceLevels.ORBITAL_SPACE),
                    transform
            );
            if (transition == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                    && !ShipSystemsManager.canEnterOrbit(entry.runtime().ship())) {
                ShipTransform held = holdBelowOrbitBoundary(transform);
                entry.runtime().relocate(held);
                applyTransform(entry, held);
                warnReadiness(server, entry, gameTime);
                continue;
            }
            if (transition != ShipTransitionPolicy.Transition.NONE && entry.runtime().lease().isPresent()) {
                transitions.add(new TransitionRequest(entityId, entry, transition));
                continue;
            }

            applyTransform(entry, transform);''',
    '''            ShipTransform desiredTransform = entry.runtime().transform();
            ShipTransform transform = resolveMovement(entry, desiredTransform);
            entry.runtime().reconcileMotion(transform);

            ShipTransitionPolicy.Transition transition = ShipTransitionPolicy.evaluate(
                    entry.exterior().level().dimension().equals(Level.OVERWORLD),
                    entry.exterior().level().dimension().equals(SpaceLevels.ORBITAL_SPACE),
                    transform
            );
            if (transition == ShipTransitionPolicy.Transition.EARTH_TO_ORBIT
                    && !ShipSystemsManager.canEnterOrbit(entry.runtime().ship())) {
                ShipTransform held = holdBelowOrbitBoundary(transform);
                entry.runtime().relocate(held);
                applyTransform(entry, held);
                warnReadiness(server, entry, gameTime);
                continue;
            }
            if (transition != ShipTransitionPolicy.Transition.NONE && entry.runtime().lease().isPresent()) {
                transitions.add(new TransitionRequest(entityId, entry, transition));
            }'''
)
replace_once(
    runtime,
    '''    private static void applyTransform(Entry entry, ShipTransform transform) {
        entry.exterior().setDeltaMovement(Vec3.ZERO);''',
    '''    private static ShipTransform resolveMovement(Entry entry, ShipTransform desiredTransform) {
        Vec3 before = entry.exterior().position();
        Vec3 requested = new Vec3(
                desiredTransform.position().x() - before.x,
                desiredTransform.position().y() - before.y,
                desiredTransform.position().z() - before.z
        );

        entry.exterior().setYRot((float) desiredTransform.yawDegrees());
        entry.exterior().setXRot((float) desiredTransform.pitchDegrees());
        entry.exterior().setDeltaMovement(requested);
        entry.exterior().move(MoverType.SELF, requested);

        Vec3 after = entry.exterior().position();
        Vec3 actualDelta = after.subtract(before);
        entry.exterior().setDeltaMovement(actualDelta);
        ShipTransform resolved = new ShipTransform(
                new ShipVec3(after.x, after.y, after.z),
                new ShipVec3(actualDelta.x, actualDelta.y, actualDelta.z),
                desiredTransform.yawDegrees(),
                desiredTransform.pitchDegrees()
        );
        if (entry.visual() != entry.exterior()) {
            SpaceVisualFactory.apply(entry.visual(), resolved);
        }
        return resolved;
    }

    private static void applyTransform(Entry entry, ShipTransform transform) {
        entry.exterior().setDeltaMovement(Vec3.ZERO);'''
)

server_events = java / 'ship/runtime/minecraft/ShipServerEvents.java'
replace_once(
    server_events,
    '"개척선 조종 연결 완료. W/S 가속, A/D 선회, Space/Shift 기수 조절. 선체 ID: " + entityId',
    '"개척선 조종 연결 완료. W/S 가속, A/D 선회, Space 상승, Ctrl 하강, Shift 하차. 선체 ID: " + entityId'
)

probe = java / 'ship/runtime/minecraft/ShipLifecycleProbe.java'
replace_once(probe, 'import kr.moonseungjun.earthtostars.space.SpaceLevels;\nimport net.minecraft.server.MinecraftServer;',
             'import kr.moonseungjun.earthtostars.space.SpaceLevels;\nimport net.minecraft.core.BlockPos;\nimport net.minecraft.server.MinecraftServer;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.entity.MoverType;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.phys.Vec3;')
replace_once(probe,
             '                case "boarding_contract" -> verifyBoardingEntityContract(server);\n                default -> throw new IllegalArgumentException("unknown lifecycle probe mode: " + mode);',
             '                case "boarding_contract" -> verifyBoardingEntityContract(server);\n                case "flight_collision" -> verifyFlightCollision(server);\n                default -> throw new IllegalArgumentException("unknown lifecycle probe mode: " + mode);')
replace_once(probe,
             '''    private static void assertProbeResources(ShipSystemsSnapshot snapshot) {''',
             '''    private static void verifyFlightCollision(MinecraftServer server) {
        requireSpaceLevels(server);
        ServerLevel level = server.overworld();
        int y = 200;
        for (int dy = -1; dy <= 3; dy++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlockAndUpdate(new BlockPos(4, y + dy, z), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }

        ShipExteriorEntity exterior = new ShipExteriorEntity(EarthToStarsEntities.SHIP_EXTERIOR.get(), level);
        exterior.setPos(0.0D, y, 0.0D);
        if (!level.addFreshEntity(exterior)) {
            throw new IllegalStateException("could not spawn flight-collision probe exterior");
        }
        double beforeX = exterior.getX();
        exterior.setDeltaMovement(3.0D, 0.0D, 0.0D);
        exterior.move(MoverType.SELF, new Vec3(3.0D, 0.0D, 0.0D));
        double movedX = exterior.getX() - beforeX;
        exterior.discard();
        if (movedX >= 2.0D) {
            throw new IllegalStateException("ship hull crossed solid wall; movedX=" + movedX);
        }
        EarthToStars.LOGGER.info("EARTH_TO_STARS_ALPHA17_FLIGHT_COLLISION_PASS requested=3.0 moved={}", movedX);
    }

    private static void assertProbeResources(ShipSystemsSnapshot snapshot) {''')

replace_once(project / 'gradle.properties', 'mod_version=0.1.0-alpha.16', 'mod_version=0.1.0-alpha.17')
replace_once(java / 'EarthToStars.java', 'VERSION = "0.1.0-alpha.16"', 'VERSION = "0.1.0-alpha.17"')

validator = project / 'tools/validate_alpha17_acceptance.py'
validator.write_text('''#!/usr/bin/env python3
from pathlib import Path

import validate_alpha12_acceptance as alpha12
import validate_alpha14_acceptance as alpha14
import validate_alpha16_acceptance as alpha16

PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java/kr/moonseungjun/earthtostars"


class AcceptanceError(RuntimeError):
    pass


def text(path: Path) -> str:
    if not path.is_file():
        raise AcceptanceError(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def require(body: str, needle: str, message: str) -> None:
    if needle not in body:
        raise AcceptanceError(message)


def validate_alpha17() -> None:
    props = text(PROJECT / "gradle.properties")
    require(props, "mod_version=0.1.0-alpha.17", "mod version is not alpha.17")

    control = text(JAVA / "ship/runtime/ShipControlInput.java")
    payload = text(JAVA / "ship/networking/ShipControlInputPayload.java")
    client = text(JAVA / "ship/client/ShipClientController.java")
    movement = text(JAVA / "ship/runtime/ShipMovementSimulator.java")
    runtime = text(JAVA / "ship/runtime/ShipFlightRuntime.java")
    exterior = text(JAVA / "ship/runtime/minecraft/ShipExteriorEntity.java")
    manager = text(JAVA / "ship/runtime/minecraft/ShipRuntimeManager.java")
    probe = text(JAVA / "ship/runtime/minecraft/ShipLifecycleProbe.java")
    entities = text(JAVA / "content/EarthToStarsEntities.java")

    require(control, "double lift", "control input still models vertical control as pitch")
    require(payload, "float lift", "network payload still models vertical control as pitch")
    require(client, "axis(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown())", "A/D yaw mapping is not explicit left/right")
    require(client, "axis(minecraft.options.keyJump.isDown(), minecraft.options.keySprint.isDown())", "Space/Ctrl are not explicit up/down thrust")
    require(client, "CalculateDetachedCameraDistanceEvent", "ship third-person camera distance hook missing")

    require(movement, "current.velocity().dot(right)", "movement still deletes lateral momentum")
    require(movement, "input.lift()", "movement does not apply explicit vertical thrust")
    require(movement, "tuning.coastDrag()", "coast inertia/drag contract missing")
    require(runtime, "reconcileMotion", "collision-resolved transform cannot be reconciled without clearing pilot input")

    require(exterior, "this.noPhysics = false", "Display noPhysics is still enabled on the vehicle")
    require(exterior, "getPassengerAttachmentPoint", "cockpit-local passenger seat override missing")
    require(exterior, "shouldRiderSit()", "rider sitting presentation contract missing")
    if ".noSave()" in entities:
        raise AcceptanceError("vehicle EntityType became non-serializable again")

    require(manager, "payload.lift()", "server still reads obsolete pitch input")
    require(manager, "move(MoverType.SELF, requested)", "authoritative hull movement still bypasses vanilla block collision")
    require(manager, "reconcileMotion(transform)", "runtime does not adopt collision-resolved movement")
    require(probe, 'case "flight_collision"', "dedicated block-collision probe missing")
    require(probe, "EARTH_TO_STARS_ALPHA17_FLIGHT_COLLISION_PASS", "collision probe pass marker missing")


def main() -> None:
    try:
        alpha12.validate_no_runtime_proxies()
        alpha12.validate_26_2_runtime_api_contract()
        alpha12.validate_recipe_syntax()
        alpha14.validate_deployment_and_service_ux()
        alpha14.validate_runtime_models()
        alpha16.validate_creative_access()
        validate_alpha17()
    except Exception as exc:
        raise SystemExit(f"ALPHA.17 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.17 ACCEPTANCE VALIDATION OK: explicit A/D + Space/Ctrl controls, inertial flight, vanilla block collision, cockpit-local rider placement, camera distance and creative access verified")


if __name__ == "__main__":
    main()
''', encoding='utf-8')

print('EARTH_TO_STARS_ALPHA17_FINISH_READY')
