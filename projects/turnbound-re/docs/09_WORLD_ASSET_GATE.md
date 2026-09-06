# 09 — WORLD & ASSET GATE

## 원칙
'판타지 마을', 'RPG 던전' 같은 단어만으로 월드를 직접 디자인하지 않는다. 기능 구조는 먼저 만들 수 있지만 production 외형은 레퍼런스와 자산 검증 뒤 확정한다.

## 1. 기능 선행
미술 전에도 아래는 개발 가능하다.
- region graph.
- Encounter anchor.
- resource node.
- fast travel.
- quest/story trigger.
- dungeon/boss gate.
- debug arena.
기능 테스트 맵은 평면/단순 블록이어도 된다.

## 2. Production World Gate
지역별로 다음을 준비한다.
1. gameplay 목적.
2. 플레이어 이동/시야/전투/채집 동선.
3. 외부 건축/게임/환경 레퍼런스.
4. block palette 후보와 출처.
5. 규모 기준(거리, 건물 크기, 랜드마크 빈도).
6. 실제 Minecraft prototype.
7. screenshot 비교.
8. 성능/충돌/길찾기 검증.

## 3. 외부 맵/건축 사용
- 다운로드/복제 가능 여부와 라이선스 확인.
- 제작자/원본 URL/버전 기록.
- 수정본도 출처를 유지.
- 정본 저장소에 넣기 전 `THIRD_PARTY_ASSETS.md` 갱신.
- 외부 asset이 게임 버전과 충돌할 경우 변환 절차를 문서화.

## 4. 캐릭터/모델/VFX
바닐라 Entity를 그대로 쓰는 캐릭터도 고유성 필요 여부를 캐릭터별로 판단한다. 모델/애니메이션/VFX를 새로 만들 때 AI 즉흥 시안 하나를 정답으로 삼지 않는다. 외부 레퍼런스를 고른 뒤 구현 가능한 Minecraft 표현으로 축약한다.

## 5. 성능
production asset은 시각 품질뿐 아니라 chunk load, entity count, particle count, translucent overdraw, animation cost를 실제 client profiler로 검증한다.
