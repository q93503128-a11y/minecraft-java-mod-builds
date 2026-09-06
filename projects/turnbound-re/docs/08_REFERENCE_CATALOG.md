# 08 — REFERENCE CATALOG

이 문서는 복제 목록이 아니라 **어떤 문제를 어떤 기존 사례에서 배웠는지** 기록한다.

## R-001 Persona 5 Royal
- 공식: https://persona.atlus.com/p5r/
- 관찰: 적 약점을 공략하면 `One More`로 추가 행동을 얻어 약점 지식이 즉각적인 템포 보상으로 이어진다.
- 채택하는 원리: 약점 정보가 단순 damage multiplier가 아니라 전투 흐름을 바꿔야 재미가 커진다.
- TURNBOUND: RE 변환: 추가턴을 그대로 복제하지 않고 WEAK가 Poise 피해를 크게 만들어 EXPOSED 창을 앞당긴다.

## R-002 OCTOPATH TRAVELER series
- 공식 예: https://na.store.square-enix-games.com/octopath-traveler-ii---switch-2
- 관찰: Break/Boost가 방어를 무너뜨릴 시점과 자원을 몰아쓸 시점을 분리한다.
- 채택하는 원리: `준비 단계 -> 명확한 공격 창`의 리듬.
- TURNBOUND: RE 변환: 독자 명칭/규칙인 Poise/EXPOSED + Energy를 사용한다. 원작 수치/화면/명칭을 복제하지 않는다.

## R-003 Clair Obscur: Expedition 33
- 공식: https://www.expedition33.com/overview
- 관찰: turn-based의 계획성과 공격/방어 중 반응형 입력을 결합한다.
- 채택하는 원리: 플레이어가 적 차례에도 전투를 읽고 대응한다는 감각.
- TURNBOUND: RE 변환: 초기에는 네트워크 안정성을 위해 실시간 parry를 핵심에 넣지 않고, Enemy Intent를 통해 적 차례 전 대응 결정을 만든다. QTE는 추후 실험으로 격리.

## R-004 TurnBasedMinecraftMod
- 저장소: https://github.com/Stephen-Seo/TurnBasedMinecraftMod
- branch: `neoforge`
- license: MIT (저장소 기준, 실제 코드 사용 전 다시 확인)
- 관찰: Minecraft에서 battle/combatant, GUI, network, mob config를 별도 책임으로 나누는 실제 구현 사례이며 NeoForge branch가 존재한다.
- 채택하는 원리: Minecraft world adapter와 battle state/network를 분리해야 유지보수가 쉽다.
- 현재 실제 코드 복사: 없음.

## 참고 원칙
- 시스템 이름/수치/콘텐츠/화면을 통째로 복제하지 않는다.
- '재미있어 보임'이 아니라 문제→원리→TURNBOUND: RE 변환을 기록한다.
- 최종 UI/월드/캐릭터/VFX 레퍼런스는 아직 **미선정**이다. 해당 자료는 M5 전에 별도 섹션에 URL/스크린샷 출처/사용 이유를 채워야 한다.

## Visual Reference Gate — 미완료
다음은 의도적으로 TODO이며 M0~M4 backend를 막지 않는다.
- [ ] Battle HUD reference set.
- [ ] Character/party screen reference set.
- [ ] World architecture/reference set.
- [ ] Skill/VFX reference language.
- [ ] Iconography/typography reference.
