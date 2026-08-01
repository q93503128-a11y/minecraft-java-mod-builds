# Living Kingdoms 외부 리소스 원장

외부 리소스는 출처와 라이선스가 확인되고, 배포에 적합하며, 빌드가 동일한 파일을 재현할 수 있을 때만 포함한다.

## 1. 직접 포함한 UI 리소스

### 출처

- 제작자/배포명: Kenney
- 라이선스: CC0 1.0
- 미러 저장소: `KooshaPari/Dino`
- 고정 커밋: `6c6d1bf81050a21e659356373f3f39029c8d7a0c`
- 빌드 방식: Gradle이 고정 커밋의 raw 파일을 내려받은 뒤 Git blob SHA-1을 검증한다.
- 검증 실패 시 빌드는 즉시 중단된다.

| 프로젝트 내 파일 | 원본 계열 | Git blob SHA-1 | 사용처 |
|---|---|---:|---|
| `fantasy_panel.png` | fantasy-ui-borders `panel_ornate.png` | `82c2a379b877249029a817e3a0ef36b55cf9a798` | 기록부·선택창 외곽 장식 |
| `panel_background.png` | ui-pack `panel_background.png` | `258d992070f873046c5623fbc694e45a526c4b8a` | 카드·창 내부 배경 |
| `button_normal.png` | ui-pack `button_rectangleFlat.png` | `a9390478c61d3ced14d16a5e2e396c806966989a` | 일반 버튼 |
| `button_pressed.png` | ui-pack `button_rectangleDepressed.png` | `d9492b4e48b28823c81b3bc08a1b31156f68c79c` | 선택·호버 버튼 |
| `hud_panel_brown.png` | ui-pack-adventure `panel_brown.png` | `3e967c361edf770c1a54e0558ff68102ad5e6976` | 생명·화폐·달력 HUD |
| `minimap_ring_brown_detail.png` | ui-pack-adventure minimap ring | `c09659401f7abe506447b56ca56b5137eab9f697` | 나침반 프레임 |
| `hud_button_brown.png` | ui-pack-adventure `button_brown.png` | `73c062e9213a5a10c506e93b71b272c8ea2aec4b` | 9칸 도구띠 |
| `hud_progress_red.png` | ui-pack-adventure `progress_red.png` | `570310fff678b3720e8fe4b23f570894d242a492` | 검증·향후 진행 바 |
| `hud_progress_green.png` | ui-pack-adventure `progress_green.png` | `9d7c584007202d94ddcd4a9fc415ad8ca768e17f` | 검증·향후 진행 바 |
| `minimap_arrow.png` | ui-pack-adventure `minimap_arrow_a.png` | `d3999611a2608d9953db7ebdd1cda9c293c47b09` | 방향 표시 |

## 2. 조사하고 설계 기준으로 사용한 CC0 환경 리소스

다음 자료는 세계 구조와 오브젝트 목록의 기준으로 조사했지만, 현재 JAR에 모델 원본을 직접 포함하지 않았다. Minecraft 블록 구조물 또는 향후 변환 파이프라인으로 재구성한다.

### Quaternius Medieval Village MegaKit

- 라이선스: CC0
- 300개 이상의 모듈식 중세 환경 부품
- 벽, 지붕, 계단, 문, 창문, 시장, 방어 구조 등
- FBX, OBJ, glTF 및 엔진 프로젝트 제공
- 용도: 에르덴의 모듈식 건물 비율, 건물 기능 분류, 거리 정면 구성 참고

### Quaternius Fantasy Props MegaKit

- 라이선스: CC0
- 200개 이상의 중세·판타지 소품
- 가구, 공구, 무기, 책, 약병, 시장 소품 등
- 용도: 공방, 시장, 기록원, 약제원, 병기창의 소품 목록 참고

### Quaternius Stylized Nature MegaKit

- 라이선스: CC0
- 116개 자연 모델
- 용도: 실바나 수림의 수관, 바위, 식생, 숲길 밀도 참고

## 3. 사용하지 않은 리소스

다음 조건 중 하나라도 해당하면 포함하지 않는다.

- 라이선스가 불명확하거나 재배포를 금지함
- 제작자 표기가 사라진 재업로드 파일
- 원본과 해시를 확인할 수 없음
- Minecraft/NeoForge 배포 형태로 변환할 권리가 불확실함
- 과도한 고해상도 텍스처로 성능 예산을 초과함
- 한 리소스팩의 시각 언어와 맞지 않아 조합 시 이질감이 큼

## 4. 외부 리소스 통합 규칙

1. UI, 건축, 자연, 소품마다 한 계열의 시각 문법을 정한다.
2. 외부 자산을 그대로 마구 섞지 않고 크기, 명암, 테두리, 재질 밀도를 통일한다.
3. 출처 URL, 라이선스, 고정 버전 또는 커밋, 파일 해시를 기록한다.
4. 빌드가 외부 네트워크에 의존할 경우 무결성 검사를 반드시 수행한다.
5. 검증된 JAR에는 실제 사용 파일만 포함한다.
6. 모델을 Minecraft 구조물로 변환할 때 원본 모델과 결과 구조의 대응표를 남긴다.
7. 유료·비상업·출처불명 자산은 사용하지 않는다.

## 5. 기술 검증

- `prepareKenneyUiAssets` 작업은 각 파일의 Git blob SHA-1을 계산한다.
- 예상 해시와 다르면 `GradleException`으로 중단한다.
- `processResources`는 검증 작업을 선행한다.
- 최종 JAR 검사에서 `assets/livingkingdoms/textures/gui/kenney/` 아래 파일 존재를 확인한다.
- 클라이언트 연기 검사에서 선택창, 로딩창, 기록부, HUD가 리소스 바인딩 이후 렌더링되는지 확인한다.
