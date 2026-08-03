# Village Guardians 단축키 감사

## 실제 등록 및 전송

| 필드 | 번역 ID | 기본키 | 서버 액션 |
|---|---|---:|---|
| `ROLE_SKILL_ONE` | `key.villageguardians.role_skill_one` | `Z` | `use_skill:0` |
| `ROLE_SKILL_TWO` | `key.villageguardians.role_skill_two` | `X` | `use_skill:1` |
| `QUICK_COMMUNICATION` | `key.villageguardians.quick_communication` | `B` | `open_quick_chat` |
| `STATUS` | `key.villageguardians.status` | `H` | `open_status` |
| `GROWTH` | `key.villageguardians.personal_progress` | `J` | `open_skill_tree` |
| `ROLE_PROGRESS` | `key.villageguardians.role_progress` | `K` | `open_role_progress_current` |
| `CALLER` | `key.villageguardians.caller` | `U` | `open_quick_chat` |

## 언어 파일

```json
{
  "villageguardians.title": "마을 지키기",
  "villageguardians.role.vanguard": "선봉검사",
  "villageguardians.role.ranger": "성루사수",
  "villageguardians.role.arcanist": "비전술사",
  "villageguardians.role.luminar": "성휘사제",
  "villageguardians.role.warden": "철벽수호자",
  "key.category.villageguardians.controls": "마을 지키기",
  "key.villageguardians.role_skill_one": "장착 기술 1 사용",
  "key.villageguardians.role_skill_two": "장착 기술 2 사용",
  "key.villageguardians.quick_communication": "수호단 빠른 통신",
  "key.villageguardians.status": "수호자 상태 열기",
  "key.villageguardians.personal_progress": "성장 열기",
  "key.villageguardians.role_progress": "직업 성장 열기",
  "key.villageguardians.caller": "빠른 통신 열기",
  "villageguardians.time.morning": "아침",
  "villageguardians.time.day": "낮",
  "villageguardians.time.evening": "저녁",
  "villageguardians.time.night": "밤"
}
```

## 코드 내 키/설명 참조

### `src/main/java/kr/moonseungjun/villageguardians/VillageClientKeys.java`

- L20: `private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z);`
- L21: `private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X);`
- L22: `private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_B);`
- L23: `private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_H);`
- L24: `private static final KeyMapping GROWTH = key("personal_progress", GLFW.GLFW_KEY_J);`
- L25: `private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K);`
- L26: `private static final KeyMapping CALLER = key("caller", GLFW.GLFW_KEY_U);`
- L33: `return new KeyMapping("key.villageguardians." + id, InputConstants.Type.KEYSYM, key, CATEGORY);`
- L80: `boolean oldPair = (first == GLFW.GLFW_KEY_R && second == GLFW.GLFW_KEY_G)`
- L81: `|| (first == GLFW.GLFW_KEY_G && second == GLFW.GLFW_KEY_R);`
- L83: `ROLE_SKILL_ONE.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z));`
- L84: `ROLE_SKILL_TWO.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X));`

### `src/main/java/kr/moonseungjun/villageguardians/VillageFacilityScreen.java`

- L87: `case "skill_test_skill" -> "청금석 관리함 · Z/X 기술 장착";`

### `src/main/java/kr/moonseungjun/villageguardians/VillageInventoryPanel.java`

- L73: `graphics.text(minecraft.font, fit(minecraft, "B 통신 · Z/X 기술", layout.width() - 18),`
- L80: `layout.left() + 9, firstY, buttonWidth, "상태 H", ACCENT);`
- L82: `layout.left() + 9 + buttonWidth + gap, firstY, buttonWidth, "성장 J", GOLD);`
- L84: `layout.left() + 9, firstY + 21, buttonWidth, "직업 K", ACCENT);`
- L86: `layout.left() + 9 + buttonWidth + gap, firstY + 21, buttonWidth, "통신 U", GOLD);`

### `src/main/java/kr/moonseungjun/villageguardians/VillageQuickChatScreen.java`

- L47: `graphics.text(font, fit("B/U 빠른 통신 · 선택 즉시 전송 · ESC 닫기", Math.max(1, width - 20)),`

### `src/main/java/kr/moonseungjun/villageguardians/VillageSkillTestSystem.java`

- L87: `+ "\n금색 바닥 관리함은 직업, 청금석 바닥 관리함은 Z/X 기술을 담당합니다."`

### `src/main/java/kr/moonseungjun/villageguardians/VillageStarterKit.java`

- L46: `"§6[수호단 조작] §f빠른 통신은 인벤토리 버튼이나 B/U 키로 엽니다. "`
- L47: `+ "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신"));`
- L50: `"§e기존 호출기 아이템을 제거했습니다. 인벤토리 화면의 빠른 통신 버튼을 사용하세요."));`
- L70: `"§e호출기 아이템은 폐지되었습니다. 인벤토리 화면의 빠른 통신 버튼을 사용하세요."));`

### `src/main/java/kr/moonseungjun/villageguardians/VillageUiController.java`

- L73: `+ "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신";`
- L317: `"open_skill_test_skills", "스킬 관리함 열기|현재 시험 직업의 Z/X 기술 장착 화면",`

### `src/main/java/kr/moonseungjun/villageguardians/VillageUiService.java`

- L307: `List.of("open_quick_chat"), List.of("빠른 통신|수호단 신호 전송"));`

### `src/main/resources/assets/villageguardians/lang/ko_kr.json`

- L9: `"key.villageguardians.role_skill_one": "장착 기술 1 사용",`
- L10: `"key.villageguardians.role_skill_two": "장착 기술 2 사용",`
- L11: `"key.villageguardians.quick_communication": "수호단 빠른 통신",`
- L12: `"key.villageguardians.status": "수호자 상태 열기",`
- L13: `"key.villageguardians.personal_progress": "성장 열기",`
- L14: `"key.villageguardians.role_progress": "직업 성장 열기",`
- L15: `"key.villageguardians.caller": "빠른 통신 열기",`

