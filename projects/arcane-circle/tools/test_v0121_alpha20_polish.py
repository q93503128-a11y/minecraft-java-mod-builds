#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def t(rel): return (ROOT/rel).read_text(encoding='utf-8')
def need(src,token,label):
    if token not in src: raise SystemExit(f'{label}: missing {token!r}')
def forbid(src,token,label):
    if token in src: raise SystemExit(f'{label}: forbidden {token!r}')
def section(src,start,end):
    a=src.find(start); b=src.find(end,a+len(start))
    if a<0 or b<0: raise SystemExit(f'section missing: {start!r}')
    return src[a:b]

props=t('gradle.properties')
main=t('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
casting=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java')
gear=t('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java')
tooltip=t('src/main/java/kr/moonseungjun/arcanecircle/client/MageGearTooltip.java')
screen=t('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')
tracker=t('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java')
signature=t('src/main/java/kr/moonseungjun/arcanecircle/client/SpellVisualSignature.java')
toml=t('src/main/templates/META-INF/neoforge.mods.toml')
index=t('src/main/resources/data/arcanecircle/spell_catalog/index.json')

need(props,'mod_version=0.12.1-alpha.20','version')
need(main,'VERSION = "0.12.1-alpha.20"','runtime version')
need(index,'"version": "0.12.1-alpha.20"','catalog version')
forbid(toml,'현재 1~3써클','stale metadata')
forbid(toml,'X 홀드식','stale fusion metadata')

need(casting,'READY_HOLD_TIMEOUT_TICKS = 12000L','ready hold window')
need(casting,'required <= 0 ? 1.0 : 0.0','zero-time completed sigil')
need(casting,'완성 후 키를 놓으면 발동','release timing copy')
forbid(casting,'if (required <= 0) {\n            castPrepared','zero-time immediate cast')
tick=section(casting,'    public static void tickCharge(ServerPlayer player) {','    private static long chargeTimeoutTicks')
forbid(tick,'castPrepared(','charge completion auto-fire')
need(tick,'WorldMagicService.charge(player, spell, false, List.of(), cast.range(), progress)','held sigil refresh')

need(gear,'LINKED_ROBES','atomic robe state')
need(gear,'syncAtomicRobe(player)','atomic robe tick')
need(gear,'replaceLooseHemWithRobe','hem removal returns whole robe')
need(gear,'player.containerMenu.setCarried(robe)','cursor atomic replacement')
need(gear,'purgeLooseHems(player)','loose hem purge')
forbid(gear,' · 바지 슬롯 필요','split robe display')
need(tooltip,'따로 보관·장착할 수 없는 내부 표시용 장비','hem tooltip')

for token in ('academyCircleViewport','maxAcademyCircleScroll','questViewport','maxQuestScroll','coreViewport','maxCoreScroll'):
    need(screen,token,'responsive UI')
need(screen,'academyCircleCard(circle,scroll)','academy scrolled cards')
need(screen,'l.panelW()>=520','responsive header')
need(screen,'g.enableScissor(circles.x(),circles.y(),circles.right(),circles.bottom())','academy clipping guard')

need(tracker,'MAX_RELEASE_GEOMETRY = 3600','release geometry budget')
need(tracker,'MAX_FRAME = 26000','frame budget')
need(tracker,'SpellVisualSignature.appendCharge','per-spell charge identity')
need(tracker,'SpellVisualSignature.appendRelease','per-spell release identity')
need(tracker,'SpellVisualSignature.prismaticAccent','prismatic multicolor layer')
for token in ('fingerprint(','time_stop','wish','meteor_swarm','power_word_kill','prismatic_spray','reverse_gravity','shapechange','true_polymorph','foresight'):
    need(signature,token,'signature coverage')
need(signature,'SignatureGeometry.append','revived authored high-circle geometry')
need(signature,'PRISM','prismatic palette')

print('Arcane Circle alpha.20 pre-play polish audit: PASS')
