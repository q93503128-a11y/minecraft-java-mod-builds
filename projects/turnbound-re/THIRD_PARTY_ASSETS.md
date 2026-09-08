# THIRD PARTY / REFERENCE REGISTER

외부 코드·맵·모델·텍스처·UI 키트·사운드·폰트를 실제 프로젝트에 넣기 전 반드시 이 문서를 갱신한다.

| ID | 종류 | 출처 | 라이선스 | 현재 사용 | 허용 범위 |
|---|---|---|---|---|---|
| REF-CODE-001 | 구조 참고 | Stephen-Seo/TurnBasedMinecraftMod, `neoforge` branch | MIT | 코드 복사 없음 | Battle/Combatant/manager/network 분리 및 Minecraft 턴제 구현 사례 조사만 |
| REF-UI-001 | UI 자산 후보 / 1순위 | Kenney, `UI Pack - Pixel Adventure` — https://kenney.nl/assets/ui-pack-pixel-adventure | CC0 1.0 | 파일 반입 전 | M5 공통 panel/button/frame/bar/slot의 최종 skin 후보. thin/thick outline과 별도 PNG/tilesheet를 필요한 부분만 선별 사용하고 TURNBOUND: RE layout에 맞게 crop/9-slice/색 조정 가능 |
| REF-UI-002 | UI 자산 후보 / 비교 | tiopalada, `Tiny RPG - Dragon Regalia GUI` — https://tiopalada.itch.io/tiny-rpg-dragon-regalia-gui | CC0 1.0 | 파일 반입 전 | 9-slice frame, rest/hover/click/disabled 상태, target cursor, meter 구조 참고 및 보조 후보. 원본의 강한 JRPG 색/장식은 TURNBOUND: RE 전체 skin으로 그대로 혼합하지 않음 |
| REF-UI-003 | UI 자산 후보 / 입력 glyph | Kenney `Input Prompts Pixel 16×` — Kenney Game Assets preview/catalog | CC0 1.0 | 파일 반입 전 | 키보드/패드 입력 glyph 후보. 실제 파일 반입 전 개별 pack의 공식 배포 페이지와 CC0 표시를 다시 고정 확인 |

## M5 현재 선택

- **Primary skin candidate:** `REF-UI-001` Kenney UI Pack - Pixel Adventure.
- 이유: Minecraft와 충돌이 적은 픽셀 해상도, 500+ 분리 sprite, thin/thick outline, panel/button/bar 계열을 한 family에서 공급하며 CC0라 수정/재배포 제약이 가장 낮다.
- `REF-UI-002`는 상태별 frame/9-slice/target cursor의 구조가 매우 좋지만, 원본의 분홍/주황/청색 JRPG visual identity를 그대로 섞으면 화면별 언어가 갈라질 위험이 있어 보조 레퍼런스로 제한한다.
- 실제 PNG가 저장소에 들어오기 전까지 `현재 사용`은 **파일 반입 전**이다. 코드가 지금 사용하는 vanilla advancement/boss-bar sprite는 구조 검증 bridge이며 최종 skin으로 확정하지 않는다.

## 규칙
- `현재 사용=없음` 또는 `파일 반입 전`은 실제 외부 파일이 저장소에 들어오지 않았다는 뜻이다.
- 코드/리소스를 가져오면 원본 URL, commit/tag 또는 배포 버전, 파일 경로, 라이선스, 수정 내용을 기록한다.
- 라이선스 파일/고지 의무가 있으면 배포 형태와 무관하게 보존한다.
- 출처가 불명확한 리소스는 production에 넣지 않는다.
- 개인 테스트 목적이라도 정본 저장소에는 출처 불명 파일을 넣지 않는다.
- 여러 UI pack을 한 화면에 무분별하게 혼합하지 않는다. 하나의 primary sprite family를 정하고 부족한 기능만 동일 화풍으로 보완한다.
