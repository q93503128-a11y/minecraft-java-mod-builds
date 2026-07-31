# Shader Lab Reverie 0.8.0-alpha.9

Minecraft Java 26.2 + NeoForge용 단일 JAR 클라이언트 쉐이더 모드다.

## 이번 버전

- Iris가 `RENDER_SCALE`을 숫자로 읽지 못하던 compute directive 전부 숫자 상수로 정규화
- 무거운 현실형 볼류메트릭 구름과 반사 compute 비활성화
- 청보라 그라데이션, 석양 분홍빛, 느리게 흐르는 빛의 띠로 구성된 저비용 몽환 하늘
- 평상시에도 확실히 보이는 레이마칭 저층 청보라 안개
- 눈밭 과노출을 줄이기 위해 햇빛, 자동 노출, LUT, 블룸과 글레어 조정
- 그림자 맵 1024, 거리 64, 샘플 4 및 물·AO 샘플 축소
- 식물 알파 오류 수정과 SPBR 21 LabPBR 재질 유지
- 구형 Shader Lab ZIP과 0.7 폴더 자동 삭제

## 기반과 라이선스

- Noble Shaders 1.9.6 (`3cIADbit`), GPL-3.0
- SPBR 21 (`S17DzSfS`), GPL-3.0-or-later
- Iris 1.11.2 NeoForge
- Sodium 0.9.1 NeoForge

## 설치

이전 Shader Lab JAR를 삭제하고 `shaderlab-0.8.0-alpha.9.jar` 하나만 `mods` 폴더에 넣는다.
내부 쉐이더팩은 `shaderpacks/ShaderLab-Reverie-0.8` 폴더로 설치되며 별도 ZIP을 콘텐츠 목록에 유지하지 않는다.

## 검증

- Java 25 + NeoForge 26.2.0.40-beta clean build
- Noble 1.9.6 안개 선언 형식 차이를 정규화한 뒤 동일 소스 검증
- 식물 알파 `discard` 패치
- 모든 Iris compute work-group 숫자 상수화
- 현실형 구름·반사 compute 비활성화
- 몽환 하늘과 저층 안개 소스 검사
- SPBR LabPBR 맵, Iris, 단일 계층 Sodium, 최종 JAR 무결성 검사
