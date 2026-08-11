#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")

def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    if new in text and old not in text:
        return
    if old not in text:
        raise SystemExit(f"{label}: marker missing")
    write(path, text.replace(old, new, 1))

def replace_between(path: Path, start: str, end: str, replacement: str, label: str) -> None:
    text = read(path)
    a = text.find(start)
    b = text.find(end, a + len(start))
    if a < 0 or b < 0:
        if replacement.strip() and replacement.strip() in text:
            return
        raise SystemExit(f"{label}: boundaries missing")
    write(path, text[:a] + replacement + text[b:])

# Version and metadata
replace_once(ROOT / "gradle.properties", "mod_version=0.12.1-alpha.19",
             "mod_version=0.12.1-alpha.20", "gradle version")
replace_once(JAVA / "ArcaneCircle.java", 'VERSION = "0.12.1-alpha.19"',
             'VERSION = "0.12.1-alpha.20"', "runtime version")
index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(read(index_path))
index["version"] = "0.12.1-alpha.20"
index["robe_equipment"] = "atomic_chest_and_visual_hem"
index["cast_release"] = "hold_to_release_even_when_cast_time_is_zero"
index["visual_identity"] = "per_spell_fingerprint_and_high_circle_signature"
write(index_path, json.dumps(index, ensure_ascii=False, indent=2) + "\n")
toml = ROOT / "src/main/templates/META-INF/neoforge.mods.toml"
replace_once(toml,
    "구중 마법학 세계관을 기반으로 1~5 숫자키 주문 슬롯, X 홀드식 2~3중 즉석 융합, 실전 숙련 각인, 드래그·스크롤 가능한 마도서, 쿨타임 HUD와 다양한 능력의 마도 지팡이를 구현한 Minecraft Java 마법 시스템. 현재 1~3써클을 지원한다.",
    "1~9써클 구중 마법 체계, 숫자키 홀드·릴리즈 시전과 2~3중 동시입력 융합, 주문별 마법진·투사체·의식 연출, 마도회·의뢰·장비·아르카나 성장, 전투형 NPC 마도사를 갖춘 Minecraft Java 마법 RPG 시스템.",
    "mod description")

# Hold/release casting, including zero cast time.
casting = JAVA / "magic/SpellCastingService.java"
replace_once(casting,
    "    private static final long CHARGE_TIMEOUT_TICKS = 1600L;\n",
    "    private static final long CHARGE_TIMEOUT_TICKS = 1600L;\n"
    "    private static final long READY_HOLD_TIMEOUT_TICKS = 12000L;\n",
    "hold timeout")
replace_once(casting, '''        clearFusion(player, false);
        int required = requiredCastTicks(player, cast.spell());
        if (required <= 0) {
            castPrepared(player, data, cast);
            return;
        }

        ChargeState charge = new ChargeState(slot, cast.spell().id(), now, required);
        CHARGES.put(player.getUUID(), charge);
        WorldMagicService.charge(player, cast.spell(), false, List.of(), cast.range(), 0.0);
        ArcaneNoticeService.push(player, Component.literal("§5[회로 전개] §f" + cast.spell().name()
                + " §7· " + String.format("%.1f", required / 20.0) + "초"));
''', '''        clearFusion(player, false);
        int required = requiredCastTicks(player, cast.spell());
        ChargeState charge = new ChargeState(slot, cast.spell().id(), now, required);
        CHARGES.put(player.getUUID(), charge);
        WorldMagicService.charge(player, cast.spell(), false, List.of(), cast.range(),
                required <= 0 ? 1.0 : 0.0);
        String timing = required <= 0
                ? "완성 · 키를 놓으면 발동"
                : String.format("%.1f초 전개 · 완성 후 키를 놓으면 발동", required / 20.0);
        ArcaneNoticeService.push(player, Component.literal("§5[회로 전개] §f"
                + cast.spell().name() + " §7· " + timing));
''', "zero cast hold")
replace_once(casting, "        if (elapsed > CHARGE_TIMEOUT_TICKS) {\n",
             "        if (elapsed > chargeTimeoutTicks(charge)) {\n", "release timeout")
replace_once(casting, "        if (!player.isAlive() || player.isSpectator() || elapsed > CHARGE_TIMEOUT_TICKS) {\n",
             "        if (!player.isAlive() || player.isSpectator() || elapsed > chargeTimeoutTicks(charge)) {\n",
             "tick timeout")
replace_once(casting, '''        if (charge.requiredTicks <= 0) return;
        WorldMagicService.charge(player, spell, false, List.of(), cast.range(),
                Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks)));
''', '''        double progress = charge.requiredTicks <= 0 ? 1.0
                : Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks));
        WorldMagicService.charge(player, spell, false, List.of(), cast.range(), progress);
''', "charge refresh")
text = read(casting)
helper = '''    private static long chargeTimeoutTicks(ChargeState charge) {
        return Math.max(CHARGE_TIMEOUT_TICKS,
                (long) Math.max(0, charge.requiredTicks) + READY_HOLD_TIMEOUT_TICKS);
    }

'''
marker = "    public static void cancelCharge(ServerPlayer player, boolean notify) {\n"
if helper not in text:
    if marker not in text:
        raise SystemExit("charge helper marker missing")
    write(casting, text.replace(marker, helper + marker, 1))

# Atomic robe: leggings proxy is never a separately owned piece.
gear = JAVA / "magic/MageGearService.java"
replace_once(gear,
    "    private static final Map<UUID,Long> STABLE_DESCENT_UNTIL=new HashMap<>();\n",
    "    private static final Map<UUID,Long> STABLE_DESCENT_UNTIL=new HashMap<>();\n"
    "    private static final Map<UUID,Item> LINKED_ROBES=new HashMap<>();\n",
    "robe link map")
new_tick = '''    public static void tick(ServerPlayer player){
        syncAtomicRobe(player);
        ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST);

        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();int tier=bootsTier(boots);
        if(tier>0){player.addEffect(new MobEffectInstance(MobEffects.SPEED,30,Math.max(0,tier-1),true,false));player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,30,Math.max(0,tier-1),true,false));}
        if(isFrostBoots(boots))freezeWater(player);
        setFlight(player,isFlightBoots(boots));

        Item hat=player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        if(hat==ModItems.RIFT_CROWN.get())player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,true,false));
        if(hat==ModItems.CINDER_HOOD.get()||chest.getItem()==ModItems.CINDER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,true,false));
        if(chest.getItem()==ModItems.GLACIER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,1,true,false));
        if(chest.getItem()==ModItems.TEMPEST_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.SPEED,40,1,true,false));
        int robeTier=robeTier(chest.getItem());if(robeTier>=2&&hemFor(chest.getItem())==player.getItemBySlot(EquipmentSlot.LEGS).getItem())player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,30,robeTier>=3?1:0,true,false));
    }

    private static void syncAtomicRobe(ServerPlayer player){
        UUID id=player.getUUID();
        ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs=player.getItemBySlot(EquipmentSlot.LEGS);
        Item expected=hemFor(chest.getItem());
        Item linked=LINKED_ROBES.get(id);

        if(expected==null){
            LINKED_ROBES.remove(id);
            if(isHem(legs.getItem()))player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);
            purgeLooseHems(player);
            ROBE_SLOT_WARNED.remove(id);
            return;
        }

        boolean sameLinked=linked==chest.getItem();
        boolean lowerMissing=legs.isEmpty()||legs.getItem()!=expected;
        boolean removedHemIsLoose=hasLooseHem(player,expected);

        if(sameLinked&&lowerMissing){
            ItemStack robe=chest.copy();
            player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
            if(isHem(legs.getItem()))player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);
            replaceLooseHemWithRobe(player,expected,robe);
            LINKED_ROBES.remove(id);
            purgeLooseHems(player);
            ROBE_SLOT_WARNED.remove(id);
            return;
        }

        if(!sameLinked){
            if(legs.isEmpty()&&removedHemIsLoose){
                ItemStack robe=chest.copy();
                player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                replaceLooseHemWithRobe(player,expected,robe);
                LINKED_ROBES.remove(id);
                purgeLooseHems(player);
                return;
            }
            if(legs.isEmpty()||isHem(legs.getItem())){
                player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(expected));
                LINKED_ROBES.put(id,chest.getItem());
                ROBE_SLOT_WARNED.remove(id);
            }else{
                ItemStack robe=chest.copy();
                player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
                giveOrDrop(player,robe);
                LINKED_ROBES.remove(id);
                if(ROBE_SLOT_WARNED.add(id))ArcaneNoticeService.push(player,
                        Component.literal("§c[로브 장착 취소] §f로브는 한 벌 장비입니다. 바지 슬롯을 비우고 다시 장착하세요."),100);
            }
        }
        purgeLooseHems(player);
    }

    private static boolean hasLooseHem(ServerPlayer player,Item hem){
        ItemStack carried=player.containerMenu.getCarried();
        if(!carried.isEmpty()&&carried.getItem()==hem)return true;
        int limit=Math.min(36,player.getInventory().getContainerSize());
        for(int slot=0;slot<limit;slot++)if(player.getInventory().getItem(slot).getItem()==hem)return true;
        return false;
    }

    private static void replaceLooseHemWithRobe(ServerPlayer player,Item hem,ItemStack robe){
        ItemStack carried=player.containerMenu.getCarried();
        if(!carried.isEmpty()&&carried.getItem()==hem){
            player.containerMenu.setCarried(robe);
            return;
        }
        int limit=Math.min(36,player.getInventory().getContainerSize());
        for(int slot=0;slot<limit;slot++){
            ItemStack stack=player.getInventory().getItem(slot);
            if(stack.getItem()!=hem)continue;
            player.getInventory().setItem(slot,robe);
            return;
        }
        giveOrDrop(player,robe);
    }

    private static void purgeLooseHems(ServerPlayer player){
        ItemStack carried=player.containerMenu.getCarried();
        if(!carried.isEmpty()&&isHem(carried.getItem()))player.containerMenu.setCarried(ItemStack.EMPTY);
        int limit=Math.min(36,player.getInventory().getContainerSize());
        for(int slot=0;slot<limit;slot++){
            ItemStack stack=player.getInventory().getItem(slot);
            if(isHem(stack.getItem()))player.getInventory().setItem(slot,ItemStack.EMPTY);
        }
    }

    private static void giveOrDrop(ServerPlayer player,ItemStack stack){
        if(stack.isEmpty())return;
        if(!player.getInventory().add(stack))player.drop(stack,false);
    }

'''
replace_between(gear, "    public static void tick(ServerPlayer player){",
                "    /** Spell-driven feather fall remains explicit; armor no longer edits airborne velocity. */",
                new_tick, "atomic robe tick")
replace_once(gear,
    '    public static String robeName(Player p){Item c=p.getItemBySlot(EquipmentSlot.CHEST).getItem();if(hemFor(c)==null)return"로브 없음";return name(c,"로브 없음")+(hemFor(c)==p.getItemBySlot(EquipmentSlot.LEGS).getItem()?"":" · 바지 슬롯 필요");}\n',
    '    public static String robeName(Player p){Item c=p.getItemBySlot(EquipmentSlot.CHEST).getItem();return hemFor(c)==null?"로브 없음":name(c,"로브 없음");}\n',
    "robe name")
replace_once(gear,
    "    public static void clear(UUID id){ROBE_SLOT_WARNED.remove(id);FLIGHT_GRANTED.remove(id);STABLE_DESCENT_UNTIL.remove(id);}\n",
    "    public static void clear(UUID id){ROBE_SLOT_WARNED.remove(id);FLIGHT_GRANTED.remove(id);STABLE_DESCENT_UNTIL.remove(id);LINKED_ROBES.remove(id);}\n",
    "robe clear")

tooltip = JAVA / "client/MageGearTooltip.java"
replace_once(tooltip,
    'else if(hem(i)){title(l,"로브 하단부");l.add(Component.literal("흉갑 로브 착용 시 자동 장착·해제").withStyle(ChatFormatting.GRAY));l.add(Component.literal("독립 장비 효과 없음").withStyle(ChatFormatting.DARK_GRAY));}',
    'else if(hem(i)){title(l,"로브 내부 자락");l.add(Component.literal("로브 흉갑과 한 벌로 자동 연동").withStyle(ChatFormatting.GRAY));l.add(Component.literal("따로 보관·장착할 수 없는 내부 표시용 장비").withStyle(ChatFormatting.DARK_GRAY));}',
    "hem tooltip")

print("Arcane Circle alpha.20 runtime migration: PASS")
