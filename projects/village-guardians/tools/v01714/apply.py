#!/usr/bin/env python3
from pathlib import Path
import base64
import io
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
STAGE = Path(__file__).resolve().parent
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"

encoded = "".join(path.read_text(encoding="ascii") for path in sorted(STAGE.glob("payload.part*")))
archive = base64.b64decode(encoded)
with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
    bundle.extractall(STAGE)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


for name in (
    "VillageSkillEffectEntities.java",
    "VillageSkillEffectEntity.java",
    "VillageSkillEffectRenderState.java",
    "VillageSkillEffectRenderer.java",
    "VillageSkillEffectClient.java",
    "VillageSkillMeshLibrary.java",
    "VillageSkillEffectSystem.java",
):
    shutil.copyfile(STAGE / name, JAVA / name)
shutil.copyfile(STAGE / "generate_skill_mesh_texture.py", TOOLS / "generate_skill_mesh_texture.py")
shutil.copyfile(STAGE / "test_v01714_custom_mesh_effects.py", TOOLS / "test_v01714_custom_mesh_effects.py")

props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.13-alpha.1",
                    "mod_version=0.17.14-alpha.1", "version")
write(props, text)

path = JAVA / "VillageGuardians.java"
text = read(path)
text = replace_once(
    text,
    "    public VillageGuardians(IEventBus modEventBus) {\n"
    "        modEventBus.addListener(VillageNetwork::registerPayloads);\n",
    "    public VillageGuardians(IEventBus modEventBus) {\n"
    "        VillageSkillEffectEntities.register(modEventBus);\n"
    "        modEventBus.addListener(VillageNetwork::registerPayloads);\n",
    "effect entity registration",
)
write(path, text)

path = JAVA / "VillageRoleSkillSystem.java"
text = read(path)
text = replace_once(text, '"회전 칼날"', '"회전 검무"', "skill display name")
write(path, text)

path = JAVA / "VillageNetwork.java"
text = read(path)
text = replace_once(
    text,
    "        registrar.playToClient(PlayerStatusPayload.TYPE, PlayerStatusPayload.STREAM_CODEC);\n",
    "        registrar.playToClient(PlayerStatusPayload.TYPE, PlayerStatusPayload.STREAM_CODEC);\n"
    "        registrar.playToClient(SkillMotionPayload.TYPE, SkillMotionPayload.STREAM_CODEC);\n",
    "motion payload registration",
)
text = replace_once(
    text,
    "    public static void sendPlayerStatus(ServerPlayer player) {\n",
    "    public static void sendSkillMotion(\n"
    "            net.minecraft.server.level.ServerLevel level,\n"
    "            ServerPlayer owner,\n"
    "            String motion,\n"
    "            int durationTicks) {\n"
    "        if (level == null || owner == null || motion == null || durationTicks <= 0) return;\n"
    "        SkillMotionPayload payload = new SkillMotionPayload(\n"
    "                owner.getId(), motion, durationTicks);\n"
    "        for (ServerPlayer viewer : level.players()) {\n"
    "            PacketDistributor.sendToPlayer(viewer, payload);\n"
    "        }\n"
    "    }\n\n"
    "    public static void sendPlayerStatus(ServerPlayer player) {\n",
    "motion payload sender",
)
marker = "    public record VillageUiActionPayload(String action) implements CustomPacketPayload {\n"
record = '''    public record SkillMotionPayload(
            int entityId,
            String motion,
            int durationTicks) implements CustomPacketPayload {
        public static final Type<SkillMotionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "skill_motion"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillMotionPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SkillMotionPayload::entityId,
                        ByteBufCodecs.STRING_UTF8, SkillMotionPayload::motion,
                        ByteBufCodecs.VAR_INT, SkillMotionPayload::durationTicks,
                        SkillMotionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VillageUiActionPayload(String action) implements CustomPacketPayload {
'''
text = replace_once(text, marker, record, "motion payload record")
write(path, text)

path = JAVA / "VillageClientUi.java"
text = read(path)
text = replace_once(
    text,
    "        event.register(VillageNetwork.PlayerStatusPayload.TYPE,\n"
    "                (payload, context) -> VillageInventoryPanel.updateStatus(payload));\n",
    "        event.register(VillageNetwork.SkillMotionPayload.TYPE,\n"
    "                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));\n"
    "        event.register(VillageNetwork.PlayerStatusPayload.TYPE,\n"
    "                (payload, context) -> VillageInventoryPanel.updateStatus(payload));\n",
    "client motion handler",
)
write(path, text)

path = JAVA / "VillageRoleAbilitySystem.java"
text = read(path)
text = text.replace("import net.minecraft.world.entity.LightningBolt;\n", "")
text = replace_once(
    text,
    "                if (now % 3L == 0L) {\n"
    "                    player.swing(InteractionHand.MAIN_HAND, true);\n"
    "                    damageRadius(level, player, player.position(), 4.7, 10,\n",
    "                if (now % 3L == 0L) {\n"
    "                    damageRadius(level, player, player.position(), 4.7, 10,\n",
    "spin holds one weapon instead of repeated arm swing",
)
text = text.replace("                        spawnVisualLightning(level, strike);\n", "")
text, count = re.subn(
    r"\n    private static void spawnVisualLightning\(ServerLevel level, Vec3 position\) \{.*?\n    \}\n",
    "\n",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("vanilla lightning visual method not found")
text = replace_once(
    text,
    "        projectile.setOwner(player);\n"
    "        projectile.setItem(item);\n"
    "        projectile.setPos(origin.x, origin.y, origin.z);\n",
    "        projectile.setOwner(player);\n"
    "        projectile.setItem(ItemStack.EMPTY);\n"
    "        projectile.setInvisible(true);\n"
    "        projectile.setPos(origin.x, origin.y, origin.z);\n",
    "invisible gameplay carrier",
)
text = replace_once(
    text,
    "        arrow.setDeltaMovement(0.0, -2.4, 0.0);\n"
    "        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;\n",
    "        arrow.setDeltaMovement(0.0, -2.4, 0.0);\n"
    "        arrow.setInvisible(true);\n"
    "        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;\n",
    "invisible generated rain arrow",
)
write(path, text)

path = TOOLS / "verify_jar.py"
text = read(path)
text = replace_once(
    text,
    '    "META-INF/villageguardians/licensed-gui-assets.txt",\n',
    '    "META-INF/villageguardians/licensed-gui-assets.txt",\n'
    '    "assets/villageguardians/textures/effect/skill_mesh.png",\n',
    "mesh texture requirement",
)
text = replace_once(
    text,
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem.class",\n',
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectEntities.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectEntity.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectRenderState.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectRenderer.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectClient.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillMeshLibrary.class",\n',
    "mesh runtime class requirements",
)
write(path, text)

for test_path in TOOLS.glob("test_*.py"):
    test_text = read(test_path)
    test_text = test_text.replace("mod_version=0.17.13-alpha.1",
                                  "mod_version=0.17.14-alpha.1")
    write(test_path, test_text)

path = TOOLS / "test_enemy_content.py"
text = read(path)
text = text.replace(
    '"SPIN_UNTIL", "EntityTypes.SNOWBALL", "Arrow", "spawnVisualLightning",\n'
    '        "healLowestAlly", "reviveNow", "player.swing", "target.push", "SoundEvents",\n',
    '"SPIN_UNTIL", "EntityTypes.SNOWBALL", "Arrow",\n'
    '        "healLowestAlly", "reviveNow", "player.swing", "target.push", "SoundEvents",\n',
)
text = replace_once(
    text,
    '    for token in (\n'
    '        "Display.ItemDisplay", "Display.BlockDisplay", "Mode.WHIRLWIND",\n'
    '        "Mode.FROST_FIELD", "Mode.TORNADO", "Mode.HEAL_FIELD",\n'
    '        "Mode.FORTRESS", "Mode.AEGIS"\n'
    '    ):\n'
    '        assert token in effects\n',
    '    for token in (\n'
    '        "VillageSkillEffectEntity.spawn", "vanguard_spin", "ranger_energy_projectile",\n'
    '        "arcanist_frost", "arcanist_tornado", "luminar_healing_field",\n'
    '        "warden_fortress", "warden_aegis"\n'
    '    ):\n'
    '        assert token in effects\n'
    '    assert "Display.ItemDisplay" not in effects and "Display.BlockDisplay" not in effects\n',
    "enemy content mesh expectations",
)
text = text.replace("dedicated display-actor scenes", "dedicated procedural-mesh scenes")
write(path, text)

path = TOOLS / "test_v0175_gameplay_ui.py"
text = read(path)
text = replace_once(
    text,
    '    assert "Display.ItemDisplay" in visuals and "Display.BlockDisplay" in visuals\n'
    '    assert "Mode.WHIRLWIND" in visuals and "Mode.FORTRESS" in visuals\n',
    '    assert "VillageSkillEffectEntity.spawn" in visuals\n'
    '    assert "vanguard_spin" in visuals and "warden_fortress" in visuals\n'
    '    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals\n',
    "v0175 mesh expectations",
)
text = text.replace("non-particle display actors", "non-particle procedural mesh actors")
write(path, text)

path = TOOLS / "test_v0178_gameplay.py"
text = read(path)
text = replace_once(
    text,
    '    assert "Display.ItemDisplay" in visuals and "Display.BlockDisplay" in visuals\n',
    '    assert "VillageSkillEffectEntity.spawn" in visuals\n'
    '    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals\n',
    "v0178 mesh expectation",
)
text = text.replace("keyed cast/display path", "keyed cast/custom-mesh path")
write(path, text)

path = TOOLS / "test_v01712_role_abilities.py"
text = read(path)
text = text.replace('"회전 칼날"', '"회전 검무"')
text = text.replace('        "AreaKind.TORNADO", "spawnVisualLightning", "healLowestAlly", "cleanseAllies",\n',
                    '        "AreaKind.TORNADO", "healLowestAlly", "cleanseAllies",\n')
text = replace_once(
    text,
    '    for token in [\n'
    '        "Display.ItemDisplay", "Display.BlockDisplay", "Mode.WHIRLWIND", "Mode.ARROW_RAIN",\n'
    '        "Mode.FROST_FIELD", "Mode.TORNADO", "Mode.HEAL_FIELD", "Mode.FORTRESS", "Mode.AEGIS"\n'
    '    ]:\n'
    '        assert token in effects, token\n',
    '    for token in [\n'
    '        "VillageSkillEffectEntity.spawn", "vanguard_spin", "ranger_rain_field",\n'
    '        "arcanist_frost", "arcanist_tornado", "luminar_healing_field",\n'
    '        "warden_fortress", "warden_aegis"\n'
    '    ]:\n'
    '        assert token in effects, token\n'
    '    assert "Display.ItemDisplay" not in effects and "Display.BlockDisplay" not in effects\n',
    "v01712 mesh expectations",
)
text = text.replace("field and display-actor motions", "field and procedural-mesh motions")
write(path, text)

path = TOOLS / "test_v01713_effects_and_keys.py"
text = read(path)
start = text.index("    for token in (\n", text.index('assert "VillageSkillEffectSystem.startCast"'))
end = text.index('    assert "ParticleTypes" not in effects', start)
replacement = '''    for token in (
        "VillageSkillEffectEntity.spawn", "vanguard_spin", "vanguard_rally",
        "ranger_energy_projectile", "arcanist_fire_orb", "arcanist_frost",
        "arcanist_tornado", "arcanist_lightning", "luminar_healing_field",
        "warden_fortress", "warden_aegis"
    ):
        assert token in effects, token
    for forbidden in (
        "Display.ItemDisplay", "Display.BlockDisplay", "Transformation",
        "Items.", "Blocks.", "DisplayAccess"
    ):
        assert forbidden not in effects, forbidden
'''
text = text[:start] + replacement + text[end:]
text = text.replace('    assert "private static final class DisplayAccess" in effects\n', '')
text = text.replace('    assert "setTransformationInterpolationDuration" in effects\n', '')
text = text.replace(
    '    print("[PASS] Garen-style whirlwind owns six visible swords and six rotating arc actors")\n',
    '    print("[PASS] Garen-style spin rotates the avatar holding one actual weapon")\n',
)
text = text.replace(
    '    print("[PASS] All twenty skills use bounded item/block display scenes without particles")\n',
    '    print("[PASS] All twenty skills route to bounded custom-mesh scenes without particles")\n',
)
write(path, text)

print("Applied Village Guardians v0.17.14 custom mesh and avatar motion overhaul")
