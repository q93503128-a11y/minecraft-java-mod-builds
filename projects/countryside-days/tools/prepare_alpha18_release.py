from pathlib import Path
import re

P = Path(__file__).resolve().parents[1]
SRC = P/'src/main/java/kr/countrysidedays'

def read(rel): return (P/rel).read_text('utf-8')
def write(rel,s): (P/rel).write_text(s,'utf-8')
def req_replace(rel, old, new, count=-1):
    s=read(rel)
    if old not in s:
        raise SystemExit(f'missing target {rel}: {old[:80]!r}')
    write(rel,s.replace(old,new,count))

# Version
req_replace('gradle.properties','mod_version=0.1.0-alpha.17','mod_version=0.1.0-alpha.18')
req_replace('src/main/java/kr/countrysidedays/CountrysideDays.java','VERSION = "0.1.0-alpha.17"','VERSION = "0.1.0-alpha.18"')

# Correct block update flags: UPDATE_KNOWN_SHAPE (16) suppresses neighbour shape recomputation.
for rel in [
    'src/main/java/kr/countrysidedays/world/StarterHomesteadGenerator.java',
    'src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java',
    'src/main/java/kr/countrysidedays/world/SharedRestaurantBuilder.java',
]:
    s=read(rel)
    s=s.replace('private static final int FLAGS = Block.UPDATE_ALL | 16 | 32; // clients + known shape + suppress drops',
                'private static final int FLAGS = Block.UPDATE_ALL | 32; // neighbours + clients + suppress drops')
    write(rel,s)

# Force one migration pass so existing alpha.17 worlds rebuild fence connections.
req_replace('src/main/java/kr/countrysidedays/world/StarterHomesteadGenerator.java',
            'Blocks.IRON_BLOCK.defaultBlockState()', 'Blocks.GOLD_BLOCK.defaultBlockState()')
req_replace('src/main/java/kr/countrysidedays/world/StarterHomesteadGenerator.java',
            'is(Blocks.IRON_BLOCK)', 'is(Blocks.GOLD_BLOCK)')
req_replace('src/main/java/kr/countrysidedays/world/SharedRestaurantBuilder.java',
            'Blocks.DIAMOND_BLOCK.defaultBlockState()', 'Blocks.LAPIS_BLOCK.defaultBlockState()')
req_replace('src/main/java/kr/countrysidedays/world/SharedRestaurantBuilder.java',
            'is(Blocks.DIAMOND_BLOCK)', 'is(Blocks.LAPIS_BLOCK)')
req_replace('src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java',
            'private static final int MAP_REVISION = 17;', 'private static final int MAP_REVISION = 18;')
req_replace('src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java',
            'is(Blocks.EMERALD_BLOCK)', 'is(Blocks.REDSTONE_BLOCK)')
req_replace('src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java',
            'Blocks.EMERALD_BLOCK.defaultBlockState()', 'Blocks.REDSTONE_BLOCK.defaultBlockState()')

# Update corresponding required GameTests.
tests='src/main/java/kr/countrysidedays/gametest/ModGameTests.java'
s=read(tests).replace('Blocks.IRON_BLOCK','Blocks.GOLD_BLOCK').replace('Blocks.DIAMOND_BLOCK','Blocks.LAPIS_BLOCK')
s=s.replace('authoredMapRevision() == 17','authoredMapRevision() == 18')
write(tests,s)

# Fishing in the public river always yields exactly the custom freshwater fish.
rel='src/main/java/kr/countrysidedays/gameplay/RuralGameplayHandler.java'
s=read(rel)
old='''        event.getDrops().removeIf(stack -> stack.is(Items.COD)\n                || stack.is(Items.SALMON)\n                || stack.is(Items.PUFFERFISH)\n                || stack.is(Items.TROPICAL_FISH));\n        event.getDrops().add(ModItems.RIVER_FISH.get().getDefaultInstance());'''
new='''        // The public river is a freshwater-only fishing zone. Replace the entire vanilla catch,\n        // rather than appending our fish beside cod/salmon/pufferfish or treasure.\n        event.getDrops().clear();\n        event.getDrops().add(ModItems.RIVER_FISH.get().getDefaultInstance());'''
if old not in s: raise SystemExit('fishing target missing')
write(rel,s.replace(old,new))

# Add occupied-seat count to the server-authoritative HUD packet.
rel='src/main/java/kr/countrysidedays/network/EstateHudPayload.java'
s=read(rel)
s=s.replace('''        int customersToday,\n        int customerCap,\n        int totalCustomers,''','''        int customersToday,\n        int customerCap,\n        int occupiedSeats,\n        int totalCustomers,''')
s=s.replace('''                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),\n                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)''','''                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),\n                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),\n                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)''')
s=s.replace('''            ByteBufCodecs.VAR_INT.encode(buffer, value.customerCap());\n            ByteBufCodecs.VAR_INT.encode(buffer, value.totalCustomers());''','''            ByteBufCodecs.VAR_INT.encode(buffer, value.customerCap());\n            ByteBufCodecs.VAR_INT.encode(buffer, value.occupiedSeats());\n            ByteBufCodecs.VAR_INT.encode(buffer, value.totalCustomers());''')
write(rel,s)

rel='src/main/java/kr/countrysidedays/client/ClientEstateState.java'
s=read(rel)
s=s.replace('''    private static int customerCap;\n    private static int totalCustomers;''','''    private static int customerCap;\n    private static int occupiedSeats;\n    private static int totalCustomers;''')
s=s.replace('''            customerCap = payload.customerCap();\n            totalCustomers = payload.totalCustomers();''','''            customerCap = payload.customerCap();\n            occupiedSeats = payload.occupiedSeats();\n            totalCustomers = payload.totalCustomers();''')
s=s.replace('''    public static int totalCustomers() {\n        return totalCustomers;\n    }''','''    public static int occupiedSeats() {\n        return occupiedSeats;\n    }\n\n    public static int totalCustomers() {\n        return totalCustomers;\n    }''')
write(rel,s)

# Populate occupied seats from the active guest roster.
rel='src/main/java/kr/countrysidedays/gameplay/RuralGameplayHandler.java'
s=read(rel)
s=s.replace('''                restaurantEstate.customersServedToday(day),\n                PlayerEstateLayout.RESTAURANT_SEAT_COUNT,\n                restaurantEstate.customersServed(),''','''                restaurantEstate.customersServedToday(day),\n                PlayerEstateLayout.RESTAURANT_SEAT_COUNT,\n                RuralNpcManager.activeGuestCount(player.level(), restaurantEstate.originPos()),\n                restaurantEstate.customersServed(),''')
write(rel,s)

# HUD: explicit business state, live occupancy, and date/time row beneath it.
rel='src/main/java/kr/countrysidedays/client/CountrysideHud.java'
s=read(rel)
s=s.replace('''        drawPanel(graphics, 7, 31, shiftStatus(), 0xD93E4D31, 0xFF9BC978);''','''        drawPanel(graphics, 7, 31, shiftStatus(), 0xD93E4D31, 0xFF9BC978);\n        drawPanel(graphics, 7, 55, currentDateTime(), 0xD93A4038, 0xFFD9B85C);''')
start=s.index('    private static Component shiftStatus() {')
end=s.index('    private static void drawPanel(', start)
replacement='''    private static Component shiftStatus() {\n        String state = ClientEstateState.restaurantOpen() ? "식당 영업 중" : "식당 영업 닫힘";\n        return Component.literal(\n                state\n                        + "   착석 " + ClientEstateState.occupiedSeats() + "/" + ClientEstateState.customerCap()\n                        + "   오늘 " + ClientEstateState.customersToday() + "명"\n                        + "   목장 " + ClientEstateState.pendingRanchProducts()\n        );\n    }\n\n    private static Component currentDateTime() {\n        Minecraft minecraft = Minecraft.getInstance();\n        if (minecraft.level == null) return Component.literal("날짜와 시간 동기화 중");\n        long worldTime = minecraft.level.getDayTime();\n        long day = Math.floorDiv(worldTime, 24000L) + 1L;\n        long dayTicks = Math.floorMod(worldTime, 24000L);\n        int minutes = (int) Math.floorMod((dayTicks * 1440L / 24000L) + 360L, 1440L);\n        return Component.literal(String.format(\n                java.util.Locale.ROOT,\n                "%d일차   %02d:%02d",\n                day, minutes / 60, minutes % 60\n        ));\n    }\n\n'''
s=s[:start]+replacement+s[end:]
write(rel,s)

# Remove routine teleport recovery; residents keep walking toward their destinations.
for rel in [
    'src/main/java/kr/countrysidedays/gameplay/VillageLifeManager.java',
    'src/main/java/kr/countrysidedays/gameplay/RuralNpcManager.java',
]:
    s=read(rel)
    s=re.sub(r'''\n\s*if \(!isWalkable\(level, villager\.blockPosition\(\)\) && villager\.getPose\(\) != Pose\.SITTING\) \{\n\s*villager\.getNavigation\(\)\.stop\(\);\n\s*villager\.setPos\(target\.getX\(\) \+ 0\.5, target\.getY\(\), target\.getZ\(\) \+ 0\.5\);\n\s*\}''','',s)
    s=s.replace('''        if (!isWalkable(level, villager.blockPosition()) && villager.getPose() != Pose.SITTING) moveImmediately(villager, target);\n''','')
    if rel.endswith('VillageLifeManager.java'):
        s=s.replace('villager.setNoAi(false);\n            return;', 'villager.setNoAi(stationaryAtTarget);\n            return;')
    write(rel,s)

# Guests walk away after service instead of popping to the village centre.
rel='src/main/java/kr/countrysidedays/gameplay/RuralNpcManager.java'
s=read(rel)
s=re.sub(r'\n    private static void moveImmediately\(Villager villager, BlockPos pos\) \{.*?\n    \}\n', '\n', s, flags=re.S)
s=s.replace('''        guest.setPos(returnPos.getX() + 0.5, returnPos.getY(), returnPos.getZ() + 0.5);''','''        guest.getNavigation().moveTo(\n                returnPos.getX() + 0.5, returnPos.getY(), returnPos.getZ() + 0.5, 0.62D\n        );''')
# Report movement honestly instead of saying a resident is already working while still walking from home.
s=s.replace('''        String phrase = residentDialogue(name, time, holiday);''','''        String phrase = residentDialogue(name, time, holiday, villager.getNavigation().isInProgress());''')
s=s.replace('''    private static String residentDialogue(String name, long time, boolean holiday) {''','''    private static String residentDialogue(String name, long time, boolean holiday, boolean moving) {''')
s=s.replace('''        String phase = time < 2000L ? "아침 준비 중이에요. "\n                : time < 6000L ? "지금 한창 일하는 중이에요. "''','''        String phase = moving ? "지금 다음 장소로 걸어가는 중이에요. "\n                : time < 2000L ? "아침 준비 중이에요. "\n                : time < 6000L ? "일터에 도착해 맡은 일을 하고 있어요. "''')
write(rel,s)

# Improve hay efficiency from four to eight animals per block. Water in the trough is already reusable.
rel='src/main/java/kr/countrysidedays/gameplay/RanchLifeManager.java'
s=read(rel)
s=s.replace('''    private static final String HUNGER_PREFIX = "cd_hunger_";''','''    private static final String HUNGER_PREFIX = "cd_hunger_";\n    private static final int ANIMALS_PER_HAY_BLOCK = 8;''')
s=s.replace('''        int animalsFed = Math.min(hungry.size(), hayAvailable * 4);''','''        int animalsFed = Math.min(hungry.size(), hayAvailable * ANIMALS_PER_HAY_BLOCK);''')
s=s.replace('''        int hayNeeded = (animalsFed + 3) / 4;''','''        int hayNeeded = (animalsFed + ANIMALS_PER_HAY_BLOCK - 1) / ANIMALS_PER_HAY_BLOCK;''')
write(rel,s)

# Written-book page text uses black formatting, including headings.
for rel in [
    'src/main/java/kr/countrysidedays/item/LifeGuideItem.java',
    'src/main/java/kr/countrysidedays/item/RecipeNotebookItem.java',
]:
    s=read(rel).replace('§6§l','§0§l')
    if rel.endswith('LifeGuideItem.java'):
        s=s.replace('건초 1개로 동물 최대 4마리 급식','건초 1개로 동물 최대 8마리 급식')
        s=s.replace('• 먹은 건초는 실제로 사라짐','• 물통의 물은 목장 전체가 함께 사용')
    write(rel,s)

# Contract sanity checks.
alljava='\n'.join(p.read_text('utf-8') for p in SRC.rglob('*.java'))
checks={
 'version':'0.1.0-alpha.18' in read('gradle.properties'),
 'occupied payload':'int occupiedSeats' in alljava and 'value.occupiedSeats()' in alljava,
 'date/time':'currentDateTime()' in alljava and '%02d:%02d' in alljava,
 'freshwater clear':'event.getDrops().clear();' in alljava,
 'fence flags':'Block.UPDATE_ALL | 32' in alljava,
 'hay eight':'ANIMALS_PER_HAY_BLOCK = 8' in alljava,
 'black books':'§0§l시골생활' in alljava and '§0§l시골 전골' in alljava,
}
print(checks)
if not all(checks.values()): raise SystemExit('contract failure')
