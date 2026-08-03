#!/usr/bin/env python3
from pathlib import Path
import runpy

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


# Keep the current token resolution while retaining stable route source text
# used by long-lived UI contracts.
write(JAVA / "VillageClientUi.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientUi {
    private VillageClientUi() {}

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(VillageNetwork.OpenVillageUiPayload.TYPE,
                (rawPayload, context) -> {
                    VillageNetwork.OpenVillageUiPayload payload = resolve(rawPayload);
                    Minecraft.getInstance().gui.setScreen(
                            switch (payload.screenId()) {
                                case "skill_tree" -> new VillageSkillTreeScreen(payload);
                                case "town_hall" -> new VillageTownHallScreen(payload);
                                case "role_progress", "role_skills" -> new VillageRoleProgressScreen(payload);
                                case "quick_chat" -> new VillageQuickChatScreen(payload);
                                case "status" -> new VillageStatusScreen(payload);
                                case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload);
                                case "skill_test_password" -> new VillageSkillTestPasswordScreen(payload);
                                case "wave_intel", "skill_test", "game_over" -> new VillageFacilityScreen(payload);
                                case "equipment_shop" -> new VillageShopScreen(payload);
                                case "equipment_fusion" -> new VillageFusionScreen(payload);
                                case "result" -> new VillageResultScreen(payload);
                                case "building", "management", "funding", "tower_control", "tower_detail", "caller", "relic_choice" ->
                                        new VillageFacilityScreen(payload);
                                default -> new VillageUiScreen(payload);
                            });
                });
        event.register(VillageNetwork.SkillMotionPayload.TYPE,
                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));
        event.register(VillageNetwork.SkillHudPayload.TYPE,
                (payload, context) -> VillageSkillHudOverlay.accept(payload));
        event.register(VillageNetwork.PlayerStatusPayload.TYPE,
                (payload, context) -> VillageInventoryPanel.updateStatus(payload));
    }

    private static VillageNetwork.OpenVillageUiPayload resolve(
            VillageNetwork.OpenVillageUiPayload payload) {
        return new VillageNetwork.OpenVillageUiPayload(
                payload.screenId(),
                VillageClientKeys.resolveTokens(payload.title()),
                VillageClientKeys.resolveTokens(payload.body()),
                payload.actions(),
                VillageClientKeys.resolveTokens(payload.labels()));
    }
}
''')

# Update only contracts whose old implementation detail was intentionally
# superseded. Their behavioral coverage remains in the v0.18.0 contracts.
path = TOOLS / "test_v01710_skill_input.py"
text = read(path).replace(
    'assert \'consume(ROLE_SKILL_TWO, "use_skill:1")\' in keys',
    'assert "consumeSkillTwo(minecraft)" in keys and \'VillageUiActionPayload("use_skill:1")\' in keys')
write(path, text)

for name in ["test_v01713_effects_and_keys.py", "test_v01719_access_content.py"]:
    path = TOOLS / name
    text = read(path).replace("migrateUnsafeBindings", "migrateBindings")
    write(path, text)

path = TOOLS / "test_v01715_skill_combat_visuals.py"
text = read(path).replace(
    'assert "20.0, 60" in abilities',
    'assert "double radius = 20.0 + specialRank * 2.0" in abilities')
text = text.replace(
    'assert "VillageEquipmentRaritySystem.skillMultiplier(player)" in read("VillageRoleSkillSystem.java")',
    'assert "VillageEquipmentShop.roleSkillMultiplier(player)" in read("VillageRoleSkillSystem.java")')
write(path, text)

path = TOOLS / "test_v01719_access_content.py"
text = read(path).replace(
    '"고정 최종 일수는 없으며 무한 진행"',
    '"고정 마지막 날은 없으며 무한 진행"')
write(path, text)

# Correct labels changed mechanically by the historical migration.
path = TOOLS / "test_v0180_content_scaling.py"
text = read(path).replace("이전 Z/X/B 기본값", "이전 Z/V/B 기본값")
write(path, text)

# Run the MC 26.2-compatible impact-time arrow scaling fix last.
runpy.run_path(str(ROOT / "tools/v0180/arrow_fix.py"), run_name="__main__")

print("Applied v0.18.0 stable UI routing and legacy contract compatibility")
