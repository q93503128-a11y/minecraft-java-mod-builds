# Survival Ascension

- Mod version: `0.9.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge 26.2.0.38-beta`
- Existing-world compatibility: 기존 `mining_progress_v1` SavedData와 공용 스킬 XP 맵을 그대로 유지한다. 0.9 강화 적 데이터는 엔티티 NBT에 별도 저장한다.

## 정체성
바닐라 서바이벌에서 숙련이 오를수록 수치뿐 아니라 한 번의 행동이 처리하는 물리적 범위와 행동 선택지 자체가 커지는 성장 모드다. 플레이어 체급이 커지면 적 세계도 함께 상승한다.

## 현재 활성 성장
- 채굴: Lv.10/30/60/90 = 3×3/5×5/7×7/9×9 굴착, 광맥 최대 24/64/128.
- 벌목: 연결 통나무 최대 16/48/128/256.
- 농사: 성숙 작물 3×3/5×5/7×7/9×9 광역 수확.
- 전투: Lv.100 약 1.8× 피해, Lv.30/60/90 근접 파급 2/4/8체.
- 건축: 선 5/9/17/33, 벽·바닥 3×3/5×5/9×9. 실제 재료와 보호 이벤트를 사용.
- 기동: 지상 질주 XP, Lv.10 단차/안전낙하, Lv.30 R 지상돌진, Lv.60 공중돌진 1회, Lv.90 극한돌진.

## 0.9 강화 적 세계
- 주변 플레이어 6숙련 평균으로 강화 적 발생률과 상위 랭크 확률을 계산한다.
- 랭크: 정예 I / 승천 II / 신화 III.
- 랭크마다 체력·방어·공격·이속·넉백 저항이 함께 강화된다.
- 특성: 신속 / 철벽 / 흡혈 / 광전사.
- 신화 III는 주변 플레이어에게 출현을 알리고 처치 시 추가 바닐라 경험치를 준다.
- 스포너 계열 생성은 반복 보상 파밍 방지를 위해 강화 판정에서 제외한다.
- 랭크/특성은 엔티티 persistent NBT에 저장하고 랭크 속성은 permanent modifier로 저장한다.

## M 통합 메뉴
M을 통합 메뉴키로 사용한다. MineMenu(MIT)의 radial interaction/presentation을 기반으로 한 7방향 메뉴다.
- 숙련
- 건축
- 가이드
- 해금표
- 통계
- 조작
- 닫기

건축은 두 번째 라디얼에서 단일/선/벽/바닥을 고른다. 숙련/가이드 화면은 Skill Proficiencies(MIT)의 native Minecraft screen information architecture를 기반으로 한다.

## 조작
- `M`: 통합 라디얼 메뉴
- `R`: 기동 액션 / 돌진
- `Shift`: 광역·대량 작업의 정밀 단일 모드

## 외부 코드 정책
Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions처럼 MIT로 명시된 코드는 고지를 보존하고 필요한 부분만 포팅/적응한다. Project MMO 2.0, ParCool 및 제한/카피레프트/커스텀 라이선스 소스는 의도적으로 의무를 수용하지 않는 한 기능·UX 참고만 하고 코드는 독립 구현한다.
