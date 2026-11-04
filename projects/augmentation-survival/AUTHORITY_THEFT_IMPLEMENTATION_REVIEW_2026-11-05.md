# 권능 탈취 실코드 조사 — 2026-11-05

대상 HEAD: 176b0f00b049dcc8a28628a069d166648ac40b84
기준: augment/active/AuthorityTheftService.java + entity_compatibility_profiles.json + player state.
상태: CODE REVIEWED; 실제 인게임, 멀티플레이, 성능 측정은 NOT TESTED.

## 실제 지원 24종
1. 위더 (바닐라, `vanilla_wither`): 24블록 내 적 최대 3명에게 공격·시듦 II(8초). 근접 5블록 폭발·넉백, 자신 체력 1 회복.
2. 엔더 드래곤 (바닐라, `vanilla_ender_dragon`): 6블록 근접 충격파와 넉백. 바라보는 방향 12블록·70도 범위에 드래곤 브레스 피해.
3. Time Guardian (Chrono Dawn, `chronodawn_time_guardian`): 적 뒤로 순간이동, 대상 둔화. 주변 4블록에 시간 둔화·채굴 피로와 광역 피해.
4. Chronos Warden (Chrono Dawn, `chronodawn_chronos_warden`): 저항 IV 5초. 주변 4블록에 충격파·넉백, 가까운 대상에게 채굴 피로.
5. Clockwork Colossus (Chrono Dawn, `chronodawn_clockwork_colossus`): 신속 II 8초, 최대체력 15% 회복. 가까운 대상 타격과 전방 10블록 지면 충격파.
6. Temporal Phantom (Chrono Dawn, `chronodawn_temporal_phantom`): 저항 V 2초, 대상 뒤로 순간이동 공격·둔화. 15초 동안 전투 분신 2체 소환.
7. Entropy Keeper (Chrono Dawn, `chronodawn_entropy_keeper`): 적 집단에 둔화·독. 6블록 내 적에게 강한 피해와 시듦.
8. Time Tyrant (Chrono Dawn, `chronodawn_time_tyrant`): 대상 강한 둔화·순간이동. 신속 II 8초, 5블록 광역 공격, 최대체력 10% 회복.
9. Sylvaron (World Bosses, `worldbosses_sylvaron`): 저항 I 4초. 7블록 안의 적에게 강한 둔화·나약함·독과 피해.
10. Aureon (World Bosses, `worldbosses_aureon`): 18블록 내 대상 집중 공격. 7블록 안에 둔화·발광·광역 피해.
11. Nekhraz (World Bosses, `worldbosses_nekhraz`): 8블록 주변 적에게 어둠·둔화. 대상 뒤로 순간이동해 강타하고 띄움.
12. Varkhul (World Bosses, `worldbosses_varkhul`): 주 대상 강한 둔화·나약함. 6블록 안의 적을 점화하고 피해.
13. Morvath (World Bosses, `worldbosses_morvath`): 8블록 어둠·나약함·넉백. 대상에게 피해를 주고 최대체력 15% 한도 회복.
14. Elyndra (World Bosses, `worldbosses_elyndra`): 9블록 주변 적을 끌어당겨 둔화·피해를 주고 위로 띄움.
15. Astrael (World Bosses, `worldbosses_astrael`): 저항 II·신속 II·화염 저항 6초. 9블록 적에게 강한 광역 피해·둔화·나약함·넉백.
16. Shadow King (World Bosses, `worldbosses_shadow_king`): 대상 타격·순간이동. 7블록 어둠·피해, 12초 전투 분신 2체.
17. Dragon Mother (World Bosses, `worldbosses_dragon_mother`): 전방 화염 브레스와 4블록 넉백. 주요 대상 화염 공격, 전투 분신 2체(10초).
18. Storm Griffin (World Bosses, `worldbosses_storm_griffin`): 9블록 강한 돌풍·넉백·발광·광역 피해. 18블록 대상 추가 타격.
19. Shadow Weaver (World Bosses, `worldbosses_shadow_weaver`): 저항 III 4초, 8블록 둔화·나약함·피해, 전투 분신 3체(12초)와 대상 추가 공격.
20. Arachne (MCEndgame, `mcendgame_arachne`): 적을 당겨 둔화한 뒤 전방 4블록·3블록 폭의 근접 휩쓸기.
21. Bonecrusher (MCEndgame, `mcendgame_bonecrusher`): 최대 50블록 적 뒤로 순간이동해 접근. 6블록 강한 충격파와 넉백.
22. Elf Duelist (MCEndgame, `mcendgame_elf_duelist`): 근접 공격과 후방 도약. 10블록 내 전방 투사체를 50% 확률로 반사.
23. Beakburn (MCEndgame, `mcendgame_beakburn`): 12블록 돌풍, 전방 8블록·70도 화염 브레스. 15블록 적에게 7% 추가 화염 타격.
24. Beastweaver (MCEndgame, `mcendgame_beastweaver`): 9블록 강력한 충격파·둔화, 원거리 대상 추가 공격, 전투 분신 3체(15초)·신속.

## 현재 서버 권한 획득 및 해방
- 권능 탈취 증강 보유 중, 서버가 잡은 root-direct 플레이어 kill 이벤트만 처리.
- profile.boss=true이고 지원 패키지 ID를 찾아 획득. Chrono Dawn/World Bosses는 identity profile, MCEndgame은 dynamic dungeon boss accessor.
- 플레이어별 stolenAuthorityBossIds에 최초 1회만 저장. 복수 처치로 중첩되지 않음.
- 기본 J: 저장된 모든 권능을 한 틱에 순회 발동. 120초 공통 쿨다운. 효과별 개별 발동 기능 없음.
- Shift+J: 기존 메인에는 미구현. 이번 별도 입력/서버 snapshot 읽기 화면으로 추가.
- 바닐라 워든은 profile.boss=true지만 releasePackage whitelist에서 지원하지 않으므로 탈취 불가.

## 게임성 및 리스크
- 한 번에 여러 공격이 발동: 같은 타깃에 동틱 hurtServer 반복 → 무적시간/피해 처리에 따라 기댓값보다 낮거나 높은 체감 가능성. 인게임으로 확인 필요.
- 다수의 순간이동 권능 순차 발동 시 뒤 권능의 중심/범위가 달라짐; 근거리 넉백이 뒤 공격 범위 밖으로 대상을 밀 수도 있음.
- 충격파·이동·상태효과·분신·파티클이 한 틱에 겹쳐 읽기 어렵고 24종 극단 입력은 렌더/서버 부하 우려. profiler 없이 성능 단정 금지.
- 일부 명칭과 실제 효과가 다름: Time Tyrant 시간 정지는 실제 영역 시간정지 필드가 아니라 둔화. Temporal Phantom의 phase도 실 구현은 저항 + 이동.
- 강점은 광역 CC, 전방 공격, 대량 분신·순간이동, 생존 버프 조합. 단일 권능은 쓸 만하나 누적 발동의 정체성·정보 UI가 불분명.
- 디버깅: 권능 획득 안내, Shift+J 목록, J 한 번의 발동 로그·피해/대상 수 telemetry, invulnerability와 연속 hurt 검증 필요.
- 권능 탈취는 일반 액티브 10 슬롯과 분리하고 보스 권능을 임의 버프 스택/스킬북 개별 선택 구조로 변경하지 말 것. 구조 재설계는 별도 사용자 의사결정 필요.

## 지배 조작 확인
- CompanionAugmentService.tryDominate(): 자기 소유 지배 몹을 조준해 다시 사용하면 해제 (지배 칸이 가득 차도 가능).
- 사망하면 슬롯 회수, 소생하면 다시 지배 점유 처리. 상태는 owner UUID와 save-wide occupied count.
- 장거리/미로드/오프라인 개체 목록 관리 UI는 실제 코드에 없음. Shift+지배 관리창도 없음.
- 관리 GUI를 구현하려면 실제 영구 로스터·차원·상태·해제 권한/원거리 가용성 및 레벨 한도 정확성부터 설계.
