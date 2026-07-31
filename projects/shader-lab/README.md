# Shader Lab Reverie 0.7.0-alpha.8

Minecraft Java 26.2 + NeoForge용 단일 JAR 클라이언트 쉐이더 모드다.

## 이번 버전

- 식물과 꽃을 검은 사각형으로 만들던 Noble G-buffer 알파 처리 오류 수정
- 지형 POM 비활성화 및 SPBR 21 LabPBR 재질을 JAR 리소스로 직접 포함
- 그림자 맵 2048, 거리 128, 샘플 6으로 GTX 1660 SUPER 기준 최적화
- 반사·굴절·구름·수면 샘플 수를 줄이되 물과 하늘의 핵심 품질 유지
- 평상시에도 보이는 해수면 부근의 낮은 청보라 볼류메트릭 안개
- Valley Mist LUT, 약한 블룸과 글레어, 전 화면 DOF·비네트·필름 그레인 비활성화
- 기존 청록색 화면 오버레이는 포함하지 않음

## 렌더링 기반과 라이선스

- Noble Shaders 1.9.6 (`3cIADbit`), GPL-3.0
- SPBR 21 (`S17DzSfS`), GPL-3.0-or-later
- Iris 1.11.2 NeoForge
- Sodium 0.9.1 NeoForge

원본 Modrinth 파일의 SHA-512를 검증하고 Noble 파생 소스, GPLv3 원문, SPBR 출처와 변경 기록을 JAR 안에 보존한다.

## 설치

이전 Shader Lab JAR를 삭제하고 `shaderlab-0.7.0-alpha.8.jar` 하나만 `mods` 폴더에 넣는다.

내부 쉐이더팩은 실행 시 `shaderpacks/ShaderLab-Reverie-0.7` 폴더로 설치된다. 이전 `ShaderLab-Reverie-0.6.zip`과 구형 Dreamscape 파일은 자동 삭제하므로 Modrinth 콘텐츠 목록에 별도의 Shader Lab ZIP을 유지하지 않는다.

## 검증

- Java 25 + NeoForge 26.2.0.40-beta clean build
- 식물 알파 `discard` 패치 검사
- GTX 1660 SUPER 균형 프리셋 검사
- Noble 저층 안개 소스 패치 검사
- SPBR LabPBR 맵 200개 이상 검사
- Iris 및 단일 계층 Sodium JAR 검사
- 최종 JAR와 중첩 쉐이더팩 무결성 검사
