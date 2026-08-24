#!/usr/bin/env python3
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
