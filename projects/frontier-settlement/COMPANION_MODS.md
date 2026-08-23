# Frontier Settlement — Companion Mod Stack (Minecraft 26.2 / NeoForge)

원본 기획서 v0.2의 외부 모드팩 연동 방향을 실제 테스트 구성으로 고정한다.

원칙:

- Frontier Settlement JAR은 단독 실행 가능해야 한다.
- 외부 모드 JAR을 Frontier JAR 내부에 무단 번들하지 않는다.
- 실제 사용자 테스트 인스턴스에서는 아래 companion stack을 함께 사용한다.
- 버전은 Minecraft 26.2 / NeoForge 호환 배포를 기준으로 고정하고, 업데이트 시 다시 호환 검증한다.
- 월드 생성 계열(Terralith, Dungeons and Taverns)은 새 월드 생성 전에 설치한다.

## 권장 기본 스택

| 역할 | 모드 | 26.2 기준 검증 버전/계열 | 설치 위치 | Frontier 연결 목적 |
| --- | --- | --- | --- | --- |
| 월드/바이옴 | Terralith | 2.6.4 NeoForge 26.2 | 서버/싱글 월드 생성 전 | 전초기지 후보 지형 다양화 |
| 구조물/던전 | Dungeons and Taverns | 5.3.x / 26.2 호환 데이터팩·모드 계열 | 월드 생성 전 | 탐험 목표와 영지 성장 입력 |
| 전투 | Better Combat | 3.2.2+26.2-neoforge | 클라이언트+서버 | Frontier가 별도 전투 시스템을 과도하게 재구현하지 않도록 역할 분리 |
| 탐험 수납 | Sophisticated Backpacks | 26.2-3.25.x | 클라이언트+서버 | 장거리 개척 왕복 피로 감소 |
| 필수 라이브러리 | Sophisticated Core | 26.2-1.4.x | 클라이언트+서버 | Sophisticated Backpacks 의존성 |
| 최소 상태 표시 | Jade | 26.2.2+neoforge | 클라이언트/서버 호환 | 향후 Frontier 건물·주민 상태 provider 연결 |
| 위치/지도 | Xaero's Minimap | 26.4.2 NeoForge 26.2 | 주로 클라이언트 | 본진·전초기지·도로 위치 파악 |

## 현재 확인한 배포

### Terralith
- 프로젝트: https://modrinth.com/datapack/terralith
- 26.2 NeoForge 배포: `2.6.4`
- 라이선스: Stardust Labs License
- Frontier 코드 의존성: 없음
- 월드 생성 전에 설치 권장.

### Dungeons and Taverns
- 프로젝트: https://modrinth.com/datapack/dungeons-and-taverns
- Minecraft 26.2 호환 프로젝트 배포 확인.
- 데이터팩 버전을 사용하면 loader 종속성을 줄일 수 있으며, 모드 버전은 리소스를 자동 로드한다.
- 라이선스: ARR.
- Frontier 저장소에 자산을 복사하지 않는다.

### Better Combat
- 프로젝트: https://modrinth.com/mod/better-combat
- 26.2 NeoForge 배포: `3.2.2+26.2-neoforge`
- 라이선스: ARR.
- Frontier는 공격 애니메이션/무기 체계를 복제하지 않고 호환만 유지한다.

### Sophisticated Backpacks
- 프로젝트: https://modrinth.com/mod/sophisticated-backpacks
- 26.2 NeoForge `3.25.x` 계열 사용.
- Sophisticated Core 26.2 `1.4.x` 계열 필요.
- 두 프로젝트 모두 Frontier 저장소에 번들하지 않는다.

### Jade
- 프로젝트: https://modrinth.com/mod/jade
- 26.2 NeoForge 배포: `26.2.2+neoforge`
- 라이선스: CC-BY-NC-SA-4.0.
- 현재는 일반 호환만, 후속 Alpha에서 건물/주민 최소 상태 provider를 선택적으로 추가한다.

### Xaero's Minimap
- 프로젝트: https://modrinth.com/mod/xaeros-minimap
- 26.2 NeoForge 배포: `26.4.2`
- 라이선스: ARR.
- 현재는 클라이언트 지도 용도. 직접 코드 의존은 두지 않는다.

## 설치 원칙

### 실제 테스트 인스턴스

필수:
- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Frontier Settlement 최신 테스트 JAR

권장 companion:
- Terralith
- Dungeons and Taverns
- Better Combat
- Sophisticated Backpacks
- Sophisticated Core
- Jade
- Xaero's Minimap

### 멀티

서버 로직이 필요한 Better Combat / Sophisticated 계열은 호스트와 참가자가 같은 호환 버전을 맞춘다.
Terralith/Dungeons and Taverns의 월드 생성 데이터는 호스트/서버가 월드를 생성하기 전에 적용한다.
Jade/Xaero의 클라이언트 기능은 각 모드의 공식 지원 범위에 따른다.

## 아직 남은 선택적 통합

- Jade: Frontier 건물 / 주민 / 전초기지 최소 상태 provider
- Terralith: 바이옴 태그를 이용한 전초기지 전문화 판정 보강
- Dungeons and Taverns: 구조물 탐험을 영지 성장/희귀 자원 입력과 연결
- Xaero: 공식 API/호환 방식이 안전할 경우 본진·전초기지 waypoint 연동 검토
- Better Combat: Frontier 경비/플레이어 전투가 깨지지 않는 회귀검사
- Sophisticated Backpacks: 공동 저장소 스캔이 플레이어 휴대 가방을 마을 자원으로 오인하지 않는지 회귀검사

## 금지

- 외부 ARR 자산/JAR을 Frontier JAR에 재포장
- 특정 companion이 없으면 Frontier가 부팅하지 못하게 만드는 하드 의존
- 원본 기획의 핵심 마을/도로/전초/물류를 외부 모드에 떠넘기는 것
