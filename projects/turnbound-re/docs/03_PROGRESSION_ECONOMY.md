# 03 — PROGRESSION & ECONOMY

## 1. 별 체계
`originStar`와 `currentStar`를 분리한다.
- originStar: 1~5, 획득 시 결정, 영구 불변.
- currentStar: originStar 이상 6 이하, 승급으로 증가.

레벨 상한:
| currentStar | maxLevel |
|---:|---:|
| 1 | 20 |
| 2 | 30 |
| 3 | 40 |
| 4 | 50 |
| 5 | 60 |
| 6 | 70 |

승급은 레벨 상한과 명시된 ascension bonus/node를 연다. origin별 성장률을 뒤집는 숨은 보정은 없다.

## 2. 스탯 성장
```text
stat(level, currentStar)
 = floor(baseStat + growthPerLevel * (level - 1) + ascensionFlat[currentStar])
```
모든 base/growth/ascensionFlat은 CharacterDefinition에 보이게 기록한다. HP/ATK/DEF/SPD/POISE가 기본 전투 스탯이다.

## 3. 저태생/고태생 관계
고태생을 키우는 맛을 보존하기 위해 저태생의 level당 growth를 의도적으로 더 크게 주어 역전시키지 않는다.

저태생의 가치 수단:
- 낮은 squad cost.
- 특정 tag/Poise/상태에 특화된 고효율 도구.
- 낮은 육성 비용.
- 좁지만 강한 조합 시너지.
- 특정 지역/보스 기믹 대응.

고태생의 가치 수단:
- 높은 평균 스탯 예산.
- 더 완성된 kit.
- 범용성 또는 강력한 specialization.
- Burst/Passive의 높은 ceiling.

## 4. Squad Cost
파티 슬롯은 4지만 편성에는 비용 상한이 있다.
- 기본 cost는 `originStar` 기준 1/2/3/4/5.
- 승급으로 cost가 오르지 않는다.
- 초기 tuning target party capacity는 12. 진행에 따라 확장 가능하지만 4명의 ★5를 아무 제약 없이 넣는 구조는 초기 목표가 아니다.
- 정확한 capacity 성장곡선은 playtest tuning data로 관리하며 CANON이 아니다.

## 5. 성장 자원
초기 재화는 최소화한다.
- `Coin`: 상점/일반 서비스/장비 강화.
- `Essence`: 캐릭터 level/승급 공용 성장.
- `Character Shard`: 캐릭터 해금 및 고유 승급 요구량에 사용 가능.
- Minecraft 재료: 제작/장비/음식.
별도 talent currency는 초기에는 만들지 않는다.

## 6. 획득
현금 가챠를 전제하지 않는다. 캐릭터 획득은 Encounter shard, 보스, 퀘스트, 상점/교환, 탐험 보상 등 데이터 소스로 지정한다.

## 7. 승급 비용 설계
정확한 수치는 data table로 두며 다음 단조성만 보장한다.
- 별이 높을수록 Essence/Shard 요구량 증가.
- 저태생은 동일 currentStar에 도달하는 총비용이 고태생보다 낮거나 같아야 한다.
- ★6은 모든 origin에 열려 있지만 origin의 kit budget 차이는 유지한다.

## 8. 장비
초기 Vertical Slice는 최소 장비 슬롯만 사용해 전투 코어를 검증한다. 장비가 캐릭터 정체성을 덮지 않게 총 전투력 중 장비 기여도를 측정한다. 장비 강화는 Coin + Minecraft 소재 중심으로 단순화한다.

## 9. 경제 검증
- 시간당 Coin/Essence/Shard 기대값 기록.
- 주요 승급까지 필요한 Encounter 수 기록.
- 한 활동만 반복하는 것이 모든 자원의 최적해가 되지 않는지 확인.
- 보상량을 낮추기 위해 전투 시간을 불필요하게 늘리지 않는다.
