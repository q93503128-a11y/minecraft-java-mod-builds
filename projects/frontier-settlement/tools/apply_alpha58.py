#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def repl(p,old,new):
    s=read(p); n=s.count(old)
    if n!=1: raise SystemExit(f'{p}: expected one anchor, got {n}: {old[:120]!r}')
    write(p,s.replace(old,new,1))

service=JAVA/'settlement/SettlementService.java'
repl(service,
'''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) refreshResources(server, data);
        sync(player, data);
    }
''',
'''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) {
            // A joining player may make town storage loaded again. Refresh once, then publish the
            // same authoritative snapshot to every connected player so existing HUDs cannot stay stale.
            refreshResources(server, data);
            broadcast(server, data);
        } else {
            sync(player, data);
        }
    }
''')

network=JAVA/'network/SettlementNetwork.java'
repl(network,
'import net.neoforged.neoforge.network.registration.PayloadRegistrar;\n',
'import net.neoforged.neoforge.network.registration.HandlerThread;\nimport net.neoforged.neoforge.network.registration.PayloadRegistrar;\n')
repl(network,
'        PayloadRegistrar registrar=event.registrar(PROTOCOL);',
'        PayloadRegistrar registrar=event.registrar(PROTOCOL).executesOn(HandlerThread.MAIN);')

state=JAVA/'client/ClientSettlementState.java'
repl(state,
'''public final class ClientSettlementState {
    private static volatile SettlementSnapshotPayload snapshot =
            new SettlementSnapshotPayload(false, 0L, 0L, 0L, 0L, 0, "개척 캠프", 0, "", SettlementContextPayload.EMPTY);
''',
'''public final class ClientSettlementState {
    private static final SettlementSnapshotPayload EMPTY_SNAPSHOT =
            new SettlementSnapshotPayload(false, 0L, 0L, 0L, 0L, 0, "개척 캠프", 0, "", SettlementContextPayload.EMPTY);
    private static volatile SettlementSnapshotPayload snapshot = EMPTY_SNAPSHOT;
''')
repl(state,
'''    public static SettlementSnapshotPayload snapshot() { return snapshot; }
    public static SettlementContextPayload context() { return context; }
''',
'''    public static synchronized void reset() {
        snapshot = EMPTY_SNAPSHOT;
        context = SettlementContextPayload.EMPTY;
        snapshotInitialized = false;
        contextInitialized = false;
    }

    public static SettlementSnapshotPayload snapshot() { return snapshot; }
    public static SettlementContextPayload context() { return context; }
''')

notices=JAVA/'client/SettlementNoticeQueue.java'
repl(notices,
'''    public static synchronized void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
''',
'''    public static synchronized void clear() {
        NOTICES.clear();
    }

    public static synchronized void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
''')

client=JAVA/'client/FrontierSettlementClient.java'
repl(client,
'import net.neoforged.neoforge.client.event.EntityRenderersEvent;\n',
'import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;\nimport net.neoforged.neoforge.client.event.EntityRenderersEvent;\n')
repl(client,
'''        NeoForge.EVENT_BUS.addListener(CivilWorkGhostRenderer::submit);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
''',
'''        NeoForge.EVENT_BUS.addListener(CivilWorkGhostRenderer::submit);
        NeoForge.EVENT_BUS.addListener(FrontierSettlementClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSettlementState.reset();
        BuildingPlacementClient.cancelAllModes();
        SettlementNoticeQueue.clear();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
''')

props=ROOT/'gradle.properties'
repl(props,'mod_version=0.1.0-alpha.57','mod_version=0.1.0-alpha.58')
repl(props,'and automated physical barracks armament from real external-weapon ItemStacks without per-soldier micromanagement.','and automated physical barracks armament from real external-weapon ItemStacks without per-soldier micromanagement, plus multiplayer snapshot/session pre-acceptance hardening.')

lock=ROOT/'COMPANION_LOCK.json'
repl(lock,'"frontier_settlement": "0.1.0-alpha.57"','"frontier_settlement": "0.1.0-alpha.58"')
repl(lock,
'    "Alpha.57 upgrades only loaded town barracks soldiers: an idle soldier with an empty MAINHAND walks to the nearest loaded shared-storage container, extracts exactly one real Frontier-recognized external weapon ItemStack, equips it through vanilla Mob equipment persistence/sync, and returns that exact weapon as the sole recoverable military drop on death; remote military-outpost weapon supply remains deferred to the existing road transporter authority.",\n',
'    "Alpha.57 upgrades only loaded town barracks soldiers: an idle soldier with an empty MAINHAND walks to the nearest loaded shared-storage container, extracts exactly one real Frontier-recognized external weapon ItemStack, equips it through vanilla Mob equipment persistence/sync, and returns that exact weapon as the sole recoverable military drop on death; remote military-outpost weapon supply remains deferred to the existing road transporter authority.",\n    "Alpha.58 is multiplayer pre-acceptance hardening rather than new gameplay: serverbound placement handlers explicitly stay on NeoForge MAIN handling, a founded-world login republishes one refreshed authoritative settlement snapshot to all connected players, and client logout clears cached settlement/context/placement/notice state before another world/server can reuse it. Long two-player runtime acceptance is still not claimed.",\n')
repl(lock,'so Alpha.57 keeps only HUD collision avoidance','so Alpha.58 keeps only HUD collision avoidance')

readme=ROOT/'README.md'
repl(readme,'## Current version: 0.1.0-alpha.57','## Current version: 0.1.0-alpha.58')
repl(readme,'No new Alpha.57 key was added.','No new Alpha.58 key was added.')
repl(readme,'Alpha.40–57 deepen existing systems','Alpha.40–58 deepen existing systems')
section='''## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening

Alpha.58 does **not** claim that the required long two-player survival acceptance has been completed. It closes deterministic multiplayer-state holes found before that real-play pass.

- the settlement is still one server-owned `SettlementData`; no per-player settlement copy/save authority is introduced;
- NeoForge 26.2 payload registration is now explicitly `.executesOn(HandlerThread.MAIN)`, making building/road/outpost/civil requests visibly serialized on the server main thread before each service revalidates current shared state;
- if a player logs into an already-founded world, their presence may make common storage loaded again. The server refreshes the physical storage ledger once and **broadcasts the same authoritative snapshot to every connected player**, not only the joiner;
- this closes a stale-HUD edge where an existing player could otherwise keep the pre-login resource snapshot after the joiner caused a ledger refresh;
- `ClientPlayerNetworkEvent.LoggingOut` now clears the client settlement snapshot/context initialization flags, all placement modes/previews, and queued settlement notices;
- moving from one server/world to another therefore cannot compare the previous world's tier/context against the new world and emit fake growth/completion notices;
- no payload schema change, new key, building, currency, player-specific progression or async mutation authority is added;
- existing server confirmation still wins if two players preview the same opportunity: the later MAIN-thread request rechecks the now-current shared project state instead of trusting its old client preview.

This is **pre-acceptance hardening** only. Long survival + two-player gameplay, reconnect/save-reload, simultaneous placement attempts and full companion-stack runtime remain explicit real-play acceptance work.

'''
repl(readme,'## Alpha.57 — automated physical barracks armament\n',section+'## Alpha.57 — automated physical barracks armament\n')

can=ROOT/'CANONICAL_PLAN.md'
repl(can,'Current canonical implementation: **0.1.0-alpha.57**.','Current canonical implementation: **0.1.0-alpha.58**.')
repl(can,'Alpha.40–57 deepen systems','Alpha.40–58 deepen systems')
repl(can,
'''- clients submit bounded requests and render synchronized state;
- the server revalidates building/road/outpost/civil-work requests before mutation;
''',
'''- clients submit bounded requests and render synchronized state;
- Alpha.58 explicitly keeps all play payload handlers on NeoForge `HandlerThread.MAIN`, so simultaneous player requests are serialized before mutation;
- the server revalidates building/road/outpost/civil-work requests before mutation;
- founded-world login refreshes common physical storage once and republishes the same authoritative snapshot to all connected players;
- client logout clears cached settlement/context/placement/notice state so another world/server cannot inherit UI state;
''')
section2='''### Alpha.58 multiplayer pre-acceptance hardening

Alpha.58 is a bounded correctness pass before real two-player acceptance, not a claim that multiplayer acceptance is finished.

- `SettlementData` and civil SavedData remain world/server shared, never keyed per player;
- `PayloadRegistrar` explicitly executes play handlers on `HandlerThread.MAIN`; building/road/outpost/civil confirms therefore serialize through one game-thread authority and every start path still revalidates current state;
- founded-world login runs one physical storage refresh then broadcasts one current snapshot to **all** connected players, avoiding a join-triggered ledger update that only the joiner sees;
- client `LoggingOut` resets settlement snapshot/context initialization, cancels all construction placement modes and clears transient settlement notices;
- reconnect/server-switch cannot generate notices by comparing unrelated old/new settlement contexts;
- no new protocol payload, per-player resource cache authority, settlement duplication or async world mutation is introduced.

Remaining acceptance is intentionally real-play: two clients, one shared settlement, simultaneous requests, long hauling/construction, disconnect/reconnect and save/reload under the full candidate companion stack.

'''
repl(can,'### Alpha.57 automated physical barracks armory\n',section2+'### Alpha.57 automated physical barracks armory\n')
repl(can,'## 14. Current playable slice after Alpha.57','## 14. Current playable slice after Alpha.58')
repl(can,'- Alpha.57 loaded town-garrison physical external-weapon armament from shared storage, with exact weapon recovery;','- Alpha.57 loaded town-garrison physical external-weapon armament from shared storage, with exact weapon recovery;\n- Alpha.58 shared-login snapshot rebroadcast + explicit MAIN-thread request serialization + client session reset pre-hardening;')
repl(can,'## 15. Unfinished original-scope priorities after Alpha.57','## 15. Unfinished original-scope priorities after Alpha.58')
repl(can,'1. long survival + two-player multiplayer acceptance;','1. long survival + two-player multiplayer acceptance; Alpha.58 only closes pre-acceptance deterministic state holes and does not satisfy this runtime item;')
repl(can,'15. Alpha.57 weapon storage→soldier walk/extract/save-reload/render/death-recovery/no-dup acceptance;','15. Alpha.57 weapon storage→soldier walk/extract/save-reload/render/death-recovery/no-dup acceptance;\n16. Alpha.58 two-client shared-login refresh, simultaneous confirmation, logout/server-switch reset and reconnect acceptance;')
repl(can,'16. full companion lock fresh-world client/server runtime;\n17. true Xaero markers only if a stable supported API appears;\n18. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.','17. full companion lock fresh-world client/server runtime;\n18. true Xaero markers only if a stable supported API appears;\n19. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.')

gap=ROOT/'COMPLETION_GAP_AUDIT.md'
repl(gap,'현재 구현 기준: `0.1.0-alpha.57`','현재 구현 기준: `0.1.0-alpha.58`')
repl(gap,
'| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |',
'| 한 월드 하나의 공동 마을 | 완료 | SavedData 기반 공유 정착지 |\n| 2인 snapshot/session 정합 pre-hardening | **완료/부분** | Alpha.58 MAIN-thread request + login rebroadcast + client logout reset; 장시간 실제 2인 acceptance는 남음 |')
section3='''### Alpha.58 멀티 snapshot/session pre-acceptance 감사

- world/server shared `SettlementData` 유지, per-player settlement/save 없음;
- serverbound play payload registration을 NeoForge `HandlerThread.MAIN`으로 명시;
- building/road/outpost/civil confirm은 MAIN thread에서 직렬화된 뒤 각 service가 current shared state를 다시 검사;
- founded-world player login은 common physical storage refresh 후 모든 connected player에게 같은 authoritative snapshot broadcast;
- joiner 때문에 storage ledger가 갱신돼도 기존 접속자 HUD만 stale로 남는 경로 제거;
- client `ClientPlayerNetworkEvent.LoggingOut`에서 snapshot/context initialized flags + placement modes/previews + notices reset;
- 다른 server/world 진입 때 이전 tier/context와 비교한 가짜 성장/완공 알림 방지;
- 새 payload schema/key/building/currency/per-player authority/async world mutation 없음;
- **실제 장시간 2인 acceptance는 아직 미완료**이며 Alpha.58을 그 완료로 기록하지 않음.

'''
repl(gap,'## 2. 자원 / 물류 / 경제\n',section3+'## 2. 자원 / 물류 / 경제\n')
repl(gap,'1. long survival + two-player multiplayer acceptance;','1. long survival + two-player multiplayer acceptance; Alpha.58은 pre-hardening만 완료했고 실제 runtime acceptance는 남음;')
repl(gap,'14. Alpha.57 shared-storage weapon walk/extract/persistence/render/death-recovery/no-dup acceptance;','14. Alpha.57 shared-storage weapon walk/extract/persistence/render/death-recovery/no-dup acceptance;\n15. Alpha.58 simultaneous player confirm/login-refresh/logout-reset/reconnect acceptance;')
repl(gap,'15. full companion lock fresh-world client/server runtime;\n16. true Xaero marker는 stable supported API가 생길 때만;\n17. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.','16. full companion lock fresh-world client/server runtime;\n17. true Xaero marker는 stable supported API가 생길 때만;\n18. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.')

write(ROOT/'tools/test_alpha58_source.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A57=ROOT/'tools/test_alpha57_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A57).replace("print('Frontier Settlement alpha.23-57 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.57','0.1.0-alpha.58'); ns={'__file__':str(A57),'__name__':'__main__'}; exec(compile(a,str(A57),'exec'),ns,ns)
service=text(JAVA/'settlement/SettlementService.java'); network=text(JAVA/'network/SettlementNetwork.java'); state=text(JAVA/'client/ClientSettlementState.java'); notices=text(JAVA/'client/SettlementNoticeQueue.java'); client=text(JAVA/'client/FrontierSettlementClient.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(service,('public static void onPlayerLoggedIn','if (data.founded()) {','refreshResources(server, data);','broadcast(server, data);','} else {','sync(player, data);','same authoritative snapshot to every connected player'),'alpha.58 founded-login shared snapshot')
must(network,('import net.neoforged.neoforge.network.registration.HandlerThread;','event.registrar(PROTOCOL).executesOn(HandlerThread.MAIN)','playToServer(PlacementRequestPayload.TYPE','playToServer(RoadPlacementRequestPayload.TYPE','playToServer(OutpostPlacementRequestPayload.TYPE','playToServer(CivilWorkRequestPayload.TYPE'),'alpha.58 explicit main-thread server requests')
must(state,('EMPTY_SNAPSHOT','public static synchronized void reset()','snapshot = EMPTY_SNAPSHOT','context = SettlementContextPayload.EMPTY','snapshotInitialized = false','contextInitialized = false'),'alpha.58 client settlement reset')
must(notices,('public static synchronized void clear()','NOTICES.clear()'),'alpha.58 notice reset')
must(client,('ClientPlayerNetworkEvent','NeoForge.EVENT_BUS.addListener(FrontierSettlementClient::onLoggingOut)','private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)','ClientSettlementState.reset()','BuildingPlacementClient.cancelAllModes()','SettlementNoticeQueue.clear()'),'alpha.58 session reset hook')
forbid(service+network,('CompletableFuture.runAsync','new Thread(','parallelStream('),'alpha.58 no async settlement mutation path')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.58 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.58','multiplayer snapshot/session pre-acceptance hardening'),'alpha.58 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.58"','Alpha.58 is multiplayer pre-acceptance hardening','Long two-player runtime acceptance is still not claimed','"status": "candidate_runtime_lock"'),'alpha.58 lock')
print('Frontier Settlement alpha.23-58 cumulative source audit: PASS')
''')

write(ROOT/'tools/test_alpha58_docs.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.58','## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening','executesOn(HandlerThread.MAIN)','broadcasts the same authoritative snapshot to every connected player','ClientPlayerNetworkEvent.LoggingOut','pre-acceptance hardening','Long survival + two-player gameplay'),'alpha.58 README')
must(can,('Current canonical implementation: **0.1.0-alpha.58**','### Alpha.58 multiplayer pre-acceptance hardening','HandlerThread.MAIN','republishes the same authoritative snapshot to all connected players','client `LoggingOut` resets','Remaining acceptance is intentionally real-play','after Alpha.58'),'alpha.58 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.58`','2인 snapshot/session 정합 pre-hardening | **완료/부분**','### Alpha.58 멀티 snapshot/session pre-acceptance 감사','실제 장시간 2인 acceptance는 아직 미완료','Alpha.58 simultaneous player confirm/login-refresh/logout-reset/reconnect acceptance'),'alpha.58 gap')
print('Frontier Settlement alpha.58 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.58 multiplayer snapshot/session pre-hardening.')
