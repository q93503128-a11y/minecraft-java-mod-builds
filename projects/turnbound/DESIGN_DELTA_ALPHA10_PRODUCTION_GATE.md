# TURNBOUND Production Gate — alpha.10

상태: v0.4 기획/수치/캐릭터 정본을 실제 제작 순서로 고정하는 전환 문서.

## 1. 전투 UX 기준 고정
alpha.9에서 수정한 직접 타겟, 명시적 행동 확정, 2열 action dock, 스킬 hover 상세설명을 기본 입력 규약으로 유지한다.

카메라는 수치 위키 v0.4 기술 정본에 맞춘다.
- orbit pivot: 실제 Combatant anchor 평균점
- default pitch: 22°
- default distance: 11 blocks
- pitch clamp: -10° ~ 58°
- distance clamp: 6 ~ 18 blocks
- horizontal drag: 0.18°/pixel
- vertical drag: 0.15°/pixel
- wheel: 0.75 block/step
- Minecraft third-person collision raycast를 유지해 벽 관통을 막는다.

## 2. 제작 진행 순서
이제 개별 P0 화면을 계속 재설계하는 단계에서 벗어나 정본의 게임 전체 목표를 순서대로 구현한다.

### P2
- Aster March 고정 월드 확장
- Southgate Meadow ENC_M01~M05
- E001~E005 정식 전투 데이터
- NPC / Quest / Reward / Fast Travel
- B01 Graul
- Chapter 1 진행 및 첫 클리어 해금

### P3
- saveSchemaVersion 4 영속 진행 데이터
- 캐릭터 보유/레벨/XP/성급/승급
- Party 4 Slot / Preset
- Echo Archive 가챠 / pity / duplicate conversion
- Weapon / Armor / Accessory
- Gold only +0~+20 강화
- CP

### P4
- P01~P08 전체 캐릭터 정식 구현
- ★6 Awakening Package
- Signature slot / 캐릭터별 Signature
- B02~B05 / 스토리 / 전체 Aster March 지역
- 고난도 반복 콘텐츠
- AUTO 고도화

## 3. Presentation 병행
기능 진행만 끝낸 뒤 마지막에 외형을 한꺼번에 붙이지 않는다.
각 지역/캐릭터가 플레이 가능한 단계에 도달할 때마다 모델/애니메이션/VFX/SFX를 병행한다.
ArmorStand는 시스템 stand-in으로만 남기며 최종 완료 기준에 포함하지 않는다.

## 4. 검증 게이트
- 각 alpha: Java 25 clean test/build + NeoForge server smoke
- P2 vertical slice: 실제 client field→encounter→battle→reward→return 검사
- P3: 저장/재접속/중복/경제 회귀검사
- P4: P01~P08 조합/보스/각성/Signature 회귀전투
- 완성 후보: fresh world canonical audit + JAR 내부 검증
