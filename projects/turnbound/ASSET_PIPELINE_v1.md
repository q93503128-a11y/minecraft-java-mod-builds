# TURNBOUND External Asset Pipeline v1

> 목적: 외부 UI/폰트/모델/아이콘을 실제 production에 가져올 때 품질과 라이선스, 경로 복잡도, 교체 가능성을 동시에 지키는 작업 규칙.

## 1. 다른 Minecraft 프로젝트에서 가져온 작업 패턴

### Resource-pack driven UI

MCUI와 CustomGUI 계열은 GUI 외형을 Java 코드에 직접 박는 대신 resource pack이 HUD/UI texture를 교체할 수 있게 설계한다.

TURNBOUND 적용:
- Java는 layout/state/input을 담당한다.
- texture/font/icon은 resource namespace에서 교체 가능하게 둔다.
- 동일 기능을 새 스킨으로 바꾸기 위해 Screen 코드를 다시 쓰지 않는다.

### Additional asset license를 별도로 관리

Modern UI for Minecraft는 코드 라이선스와 별개로 Source Han Sans, JetBrains Mono, Inter 같은 추가 폰트의 라이선스를 명시하고 원 저작권 고지를 유지한다.

TURNBOUND 적용:
- 프로젝트 전체 라이선스 한 줄로 외부 자산 권리를 덮지 않는다.
- 폰트/아이콘/텍스처/모델별 source/license를 기록한다.
- 라이선스 고지가 필요한 경우 asset 옆 또는 third-party notice에 유지한다.

### Third-party 파일 근처에 LICENSE 유지

외부 리소스를 포함하는 공개 Minecraft 프로젝트들은 특정 third-party asset 폴더 옆에 원 라이선스 파일을 두는 방식을 사용한다.

TURNBOUND 적용:
- 직접 사용 asset 묶음은 source note와 license를 같은 third-party entry에서 추적한다.
- 어디서 왔는지 알 수 없는 png/json/model 파일을 만들지 않는다.

### 재배포 불명확 자산은 교체하거나 별도 설치

모드팩/리소스팩 프로젝트에서도 원본 자산의 재배포 허가가 불명확하면 명시적으로 라이선스가 허용된 대체재를 쓰는 사례가 있다.

TURNBOUND 적용:
- Drehmal 원본처럼 별도 설치가 필요한 자산은 repository/JAR에 넣지 않는다.
- unknown-license 자료는 reference-only.
- 개인용 프로젝트라도 출처를 잃지 않는다.

### 실사용 Minecraft 모드의 import/credit 패턴을 TURNBOUND에 적용

공개 대형/장기 Minecraft 프로젝트를 확인했을 때 유용한 공통점:

- Oritech는 외부 프로젝트에서 가져온 모델/텍스처/사운드를 **무엇을 가져왔는지 항목별로 적고**, “slightly modified / heavily modified”처럼 변형 정도도 같이 기록한다.
- Twilight Forest / Create 계열은 code license와 asset license를 분리한다. 즉 repository가 MIT/GPL이라고 해서 `assets/`도 같은 권리라고 추정하지 않는다.
- Botania는 외부에서 들어온 코드와 asset provider를 별도 credits / alternate-license 기록으로 유지한다.
- MineClone 계열은 큰 외부 resource lineage를 쓸 때 원저작자, pack, 변형/추가 저작자를 모듈 단위로 계속 추적한다.

TURNBOUND 적용:
1. repository 최상위 LICENSE만 보고 자산 사용 가능 판정 금지.
2. `assets/`, `resources/`, sound/model/texture 하위 경로에 별도 라이선스가 있는지 먼저 확인.
3. upstream asset이 다시 다른 프로젝트에서 온 것이라면 **provenance chain을 원출처까지 따라간다**.
4. 실제 사용한 파일만 가져오고 source path + immutable commit + blob SHA를 SOURCE.md에 남긴다.
5. 수정한 파일은 수정 사실과 변환 내용을 명시한다. Apache-2.0처럼 modified-file notice를 요구하는 경우 반드시 표시한다.
6. upstream NOTICE가 있으면 적용 범위를 확인하고 필요한 notice를 같이 보존한다.
7. model/texture/animation/weapon처럼 한 캐릭터를 구성하는 자산은 가능하면 **동일 visual family**에서 가져온다. 여러 pack을 섞는 것은 품질상 이유가 있을 때만 한다.
8. 전체 upstream pack을 통째로 vendor하지 않는다. runtime에 쓰는 조각 + 필요한 license/notice + provenance만 보존한다.
9. external asset을 재사용한 사실 자체가 완성도를 보장하지 않는다. 실제 Minecraft camera/GUI scale에서 production 품질을 확인한다.


## 2. 자산 등급

모든 외부 자료는 다음 중 하나다.

- `reference`: 구조/실루엣/정보 계층만 참고. 파일 복사 금지.
- `editable_base`: 수정 허용이 확인된 원본. 파생 작업 가능.
- `direct_asset`: 그대로 또는 최소 변환 후 runtime 사용 가능.
- `code_library`: 라이브러리/API 의존성.
- `unknown_license`: 조사 전까지 runtime/repository에 넣지 않음.

## 3. No temporary visual pass

TURNBOUND는 player-facing visual에 별도의 disposable placeholder 단계를 두지 않는다.

- UI frame/button/icon은 첫 visible pass부터 검증된 외부 pack 또는 final-quality asset을 사용한다.
- 캐릭터/적/NPC 모델은 vanilla entity, armor stand, 단색 skin 등을 “나중에 교체할 임시 외형”으로 먼저 배치하지 않는다.
- texture/portrait/VFX도 최종 방향과 무관한 임시 이미지를 normal gameplay에 넣지 않는다.
- 외부 자산을 직접 사용할 때는 같은 작업 단위에서 source/license를 기록한다.
- 적합한 자산을 찾지 못했으면 해당 visual binding을 보류하고 조사/제작을 먼저 한다.
- 기능 검증이 필요하면 플레이어에게 보이지 않고 production에서 비활성인 logic-only scaffold를 사용할 수 있다. 이것은 visual completion으로 계산하지 않는다.

즉, **첫 player-visible 구현이 곧 production visual 방향이어야 한다.**

## 4. 경로 규칙

Runtime 자산은 얕게 유지한다.

```
assets/turnbound/ui/common/
assets/turnbound/ui/battle/
assets/turnbound/ui/party/
assets/turnbound/ui/map/
assets/turnbound/ui/summon/
assets/turnbound/ui/icons/
assets/turnbound/ui/portraits/
assets/turnbound/ui/fonts/

assets/turnbound/geckolib/models/
assets/turnbound/geckolib/animations/
assets/turnbound/textures/entity/
```

출처 기록:

```
THIRD_PARTY/<asset-id>/
  SOURCE.md
  LICENSE
```

필요한 경우 원본 전체 pack을 보관하지 않고 사용한 조각의 출처/변환만 SOURCE.md에 기록한다.

## 5. SOURCE.md 최소 항목

- asset id
- 원 출처 URL
- 저작자/프로젝트
- 원본 버전 또는 immutable commit SHA
- 분류(reference/editable_base/direct_asset/code_library)
- license와 **license scope** (code / asset / path-specific 여부)
- upstream NOTICE 존재 여부와 보존 필요 여부
- 원본 파일명/경로
- 가능하면 원본 blob SHA
- upstream이 다른 asset을 재사용한 경우 원출처 provenance chain
- TURNBOUND destination
- unchanged / modified 구분
- 변경 사항(crop, recolor, retarget, bone anchor, 9-slice, atlas, format conversion 등)
- runtime에서 실제 사용되는 방식
- 확인 날짜

## 6. UI import 절차

1. 화면의 정보 구조를 먼저 확정한다.
2. 후보 asset pack 1~3개만 비교한다.
3. 라이선스와 실제 픽셀/9-slice 구조를 확인한다.
4. 한 화면 mockup으로 Minecraft GUI Scale을 확인한다.
5. primary skin을 정한 뒤 필요한 조각만 import한다.
6. SOURCE/LICENSE 기록을 같은 작업에서 추가한다.
7. renderer는 texture dimensions/corner size를 data/token으로 받는다.
8. 실제 16:9, 16:10, 4:3과 여러 GUI Scale에서 확인한다.

금지:
- 여러 pack을 화면별로 무계획 혼합
- 전체 png를 늘여 테두리 왜곡
- license 기록을 나중으로 미루기
- 임시 검정 패널을 production으로 굳히기

## 7. Font import

폰트는 장식이 아니라 가독성 시스템이다.

- 한국어 glyph coverage 확인
- small size readability 확인
- weight별 실제 필요한 파일만 사용
- OFL 등 고지 조건 유지
- Minecraft font provider 호환 확인
- 폰트 파일 자체를 외부 전달물로 따로 배포하지 않음

Pretendard는 현재 후보이며 실제 import는 runtime 검증 후 진행한다.

## 8. Character model → portrait

portrait를 별도 그림으로 다시 그리는 것보다 최종 3D character source와 연결한다.

우선:
1. 동일 rig/model을 고정 camera/light로 offscreen render
2. head/upper-body framing preset
3. 캐릭터별 pose override가 필요한 경우 metadata
4. 결과를 cache/atlas로 사용

외부 모델 라이선스가 파생 render를 허용하는지 확인한다.

자동 crop이 얼굴/머리를 자르거나 캐릭터마다 scale이 흔들리면 production 사용 금지.

## 9. 3D summon presentation

- 소환 결과는 서버에서 먼저 확정
- presentation scene은 결과를 바꾸지 않음
- 실제 character model/animation 재사용
- 별도 duplicate model을 만들지 않음
- rarity별 camera/light/VFX preset 사용
- SKIP 가능
- 10회 결과 summary는 2D여도 됨

## 10. Resource override

TURNBOUND 자체 resource가 기본 skin을 제공하되 외부 resource pack으로 시각 요소를 override할 수 있는 구조를 우선한다.

Java 코드에서 색/texture path를 화면마다 직접 복제하지 않는다.
shared theme/token/renderer에서 읽는다.

## 11. 완료 조건

외부 디자인을 “가져왔다”의 기준:
- 라이선스/출처 기록 완료
- 실제 runtime 사용
- UI나 모델에서 임시 placeholder 제거
- scale/9-slice/font 깨짐 없음
- source asset과 transformed asset의 관계 추적 가능
- unused raw asset이 JAR에 쌓이지 않음
