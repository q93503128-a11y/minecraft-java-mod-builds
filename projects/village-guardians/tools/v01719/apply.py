#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Version.
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.17.18-alpha.1", "mod_version=0.17.19-alpha.1", "version")

# Fire orb contact: slightly larger than v0.17.18, but based on each target's
# real bounding box rather than pretending every enemy has the same body size.
ability = JAVA / "VillageRoleAbilitySystem.java"
replace_once(
    ability,
    """            double contactRadius = moving.kind() == MovingKind.FIRE_ORB
                    ? fireOrbContactRadius(moving.specialRank()) : moving.radius();
            List<Mob> hits = targetsNear(level, owner, position, contactRadius, 40);
""",
    """            List<Mob> hits = moving.kind() == MovingKind.FIRE_ORB
                    ? fireOrbContacts(level, owner, position, moving.specialRank(), 40)
                    : targetsNear(level, owner, position, moving.radius(), 40);
""",
    "fire orb contact path",
)
replace_once(
    ability,
    """    private static double fireOrbContactRadius(int specialRank) {
        return Math.min(1.80, 1.40 + Math.max(0, specialRank) * 0.08);
    }

""",
    """    private static List<Mob> fireOrbContacts(
            ServerLevel level, ServerPlayer owner, Vec3 position, int specialRank, int limit) {
        double padding = fireOrbContactPadding(specialRank);
        List<Mob> candidates = new ArrayList<>(targetsNear(
                level, owner, position, padding + 3.5, Math.max(40, limit)));
        candidates.removeIf(target -> !target.getBoundingBox().inflate(padding).contains(position));
        if (candidates.size() > Math.max(0, limit)) {
            return new ArrayList<>(candidates.subList(0, Math.max(0, limit)));
        }
        return candidates;
    }

    private static double fireOrbContactPadding(int specialRank) {
        return Math.min(1.95, 1.55 + Math.min(5, Math.max(0, specialRank)) * 0.08);
    }

""",
    "size-aware fire orb helper",
)

# Replace the client key registry as a single coherent source of truth. X and C
# are vanilla creative-toolbar activators, P/Q are vanilla social/drop keys.
client_keys = JAVA / "VillageClientKeys.java"
client_keys.write_text(r'''package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "controls"));

    // Deliberately avoid every ordinary vanilla gameplay/inventory/social key.
    private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z);
    private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_V);
    private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_B);
    private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_H);
    private static final KeyMapping GROWTH = key("personal_progress", GLFW.GLFW_KEY_J);
    private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K);

    private static final Set<Integer> VANILLA_RESERVED = Set.of(
            GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL,
            GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_T,
            GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ENTER,
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F11,
            GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7,
            GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9);

    private static boolean tickListenerRegistered;
    private static boolean bindingsChecked;

    private VillageClientKeys() {}

    private static KeyMapping key(String id, int key) {
        return new KeyMapping("key.villageguardians." + id, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : mappings()) event.register(mapping);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            migrateUnsafeBindings(minecraft);
        }
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {
            for (KeyMapping mapping : mappings()) drain(mapping);
            return;
        }
        consume(ROLE_SKILL_ONE, "use_skill:0");
        consume(ROLE_SKILL_TWO, "use_skill:1");
        consume(QUICK_COMMUNICATION, "open_quick_chat");
        consume(STATUS, "open_status");
        consume(GROWTH, "open_skill_tree");
        consume(ROLE_PROGRESS, "open_role_progress_current");
    }

    public static String skillOneKeyName() { return keyName(ROLE_SKILL_ONE); }
    public static String skillTwoKeyName() { return keyName(ROLE_SKILL_TWO); }
    public static String quickCommunicationKeyName() { return keyName(QUICK_COMMUNICATION); }
    public static String statusKeyName() { return keyName(STATUS); }
    public static String growthKeyName() { return keyName(GROWTH); }
    public static String roleProgressKeyName() { return keyName(ROLE_PROGRESS); }

    public static String compactSummary() {
        return quickCommunicationKeyName() + " 통신 · "
                + skillOneKeyName() + "/" + skillTwoKeyName() + " 기술";
    }

    private static List<KeyMapping> mappings() {
        return List.of(ROLE_SKILL_ONE, ROLE_SKILL_TWO, QUICK_COMMUNICATION,
                STATUS, GROWTH, ROLE_PROGRESS);
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static void migrateUnsafeBindings(Minecraft minecraft) {
        if (bindingsChecked) return;
        bindingsChecked = true;
        Set<Integer> used = new HashSet<>();
        boolean unsafe = false;
        for (KeyMapping mapping : mappings()) {
            int value = mapping.getKey().getValue();
            if (value <= 0 || VANILLA_RESERVED.contains(value) || !used.add(value)) {
                unsafe = true;
            }
        }
        if (!unsafe) return;

        set(ROLE_SKILL_ONE, GLFW.GLFW_KEY_Z);
        set(ROLE_SKILL_TWO, GLFW.GLFW_KEY_V);
        set(QUICK_COMMUNICATION, GLFW.GLFW_KEY_B);
        set(STATUS, GLFW.GLFW_KEY_H);
        set(GROWTH, GLFW.GLFW_KEY_J);
        set(ROLE_PROGRESS, GLFW.GLFW_KEY_K);
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private static void set(KeyMapping mapping, int key) {
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Discard clicks captured while another screen owns keyboard input.
        }
    }

    private static void consume(KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }
}
''', encoding="utf-8")

# Server-owned UI text cannot know arbitrary client remaps, but it must at least
# match the safe defaults and avoid the retired Z/X wording.
for name in [
    "VillageRoleSkillSystem.java", "VillageSkillTestSystem.java", "VillageUiController.java",
    "VillageFacilityScreen.java", "VillageSkillTestScreen.java", "VillageStarterKit.java",
]:
    path = JAVA / name
    text = path.read_text(encoding="utf-8")
    text = text.replace("Z/X", "Z/V")
    text = text.replace(" | X: ", " | V: ")
    text = text.replace('slot == 0 ? "Z" : "X"', 'slot == 0 ? "Z" : "V"')
    text = text.replace('"X · "', '"V · "')
    text = text.replace("X 슬롯", "V 슬롯")
    text = text.replace("X 기술", "V 기술")
    path.write_text(text, encoding="utf-8")

# Password-gated test arena entry. Management boxes remain usable only after a
# successful entry, so direct network actions cannot bypass the gate.
controller = JAVA / "VillageUiController.java"
replace_once(
    controller,
    """    public static void openSkillTest(ServerPlayer player) {
        boolean alreadyEnabled = VillageSkillTestSystem.isEnabled(player);
        String mode = prepareSkillTest(player);
        if (mode == null) return;
        if (alreadyEnabled) sendSkillTestSkillManager(player, mode);
        else sendSkillTestRoleManager(player, mode);
    }

""",
    """    public static void openSkillTest(ServerPlayer player) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            sendSkillTestSkillManager(player, "외부 시험장 활성화");
            return;
        }
        if (!VillageLocationRules.isNearSkillHall(player)) {
            openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                    "open_role_skill_research");
            return;
        }
        openSkillTestPassword(player, "기술 시험관 접근 코드를 입력하세요.");
    }

    private static void openSkillTestPassword(ServerPlayer player, String message) {
        send(player, "skill_test_password", "기술 시험관 인증", message,
                List.of(), List.of());
    }

""",
    "skill test entry",
)
replace_once(
    controller,
    """    private static String prepareSkillTest(ServerPlayer player) {
        boolean alreadyEnabled = VillageSkillTestSystem.isEnabled(player);
        if (!alreadyEnabled && !VillageLocationRules.isNearSkillHall(player)) {
            openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                    "open_role_skill_research");
            return null;
        }
        String mode = alreadyEnabled
                ? "외부 시험장 활성화"
                : VillageSkillTestSystem.enable(player);
        if (!VillageSkillTestSystem.isEnabled(player)) {
            openResult(player, "기술 시험", mode, "open_role_skill_research");
            return null;
        }
        return mode;
    }

""",
    """    private static String prepareSkillTest(ServerPlayer player) {
        if (!VillageSkillTestSystem.isEnabled(player)) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                        "open_role_skill_research");
            } else {
                openSkillTestPassword(player, "인증 후 시험장 관리 기능을 사용할 수 있습니다.");
            }
            return null;
        }
        return "외부 시험장 활성화";
    }

""",
    "skill test gate",
)
insert_marker = """        if (action.startsWith("test_role:")) {
"""
insert_block = """        if (action.startsWith("skill_test_password:")) {
            String code = action.substring("skill_test_password:".length()).trim();
            if (!"1557".equals(code)) {
                openSkillTestPassword(player, "접근 코드가 올바르지 않습니다. 다시 입력하세요.");
                return true;
            }
            if (!VillageLocationRules.isNearSkillHall(player)) {
                openResult(player, "기술 시험", "기술 연구소 연구대 근처에서만 인증할 수 있습니다.",
                        "open_role_skill_research");
                return true;
            }
            String mode = VillageSkillTestSystem.enable(player);
            if (!VillageSkillTestSystem.isEnabled(player)) {
                openResult(player, "기술 시험", mode, "open_role_skill_research");
            } else {
                sendSkillTestRoleManager(player, mode);
            }
            return true;
        }

""" + insert_marker
replace_once(controller, insert_marker, insert_block, "password action")

# Remove obsolete single-slot test action left over from the old test UI.
old_test_choose = """        if (action.startsWith("test_choose:")) {
            player.sendSystemMessage(Component.literal("§b"
                    + VillageSkillTestSystem.equip(player, action.substring(12), 0)));
            openSkillTest(player);
            return true;
        }
"""
text = controller.read_text(encoding="utf-8")
if old_test_choose not in text:
    raise SystemExit("legacy test_choose action marker missing")
controller.write_text(text.replace(old_test_choose, "", 1), encoding="utf-8")

# Expose next-night intelligence at both sensible military facilities.
replace_once(
    controller,
    """            case BARRACKS -> add(actions, labels,
                    "open_mercenary_command", "용병 고용·성장|병과를 선택해 지속 용병 배치");
""",
    """            case BARRACKS -> add(actions, labels,
                    "open_wave_intel", "다음 밤 적 정찰|웨이브별 병과·수량·특성·보스 편성 확인",
                    "open_mercenary_command", "용병 고용·성장|병과를 선택해 지속 용병 배치");
""",
    "barracks wave intel",
)
text = controller.read_text(encoding="utf-8").replace(
    "case BARRACKS -> \"용병 고용과 모든 경험치 획득량 증가 패시브를 담당합니다. 현재 XP +\"",
    "case BARRACKS -> \"다음 밤 적 정찰과 용병 고용, 모든 경험치 획득량 증가 패시브를 담당합니다. 현재 XP +\"",
)
controller.write_text(text, encoding="utf-8")

wave_intel = JAVA / "VillageWaveIntelSystem.java"
text = wave_intel.read_text(encoding="utf-8").replace(
    "낮 정비 시간에 성벽 정찰 화면에서 웨이브별 편성을 확인하세요.",
    "낮 정비 시간에 성벽 또는 병영 단말기에서 웨이브별 편성을 확인하세요.",
)
wave_intel.write_text(text, encoding="utf-8")

skill_test = JAVA / "VillageSkillTestSystem.java"
text = skill_test.read_text(encoding="utf-8").replace(
    "/** Temporary developer-facing outdoor arena for observing real Z/V skill motion. */",
    "/** Password-gated outdoor arena for observing real role-skill motion. */",
)
skill_test.write_text(text, encoding="utf-8")

# Password keypad screen avoids chat/command leakage and does not depend on a
# text widget. Four digits are submitted only through the dedicated action.
password_screen = JAVA / "VillageSkillTestPasswordScreen.java"
password_screen.write_text(r'''package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class VillageSkillTestPasswordScreen extends Screen {
    private static final int OVERLAY = 0x88000000;
    private static final int PANEL = 0xFFF1E9D7;
    private static final int BORDER = 0xFF6F5B43;
    private static final int TEXT = 0xFF211A14;
    private static final int MUTED = 0xFF62584D;
    private static final int ACCENT = 0xFF267E73;
    private static final int BUTTON = 0xFFE1C98F;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private String input = "";
    private String localMessage = "";

    public VillageSkillTestPasswordScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(260, Math.max(210, width - 30));
        int panelHeight = Math.min(258, Math.max(224, height - 24));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 5, top + panelHeight, ACCENT);
        graphics.centeredText(font, payload.title(), width / 2, top + 13, TEXT);
        String message = localMessage.isBlank() ? payload.body() : localMessage;
        graphics.centeredText(font, fit(message, panelWidth - 28), width / 2, top + 34, MUTED);

        int boxGap = 8;
        int boxWidth = 34;
        int total = boxWidth * 4 + boxGap * 3;
        int boxLeft = width / 2 - total / 2;
        for (int index = 0; index < 4; index++) {
            int x = boxLeft + index * (boxWidth + boxGap);
            graphics.fill(x - 1, top + 54, x + boxWidth + 1, top + 84, BORDER);
            graphics.fill(x, top + 55, x + boxWidth, top + 83, 0xFFF8F2E4);
            String shown = index < input.length() ? "●" : "";
            graphics.centeredText(font, shown, x + boxWidth / 2, top + 65, TEXT);
        }

        for (int index = 0; index < 12; index++) {
            Bounds bounds = bounds(index, top);
            boolean hovered = inside(mouseX, mouseY, bounds.x(), bounds.y(), bounds.w(), bounds.h());
            graphics.fill(bounds.x() - 1, bounds.y() - 1,
                    bounds.x() + bounds.w() + 1, bounds.y() + bounds.h() + 1,
                    hovered ? ACCENT : BORDER);
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.w(), bounds.y() + bounds.h(),
                    hovered ? 0xFFFFE8B5 : BUTTON);
            graphics.centeredText(font, label(index), bounds.x() + bounds.w() / 2,
                    bounds.y() + 6, TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int panelHeight = Math.min(258, Math.max(224, height - 24));
        int top = (height - panelHeight) / 2;
        for (int index = 0; index < 12; index++) {
            Bounds bounds = bounds(index, top);
            if (!inside(click.x(), click.y(), bounds.x(), bounds.y(), bounds.w(), bounds.h())) continue;
            if (index <= 8) append(Integer.toString(index + 1));
            else if (index == 9) backspace();
            else if (index == 10) append("0");
            else submit();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void append(String digit) {
        if (input.length() < 4) input += digit;
        localMessage = "";
    }

    private void backspace() {
        if (!input.isEmpty()) input = input.substring(0, input.length() - 1);
        localMessage = "";
    }

    private void submit() {
        if (input.length() != 4) {
            localMessage = "네 자리 접근 코드를 모두 입력하세요.";
            return;
        }
        ClientPacketDistributor.sendToServer(
                new VillageNetwork.VillageUiActionPayload("skill_test_password:" + input));
        onClose();
    }

    private Bounds bounds(int index, int top) {
        int buttonWidth = 54;
        int buttonHeight = 24;
        int gap = 7;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int left = width / 2 - totalWidth / 2;
        int row = index / 3;
        int column = index % 3;
        return new Bounds(left + column * (buttonWidth + gap), top + 101 + row * 31,
                buttonWidth, buttonHeight);
    }

    private String label(int index) {
        if (index <= 8) return Integer.toString(index + 1);
        if (index == 9) return "지우기";
        if (index == 10) return "0";
        return "입장";
    }

    private String fit(String value, int maxWidth) {
        String normalized = value == null ? "" : value.replace('\n', ' ');
        if (font.width(normalized) <= maxWidth) return normalized;
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + "…") > maxWidth) end--;
        return normalized.substring(0, end) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }

    private record Bounds(int x, int y, int w, int h) {}
}
''', encoding="utf-8")

client_ui = JAVA / "VillageClientUi.java"
replace_once(
    client_ui,
    '                            case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload);\n',
    '                            case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(payload);\n'
    '                            case "skill_test_password" -> new VillageSkillTestPasswordScreen(payload);\n',
    "password screen routing",
)

# Keep every historical contract aligned to the new current version and safe
# second-skill key. This changes assertions only, not gameplay code.
for path in sorted(TOOLS.glob("test_*.py")):
    text = path.read_text(encoding="utf-8")
    text = text.replace("mod_version=0.17.18-alpha.1", "mod_version=0.17.19-alpha.1")
    text = text.replace("GLFW.GLFW_KEY_X", "GLFW.GLFW_KEY_V")
    text = text.replace("GLFW_KEY_X", "GLFW_KEY_V")
    text = text.replace("Z/X", "Z/V")
    text = text.replace("Z and X", "Z and V")
    text = text.replace("Z/X/B/H/J/K", "Z/V/B/H/J/K")
    path.write_text(text, encoding="utf-8")

# Current content audit, generated from the actual current source design.
(ROOT / "CONTENT-AUDIT-v0.17.19.md").write_text("""# Village Guardians v0.17.19 콘텐츠 감사

## 현재 수량

- 직업 5종, 액티브 기술 20종, 직업별 패시브 1종
- 공통 성장 노드 50개, 직업 성장 노드 75개
- 상점 고유 장비 10종: 장비 7종, 방어 계열 3종
- 전리품 장비 기반 아이템군 12종, 등급 5단계, 개별 강화 최대 +5
- 일반 적 병과 10종, 보스 병과 4종, 판매용 전리품 14종
- 웨이브 특성 8종
- 포탑 4종, 포탑별 전문 분기 3개로 총 12분기
- 용병 4병과, 유물 6종

## 캠페인 길이

- 고정 최종 일수는 없으며 무한 진행입니다.
- 1~4일 변경 수비, 5~9일 공성 전쟁, 10~14일 저주 군단,
  15~19일 균열 공세, 20일 이후 끝없는 전쟁 단계로 반복 확장됩니다.
- 하루 웨이브는 3개에서 시작해 11일차부터 최대 8개입니다.
- 3일차부터 마지막 웨이브에 보스가 등장하고, 5일마다 대침공으로 보스 수가 증가합니다.

## 현재 수준 판단

성장·강화·시설·포탑·용병·웨이브 정찰을 포함한 시스템 깊이는 높은 편이지만,
고유 장비 10종과 보스 4종이 순환하는 구조라 장기 콘텐츠 폭은 아직 중간 수준입니다.
약 10~20일 구간까지는 새 병과·특성·장비 해금이 이어지지만, 20일 이후에는 수치와
복수 보스 중심의 무한 확장이므로 전용 보스 외형·장비 세트·지역/목표 변주가 다음 확장 우선순위입니다.
""", encoding="utf-8")

print("Applied Village Guardians v0.17.19 access, collision and content-audit patch")
