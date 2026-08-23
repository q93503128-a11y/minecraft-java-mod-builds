# Frontier Settlement — External Content Register

기준일: 2026-08-23

목적: 외부 모드를 콘텐츠 생산 수단으로 적극 사용하되, 공개 Frontier 저장소에 무엇을 실제로 가져올 수 있는지 명확히 구분한다.

상태:
- `DEPENDENCY`: 공식 JAR/데이터팩을 설치해 그대로 사용. 저장소에 자산 복사 안 함.
- `REFERENCE`: 동작/정보 구조/아키텍처 참고. 직접 코드 복사 안 함.
- `CODE_REUSE`: 라이선스 조건을 지키면 코드/데이터 일부 재사용 후보.
- `ASSET_REUSE`: 텍스처/모델/구조물 등 자산까지 재사용 가능하다고 별도 확인된 경우에만 부여.

`GitHub에 소스가 보인다`는 것만으로 CODE_REUSE/ASSET_REUSE가 되지 않는다.

| 프로젝트 | 현재 26.2 상태 | 라이선스 | 사용 상태 | Frontier에서 맡길 콘텐츠 | 비고 |
| --- | --- | --- | --- | --- | --- |
| Terralith | 2.6.4 NeoForge 26.2 | Stardust Labs License | DEPENDENCY | 바이옴/지형/월드 밀도 | 공식 배포 사용, 자산 복사 안 함 |
| Dungeons and Taverns | 5.3.0 Data Pack 26.2 | ARR | DEPENDENCY | 던전/탐험 구조물 | 공식 팩 사용, 구조물 파일 복사 안 함 |
| Repurposed Structures | 7.7.5+26.2-neoforge | LGPL-3.0-only | DEPENDENCY + CODE_REUSE 검토 | 구조물 다양성 | 우선 설치 사용. 직접 코드 재사용 시 LGPL 경계 검토 |
| Better Combat | 3.2.2+26.2-neoforge | ARR | DEPENDENCY | 전투 체감/무기 모션 | JSON 호환 사용, 자산 복사 안 함 |
| Weapons Expanded | 26.2 NeoForge | MIT | DEPENDENCY + CODE_REUSE | 무기 종류/전투 콘텐츠 | MIT 고지 유지. 자산 범위는 실제 repo LICENSE 적용 범위 재확인 전 ASSET_REUSE 금지 |
| Sophisticated Backpacks | 26.2-3.25.x | ARR | DEPENDENCY | 탐험 수납 | 공식 JAR 사용 |
| Sophisticated Core | 26.2 호환 | 공식 프로젝트 라이선스 확인 | DEPENDENCY | Backpacks 의존성 | 직접 콘텐츠 재사용 대상 아님 |
| Lootr | 26.2 NeoForge | MIT | DEPENDENCY + CODE_REUSE | 멀티 던전 보상 | 공동 탐험에서 상자 선점 문제 해결 |
| Jade | 26.2.2+neoforge | CC-BY-NC-SA-4.0 | DEPENDENCY / API | 건물·주민 최소 상태 HUD | provider/API 통합 우선 |
| Xaero's Minimap | 26.4.2 NeoForge 26.2 | ARR | DEPENDENCY | 위치/waypoint | 공식 지원 방식 외 코드/자산 복사 안 함 |
| Variants & Ventures | 1.0.26+mc26.2 NeoForge | CC BY-NC-ND 4.0 | DEPENDENCY | 바닐라형 몹 변형 | 수정/자산 흡수 금지, 공식 모드 그대로 사용 |
| Alex's Mobs Continued | 2.x+26.2 NeoForge | GPL-3.0-only | DEPENDENCY + REFERENCE | 대규모 생물/몬스터 폭 | Frontier MIT 코드에 직접 GPL 코드 혼합 금지. 안정성 검증 후 기본 스택 여부 결정 |
| MineColonies | 공식 현재 주력 1.21.1 계열 | GPL-3.0 code / 배포 측 별도 조건 존재 | REFERENCE | 주민 AI/건물/건설 UX/청사진 구조 | Frontier 핵심과 겹침. 26.2 기본 의존성 아님. 직접 코드 복사 시 라이선스 전환 문제 발생 |
| Create | 공식 현재 주력 1.21.1 계열 | code MIT / assets ARR | REFERENCE | 물리적 작업 표현/UX 아이디어 | 26.2 기본 스택 제외. 코드 패턴만 MIT 조건으로 검토 가능, assets 복사 금지 |
| Farmer's Delight | 공식 현재 1.21.1 계열 | MIT | REFERENCE / 미래 DEPENDENCY | 농업/식품 폭 | 26.2 공식 배포 확인 전 기본 스택 제외 |
| Supplementaries | 공식 1.21.1 계열 확인 | Supplementaries Team License | DEPENDENCY/REFERENCE only | Vanilla+ 기능/장식 | 공개 재배포 제한. Frontier 저장소에 코드/asset 복사 금지 |

## 즉시 채택

다음 새 월드 테스트 스택부터 우선 추가 대상으로 고정:

1. Repurposed Structures
2. Lootr
3. Weapons Expanded

기존 기본 스택:

- Terralith
- Dungeons and Taverns
- Better Combat
- Sophisticated Backpacks + Core
- Jade
- Xaero's Minimap

다음 단계 후보:

- Variants & Ventures
- Alex's Mobs Continued (별도 안정성 검사 후)

## Frontier 코드에 실제 흡수할 때 체크

외부 파일/코드를 Frontier 저장소에 추가하기 전에 반드시 기록:

- 원 프로젝트 이름
- 공식 source URL
- 정확한 버전/commit
- 원 라이선스
- 가져온 파일/클래스/데이터 범위
- 수정 내용
- attribution 파일 위치
- Frontier 현재 MIT 라이선스와 충돌 여부

GPL 소스를 직접 파생한 코드를 Frontier에 넣는 경우 현재 MIT 배포 구조를 그대로 유지할 수 있다고 가정하지 않는다. 별도 경계/라이선스 검토 없이 복사 금지.

ARR/ND/커스텀 재배포 제한 자산은 개인 플레이 목적이어도 공개 GitHub 정본에는 넣지 않는다. 필요한 경우 공식 JAR/팩 dependency로 사용한다.
