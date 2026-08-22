# Survival Ascension

- Mod version: `0.5.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge 26.2.0.38-beta`
- Final JAR: `survivalascension-0.5.0-alpha.1.jar`
- Existing-world compatibility: 기존 `mining_progress_v1` SavedData와 0.1~0.4 공용 스킬 XP 맵을 그대로 이어간다.

## 정체성

바닐라 서바이벌에서 수치만 조금 강해지는 대신, 숙련이 오를수록 한 번의 행동이 처리하는 물리적 범위와 영향력이 커지는 성장 모드다.

## 현재 활성 숙련

### 채굴
- 일반 지형: Lv.10 3×3 / Lv.30 5×5 / Lv.60 7×7 / Lv.90 9×9
- 가치 광석: Lv.30/60/90에 연결 광맥 최대 24/64/128개
- 웅크리기 정밀 1×1

### 벌목
- Lv.10/30/60/90 연결 통나무 16/48/128/256개
- 웅크리기 단일 통나무

### 농사
- 성숙 작물만 XP
- Lv.10/30/60/90 광역 수확 3×3/5×5/7×7/9×9

### 전투
- 플레이어가 직접 처치한 생물로 전투 XP 획득. 적대몹 XP 가중치가 높고 비적대 생물은 낮다.
- 전투 레벨에 따라 플레이어가 가하는 피해가 완만하게 증가하며 Lv.100에서 약 1.8×.
- 직접 근접공격은 Lv.30/60/90에 파급 공격을 해금한다.
- 파급 대상 수: 2 / 4 / 8체, 반경: 1.75 / 2.75 / 4.0블록.
- 파급은 주변 `Enemy` 대상에만 적용해 주민·동물에 무차별 전파하지 않는다.
- 투사체는 피해 성장만 적용되고 근접 파급은 발생하지 않는다.

## 공용 성장/UI

- 채굴·벌목·농사·전투가 활성. 건축·기동은 다음 단계 예약.
- K 숙련 화면에서 6개 슬롯과 XP/레벨/현재 효과를 확인.
- `/ascension stats`
- `/ascension mining|woodcutting|harvesting|combat setlevel <0..100>`

## 외부 코드 정책

Skill Proficiencies와 Veinminer++의 MIT 허용 부분만 고지와 함께 포팅한다. Project MMO 2.0 같은 제한/ARR 소스는 기능 참고만 하며 코드·리소스·에셋을 복제하지 않는다.
