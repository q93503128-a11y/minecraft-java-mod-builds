# 마력 각인석 시각 자산 계약

마력 각인석은 Minecraft 바닐라 아이템 텍스처를 재사용하지 않는다.

현재 모드팩 정본에서는 The Birth of Steve(TBOS) 원본 JAR이 제공하는 다음 런타임 텍스처를 복합 레이어로 참조한다.

- `tbos:item/curator_core`
- `tbos:item/cantor_sigil`

Survival Ascension JAR에는 TBOS PNG, 모델, 소리 또는 기타 시각 자산을 복사하거나 재배포하지 않는다. 아이템 모델 JSON은 로드된 원본 TBOS 리소스 위치만 참조한다. TBOS 자산은 All Rights Reserved이며 원본 Modrinth 파일을 콘텐츠팩 의존성으로 유지한다.

이 조합을 선택한 이유는 마력 각인석의 주요 획득처가 Fractured Archive 후반부이고, Curator Core + Cantor Sigil의 조합이 해당 던전의 시각 언어를 그대로 이어주기 때문이다. 별도의 바닐라 자수정/메아리 조각 아이콘 재탕은 금지한다.

현재 마력 각인석의 획득 루프가 TBOS Fractured Archive와 결합되어 있으므로 이 외형은 콘텐츠팩 환경을 기준으로 한다. 향후 TBOS 없이도 독립 획득처를 추가한다면 그 시점에 Survival Ascension 소유의 독립 원본 텍스처를 추가하여 시각 fallback을 제공한다.
