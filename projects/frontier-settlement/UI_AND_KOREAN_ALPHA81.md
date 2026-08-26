# Frontier Settlement 0.1.0-alpha.81 — 시작 UX / 공개 UI 레퍼런스 / 한국어 보완

## 목적
Alpha.81은 새 대형 게임 시스템이 아니라 실제 첫 플레이에서 드러난 진입 장벽을 제거하는 UX 패스다.
기존 `/frontier found` 명령은 진단/호환용으로 남기되, 정상 플레이는 명령어 없이 `B` 하나로 시작한다.

## 첫 시작
- 아직 공동 마을이 없는 월드에서 `B`를 누르면 `SettlementStartScreen`이 열린다.
- `현재 위치에 개척지 세우기`는 새 serverbound payload를 보내고 실제 설립 판정은 서버의 기존 `SettlementService.foundAt`가 수행한다.
- 따라서 오버월드/거리/안전한 블록/공동 창고 배치/롤백/한 월드 한 마을 규칙은 UI가 우회하지 않는다.
- 설립 전 HUD에도 `공동 개척지 없음 · B를 눌러 시작`이 표시된다.
- 4페이지 인게임 가이드에서 실제 ItemStack 창고, B/R/Enter, 초반 성장, 도로·전초기지 흐름을 설명한다.

## 건설 팔레트
기존 한 화면 나열을 다음 카테고리로 재구성한다.
- 기반
- 생산
- 제작·서비스
- 방어
- 인프라

레이아웃 원칙은 공개 UI 레퍼런스를 참고한다.
- YACL: LGPL-3.0-or-later. 카테고리 내비게이션, 그룹화, 설명 우선 정보 구조를 참고한다.
- Jade: 정보 HUD의 짧은 계층과 가독성을 참고한다.
- Xaero's Minimap: 바닐라 화면과 어울리는 절제된 정보 밀도만 참고한다.

다른 모드의 UI 코드/에셋을 복사하지 않는다. Frontier의 Screen/Button/GuiGraphicsExtractor 코드와 자체 색/배치로 다시 구현한다.

## 한국어 보완
현재 고정 companion에서 이미 한국어가 충분한 Better Combat, Lootr, Sophisticated 계열 등은 불필요하게 덮지 않는다.
영어 잔존이 확인된/부분 번역인 다음 네임스페이스에는 Frontier가 `ko_kr.json` 리소스 오버레이를 제공한다.
- Weapons Expanded
- Variants & Ventures
- Repurposed Structures
- YetAnotherConfigLib (YACL)

이는 외부 JAR이나 Java 코드를 수정/재배포하는 것이 아니라 Minecraft 리소스 로딩을 통한 문자열 보완이다.
외부 Java 클래스를 직접 import하지 않으며 companion이 빠져도 Frontier 핵심 로직은 부팅 가능해야 한다.

Terralith의 시작 안내는 일반 lang 키가 아닌 별도 안내 함수/설정 계층이므로 Alpha.81 JAR에서 무리하게 원본 동작을 덮지 않는다.
새 테스트용 mrpack에서 정확한 26.2 설정 경로가 검증된 뒤 시작 메시지를 끄는 방식을 우선한다.

## 검증 경계
자동 source/docs/Java 25/JAR 검사는 구조와 빌드 회귀를 검증한다.
실제 그래픽 클라이언트에서 시작 화면 클릭, 화면 크기별 팔레트 가독성, companion 문자열 표시, 키 충돌은 실제 플레이 검증 대상이다.
