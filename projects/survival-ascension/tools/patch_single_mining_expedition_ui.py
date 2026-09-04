from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"
RES = ROOT / "src/main/resources/assets/survivalascension/lang/ko_kr.json"


def replace_once(path: Path, old: str, new: str):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"anchor missing in {path}: {old[:100]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"anchor count != 1 in {path}: {text.count(old)}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

# 1) Explicit single-block mining mode.
mode = JAVA / "mining/MiningMode.java"
replace_once(mode,
'''public enum MiningMode {\n    AUTO("auto", "자동", 0),''',
'''public enum MiningMode {\n    SINGLE("single", "단일", 0),\n    AUTO("auto", "자동", 0),''')

progress = JAVA / "mining/MiningProgression.java"
replace_once(progress,
'''            switch (mode) {\n                case AUTO -> {''',
'''            switch (mode) {\n                case SINGLE -> {\n                    // Explicit precision mode: the vanilla center block is the only block broken.\n                }\n                case AUTO -> {''')
replace_once(progress,
'''    int miningLevel = SkillProgressData.get(player).level(player, SkillType.MINING);\n    int areaSize = AscensionAffixes.adjustShovelArea(tool, SkillTuning.miningAreaSize(miningLevel));''',
'''    int miningLevel = SkillProgressData.get(player).level(player, SkillType.MINING);\n    if (effectiveMode(player, miningLevel) == MiningMode.SINGLE) return;\n    int areaSize = AscensionAffixes.adjustShovelArea(tool, SkillTuning.miningAreaSize(miningLevel));''')

mining_ui = JAVA / "client/MiningRadialMenuScreen.java"
replace_once(mining_ui,
'''    private static final Entry[] ENTRIES = {\n            new Entry("자동", "광석=광맥 / 일반=굴착", new ItemStack(Items.IRON_PICKAXE), MiningMode.AUTO, false),''',
'''    private static final Entry[] ENTRIES = {\n            new Entry("단일", "항상 정확히 1블록만 채굴", new ItemStack(Items.COBBLESTONE), MiningMode.SINGLE, false),\n            new Entry("자동", "광석=광맥 / 일반=굴착", new ItemStack(Items.IRON_PICKAXE), MiningMode.AUTO, false),''')
replace_once(mining_ui,
'''        String caption = "채굴 Lv." + level + " · Shift = 항상 1×1";''',
'''        String caption = "채굴 Lv." + level + " · 단일=상시 1×1 · Shift=임시 정밀";''')

# 2) Expedition snapshot request/response network.
request_payload = JAVA / "network/ExpeditionSnapshotRequestPayload.java"
request_payload.write_text('''package kr.moonseungjun.survivalascension.network;\n\nimport kr.moonseungjun.survivalascension.SurvivalAscension;\nimport net.minecraft.network.RegistryFriendlyByteBuf;\nimport net.minecraft.network.codec.StreamCodec;\nimport net.minecraft.network.protocol.common.custom.CustomPacketPayload;\nimport net.minecraft.resources.Identifier;\n\npublic record ExpeditionSnapshotRequestPayload() implements CustomPacketPayload {\n    public static final Type<ExpeditionSnapshotRequestPayload> TYPE = new Type<>(\n            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_snapshot_request"));\n    public static final StreamCodec<RegistryFriendlyByteBuf, ExpeditionSnapshotRequestPayload> CODEC = StreamCodec.of(\n            (buf, payload) -> {},\n            buf -> new ExpeditionSnapshotRequestPayload());\n\n    @Override\n    public Type<? extends CustomPacketPayload> type() { return TYPE; }\n}\n''', encoding="utf-8")

snapshot_payload = JAVA / "network/ExpeditionSnapshotPayload.java"
snapshot_payload.write_text('''package kr.moonseungjun.survivalascension.network;\n\nimport kr.moonseungjun.survivalascension.SurvivalAscension;\nimport kr.moonseungjun.survivalascension.expedition.ExpeditionData;\nimport kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;\nimport net.minecraft.network.RegistryFriendlyByteBuf;\nimport net.minecraft.network.codec.StreamCodec;\nimport net.minecraft.network.protocol.common.custom.CustomPacketPayload;\nimport net.minecraft.resources.Identifier;\nimport net.minecraft.server.level.ServerPlayer;\n\nimport java.util.HashMap;\nimport java.util.Map;\n\npublic record ExpeditionSnapshotPayload(int discoveredMask, int completedMask, Map<String, String> directives)\n        implements CustomPacketPayload {\n    public ExpeditionSnapshotPayload { directives = Map.copyOf(directives); }\n\n    public static final Type<ExpeditionSnapshotPayload> TYPE = new Type<>(\n            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_snapshot"));\n\n    public static final StreamCodec<RegistryFriendlyByteBuf, ExpeditionSnapshotPayload> CODEC = StreamCodec.of(\n            (buf, payload) -> {\n                buf.writeVarInt(payload.discoveredMask());\n                buf.writeVarInt(payload.completedMask());\n                buf.writeVarInt(payload.directives().size());\n                payload.directives().forEach((region, summary) -> {\n                    buf.writeUtf(region);\n                    buf.writeUtf(summary);\n                });\n            },\n            buf -> {\n                int discovered = buf.readVarInt();\n                int completed = buf.readVarInt();\n                int size = buf.readVarInt();\n                Map<String, String> directives = new HashMap<>(size);\n                for (int i = 0; i < size; i++) directives.put(buf.readUtf(), buf.readUtf());\n                return new ExpeditionSnapshotPayload(discovered, completed, directives);\n            });\n\n    public static ExpeditionSnapshotPayload from(ServerPlayer player) {\n        ExpeditionData data = ExpeditionData.get(player);\n        int discovered = 0;\n        int completed = 0;\n        Map<String, String> directives = new HashMap<>();\n        for (ExpeditionRegion region : ExpeditionRegion.values()) {\n            if (data.isDiscovered(player, region)) {\n                discovered |= region.bit();\n                directives.put(region.name(), data.directiveSummary(player, region));\n            }\n            if (data.isComplete(player, region)) completed |= region.bit();\n        }\n        return new ExpeditionSnapshotPayload(discovered, completed, directives);\n    }\n\n    @Override\n    public Type<? extends CustomPacketPayload> type() { return TYPE; }\n}\n''', encoding="utf-8")

network = JAVA / "network/SkillNetwork.java"
replace_once(network,
'''    private static final String PROTOCOL = "9";\n    private static volatile Consumer<SkillUpdatePayload> updateSink = payload -> {};\n    private static volatile Consumer<SkillSnapshotPayload> snapshotSink = payload -> {};''',
'''    private static final String PROTOCOL = "10";\n    private static volatile Consumer<SkillUpdatePayload> updateSink = payload -> {};\n    private static volatile Consumer<SkillSnapshotPayload> snapshotSink = payload -> {};\n    private static volatile Consumer<ExpeditionSnapshotPayload> expeditionSink = payload -> {};''')
replace_once(network,
'''        registrar.playToClient(SkillUpdatePayload.TYPE, SkillUpdatePayload.CODEC, (payload, context) -> updateSink.accept(payload));\n        registrar.playToClient(SkillSnapshotPayload.TYPE, SkillSnapshotPayload.CODEC, (payload, context) -> snapshotSink.accept(payload));''',
'''        registrar.playToClient(SkillUpdatePayload.TYPE, SkillUpdatePayload.CODEC, (payload, context) -> updateSink.accept(payload));\n        registrar.playToClient(SkillSnapshotPayload.TYPE, SkillSnapshotPayload.CODEC, (payload, context) -> snapshotSink.accept(payload));\n        registrar.playToClient(ExpeditionSnapshotPayload.TYPE, ExpeditionSnapshotPayload.CODEC, (payload, context) -> expeditionSink.accept(payload));\n        registrar.playToServer(ExpeditionSnapshotRequestPayload.TYPE, ExpeditionSnapshotRequestPayload.CODEC, (payload, context) ->\n                context.enqueueWork(() -> {\n                    if (context.player() instanceof ServerPlayer player) {\n                        PacketDistributor.sendToPlayer(player, ExpeditionSnapshotPayload.from(player));\n                    }\n                }));''')
replace_once(network,
'''    public static void installClientReceivers(Consumer<SkillUpdatePayload> updates, Consumer<SkillSnapshotPayload> snapshots) {\n        updateSink = Objects.requireNonNull(updates);\n        snapshotSink = Objects.requireNonNull(snapshots);\n    }''',
'''    public static void installClientReceivers(Consumer<SkillUpdatePayload> updates, Consumer<SkillSnapshotPayload> snapshots) {\n        updateSink = Objects.requireNonNull(updates);\n        snapshotSink = Objects.requireNonNull(snapshots);\n    }\n\n    public static void installExpeditionReceiver(Consumer<ExpeditionSnapshotPayload> snapshots) {\n        expeditionSink = Objects.requireNonNull(snapshots);\n    }''')

# 3) Client state + dedicated J-key expedition UI.
client_state = JAVA / "client/ClientExpeditionState.java"
client_state.write_text('''package kr.moonseungjun.survivalascension.client;\n\nimport kr.moonseungjun.survivalascension.network.ExpeditionSnapshotPayload;\n\nimport java.util.Map;\n\npublic final class ClientExpeditionState {\n    private static int discoveredMask;\n    private static int completedMask;\n    private static Map<String, String> directives = Map.of();\n    private static boolean loaded;\n\n    private ClientExpeditionState() {}\n\n    public static void onSnapshot(ExpeditionSnapshotPayload payload) {\n        discoveredMask = payload.discoveredMask();\n        completedMask = payload.completedMask();\n        directives = Map.copyOf(payload.directives());\n        loaded = true;\n    }\n\n    public static void reset() {\n        discoveredMask = 0;\n        completedMask = 0;\n        directives = Map.of();\n        loaded = false;\n    }\n\n    public static boolean loaded() { return loaded; }\n    public static int discoveredMask() { return discoveredMask; }\n    public static int completedMask() { return completedMask; }\n    public static String directive(String regionId) { return directives.getOrDefault(regionId, ""); }\n}\n''', encoding="utf-8")

screen = JAVA / "client/ExpeditionScreen.java"
screen.write_text('''package kr.moonseungjun.survivalascension.client;\n\nimport kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;\nimport net.minecraft.client.gui.GuiGraphicsExtractor;\nimport net.minecraft.client.gui.components.Button;\nimport net.minecraft.client.gui.screens.Screen;\nimport net.minecraft.network.chat.CommonComponents;\nimport net.minecraft.network.chat.Component;\n\npublic final class ExpeditionScreen extends Screen {\n    private static final int PANEL_MAX_WIDTH = 700;\n    private static final int TOP = 54;\n    private static final int ROW_HEIGHT = 28;\n    private static final int BOTTOM_MARGIN = 42;\n    private static final int SCROLL_STEP = 36;\n\n    private double scrollOffset;\n    private int maxScroll;\n\n    public ExpeditionScreen() {\n        super(Component.literal("원정 기록"));\n    }\n\n    @Override\n    public boolean isPauseScreen() { return false; }\n\n    @Override\n    protected void init() {\n        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())\n                .bounds(this.width / 2 - 60, this.height - 28, 120, 20).build());\n    }\n\n    @Override\n    public void onClose() {\n        this.minecraft.gui.setScreen(null);\n    }\n\n    @Override\n    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {\n        if (scrollY == 0.0D || maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);\n        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset - scrollY * SCROLL_STEP));\n        return true;\n    }\n\n    @Override\n    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {\n        super.extractRenderState(graphics, mouseX, mouseY, partialTick);\n        graphics.fill(0, 0, this.width, this.height, 0x88000000);\n\n        int panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(240, this.width - 28));\n        int left = (this.width - panelWidth) / 2;\n        int right = left + panelWidth;\n        int bottom = this.height - BOTTOM_MARGIN;\n\n        graphics.fill(left, 10, right, Math.max(44, bottom), 0xCC101820);\n        graphics.fill(left, 10, right, 12, 0xFF3B82A0);\n        graphics.text(this.font, "원정 기록", left + 12, 18, 0xFFFFFFFF, true);\n\n        if (!ClientExpeditionState.loaded()) {\n            String waiting = "서버 원정 기록 동기화 중...";\n            graphics.text(this.font, waiting, this.width / 2 - this.font.width(waiting) / 2, TOP + 24, 0xFFD0D0D0, false);\n            return;\n        }\n\n        int completed = Integer.bitCount(ClientExpeditionState.completedMask());\n        int discovered = Integer.bitCount(ClientExpeditionState.discoveredMask());\n        String header = "완료 " + completed + "/" + ExpeditionRegion.values().length + " · 발견 " + discovered + "/" + ExpeditionRegion.values().length + " · J = 닫기";\n        graphics.text(this.font, header, right - 12 - this.font.width(header), 19, 0xFFB8D9E8, false);\n\n        int viewportHeight = Math.max(1, bottom - TOP);\n        int contentHeight = ExpeditionRegion.values().length * ROW_HEIGHT;\n        maxScroll = Math.max(0, contentHeight - viewportHeight);\n        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset));\n\n        graphics.enableScissor(left + 6, TOP, right - 6, bottom);\n        int y = TOP - (int)Math.round(scrollOffset);\n        for (ExpeditionRegion region : ExpeditionRegion.values()) {\n            boolean complete = (ClientExpeditionState.completedMask() & region.bit()) != 0;\n            boolean discoveredRegion = (ClientExpeditionState.discoveredMask() & region.bit()) != 0;\n            int rowTop = y + 1;\n            int rowBottom = y + ROW_HEIGHT - 2;\n            int bg = complete ? 0x88305A42 : discoveredRegion ? 0x88634E26 : 0x88404850;\n            graphics.fill(left + 10, rowTop, right - 10, rowBottom, bg);\n\n            String status = complete ? "✓ 완료" : discoveredRegion ? "◐ 진행" : "· 미발견";\n            int statusColor = complete ? 0xFF79E39C : discoveredRegion ? 0xFFFFD166 : 0xFF8A9299;\n            graphics.text(this.font, status, left + 18, y + 6, statusColor, true);\n            graphics.text(this.font, region.koreanName(), left + 82, y + 6, 0xFFFFFFFF, true);\n\n            String stage = switch (region.requiredWorldStage()) {\n                case 0 -> "각성";\n                case 1 -> "전설";\n                default -> "종말";\n            };\n            graphics.text(this.font, stage, left + 82, y + 16, 0xFF8FB8C8, false);\n\n            String detail = discoveredRegion ? ClientExpeditionState.directive(region.name()) : "해당 지역을 탐사하면 지령이 공개됩니다.";\n            int detailX = left + 132;\n            int maxDetailWidth = Math.max(60, right - 18 - detailX);\n            if (this.font.width(detail) > maxDetailWidth) {\n                String ellipsis = "…";\n                while (!detail.isEmpty() && this.font.width(detail + ellipsis) > maxDetailWidth) detail = detail.substring(0, detail.length() - 1);\n                detail += ellipsis;\n            }\n            graphics.text(this.font, detail, detailX, y + 11, complete ? 0xFFC9F2D4 : 0xFFD5D5D5, false);\n            y += ROW_HEIGHT;\n        }\n        graphics.disableScissor();\n\n        if (maxScroll > 0) {\n            int trackX = right - 7;\n            int barHeight = Math.max(18, viewportHeight * viewportHeight / contentHeight);\n            int travel = Math.max(1, viewportHeight - barHeight);\n            int barTop = TOP + (int)Math.round(scrollOffset * travel / maxScroll);\n            graphics.fill(trackX, TOP, trackX + 2, bottom, 0x55404040);\n            graphics.fill(trackX, barTop, trackX + 2, Math.min(bottom, barTop + barHeight), 0xFFC0DCE8);\n        }\n    }\n}\n''', encoding="utf-8")

client = JAVA / "client/SurvivalAscensionClient.java"
replace_once(client,
'''import kr.moonseungjun.survivalascension.network.MobilityActionPayload;\nimport kr.moonseungjun.survivalascension.network.SkillNetwork;''',
'''import kr.moonseungjun.survivalascension.network.ExpeditionSnapshotRequestPayload;\nimport kr.moonseungjun.survivalascension.network.MobilityActionPayload;\nimport kr.moonseungjun.survivalascension.network.SkillNetwork;''')
replace_once(client,
'''    private static final KeyMapping MOBILITY_ACTION = new KeyMapping(\n            "key.survivalascension.mobility_action", InputConstants.KEY_V, KeyMapping.Category.MISC);''',
'''    private static final KeyMapping OPEN_EXPEDITION = new KeyMapping(\n            "key.survivalascension.expedition", InputConstants.KEY_J, KeyMapping.Category.MISC);\n    private static final KeyMapping MOBILITY_ACTION = new KeyMapping(\n            "key.survivalascension.mobility_action", InputConstants.KEY_V, KeyMapping.Category.MISC);''')
replace_once(client,
'''        SkillNetwork.installClientReceivers(ClientSkillState::onUpdate, ClientSkillState::onSnapshot);''',
'''        SkillNetwork.installClientReceivers(ClientSkillState::onUpdate, ClientSkillState::onSnapshot);\n        SkillNetwork.installExpeditionReceiver(ClientExpeditionState::onSnapshot);''')
replace_once(client,
'''        event.register(OPEN_MENU);\n        event.register(MOBILITY_ACTION);''',
'''        event.register(OPEN_MENU);\n        event.register(OPEN_EXPEDITION);\n        event.register(MOBILITY_ACTION);''')
replace_once(client,
'''        while (MOBILITY_ACTION.consumeClick()) {''',
'''        while (OPEN_EXPEDITION.consumeClick()) {\n            if (minecraft.player == null || minecraft.level == null) continue;\n            Screen current = minecraft.gui.screen();\n            if (current instanceof ExpeditionScreen) {\n                minecraft.gui.setScreen(null);\n            } else if (current == null) {\n                ClientExpeditionState.reset();\n                ClientPacketDistributor.sendToServer(new ExpeditionSnapshotRequestPayload());\n                minecraft.gui.setScreen(new ExpeditionScreen());\n            }\n        }\n        while (MOBILITY_ACTION.consumeClick()) {''')

# Korean key label.
replace_once(RES,
'''  "key.survivalascension.menu": "통합 라디얼 메뉴 열기",\n  "key.survivalascension.mobility_action": "기동 액션 / 돌진"''',
'''  "key.survivalascension.menu": "통합 라디얼 메뉴 열기",\n  "key.survivalascension.expedition": "원정 기록 열기",\n  "key.survivalascension.mobility_action": "기동 액션 / 돌진"''')

# Targeted source assertions.
checks = {
    mode: ['SINGLE("single", "단일", 0)'],
    progress: ['case SINGLE ->', 'effectiveMode(player, miningLevel) == MiningMode.SINGLE'],
    mining_ui: ['new Entry("단일"', '단일=상시 1×1'],
    network: ['PROTOCOL = "10"', 'ExpeditionSnapshotRequestPayload.TYPE', 'installExpeditionReceiver'],
    client: ['InputConstants.KEY_J', 'new ExpeditionScreen()', 'new ExpeditionSnapshotRequestPayload()'],
    RES: ['"key.survivalascension.expedition": "원정 기록 열기"'],
}
for path, needles in checks.items():
    text = path.read_text(encoding="utf-8")
    for needle in needles:
        if needle not in text:
            raise SystemExit(f"verification missing {needle!r} in {path}")

print("single mining + expedition UI patch applied")
