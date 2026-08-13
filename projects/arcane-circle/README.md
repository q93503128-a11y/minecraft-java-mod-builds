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
- `GrimoireScreen`: 기능 인덱스 + 안전영역 기반 1~9써클 레일 + 주문 브라우저 + 선택 주문 상세 + 양방향 장착 도크
- `ArcaneHud`: 주문 인장형 전투 HUD
- `ArcaneSigilDirector`: 주문별 술식·보조진·룬·다중 평면을 조립하는 실제 시전 마법진
- `SpellCinematicDirector`: 마법진에서 이어지는 투사체·게이트·폭풍·영역·충돌의 물리 연출
- `ArcaneRegaliaRenderer`: 보디스·라펠·맨틀·치마폭·트레인으로 구성한 입체 마도복
- `ArcaneCastingPerformance`: snap/aim/heavy/ground/ward/portal/ritual 시전 자세

마도서 장착은 주문→슬롯, 슬롯→주문 어느 순서로도 가능하고 주문 더블클릭으로 첫 빈 슬롯에 빠르게 장착할 수 있다. 하단 도크는 긴 이름 대신 인장과 상태만 보여 작은 GUI에서도 정보가 충돌하지 않는다.

모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 단, 같은 원을 복붙하지 않는다. 학파·주문·앵커에 따라 룬, 보조진, 평면, 직교환, 축방향 구조, 3D 깊이와 전개 순서가 달라지고, 높은 써클이라는 이유만으로 무조건 커지지는 않는다. 완성된 술식에서 투사체·게이트·폭풍·영역·충돌·잔류가 이어진다. 최종 사거리 증가는 술식 종류에 맞는 비율로 마법진 크기에도 반영되며, 상공/영역 의식은 전면 발사진보다 크게 반응한다.

## 빌드와 검사
```bash
chmod +x gradlew
python3 tools/test_current_source.py
./gradlew --no-daemon --no-configuration-cache clean build
python3 tools/verify_jar.py "build/libs/arcanecircle-$(sed -n 's/^mod_version=//p' gradle.properties).jar"
```

Arcane 전용 정식 CI는 `.github/workflows/build-arcane-circle.yml` 하나만 유지한다. 과거 버전용 apply/fix/migration 스크립트와 구형 presentation 구현은 active tree에 보존하지 않는다.
