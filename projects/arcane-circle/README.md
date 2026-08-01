# Arcane Circle Lab 0.1.0-alpha.1

Minecraft Java 26.2 + NeoForge용 첫 조합 마법진 테스트다.

## 바로 테스트

처음 월드에 들어가면 조합 마법진 3개와 모든 촉매를 한 번 지급한다.

1. 마법진을 바닥에 설치한다.
2. 아래 조합의 촉매를 순서대로 우클릭한다.
3. 빈손 우클릭으로 주문을 발동한다.
4. 잘못 넣었으면 웅크리기+빈손 우클릭으로 초기화하고 촉매를 돌려받는다.

## 주문 조합

- 불꽃 신성: 블레이즈 가루 → 레드스톤 → 화약
- 서리 봉인: 눈덩이 → 석영 → 설탕
- 비전 파동: 엔더 진주 → 자수정 조각 → 발광석 가루

불꽃 신성은 주변 몹을 점화하고, 서리 봉인은 주변 몹을 얼리며, 비전 파동은 주변 몹을 바깥으로 밀어낸다.
발동 때 바닥에 입자 원과 오망성 선이 나타난다.

## 제작법

```text
 자수정
레드스톤 우는 흑요석 레드스톤
 자수정
```

## 로컬 빌드

```bash
chmod +x gradlew
./gradlew --no-daemon --no-configuration-cache clean build
python3 tools/verify_jar.py build/libs/arcanecircle-0.1.0-alpha.1.jar
```
