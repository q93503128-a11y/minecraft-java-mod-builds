# Local Asset Overlay Contract

## 목적

Living Kingdoms는 개인 플레이 환경에서 사용자가 보유한 모델, 스킨, 음원, 셰이더와 기타 리소스를 연결할 수 있다. 그러나 이 저장소는 공개 저장소이므로 재배포 권한이 없는 파일 자체는 커밋하지 않는다.

## 디렉터리 계약

개발·실행 환경에서 다음 폴더를 사용할 수 있다.

```text
projects/living-kingdoms/local-assets/
├─ characters/
├─ creatures/
├─ architecture/
├─ ui/
├─ audio/
├─ shaders/
└─ manifests/
```

`local-assets/` 전체는 Git에서 제외한다. 모드는 나중에 각 리소스를 직접 하드코딩하지 않고 manifest를 통해 논리 ID에 연결한다.

예시 논리 ID:

```text
livingkingdoms:character/erden_guard_male_01
livingkingdoms:character/silvana_ranger_female_02
livingkingdoms:creature/ruin_hound_01
livingkingdoms:architecture/erden_timber_house_a
livingkingdoms:ui/origin_frame_royal
livingkingdoms:audio/velden_market_day
livingkingdoms:shader/fantasy_warm_low
```

## 에셋 계층

1. **코어 대체재**: 저장소에 포함 가능한 간단한 합법 리소스. 외부 파일이 없어도 게임이 깨지지 않게 한다.
2. **허용 공개 에셋**: CC0, CC-BY, MIT 등 실제 재배포 조건을 확인한 뒤 출처와 라이선스를 기록해 포함한다.
3. **개인용 로컬 오버레이**: 사용자가 직접 가진 파일을 `local-assets/`에 넣어 사용한다. 저장소와 빌드 artifact에는 포함하지 않는다.
4. **선택 외부 모드**: 셰이더 로더, 애니메이션 보조, 성능 모드처럼 사용자가 별도로 설치한다.

## 캐릭터 대량 추가 방식

캐릭터를 Java 코드에 하나씩 박지 않는다. 다음 데이터로 구성한다.

- 논리 캐릭터 ID
- 표시 이름과 지역별 이름 변형
- 종족, 성별 표현, 연령대, 체형
- 소속 세력과 직업
- 얼굴·머리·복장·장비 슬롯
- 모델 또는 스킨 리소스 ID
- 애니메이션 세트
- 음성 또는 대사 스타일
- 성격, 관계와 기억 데이터
- 낮·밤·전쟁·축제 일정

같은 기본 모델을 사용해도 얼굴, 머리, 의복, 장비, 색조와 행동을 조합해 반복감을 줄인다. 주요 인물은 고유 모델과 애니메이션을 사용할 수 있다.

## 셰이더 원칙

셰이더는 모드 JAR에 무단 번들하지 않는다. 대신 세 가지 시각 계층을 지원한다.

- 기본: 셰이더 없이도 지역별 안개, 하늘, 조명, 파티클과 텍스처로 판타지 분위기 유지
- 경량: 낮은 사양용 그림자·색보정·물 반사
- 고급: 볼류메트릭 조명, 구름, 물, 재질 표현과 지역별 컬러 그레이딩

셰이더가 없을 때 건축과 UI가 조악해지는 구조는 금지한다.

## manifest 최소 형식

```json
{
  "schema": 1,
  "packId": "personal-fantasy-overlay",
  "assets": [
    {
      "id": "livingkingdoms:character/erden_guard_male_01",
      "type": "character_model",
      "path": "characters/erden_guard_male_01",
      "fallback": "livingkingdoms:character/erden_guard_fallback"
    }
  ]
}
```

잘못되거나 없는 파일은 게임을 충돌시키지 않고 fallback으로 전환하며 로그에 한 번만 경고한다.
