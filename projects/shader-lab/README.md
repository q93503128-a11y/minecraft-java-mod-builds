# Shader Lab Dreamscape 0.4.0-alpha.4

Minecraft Java 26.2 + NeoForge용 비공개 고품질 쉐이더 테스트다.

0.3의 화면 공간 후처리는 깊이와 색상만으로 물·오로라를 추정해 나뭇잎과 지형을 청록색
띠로 오인했다. 이 구조는 폐기했으며 0.4부터는 Iris의 실제 월드 렌더링 파이프라인을
사용한다.

## 기반과 라이선스

- 기반 렌더러: Photon Shader by SixthSurge
- 고정 원본 커밋: `15458c0937f8647c37eb6a501bef5eb3bf3da31b`
- Photon 라이선스: MIT
- Photon의 `LICENSE`와 Shader Lab 변경 내역을 완성된 쉐이더팩 안에 그대로 보존한다.
- Complementary 등 재배포·수정 조건이 다른 쉐이더 코드는 복사하지 않는다.

## 0.4 렌더링 구조

화면에 색 띠를 덧씌우는 방식이 아니다.

- `gbuffers_water`: 실제 물·반투명 지형만 반사, 굴절, 파랑 변위, 파라랙스, 카우스틱 처리
- `gbuffers_terrain`: 지형 재질, 부드러운 그림자, GTAO, 방향광 및 간접광 처리
- 실제 하늘 패스: 밤 오로라, 대기, 구름, 별빛 처리
- 볼류메트릭 안개 패스: 월드 좌표와 거리로 낮은 지표 안개 처리
- 후처리: TAA + FXAA + CAS 선명화, 억제된 블룸

따라서 물이 없는 나무나 풀밭에 물 효과가 생기지 않으며, 오로라가 화면 중앙에 고정된
청록 띠로 나타나지 않는다.

## Dreamscape 기본 프리셋

- 오로라: 밤마다 활성화, 밝기 1.20
- 물: 환경 반사, 하늘 반사, 화면 공간 반사, 굴절, 파라랙스, 변위, 카우스틱 활성화
- 안개: Photon 볼류메트릭 안개 1.28 + 해수면 부근의 낮은 월드 공간 안개
- 선명도: TAA, FXAA, CAS 0.70
- 블룸: 강도 0.32, 확산 0.62
- 비활성화: 피사계 심도, 모션 블러

## 설치 방식

Shader Lab JAR에는 수정된 Photon 기반 `ShaderLab-Dreamscape-0.4.zip`이 포함된다.
실행 시 다음을 자동으로 수행한다.

1. 게임 폴더의 `shaderpacks`에 쉐이더팩 설치
2. 기존 `config/iris.properties`를 한 번 백업
3. Dreamscape 쉐이더팩 선택 및 쉐이더 활성화

실제 렌더링에는 Minecraft 26.2용 Iris 1.11.2 NeoForge와 Sodium 0.9.1 NeoForge가
필요하다. CI는 Shader Lab, Iris, Sodium 세 JAR가 들어 있는 테스트 키트도 함께 만든다.
Iris가 첫 실행에서 설정을 이미 읽은 경우 한 번만 재시작한다.

## 검증

CI에서 다음을 모두 통과해야 산출물을 만든다.

- Java 25 + NeoForge 26.2.0.40-beta clean build
- 고정 Photon 커밋 다운로드 및 MIT 라이선스 보존
- 실제 물·지형·하늘 프로그램 포함 여부
- 물 기능, 오로라, TAA/FXAA/CAS 설정
- 월드 공간 저층 안개 패치
- 구형 `lush_grade` 화면 후처리 완전 제거
- 중첩 쉐이더팩 ZIP과 최종 JAR 무결성
- 공식 Modrinth 버전 ID로 Iris와 Sodium 테스트 의존성 확보

## 테스트 관찰 지점

- 낮의 강과 바다: 표면 반사, 굴절, 파랑과 카우스틱이 즉시 보여야 한다.
- 해수면 부근 평야와 숲: 멀리 갈수록 낮은 안개층이 보이되 화면 전체가 흐려지면 안 된다.
- 밤: 오로라는 실제 하늘에서만 움직이고 지형과 나뭇잎 위에 고정 띠로 붙으면 안 된다.
- 블록: 바닐라보다 부드러운 그림자, AO, 간접광과 시간적 안티앨리어싱이 보여야 한다.

## 로컬 빌드

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.4.0-alpha.4.jar
```

빌드 시 고정된 Photon 원본 커밋을 다운로드하므로 인터넷 연결이 필요하다.
