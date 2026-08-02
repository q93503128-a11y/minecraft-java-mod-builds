#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
PROJECT = REPO_ROOT / "projects" / "countryside-days"
TOOLS = PROJECT / "tools"


def replace(path: Path, old: str, new: str, label: str, count: int = -1) -> None:
    text = path.read_text("utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"{label}: target not found in {path}")
    path.write_text(text.replace(old, new, count), "utf-8")


def prepare() -> None:
    subprocess.run([sys.executable, str(TOOLS / "prepare_alpha16_exact.py")], cwd=REPO_ROOT, check=True)

    replace(PROJECT / "gradle.properties", "mod_version=0.1.0-alpha.16", "mod_version=0.1.0-alpha.17", "version")
    replace(PROJECT / "src/main/java/kr/countrysidedays/CountrysideDays.java", 'VERSION = "0.1.0-alpha.16"', 'VERSION = "0.1.0-alpha.17"', "version constant")

    for rel in [
        "src/main/java/kr/countrysidedays/world/StarterHomesteadGenerator.java",
        "src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java",
        "src/main/java/kr/countrysidedays/world/SharedRestaurantBuilder.java",
    ]:
        replace(PROJECT / rel, "private static final int FLAGS = 2 | 16 | 32;", "private static final int FLAGS = Block.UPDATE_ALL | 16 | 32;", "shape update flags")

    public = PROJECT / "src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java"
    replace(public, "private static final int MAP_REVISION = 16;", "private static final int MAP_REVISION = 17;", "public revision")
    replace(public, "is(Blocks.LODESTONE)", "is(Blocks.EMERALD_BLOCK)", "public marker check")
    replace(public, "Blocks.LODESTONE.defaultBlockState()", "Blocks.EMERALD_BLOCK.defaultBlockState()", "public marker state")
    replace(public, "int minX = -68, maxX = 68, minZ = 54, maxZ = 64;", "int minX = -204, maxX = 204, minZ = 54, maxZ = 64;", "river length")
    replace(public,
        """        workshop(level, origin.offset(-27, 0, -43), Workshop.SCHOOL);
        workshop(level, origin.offset(16, 0, -43), Workshop.CLINIC);""",
        """        workshop(level, origin.offset(-27, 0, -43), Workshop.SCHOOL);
        workshop(level, origin.offset(16, 0, -43), Workshop.CLINIC);
        workshop(level, origin.offset(-27, 0, -58), Workshop.LIBRARY);
        workshop(level, origin.offset(16, 0, -58), Workshop.POST_OFFICE);
        workshop(level, origin.offset(-27, 0, 18), Workshop.KITCHEN);
        workshop(level, origin.offset(16, 0, 18), Workshop.FLORIST);""",
        "new workplaces")
    replace(public,
        """        placeDoor(level, base.offset(width / 2, 1, doorZ), front);
    }""",
        """        placeDoor(level, base.offset(width / 2, 1, doorZ), front);
        BlockPos step = base.offset(width / 2, 0, frontSouth ? depth : -1);
        set(level, step, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, front.getOpposite()));
    }""",
        "public entrance stair", 1)
    replace(public,
        """            case FORGE -> {
                set(level, base.offset(2, y, 4), Blocks.BLAST_FURNACE.defaultBlockState());
                set(level, base.offset(4, y, 4), Blocks.ANVIL.defaultBlockState());
                set(level, base.offset(6, y, 4), Blocks.SMITHING_TABLE.defaultBlockState());
            }
        }""",
        """            case FORGE -> {
                set(level, base.offset(2, y, 4), Blocks.BLAST_FURNACE.defaultBlockState());
                set(level, base.offset(4, y, 4), Blocks.ANVIL.defaultBlockState());
                set(level, base.offset(6, y, 4), Blocks.SMITHING_TABLE.defaultBlockState());
            }
            case LIBRARY -> {
                set(level, base.offset(2, y, 2), Blocks.LECTERN.defaultBlockState());
                set(level, base.offset(4, y, 2), Blocks.BOOKSHELF.defaultBlockState());
                set(level, base.offset(5, y, 2), Blocks.BOOKSHELF.defaultBlockState());
                set(level, base.offset(6, y, 2), Blocks.BOOKSHELF.defaultBlockState());
                set(level, base.offset(8, y, 2), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
            }
            case POST_OFFICE -> {
                set(level, base.offset(2, y, 2), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
                set(level, base.offset(4, y, 2), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(6, y, 2), Blocks.CHEST.defaultBlockState());
                set(level, base.offset(8, y, 2), Blocks.LECTERN.defaultBlockState());
            }
            case KITCHEN -> {
                set(level, base.offset(2, y, 2), Blocks.SMOKER.defaultBlockState());
                set(level, base.offset(3, y, 2), Blocks.FURNACE.defaultBlockState());
                set(level, base.offset(5, y, 2), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(7, y, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
            }
            case FLORIST -> {
                set(level, base.offset(2, y, 2), Blocks.COMPOSTER.defaultBlockState());
                set(level, base.offset(4, y, 2), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(6, y, 2), Blocks.FLOWER_POT.defaultBlockState());
                set(level, base.offset(8, y, 2), Blocks.FLOWER_POT.defaultBlockState());
            }
        }""",
        "workplace furnishings")
    replace(public,
        "        path(level, origin, -18, 48, -18, 53);",
        """        path(level, origin, -18, 48, -18, 53);
        path(level, origin, -22, -49, -22, -58);
        path(level, origin, 21, -49, 21, -58);
        path(level, origin, -22, 14, -22, 27);
        path(level, origin, 21, 14, 21, 27);""",
        "workplace paths")
    replace(public,
        """origin.getX() - 70.0, origin.getY() - 3.0, origin.getZ() - 48.0,
                origin.getX() + 71.0, origin.getY() + 15.0, origin.getZ() + 66.0""",
        """origin.getX() - 206.0, origin.getY() - 3.0, origin.getZ() - 60.0,
                origin.getX() + 207.0, origin.getY() + 15.0, origin.getZ() + 66.0""",
        "river debris bounds")
    replace(public, """        FORGE
    }""", """        FORGE,
        LIBRARY,
        POST_OFFICE,
        KITCHEN,
        FLORIST
    }""", "workplace enum")

    fishing = PROJECT / "src/main/java/kr/countrysidedays/gameplay/CountrysideFishingManager.java"
    replace(fishing, "return dx >= -68 && dx <= 68 && dz >= 54 && dz <= 64;", "return dx >= -204 && dx <= 204 && dz >= 54 && dz <= 64;", "fishing river bounds")

    gameplay = PROJECT / "src/main/java/kr/countrysidedays/gameplay/RuralGameplayHandler.java"
    replace(gameplay, "    private static final float RIVER_FISH_CHANCE = 0.45F;\n", "", "remove fish chance")
    replace(gameplay,
        """        if (serverLevel.getRandom().nextFloat() >= RIVER_FISH_CHANCE) return;
        giveOrDrop(player, ModItems.RIVER_FISH.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.river_fish_caught"));""",
        """        event.getDrops().removeIf(stack -> stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.TROPICAL_FISH));
        event.getDrops().add(ModItems.RIVER_FISH.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.river_fish_caught"));""",
        "guaranteed freshwater fish")

    ranch = PROJECT / "src/main/java/kr/countrysidedays/gameplay/RanchLifeManager.java"
    replace(ranch, "adoptAnimal(level, player, ownEstate, adoptionSpecies, event.getPos().above(), event.getItemStack());", "adoptAnimal(level, player, ownEstate, adoptionSpecies, event.getPos().above(), event.getHand());", "adoption hand")
    replace(ranch, "            ItemStack ticket\n    ) {", "            InteractionHand hand\n    ) {", "adoption signature")
    replace(ranch, "        if (!player.getAbilities().instabuild) ticket.shrink(1);", "        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);", "consume ticket")
    replace(ranch, "        recordDailyProduction(data, estate, animals, currentDay);", "        recordDailyProduction(level, data, estate, animals, currentDay);", "worker production call")
    replace(ranch, """    private static void recordDailyProduction(
            CountrysideWorldData data,""", """    private static void recordDailyProduction(
            ServerLevel level,
            CountrysideWorldData data,""", "worker production signature")
    replace(ranch, """        if (currentDay <= 0 || currentDay <= estate.lastRanchProductionDay()) return;
        long chickens""", """        if (currentDay <= 0 || currentDay <= estate.lastRanchProductionDay()) return;
        var worker = EstateWorkerManager.findWorker(level, estate, EstateWorkerManager.RANCH_ROLE).orElse(null);
        if (worker == null || !EstateWorkerManager.isActive(worker, currentDay)) return;
        long chickens""", "worker-only collection")
    replace(ranch, "if (currentDay <= 0 || currentDay % 3 != 0) return;", "if (currentDay <= 0) return;", "daily breeding")
    replace(ranch, "        for (int i = 0; i < adults.size(); i++) {", "        int births = 0;\n        for (int i = 0; i < adults.size(); i++) {", "breeding count", 1)
    replace(ranch, """            level.addFreshEntity(baby);
            break;""", """            level.addFreshEntity(baby);
            if (++births >= 2) break;""", "two births")

    village = PROJECT / "src/main/java/kr/countrysidedays/gameplay/VillageLifeManager.java"
    replace(village, 'new ResidentRole("우편배달부 하람", "courier", -45, -37, 0, -25, -4, -25,', 'new ResidentRole("우편배달부 하람", "courier", -45, -37, 21, -52, -4, -25,', "courier workplace")
    replace(village, 'new ResidentRole("요리사 다온", "cook", 39, 20, -17, 10, -10, 15,', 'new ResidentRole("요리사 다온", "cook", 39, 20, -22, 24, -10, 15,', "cook workplace")
    replace(village, 'new ResidentRole("꽃집 주인 봄이", "florist", -43, 9, 25, 24, 28, 24,', 'new ResidentRole("꽃집 주인 봄이", "florist", -43, 9, 21, 24, 28, 24,', "florist workplace")
    replace(village, 'new ResidentRole("도서관지기 은채", "librarian", 45, -37, -19, -37, -10, -35,', 'new ResidentRole("도서관지기 은채", "librarian", 45, -37, -22, -52, -10, -35,', "library workplace")
    replace(village, """        tickPublicResidents(level, villageOrigin);
        tickEstateWorkers(level, data);""", """        tickPublicResidents(level, villageOrigin);
        tickEstateWorkers(level, data);
        if (level.getGameTime() % 200L == 0L) accelerateEstateCrops(level, data);""", "crop tick")
    crop_method = '''
    private static void accelerateEstateCrops(ServerLevel level, CountrysideWorldData data) {
        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            BlockPos origin = estate.originPos();
            for (int x = PlayerEstateLayout.FARM_MIN_X + 1; x < PlayerEstateLayout.FARM_MAX_X; x++) {
                for (int z = PlayerEstateLayout.FARM_MIN_Z + 1; z < PlayerEstateLayout.FARM_MAX_Z; z++) {
                    BlockPos cropPos = origin.offset(x, 1, z);
                    BlockState crop = level.getBlockState(cropPos);
                    if (!crop.hasProperty(BlockStateProperties.AGE_7)
                            || crop.getValue(BlockStateProperties.AGE_7) >= 7
                            || level.getRandom().nextFloat() >= 0.35F) continue;
                    level.setBlock(cropPos,
                            crop.setValue(BlockStateProperties.AGE_7, crop.getValue(BlockStateProperties.AGE_7) + 1),
                            Block.UPDATE_ALL);
                }
            }
        }
    }

'''
    replace(village, "    public static void onEntityInteract", crop_method + "    public static void onEntityInteract", "crop acceleration method")
    replace(village, """    private static BlockPos publicTarget(BlockPos origin, ResidentRole role, long day, long time) {
        BlockPos base;""", """    private static BlockPos publicTarget(BlockPos origin, ResidentRole role, long day, long time) {
        long personalTime = Math.floorMod(time + Math.floorMod(role.id().hashCode(), 1201) - 600L, 24000L);
        BlockPos base;""", "individual schedule")
    replace(village, "} else if (time < MORNING_START || time >= NIGHT_START) {", "} else if (personalTime < MORNING_START || personalTime >= NIGHT_START) {", "individual night")
    replace(village, "} else if (isLunch(time)) {", "} else if (isLunch(personalTime)) {", "individual lunch")
    replace(village, "} else if (time >= SOCIAL_START) {", "} else if (personalTime >= SOCIAL_START) {", "individual social")
    replace(village, "return activityOffset(base, role.id(), time, radius);", "return activityOffset(base, role.id(), personalTime, radius);", "individual phase")
    replace(village, "villager.setNoAi(stationaryAtTarget);", "villager.setNoAi(false);", "real workplace AI")

    npc = PROJECT / "src/main/java/kr/countrysidedays/gameplay/RuralNpcManager.java"
    replace(npc, """        long time = Math.floorMod(player.level().getOverworldClockTime(), 24000L);
        String phrase = VillageLifeManager.isHoliday(player.level().getOverworldClockTime() / 24000L)
                ? "오늘은 쉬는 날이라 천천히 마을을 둘러보고 있어요."
                : time < 2000L ? "좋은 아침이에요. 오늘 할 일을 서두르지 않아도 괜찮아요."
                : time < 7200L ? "일하는 중이지만 잠깐 이야기는 좋아요."
                : time < 12000L ? "점심을 먹고 오후 일을 준비하고 있어요."
                : "하루가 저물기 전에 이웃들과 안부를 나누는 중이에요.";""", """        long time = Math.floorMod(player.level().getOverworldClockTime(), 24000L);
        boolean holiday = VillageLifeManager.isHoliday(player.level().getOverworldClockTime() / 24000L);
        String phrase = residentDialogue(name, time, holiday);""", "individual dialogue call")
    dialogue = '''
    private static String residentDialogue(String name, long time, boolean holiday) {
        if (holiday) return switch (name) {
            case RESIDENT_NAME -> "오늘은 장독대도 쉬는 날이야. 강가 바람이나 쐬고 오렴.";
            case FARMER_NAME -> "밭도 하루쯤 쉬어야 흙이 숨을 쉬지. 씨앗은 내일 다시 볼 거야.";
            case RANCHER_NAME -> "동물들 먹이와 물만 확인하고 오늘은 천천히 쉬는 날이에요.";
            case HALL_KEEPER_NAME -> "회관 행사는 없어요. 오늘은 마을 사람들 안부를 듣는 날이죠.";
            case "제빵사 미나" -> "오븐은 껐지만 남은 빵 냄새는 아직 따뜻해요.";
            case "목수 우진" -> "도구를 갈아 두고 쉬는 중이에요. 내일은 문짝을 손볼 거예요.";
            case "우편배달부 하람" -> "오늘 배달은 없어요. 대신 밀린 편지를 정리하고 있어요.";
            case "어부 세진" -> "낚싯줄을 말리는 날이에요. 강물이 아주 잔잔하네요.";
            case "정원사 나래" -> "꽃은 쉬는 날에도 자라요. 물만 주고 산책 중이에요.";
            case "재봉사 유리" -> "바늘을 내려놓고 천 색을 고르는 중이에요.";
            case "선생님 지호" -> "수업은 없지만 내일 쓸 문제를 천천히 고르고 있어요.";
            case "요리사 다온" -> "불은 껐어요. 오늘은 다른 집 밥을 먹어 볼 생각이에요.";
            case "의원 수현" -> "응급 환자만 보고 있어요. 무리하지 말고 쉬세요.";
            case "꽃집 주인 봄이" -> "가게는 닫았지만 시든 꽃은 정리해야 해요.";
            case "대장장이 건우" -> "화덕을 식히는 날이에요. 망치 소리도 오늘은 쉽니다.";
            case "도서관지기 은채" -> "반납된 책을 정리하고 조용히 읽는 날이에요.";
            case "과수원지기 연우" -> "나무 상태만 살피고 열매는 내일 따려고요.";
            case "양봉가 초롱" -> "벌통은 조용히 두는 게 좋아요. 오늘은 멀리서만 보고 있어요.";
            case "낙농 일꾼 태린" -> "젖소 물과 건초만 챙기고 쉬는 중이에요.";
            case "양계 농부 민호" -> "달걀만 거두고 닭들이 마당을 돌아다니게 뒀어요.";
            default -> "오늘은 쉬는 날이라 천천히 마을을 둘러보고 있어요.";
        };
        String phase = time < 2000L ? "아침 준비 중이에요. "
                : time < 6000L ? "지금 한창 일하는 중이에요. "
                : time < 7200L ? "점심을 잠깐 먹고 있어요. "
                : time < 12000L ? "오후 일을 마무리하고 있어요. "
                : "이제 집으로 돌아갈 채비를 하고 있어요. ";
        return phase + switch (name) {
            case RESIDENT_NAME -> "장독과 부엌을 살피고 이웃 반찬 걱정을 하는 게 내 일이란다.";
            case FARMER_NAME -> "밭의 물길과 익은 작물을 번갈아 확인해요.";
            case RANCHER_NAME -> "건초와 물통을 확인하고 아픈 동물이 없는지 봐요.";
            case HALL_KEEPER_NAME -> "회관 일정과 장터 물가를 정리하고 있어요.";
            case "제빵사 미나" -> "밀가루를 반죽하고 화덕 온도를 맞추고 있어요.";
            case "목수 우진" -> "부서진 울타리와 문을 고치고 목재를 재단해요.";
            case "우편배달부 하람" -> "집마다 편지를 나누고 북쪽 우체국으로 돌아가요.";
            case "어부 세진" -> "긴 강을 오가며 민물고기 상태를 살펴요.";
            case "정원사 나래" -> "과수원과 길가 꽃을 번갈아 돌보고 있어요.";
            case "재봉사 유리" -> "베틀과 실 꾸러미를 오가며 주문한 옷을 만들어요.";
            case "선생님 지호" -> "학교에서 아이들 수업과 책 읽기를 지도해요.";
            case "요리사 다온" -> "공용 부엌에서 재료를 손질하고 냄비를 준비해요.";
            case "의원 수현" -> "진료소에서 약과 침대를 점검해요.";
            case "꽃집 주인 봄이" -> "꽃집에서 화분을 정리하고 새 꽃을 골라요.";
            case "대장장이 건우" -> "화덕과 모루를 오가며 농기구를 고쳐요.";
            case "도서관지기 은채" -> "북쪽 도서관에서 반납 책과 서가를 정리해요.";
            case "과수원지기 연우" -> "나무마다 열매와 잎 상태를 확인하고 있어요.";
            case "양봉가 초롱" -> "벌통 주변을 돌며 꿀과 꽃 상태를 살펴요.";
            case "낙농 일꾼 태린" -> "젖소를 돌보고 우유 짤 준비를 해요.";
            case "양계 농부 민호" -> "닭장과 달걀 둥지를 차례로 확인해요.";
            default -> "오늘 맡은 일을 하고 있어요.";
        };
    }

'''
    replace(npc, "    private static void handleGuest", dialogue + "    private static void handleGuest", "individual dialogue method")

    starter = PROJECT / "src/main/java/kr/countrysidedays/world/StarterHomesteadGenerator.java"
    replace(starter, "Blocks.COPPER_BLOCKS.getFirst().defaultBlockState()", "Blocks.IRON_BLOCK.defaultBlockState()", "estate marker state")
    replace(starter, "is(Blocks.COPPER_BLOCKS.getFirst())", "is(Blocks.IRON_BLOCK)", "estate marker check")
    replace(starter, """        fill(level, b, doorX, 1, doorZ, doorX, 2, doorZ, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, wallHeight + 1, -1, width, wallHeight + 1, depth, palette.roof);""", """        fill(level, b, doorX, 1, doorZ, doorX, 2, doorZ, Blocks.AIR.defaultBlockState());
        BlockPos step = b.offset(doorX, 0, front == Direction.SOUTH ? depth : -1);
        set(level, step, stair(front.getOpposite()));
        fill(level, b, -1, wallHeight + 1, -1, width, wallHeight + 1, depth, palette.roof);""", "estate entrance stairs")

    shared = PROJECT / "src/main/java/kr/countrysidedays/world/SharedRestaurantBuilder.java"
    replace(shared, "Blocks.GOLD_BLOCK.defaultBlockState()", "Blocks.DIAMOND_BLOCK.defaultBlockState()", "restaurant marker state")
    replace(shared, "is(Blocks.GOLD_BLOCK)", "is(Blocks.DIAMOND_BLOCK)", "restaurant marker check")
    replace(shared, """        set(level, base.offset(width / 2, 1, 0), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 2, 0), Blocks.AIR.defaultBlockState());""", """        set(level, base.offset(width / 2, 1, 0), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 2, 0), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 0, -1), stair(Direction.SOUTH));""", "restaurant stair")

    tests = PROJECT / "src/main/java/kr/countrysidedays/gametest/ModGameTests.java"
    replace(tests, "Blocks.COPPER_BLOCKS.getFirst()", "Blocks.IRON_BLOCK", "estate marker test")
    replace(tests, "Blocks.GOLD_BLOCK", "Blocks.DIAMOND_BLOCK", "restaurant marker test")
    replace(tests, "authoredMapRevision() == 16", "authoredMapRevision() == 17", "map revision test")
    replace(tests, "alpha.16", "alpha.17", "test messages")

    print("Countryside Days alpha.17 exact source prepared successfully")


if __name__ == "__main__":
    prepare()
