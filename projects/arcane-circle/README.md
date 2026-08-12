# Arcane Circle: Ninefold Arcana

Minecraft Java 26.2 + NeoForge 26.2.0.38-beta + Java 25 기반 1~9써클 마법 RPG 모드다. 현재 버전은 `gradle.properties`의 `mod_version`을 단일 기준으로 사용한다.

## 현재 콘텐츠
- 직접 주문 90개, 융합 주문 19개, 1~9써클 전체 구현
- 주문서 학습, 숙련, 마력·쿨타임·시전시간 성장
- 소속·마법사 NPC·마법 세계·아카데미·경제·의뢰
- 지팡이/로브 장비와 테스트 키트
- 서버 권위 시전·판정과 클라이언트 월드 연출 동기화

## 조작
- `C`: 구중 마도서
- `1`~`5` 누르기: 장착 주문 전개/유지
- `1`~`5` 놓기: 준비된 주문 발동
- `X` + `1`~`5`: 최대 3개 주문을 융합 대기열에 추가
- `X` 놓기: 완성된 융합 주문 발동

시전시간이 0초인 주문도 누르는 순간 자동 발사하지 않고 ready-hold 후 release에서 발동한다.

## 현재 presentation
- `GrimoireScreen`: 기능 인덱스 + 1~9써클 인장 + 주문 브라우저 + 선택 주문 상세 + 장착 스트립
- `ArcaneHud`: 주문 인장형 전투 HUD
- `SpellCinematicDirector`: 주문의 실제 공간 사건을 기준으로 한 월드 연출
- `ArcaneRegaliaRenderer`: 복장별 독립 실루엣
- `ArcaneCastingPerformance`: snap/aim/heavy/ground/ward/portal/ritual 시전 자세

모든 주문에 같은 범용 마법진을 덧씌우지 않는다. 높은 써클이라는 이유만으로 항상 더 큰 원을 그리지 않으며, 주문의 역할·위치·고도·평면·체적·이동·충돌·잔류가 디자인의 시작점이다.

## 빌드와 검사
```bash
chmod +x gradlew
python3 tools/test_current_source.py
./gradlew --no-daemon --no-configuration-cache clean build
python3 tools/verify_jar.py "build/libs/arcanecircle-$(sed -n 's/^mod_version=//p' gradle.properties).jar"
```

Arcane 전용 정식 CI는 `.github/workflows/build-arcane-circle.yml` 하나만 유지한다. 과거 버전용 apply/fix/migration 스크립트와 구형 presentation 구현은 active tree에 보존하지 않는다.
