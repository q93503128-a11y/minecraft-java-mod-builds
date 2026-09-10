from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
project = root / "projects/earth-to-stars"
java = project / "src/main/java/kr/moonseungjun/earthtostars"
res = project / "src/main/resources/assets/earth_to_stars"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    body = read(path)
    count = body.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one replacement, found {count}")
    write(path, body.replace(old, new, 1))


write(java / "ship/runtime/minecraft/ShipExteriorEntity.java", '''package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.content.EarthToStarsItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The starter craft's visible hull, interaction target and actual passenger vehicle.
 * Persistent authority remains in ShipState/SavedData; this runtime exterior is recreated as needed.
 */
public final class ShipExteriorEntity extends Display.ItemDisplay {
    public ShipExteriorEntity(EntityType<? extends ShipExteriorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setVisualItem(Item item) {
        var slot = getSlot(0);
        if (slot == null || !slot.set(new ItemStack(item))) {
            throw new IllegalStateException("ship exterior item slot is unavailable");
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(EarthToStarsItems.PROPELLANT_CELL.get())) {
            return ShipRuntimeManager.serviceSupply(
                    serverPlayer, this, hand, ShipSystemsManager.SupplyType.PROPELLANT);
        }
        if (held.is(EarthToStarsItems.OXYGEN_CARTRIDGE.get())) {
            return ShipRuntimeManager.serviceSupply(
                    serverPlayer, this, hand, ShipSystemsManager.SupplyType.OXYGEN);
        }
        if (player.isShiftKeyDown()) {
            return ShipRuntimeManager.retireCraft(serverPlayer, this)
                    ? InteractionResult.SUCCESS_SERVER
                    : InteractionResult.FAIL;
        }
        return ShipRuntimeManager.boardAndControl(serverPlayer, this)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }
}
''')

write(java / "ship/client/ShipExteriorEntityRenderer.java", '''package kr.moonseungjun.earthtostars.ship.client;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders the same ItemDisplay entity that owns the pilot seat and interaction hitbox. */
public final class ShipExteriorEntityRenderer extends DisplayRenderer.ItemDisplayRenderer {
    public ShipExteriorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
''')

entities = java / "content/EarthToStarsEntities.java"
replace_once(
    entities,
    """                    .sized(2.4F, 1.35F)
                    .eyeHeight(0.85F)
                    .attach(EntityAttachment.PASSENGER, 0.0F, -0.50F, 0.12F)""",
    """                    .sized(5.4F, 2.3F)
                    .eyeHeight(1.25F)
                    .attach(EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F)""",
)

write(java / "content/LaunchCraftKitItem.java", '''package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipRuntimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class LaunchCraftKitItem extends Item {
    private static final int DEPLOY_RADIUS = 2;
    private static final int DEPLOY_HEIGHT = 3;

    public LaunchCraftKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.FAIL;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.earth_only"));
            return InteractionResult.FAIL;
        }

        BlockPos base = context.getClickedPos().relative(context.getClickedFace());
        if (!prepareDeploymentSite(level, base)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.blocked"), true);
            return InteractionResult.FAIL;
        }

        Vec3 spawn = Vec3.atBottomCenterOf(base).add(0.0D, 0.05D, 0.0D);
        ShipRuntimeManager.LaunchDeploymentResult result = ShipRuntimeManager.deployLaunchCraft(
                player, level, spawn, level.getGameTime());

        return switch (result) {
            case DEPLOYED -> {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.deployed"), true);
                yield InteractionResult.SUCCESS_SERVER;
            }
            case ALREADY_OWNS_CRAFT -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.existing"), true);
                yield InteractionResult.FAIL;
            }
            case CONTROL_UNAVAILABLE -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.control_unavailable"), true);
                yield InteractionResult.FAIL;
            }
            case DEPLOYMENT_FAILED -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch.failed"), true);
                yield InteractionResult.FAIL;
            }
        };
    }

    private static boolean prepareDeploymentSite(ServerLevel level, BlockPos base) {
        for (int y = 0; y < DEPLOY_HEIGHT; y++) {
            for (int x = -DEPLOY_RADIUS; x <= DEPLOY_RADIUS; x++) {
                for (int z = -DEPLOY_RADIUS; z <= DEPLOY_RADIUS; z++) {
                    BlockState state = level.getBlockState(base.offset(x, y, z));
                    if (!state.isAir() && (!state.canBeReplaced() || !state.getFluidState().isEmpty())) {
                        return false;
                    }
                }
            }
        }
        for (int y = 0; y < DEPLOY_HEIGHT; y++) {
            for (int x = -DEPLOY_RADIUS; x <= DEPLOY_RADIUS; x++) {
                for (int z = -DEPLOY_RADIUS; z <= DEPLOY_RADIUS; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        return true;
    }
}
''')

write(java / "content/ShipSupplyItem.java", '''package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipSystemsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ShipSupplyItem extends Item {
    private final ShipSystemsManager.SupplyType supplyType;

    public ShipSupplyItem(Properties properties, ShipSystemsManager.SupplyType supplyType) {
        super(properties);
        this.supplyType = supplyType;
    }

    public ShipSystemsManager.SupplyType supplyType() {
        return supplyType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.supply.use_on_ship"), true);
        }
        return InteractionResult.FAIL;
    }
}
''')

systems = java / "ship/runtime/minecraft/ShipSystemsManager.java"
replace_once(
    systems,
    """    public static SupplyLoadResult loadSupply(ServerPlayer player, SupplyType type) {
        ShipState ship = ShipRuntimeManager.accessibleShip(player, ShipPermission.INTERIOR_ACCESS).orElse(null);
        if (ship == null) return SupplyLoadResult.NO_ACCESSIBLE_SHIP;
        ShipSystemsRuntime systems = systems(ship);
        double accepted = switch (type) {
            case PROPELLANT -> systems.loadPropellantCell();
            case OXYGEN -> systems.loadOxygenCartridge();
        };
        if (accepted <= 1.0E-9D) return SupplyLoadResult.TANK_FULL;
        ShipSystemsSavedData.get(player.level().getServer()).put(systems.snapshot());
        return SupplyLoadResult.LOADED;
    }""",
    """    public static SupplyLoadResult loadSupply(ServerPlayer player, SupplyType type) {
        ShipState ship = ShipRuntimeManager.accessibleShip(player, ShipPermission.INTERIOR_ACCESS).orElse(null);
        if (ship == null) return SupplyLoadResult.NO_ACCESSIBLE_SHIP;
        return loadSupply(ship, type, player.level().getServer());
    }

    static SupplyLoadResult loadSupply(ShipState ship, SupplyType type, MinecraftServer server) {
        ShipSystemsRuntime systems = systems(ship);
        double accepted = switch (type) {
            case PROPELLANT -> systems.loadPropellantCell();
            case OXYGEN -> systems.loadOxygenCartridge();
        };
        if (accepted <= 1.0E-9D) return SupplyLoadResult.TANK_FULL;
        ShipSystemsSavedData.get(server).put(systems.snapshot());
        return SupplyLoadResult.LOADED;
    }""",
)

runtime = java / "ship/runtime/minecraft/ShipRuntimeManager.java"
replace_once(
    runtime,
    "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.entity.Display;",
    "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.InteractionHand;\nimport net.minecraft.world.InteractionResult;\nimport net.minecraft.world.entity.Display;",
)
replace_once(
    runtime,
    """    public static LaunchDeploymentResult deployLaunchCraft(ServerPlayer player, ServerLevel level, Vec3 spawn, long tick) {
        if (REPOSITORY.findOwnedBy(player.getUUID()).isPresent()) {
            return LaunchDeploymentResult.ALREADY_OWNS_CRAFT;
        }
        return createStarterCraft(player, level, spawn) == null
                ? LaunchDeploymentResult.DEPLOYMENT_FAILED
                : LaunchDeploymentResult.DEPLOYED;
    }""",
    """    public static LaunchDeploymentResult deployLaunchCraft(ServerPlayer player, ServerLevel level, Vec3 spawn, long tick) {
        if (REPOSITORY.findOwnedBy(player.getUUID()).isPresent()) {
            return LaunchDeploymentResult.ALREADY_OWNS_CRAFT;
        }
        Entry entry = createStarterCraft(player, level, spawn);
        if (entry == null) {
            return LaunchDeploymentResult.DEPLOYMENT_FAILED;
        }
        boardAndControl(player, entry.exterior(), tick);
        return LaunchDeploymentResult.DEPLOYED;
    }""",
)

marker = "    public static boolean controlNearest(ServerPlayer player, long tick) {"
service = '''    public static InteractionResult serviceSupply(
            ServerPlayer player,
            ShipExteriorEntity exterior,
            InteractionHand hand,
            ShipSystemsManager.SupplyType type
    ) {
        Entry entry = ENTRIES.get(exterior.getId());
        if (entry == null || entry.exterior() != exterior || exterior.isRemoved() || exterior.level() != player.level()) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.supply.no_ship"), true);
            return InteractionResult.FAIL;
        }
        ShipState ship = entry.runtime().ship();
        if (!ship.can(player.getUUID(), ShipPermission.INTERIOR_ACCESS)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.supply.no_ship"), true);
            return InteractionResult.FAIL;
        }

        ShipSystemsManager.SupplyLoadResult result = ShipSystemsManager.loadSupply(
                ship, type, player.level().getServer());
        return switch (result) {
            case LOADED -> {
                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(hand).shrink(1);
                }
                player.sendSystemMessage(Component.translatable(
                        type == ShipSystemsManager.SupplyType.PROPELLANT
                                ? "message.earth_to_stars.supply.propellant_loaded"
                                : "message.earth_to_stars.supply.oxygen_loaded"
                ), true);
                yield InteractionResult.SUCCESS_SERVER;
            }
            case TANK_FULL -> {
                player.sendSystemMessage(Component.translatable(
                        type == ShipSystemsManager.SupplyType.PROPELLANT
                                ? "message.earth_to_stars.supply.propellant_full"
                                : "message.earth_to_stars.supply.oxygen_full"
                ), true);
                yield InteractionResult.SUCCESS_SERVER;
            }
            case NO_ACCESSIBLE_SHIP -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.supply.no_ship"), true);
                yield InteractionResult.FAIL;
            }
        };
    }

'''
replace_once(runtime, marker, service + marker)

old_vehicle = '''    private static VehiclePair createVehiclePair(ServerLevel level, Vec3 position, float yaw, float pitch) {
        ShipExteriorEntity exterior = new ShipExteriorEntity(EarthToStarsEntities.SHIP_EXTERIOR.get(), level);
        exterior.setPos(position.x, position.y, position.z);
        exterior.setYRot(yaw);
        exterior.setXRot(pitch);
        if (!level.addFreshEntity(exterior)) {
            return null;
        }

        Display.ItemDisplay visual = SpaceVisualFactory.create(
                level,
                EarthToStarsItems.STARTER_CRAFT_VISUAL.get(),
                new ShipVec3(position.x, position.y, position.z),
                yaw,
                pitch
        );
        if (!level.addFreshEntity(visual)) {
            exterior.discard();
            return null;
        }
        return new VehiclePair(exterior, visual);
    }'''
new_vehicle = '''    private static VehiclePair createVehiclePair(ServerLevel level, Vec3 position, float yaw, float pitch) {
        ShipExteriorEntity exterior = new ShipExteriorEntity(EarthToStarsEntities.SHIP_EXTERIOR.get(), level);
        exterior.setVisualItem(EarthToStarsItems.STARTER_CRAFT_VISUAL.get());
        exterior.setPos(position.x, position.y, position.z);
        exterior.setYRot(yaw);
        exterior.setXRot(pitch);
        if (!level.addFreshEntity(exterior)) {
            return null;
        }
        return new VehiclePair(exterior, exterior);
    }'''
replace_once(runtime, old_vehicle, new_vehicle)

vendor = res / "models/kenney/space_kit"
runtime_models = res / "models/runtime"
runtime_models.mkdir(parents=True, exist_ok=True)
specs = {
    "craft_speedera.obj": ("starter_craft.obj", 2.60),
    "craft_miner.obj": ("orbital_salvage.obj", 1.35),
    "craft_racer.obj": ("orbital_interceptor.obj", 1.50),
}
for source_name, (output_name, scale) in specs.items():
    out = [
        "# Minecraft-safe runtime adaptation of Kenney Space Kit (CC0).",
        f"# source={source_name} scale={scale:.2f} uv=center-sampled",
    ]
    for line in read(vendor / source_name).splitlines():
        stripped = line.strip()
        if stripped.startswith("mtllib "):
            continue
        if stripped.startswith("v "):
            parts = stripped.split()
            if len(parts) < 4:
                raise SystemExit(f"invalid vertex in {source_name}: {line}")
            x, y, z = (float(parts[1]), float(parts[2]), float(parts[3]))
            out.append(f"v {x * scale:.7f} {y * scale:.7f} {z * scale:.7f}")
        elif stripped.startswith("vt "):
            out.append("vt 0.5000000 0.5000000")
        else:
            out.append(line)
    write(runtime_models / output_name, "\n".join(out) + "\n")

model_mapping = {
    "starter_craft_visual.json": ("earth_to_stars:models/runtime/starter_craft.obj", "earth_to_stars:models/kenney/space_kit/craft_speedera.mtl"),
    "orbital_salvage_visual.json": ("earth_to_stars:models/runtime/orbital_salvage.obj", "earth_to_stars:models/kenney/space_kit/craft_miner.mtl"),
    "orbital_interceptor_visual.json": ("earth_to_stars:models/runtime/orbital_interceptor.obj", "earth_to_stars:models/kenney/space_kit/craft_racer.mtl"),
    "launch_craft_kit.json": ("earth_to_stars:models/runtime/starter_craft.obj", "earth_to_stars:models/kenney/space_kit/craft_speedera.mtl"),
}
for name, (model_path, mtl_path) in model_mapping.items():
    path = res / "models/item" / name
    data = json.loads(read(path))
    data["model"] = model_path
    data["mtl_override"] = mtl_path
    if name == "launch_craft_kit.json":
        data["display"] = {
            "gui": {"rotation": [25, 225, 0], "scale": [0.22, 0.22, 0.22]},
            "ground": {"scale": [0.14, 0.14, 0.14]},
            "fixed": {"scale": [0.22, 0.22, 0.22]},
            "firstperson_righthand": {"rotation": [0, 225, 0], "scale": [0.18, 0.18, 0.18]},
            "firstperson_lefthand": {"rotation": [0, 135, 0], "scale": [0.18, 0.18, 0.18]},
            "thirdperson_righthand": {"rotation": [0, 225, 0], "scale": [0.16, 0.16, 0.16]},
            "thirdperson_lefthand": {"rotation": [0, 135, 0], "scale": [0.16, 0.16, 0.16]},
        }
    write(path, json.dumps(data, ensure_ascii=False, indent=2) + "\n")

for locale in ("ko_kr", "en_us"):
    path = res / "lang" / f"{locale}.json"
    data = json.loads(read(path))
    if locale == "ko_kr":
        data["message.earth_to_stars.launch.blocked"] = "개척선을 펼칠 공간이 부족합니다. 주변의 단단한 장애물을 치워 주세요."
        data["message.earth_to_stars.supply.use_on_ship"] = "보급품을 들고 개척선 본체에 사용하세요."
        data["message.earth_to_stars.launch.deployed"] = "개척선이 전개되고 조종석이 온라인 상태로 전환됩니다."
    else:
        data["message.earth_to_stars.launch.blocked"] = "The launch craft needs more room. Clear nearby solid obstacles."
        data["message.earth_to_stars.supply.use_on_ship"] = "Use this supply directly on the launch craft hull."
        data["message.earth_to_stars.launch.deployed"] = "The launch craft deploys and the pilot seat comes online."
    write(path, json.dumps(data, ensure_ascii=False, indent=2) + "\n")

replace_once(project / "gradle.properties", "mod_version=0.1.0-alpha.13", "mod_version=0.1.0-alpha.14")
replace_once(
    java / "EarthToStars.java",
    'public static final String VERSION = "0.1.0-alpha.13";',
    'public static final String VERSION = "0.1.0-alpha.14";',
)

verifier = project / "tools/verify_jar.py"
replace_once(
    verifier,
    '            "assets/earth_to_stars/models/kenney/space_kit/craft_speedera.obj",',
    '''            "assets/earth_to_stars/models/runtime/starter_craft.obj",
            "assets/earth_to_stars/models/runtime/orbital_salvage.obj",
            "assets/earth_to_stars/models/runtime/orbital_interceptor.obj",
            "assets/earth_to_stars/models/kenney/space_kit/craft_speedera.obj",''',
)
replace_once(verifier, "missing M1/alpha.13 gameplay class", "missing M1/alpha.14 gameplay class")

validator = project / "tools/validate_alpha14_acceptance.py"
write(validator, '''#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

import validate_alpha12_acceptance as alpha12

PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java"
RES = PROJECT / "src/main/resources"


class AcceptanceError(RuntimeError):
    pass


def fail(message: str) -> None:
    raise AcceptanceError(message)


def text(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def validate_target() -> None:
    props = text(PROJECT / "gradle.properties")
    for required in ("minecraft_version=26.2", "neo_version=26.2.0.76", "mod_version=0.1.0-alpha.14"):
        if required not in props:
            fail(f"target/version contract missing: {required}")


def validate_single_visible_vehicle() -> None:
    exterior = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.java")
    if "extends Display.ItemDisplay" not in exterior:
        fail("starter craft exterior is not the visible ItemDisplay vehicle")
    for required in ("setVisualItem", "isPickable()", "boardAndControl(serverPlayer, this)", "PROPELLANT_CELL", "OXYGEN_CARTRIDGE"):
        if required not in exterior:
            fail(f"visible vehicle interaction contract missing: {required}")

    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    create_body = runtime.split("private static VehiclePair createVehiclePair", 1)[1].split("private static void executeTransition", 1)[0]
    if "SpaceVisualFactory.create" in create_body:
        fail("starter craft still spawns a separate visual proxy")
    for required in (
        "exterior.setVisualItem(EarthToStarsItems.STARTER_CRAFT_VISUAL.get())",
        "new VehiclePair(exterior, exterior)",
        "player.startRiding(exterior)",
        "boardAndControl(player, entry.exterior(), tick)",
    ):
        if required not in runtime:
            fail(f"single visible vehicle/boarding contract missing: {required}")

    entities = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsEntities.java")
    if ".sized(5.4F, 2.3F)" not in entities:
        fail("starter craft hitbox did not grow with the 5-block-class visible hull")
    if "EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F" not in entities:
        fail("pilot seat is not aligned to the enlarged craft")


def validate_deployment_and_service_ux() -> None:
    launch = text(JAVA / "kr/moonseungjun/earthtostars/content/LaunchCraftKitItem.java")
    if "DEPLOY_RADIUS = 2" not in launch or "state.canBeReplaced()" not in launch or "level.removeBlock(pos, false)" not in launch:
        fail("deployment does not accept and clear harmless replaceable vegetation")
    if "3×3×3" in launch or "3x3x3" in launch:
        fail("obsolete tiny deployment footprint remains")

    supply = text(JAVA / "kr/moonseungjun/earthtostars/content/ShipSupplyItem.java")
    if "loadSupply(player" in supply:
        fail("supply item still magically loads a nearby ship from arbitrary block interaction")
    if "message.earth_to_stars.supply.use_on_ship" not in supply:
        fail("supply item does not direct service to the actual hull")

    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    if "ShipSystemsManager.loadSupply(\n                ship, type, player.level().getServer())" not in runtime:
        fail("hull service is not bound to the exact authoritative ship entry")


def obj_bounds_and_uv(path: Path) -> tuple[float, float, float, float, float, float, int]:
    xs, ys, zs = [], [], []
    uv_count = 0
    for line in text(path).splitlines():
        if line.startswith("v "):
            _, x, y, z, *_ = line.split()
            xs.append(float(x)); ys.append(float(y)); zs.append(float(z))
        elif line.startswith("vt "):
            parts = line.split()
            uv_count += 1
            if len(parts) < 3 or abs(float(parts[1]) - 0.5) > 1e-7 or abs(float(parts[2]) - 0.5) > 1e-7:
                fail(f"{path.name}: runtime UV can still bleed across the item atlas")
    if not xs or uv_count == 0:
        fail(f"{path.name}: runtime OBJ is incomplete")
    return min(xs), max(xs), min(ys), max(ys), min(zs), max(zs), uv_count


def validate_runtime_models() -> None:
    runtime_dir = RES / "assets/earth_to_stars/models/runtime"
    starter = obj_bounds_and_uv(runtime_dir / "starter_craft.obj")
    width = starter[1] - starter[0]
    height = starter[3] - starter[2]
    length = starter[5] - starter[4]
    if width < 5.0 or length < 5.0 or height < 1.8:
        fail(f"starter craft is still toy-sized: {width:.2f} x {height:.2f} x {length:.2f}")

    mapping = {
        "starter_craft_visual.json": "earth_to_stars:models/runtime/starter_craft.obj",
        "launch_craft_kit.json": "earth_to_stars:models/runtime/starter_craft.obj",
        "orbital_salvage_visual.json": "earth_to_stars:models/runtime/orbital_salvage.obj",
        "orbital_interceptor_visual.json": "earth_to_stars:models/runtime/orbital_interceptor.obj",
    }
    for name, expected in mapping.items():
        model = json.loads(text(RES / "assets/earth_to_stars/models/item" / name))
        if model.get("loader") != "neoforge:obj" or model.get("model") != expected:
            fail(f"{name}: expected adapted runtime model {expected}")
    obj_bounds_and_uv(runtime_dir / "orbital_salvage.obj")
    obj_bounds_and_uv(runtime_dir / "orbital_interceptor.obj")


def main() -> None:
    try:
        alpha12.validate_no_runtime_proxies()
        alpha12.validate_26_2_runtime_api_contract()
        alpha12.validate_recipe_syntax()
        validate_target()
        validate_single_visible_vehicle()
        validate_deployment_and_service_ux()
        validate_runtime_models()
    except (OSError, json.JSONDecodeError, AcceptanceError, alpha12.AcceptanceError) as exc:
        raise SystemExit(f"ALPHA.14 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.14 ACCEPTANCE VALIDATION OK: one visible ride entity, 5-block-class hull/hitbox, vegetation-tolerant deployment, hull-directed servicing and atlas-safe runtime OBJ meshes verified")


if __name__ == "__main__":
    main()
''')

for path in (
    java / "ship/runtime/minecraft/ShipExteriorEntity.java",
    java / "ship/runtime/minecraft/ShipRuntimeManager.java",
    java / "content/LaunchCraftKitItem.java",
    project / "tools/validate_alpha14_acceptance.py",
):
    if not path.is_file() or path.stat().st_size < 300:
        raise SystemExit(f"alpha14 patch produced invalid file: {path}")

print("EARTH_TO_STARS_ALPHA14_PATCH_READY")
