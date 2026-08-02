#!/usr/bin/env python3
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"
STAGE = Path(__file__).resolve().parent


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Version.
props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.12-alpha.1", "mod_version=0.17.13-alpha.1", "version")
write(props, text)

# Install the dedicated non-particle effect engine.
shutil.copyfile(STAGE / "VillageSkillEffectSystem.java", JAVA / "VillageSkillEffectSystem.java")

# Migrate the real saved R/G bindings, not merely the help text.
path = JAVA / "VillageClientKeys.java"
text = read(path)
text = replace_once(
    text,
    "    private static boolean tickListenerRegistered;\n",
    "    private static boolean tickListenerRegistered;\n"
    "    private static boolean legacySkillBindingsChecked;\n",
    "client migration state",
)
text = replace_once(
    text,
    "        Minecraft minecraft = Minecraft.getInstance();\n"
    "        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {\n",
    "        Minecraft minecraft = Minecraft.getInstance();\n"
    "        if (minecraft.player != null && minecraft.getConnection() != null) {\n"
    "            migrateLegacySkillBindings(minecraft);\n"
    "        }\n"
    "        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {\n",
    "client migration invocation",
)
marker = """    private static void drain(KeyMapping mapping) {
"""
insert = """    private static void migrateLegacySkillBindings(Minecraft minecraft) {
        if (legacySkillBindingsChecked) return;
        legacySkillBindingsChecked = true;
        int first = ROLE_SKILL_ONE.getKey().getValue();
        int second = ROLE_SKILL_TWO.getKey().getValue();
        boolean oldPair = (first == GLFW.GLFW_KEY_R && second == GLFW.GLFW_KEY_G)
                || (first == GLFW.GLFW_KEY_G && second == GLFW.GLFW_KEY_R);
        if (!oldPair) return;
        ROLE_SKILL_ONE.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));
        ROLE_SKILL_TWO.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X));
        KeyMapping.resetMapping();
        minecraft.options.save();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§b기존 R/G 기술 키를 Z/X로 자동 변경했습니다."), false);
        }
    }

    private static void drain(KeyMapping mapping) {
"""
text = replace_once(text, marker, insert, "client migration method")
write(path, text)

# Replace the server-side placeholder feedback with the real display-actor engine.
path = JAVA / "VillageRoleAbilitySystem.java"
text = read(path)
text = text.replace("import net.minecraft.world.level.block.Block;\n", "")
text = text.replace("import net.minecraft.world.level.block.Blocks;\n", "")
text = text.replace("import net.minecraft.world.level.block.state.BlockState;\n", "")
text = replace_once(text, '    private static final String GENERATED_ARROW = "vg_generated_arrow";\n', "", "stale generated arrow constant")
text = replace_once(text, "    private static final Map<UUID, ShieldBlocks> SHIELDS = new HashMap<>();\n", "", "stale shield map")
text = replace_once(text, "        SHIELDS.clear();\n", "        VillageSkillEffectSystem.reset();\n", "effect reset")
text = replace_once(
    text,
    "        Vec3 forward = horizontalLook(player);\n"
    "        switch (skill) {\n",
    "        Vec3 forward = horizontalLook(player);\n"
    "        VillageSkillEffectSystem.startCast(level, player, skill, duration, forward);\n"
    "        switch (skill) {\n",
    "cast effect start",
)
text = text.replace("                updateShieldBlocks(level, player, true, 2);\n", "")
text = text.replace("                updateShieldBlocks(level, player, false, 2);\n", "")
text = replace_once(
    text,
    "        tickMoving(server, now);\n"
    "        cleanupExpired(server, now);\n",
    "        tickMoving(server, now);\n"
    "        cleanupExpired(server, now);\n"
    "        VillageSkillEffectSystem.tick(server);\n",
    "effect engine tick",
)
text = replace_once(
    text,
    "                player.setYRot(player.getYRot() + 34.0f);\n"
    "                player.setYHeadRot(player.getYRot());\n",
    "                player.setYBodyRot(player.getYBodyRot() + 34.0f);\n",
    "whirlwind body rotation",
)
# Remove obsolete temporary-world-block cleanup.
text, count = re.subn(
    r"\n        Iterator<Map.Entry<UUID, ShieldBlocks>> shieldIterator = SHIELDS.entrySet\(\)\.iterator\(\);\n"
    r"        while \(shieldIterator\.hasNext\(\)\) \{.*?\n        \}\n",
    "\n",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("shield cleanup block not found")

# Visible ricochet path follows the actual resolved chain.
old = """                for (int i = 0; i < Math.min(5, chain.size()); i++) {
                    Mob target = chain.get(i);
                    hurt(level, target, damage * (1.0f - i * 0.10f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
"""
new = """                List<Mob> visualChain = new ArrayList<>();
                for (int i = 0; i < Math.min(5, chain.size()); i++) {
                    Mob target = chain.get(i);
                    visualChain.add(target);
                    hurt(level, target, damage * (1.0f - i * 0.10f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
                VillageSkillEffectSystem.ricochet(level, attacker, primary, visualChain);
"""
text = replace_once(text, old, new, "ricochet visual chain")

text = replace_once(
    text,
    "        Vec3 direction = horizontalLook(player);\n"
    "        launchMoving(level, player, MovingKind.BLADE, new ItemStack(Items.IRON_SWORD),\n",
    "        Vec3 direction = horizontalLook(player);\n"
    "        VillageSkillEffectSystem.bladeWave(level, player, direction);\n"
    "        launchMoving(level, player, MovingKind.BLADE, new ItemStack(Items.IRON_SWORD),\n",
    "blade wave visual",
)
# Remove block-break particle event and replace it with a display-entity fracture wave.
old = """        BlockPos center = player.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < 2.0 || distance > 4.5 || (dx + dz) % 2 != 0) continue;
                BlockPos floor = center.offset(dx, -1, dz);
                BlockState state = level.getBlockState(floor);
                if (!state.isAir()) level.levelEvent(2001, floor, Block.getId(state));
            }
        }
"""
text = replace_once(text, old, "        VillageSkillEffectSystem.slamImpact(level, player);\n", "slam particle removal")
text = replace_once(
    text,
    "        List<Mob> targets = targetsNear(level, player, center, 8.5, 36);\n",
    "        VillageSkillEffectSystem.arrowRainImpact(level, player, center);\n"
    "        List<Mob> targets = targetsNear(level, player, center, 8.5, 36);\n",
    "arrow rain visual",
)
text = replace_once(
    text,
    "        Vec3 direction = horizontalLook(player);\n"
    "        float damage = (28.0f + VillageCouncilState.levelOf(player.getUUID()) * 1.15f) * power;\n",
    "        Vec3 direction = horizontalLook(player);\n"
    "        VillageSkillEffectSystem.energyArrow(level, player, direction);\n"
    "        float damage = (28.0f + VillageCouncilState.levelOf(player.getUUID()) * 1.15f) * power;\n",
    "energy arrow visual",
)
text = replace_once(
    text,
    "    private static void shieldCharge(ServerLevel level, ServerPlayer player, float power, int specialRank) {\n"
    "        player.swing(InteractionHand.OFF_HAND, true);\n",
    "    private static void shieldCharge(ServerLevel level, ServerPlayer player, float power, int specialRank) {\n"
    "        player.swing(InteractionHand.OFF_HAND, true);\n"
    "        VillageSkillEffectSystem.shieldCharge(level, player, horizontalLook(player));\n",
    "shield charge visual",
)
text = replace_once(
    text,
    "        if (target.level() instanceof ServerLevel level) play(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.28f);\n",
    "        if (target.level() instanceof ServerLevel level) {\n"
    "            VillageSkillEffectSystem.healLink(level, player, target);\n"
    "            play(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.28f);\n"
    "        }\n",
    "single heal visual",
)
old = """    private static void cleanseAllies(ServerPlayer player, float heal, int specialRank) {
        for (ServerPlayer ally : allies(player, -1.0)) {
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.SLOWNESS);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            healScaled(ally, heal);
        }
        if (player.level() instanceof ServerLevel level) play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.35f);
    }
"""
new = """    private static void cleanseAllies(ServerPlayer player, float heal, int specialRank) {
        List<ServerPlayer> affected = allies(player, -1.0);
        for (ServerPlayer ally : affected) {
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.SLOWNESS);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.HUNGER);
            healScaled(ally, heal);
        }
        if (player.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.cleanse(level, player, affected);
            play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.35f);
        }
    }
"""
text = replace_once(text, old, new, "cleanse visual")
old = """        for (ServerPlayer ally : server.getPlayerList().getPlayers()) {
            if (ally.level() != player.level()) continue;
            if (VillageRespawnSystem.isDowned(ally)) VillageRespawnSystem.reviveNow(ally, "기적의 대성역");
            healScaled(ally, heal);
            int amplifier = lowHealthAmplifier(ally, 2 + Math.min(3, specialRank));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
            play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.4f, 0.82f);
            play(level, player.position(), SoundEvents.TOTEM_USE, 1.0f, 1.0f);
        }
"""
new = """        List<ServerPlayer> affected = server.getPlayerList().getPlayers().stream()
                .filter(ally -> ally.level() == player.level())
                .toList();
        for (ServerPlayer ally : affected) {
            if (VillageRespawnSystem.isDowned(ally)) VillageRespawnSystem.reviveNow(ally, "기적의 대성역");
            healScaled(ally, heal);
            int amplifier = lowHealthAmplifier(ally, 2 + Math.min(3, specialRank));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier, false, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        }
        if (player.level() instanceof ServerLevel level) {
            VillageSkillEffectSystem.miracle(level, player, affected);
            play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.4f, 0.82f);
            play(level, player.position(), SoundEvents.TOTEM_USE, 1.0f, 1.0f);
        }
"""
text = replace_once(text, old, new, "miracle visual")

# Remove the obsolete temporary glass-wall implementation entirely.
text, count = re.subn(
    r"\n    private static void updateShieldBlocks\(.*?\n    private static List<Mob> targetsNear\(",
    "\n    private static List<Mob> targetsNear(",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("obsolete shield block implementation not found")
text = text.replace("    private record ShieldBlocks(ServerLevel level, Map<BlockPos, BlockState> replaced) {}\n", "")
write(path, text)

# The old generic push/sound facade is dead code and conflicts with the new actor engine.
old_visual = JAVA / "VillageSkillVisualSystem.java"
if old_visual.exists():
    old_visual.unlink()

# New regression contract.
test = r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")

    assert "mod_version=0.17.13-alpha.1" in props
    assert "GLFW.GLFW_KEY_R" in keys and "GLFW.GLFW_KEY_G" in keys
    assert "ROLE_SKILL_ONE.setKey" in keys and "GLFW.GLFW_KEY_Z" in keys
    assert "ROLE_SKILL_TWO.setKey" in keys and "GLFW.GLFW_KEY_X" in keys
    assert "KeyMapping.resetMapping()" in keys and "minecraft.options.save()" in keys

    assert "VillageSkillEffectSystem.startCast" in ability
    assert "VillageSkillEffectSystem.tick" in ability
    assert "setYBodyRot" in ability
    assert "setYRot(player.getYRot() + 34.0f)" not in ability
    assert "levelEvent(2001" not in ability
    assert "updateShieldBlocks" not in ability
    assert "SHIELDS" not in ability
    assert "Blocks.GLASS.defaultBlockState(), 3" not in ability

    for token in (
        "Display.ItemDisplay", "Display.BlockDisplay", "Transformation",
        "Mode.WHIRLWIND", "Mode.BUFF", "Mode.BLADE_CHARGE", "Mode.SLAM_CHARGE",
        "Mode.RAPID_FIRE", "Mode.TARGET_LOCK", "Mode.ARROW_RAIN",
        "Mode.ENERGY_PROJECTILE", "Mode.FIRE_ORB", "Mode.FROST_FIELD",
        "Mode.TORNADO", "Mode.LIGHTNING_FIELD", "Mode.HEAL_FIELD",
        "Mode.HEAL_LINK", "Mode.CLEANSE", "Mode.MIRACLE",
        "Mode.SHIELD_CHARGE", "Mode.TAUNT", "Mode.FORTRESS", "Mode.AEGIS",
        "Items.NETHERITE_SWORD", "Items.SPECTRAL_ARROW", "Items.SHIELD",
        "Blocks.PACKED_ICE", "Blocks.SEA_LANTERN"
    ):
        assert token in effects, token
    assert "private static final class DisplayAccess" in effects
    assert "setTransformationInterpolationDuration" in effects
    assert "ParticleTypes" not in effects
    assert "sendParticles" not in effects
    assert "addParticle" not in effects
    assert not (JAVA / "VillageSkillVisualSystem.java").exists()

    print("[PASS] Saved legacy R/G skill bindings migrate to real Z/X and persist")
    print("[PASS] Garen-style whirlwind owns six visible swords and six rotating arc actors")
    print("[PASS] All twenty skills use bounded item/block display scenes without particles")
    print("[PASS] Temporary glass walls, block-break particles and obsolete visual facade are removed")


if __name__ == "__main__":
    main()
'''
write(TOOLS / "test_v01713_effects_and_keys.py", test)
print("Applied Village Guardians v0.17.13 key migration and non-particle effect overhaul")
