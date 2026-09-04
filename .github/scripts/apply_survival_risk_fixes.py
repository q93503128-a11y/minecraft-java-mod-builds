from pathlib import Path

root = Path('projects/survival-ascension')
java = root / 'src/main/java/kr/moonseungjun/survivalascension'

# Canonical Survival bindings in the combined pack/source are K menu, J expedition, V mobility.
for p in java.rglob('*.java'):
    s = p.read_text(encoding='utf-8')
    s2 = s.replace('M →', 'K →').replace('M→', 'K→').replace('M 메뉴', 'K 메뉴').replace('M · 통합 메뉴', 'K · 통합 메뉴')
    if s2 != s:
        p.write_text(s2, encoding='utf-8')

replacements = {
    java / 'client/GuideScreen.java': [('/ R %s', '/ V %s')],
    java / 'client/SkillsScreen.java': [('R 기동 액션', 'V 기동 액션'), (' · R ', ' · V ')],
    java / 'command/AscensionCommands.java': [(' | R ', ' | V ')],
    java / 'mobility/MobilityProgression.java': [('R · 지상 돌진', 'V · 지상 돌진'), ('공중에서 R을', '공중에서 V를')],
}
for p, pairs in replacements.items():
    s = p.read_text(encoding='utf-8')
    for old, new in pairs:
        s = s.replace(old, new)
    p.write_text(s, encoding='utf-8')

# Area harvesting should pay the same reduced automatic wear as mining/bore/woodcutting.
harvest = java / 'harvesting/HarvestingProgression.java'
s = harvest.read_text(encoding='utf-8')
anchor = 'import kr.moonseungjun.survivalascension.progress.SkillClientBridge;'
if 'import kr.moonseungjun.survivalascension.progress.AutomatedToolBreak;' not in s:
    if s.count(anchor) != 1:
        raise SystemExit('HarvestingProgression import anchor drift')
    s = s.replace(anchor, 'import kr.moonseungjun.survivalascension.progress.AutomatedToolBreak;\n' + anchor, 1)
old = '                    player.gameMode.destroyBlock(target);'
if s.count(old) != 1:
    raise SystemExit(f'HarvestingProgression direct auto-break count drift: {s.count(old)}')
s = s.replace(old, '                    AutomatedToolBreak.destroyWithReducedWear(player, target);', 1)
harvest.write_text(s, encoding='utf-8')

# Fractional durability debt belongs to the current tool type, not globally to the player.
helper = java / 'progress/AutomatedToolBreak.java'
s = helper.read_text(encoding='utf-8')
if 'import net.minecraft.core.registries.BuiltInRegistries;' not in s:
    s = s.replace('import net.minecraft.core.BlockPos;\n', 'import net.minecraft.core.BlockPos;\nimport net.minecraft.core.registries.BuiltInRegistries;\n', 1)
if 'WEAR_TOOL_KEY' not in s:
    s = s.replace(
        '    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";\n',
        '    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";\n'
        '    private static final String WEAR_TOOL_KEY = "survivalascension_bulk_tool_wear_tool";\n',
        1,
    )
old = '        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));\n'
new = (
    '        String toolId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();\n'
    '        String bankToolId = player.getPersistentData().getStringOr(WEAR_TOOL_KEY, "");\n'
    '        if (!toolId.equals(bankToolId)) {\n'
    '            player.getPersistentData().putString(WEAR_TOOL_KEY, toolId);\n'
    '            player.getPersistentData().putInt(WEAR_BANK_KEY, 0);\n'
    '        }\n'
    '        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));\n'
)
if old not in s:
    raise SystemExit('AutomatedToolBreak bank anchor drift')
s = s.replace(old, new, 1)
helper.write_text(s, encoding='utf-8')

# Refresh server-backed expedition progress once per second while J screen stays open.
screen = java / 'client/ExpeditionScreen.java'
s = screen.read_text(encoding='utf-8')
if 'import kr.moonseungjun.survivalascension.network.ExpeditionSnapshotRequestPayload;' not in s:
    s = s.replace(
        'import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;\n',
        'import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;\n'
        'import kr.moonseungjun.survivalascension.network.ExpeditionSnapshotRequestPayload;\n',
        1,
    )
if 'import net.neoforged.neoforge.client.network.ClientPacketDistributor;' not in s:
    s = s.replace(
        'import net.minecraft.network.chat.Component;\n',
        'import net.minecraft.network.chat.Component;\n'
        'import net.neoforged.neoforge.client.network.ClientPacketDistributor;\n',
        1,
    )
if 'private int refreshTicker;' not in s:
    s = s.replace(
        '    private double scrollOffset;\n    private int maxScroll;\n',
        '    private double scrollOffset;\n    private int maxScroll;\n    private int refreshTicker;\n',
        1,
    )
tick_anchor = '    @Override\n    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {\n'
tick_method = (
    '    @Override\n'
    '    public void tick() {\n'
    '        super.tick();\n'
    '        if (++refreshTicker < 20) return;\n'
    '        refreshTicker = 0;\n'
    '        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null) {\n'
    '            ClientPacketDistributor.sendToServer(new ExpeditionSnapshotRequestPayload());\n'
    '        }\n'
    '    }\n\n'
)
if 'public void tick()' not in s:
    if s.count(tick_anchor) != 1:
        raise SystemExit('ExpeditionScreen tick anchor drift')
    s = s.replace(tick_anchor, tick_method + tick_anchor, 1)
screen.write_text(s, encoding='utf-8')

# Remove Mythic runtime state tied to an old server and close bars on shutdown.
elite = java / 'elite/EliteMobSystem.java'
s = elite.read_text(encoding='utf-8')
if 'import net.neoforged.neoforge.event.server.ServerStoppingEvent;' not in s:
    s = s.replace(
        'import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n',
        'import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n'
        'import net.neoforged.neoforge.event.server.ServerStoppingEvent;\n',
        1,
    )
old = '            if (runtime.level.getServer() != event.getServer()) continue;'
new = (
    '            if (runtime.level.getServer() != event.getServer()) {\n'
    '                closeMythicBar(runtime);\n'
    '                remove.add(entry.getKey());\n'
    '                continue;\n'
    '            }'
)
if s.count(old) != 1:
    raise SystemExit('EliteMobSystem stale-server anchor drift')
s = s.replace(old, new, 1)
stop_anchor = '    public static void onDamagePre(LivingDamageEvent.Pre event) {\n'
stop_method = (
    '    public static void onServerStopping(ServerStoppingEvent event) {\n'
    '        for (MythicRuntime runtime : new ArrayList<>(MYTHICS.values())) {\n'
    '            if (runtime.level.getServer() == event.getServer()) closeMythicBar(runtime);\n'
    '        }\n'
    '        MYTHICS.entrySet().removeIf(entry -> entry.getValue().level.getServer() == event.getServer());\n'
    '        mythicTicker = 0;\n'
    '    }\n\n'
)
if 'onServerStopping(ServerStoppingEvent event)' not in s:
    if s.count(stop_anchor) != 1:
        raise SystemExit('EliteMobSystem stop anchor drift')
    s = s.replace(stop_anchor, stop_method + stop_anchor, 1)
elite.write_text(s, encoding='utf-8')

main = java / 'SurvivalAscension.java'
s = main.read_text(encoding='utf-8')
anchor = '        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerTick);\n'
if 'EliteMobSystem::onServerStopping' not in s:
    if s.count(anchor) != 1:
        raise SystemExit('SurvivalAscension elite stop registration anchor drift')
    s = s.replace(anchor, anchor + '        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerStopping);\n', 1)
main.write_text(s, encoding='utf-8')

# Extend focused persistent source audit.
audit = root / 'tools/test_gameplay_qol_061.py'
s = audit.read_text(encoding='utf-8')
read_anchor = 'commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")\n'
extra_reads = (
    read_anchor
    + 'harvesting = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")\n'
    + 'expedition_screen = read("src/main/java/kr/moonseungjun/survivalascension/client/ExpeditionScreen.java")\n'
    + 'elite = read("src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java")\n'
    + 'client = read("src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java")\n'
)
if 'expedition_screen = read(' not in s:
    if s.count(read_anchor) != 1:
        raise SystemExit('QoL audit read anchor drift')
    s = s.replace(read_anchor, extra_reads, 1)
check_anchor = 'need(commands, ["case FISHING", "낚싯대 마모 방지"], "fishing command status")\n'
extra_checks = (
    check_anchor
    + 'need(harvesting, ["AutomatedToolBreak.destroyWithReducedWear(player, target)"], "reduced-wear area harvesting")\n'
    + 'need(helper, ["WEAR_TOOL_KEY", "BuiltInRegistries.ITEM.getKey(tool.getItem())", "if (!toolId.equals(bankToolId))"], "per-tool-type bulk wear bank")\n'
    + 'need(expedition_screen, ["public void tick()", "refreshTicker < 20", "new ExpeditionSnapshotRequestPayload()"], "live expedition screen refresh")\n'
    + 'need(elite, ["onServerStopping(ServerStoppingEvent event)", "remove.add(entry.getKey())"], "mythic runtime server lifecycle")\n'
    + 'need(main, ["EliteMobSystem::onServerStopping"], "mythic stop listener wiring")\n'
    + 'need(client, ["InputConstants.KEY_K", "InputConstants.KEY_J", "InputConstants.KEY_V"], "canonical client keys")\n'
)
if 'reduced-wear area harvesting' not in s:
    if s.count(check_anchor) != 1:
        raise SystemExit('QoL audit check anchor drift')
    s = s.replace(check_anchor, extra_checks, 1)
audit.write_text(s, encoding='utf-8')

all_java = '\n'.join(p.read_text(encoding='utf-8') for p in java.rglob('*.java'))
for token in ('M →', 'M→', 'M 메뉴', 'M · 통합 메뉴', 'R · 지상 돌진', '공중에서 R을'):
    if token in all_java:
        raise SystemExit(f'stale key guidance remains: {token}')

print('SURVIVAL AUDITED RISK FIXES APPLIED')
