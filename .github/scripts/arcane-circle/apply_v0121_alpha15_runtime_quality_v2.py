from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str, marker: str | None = None) -> None:
    text = read(rel)
    if marker and marker in text:
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected one target, found {count}: {old[:90]!r}")
    write(rel, text.replace(old, new, 1))


def patch_versions() -> None:
    replace_once("gradle.properties", "mod_version=0.12.1-alpha.14",
                 "mod_version=0.12.1-alpha.15", "mod_version=0.12.1-alpha.15")
    path = "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
    text = read(path)
    if 'VERSION = "0.12.1-alpha.15"' not in text:
        text, n = re.subn(r'public static final String VERSION = "0\.12\.1-alpha\.\d+";',
                          'public static final String VERSION = "0.12.1-alpha.15";', text, count=1)
        if n != 1:
            raise SystemExit(f"{path}: VERSION constant missing")
        write(path, text)


def patch_fall_protection() -> None:
    arcane = "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
    text = read(arcane)
    if "MageGearService::onIncomingDamage" not in text:
        needle = "        NeoForge.EVENT_BUS.addListener(RpgScaleService::onIncomingDamage);\n"
        if needle not in text:
            raise SystemExit(f"{arcane}: incoming damage anchor missing")
        text = text.replace(needle, needle +
                            "        NeoForge.EVENT_BUS.addListener(MageGearService::onIncomingDamage);\n", 1)
        write(arcane, text)

    gear = "src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java"
    text = read(gear)
    if "public static void onIncomingDamage(LivingIncomingDamageEvent event)" not in text:
        if "import net.minecraft.world.damagesource.DamageTypes;" not in text:
            text = text.replace("import net.minecraft.world.entity.player.Player;\n",
                                "import net.minecraft.world.entity.player.Player;\n"
                                "import net.minecraft.world.damagesource.DamageTypes;\n"
                                "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n", 1)

        old_tick = '''    public static void tickMovement(ServerPlayer player){
        long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();
        long until=STABLE_DESCENT_UNTIL.getOrDefault(player.getUUID(),0L);
        boolean spellDescent=until>now;
        if(!spellDescent&&until>0L)STABLE_DESCENT_UNTIL.remove(player.getUUID());
        if(player.onGround()||player.getAbilities().flying||player.isShiftKeyDown())return;
        if(spellDescent){
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,12,0,true,false));
            player.fallDistance=0.0F;
            return;
        }
        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();
        int tier=bootsTier(boots);
        if(isGlideBoots(boots))stabilizeDescent(player,Math.max(1,tier));
    }
'''
        new_tick = '''    /** Spell-driven feather fall remains explicit; armor no longer edits airborne velocity. */
    public static void tickMovement(ServerPlayer player){
        long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();
        long until=STABLE_DESCENT_UNTIL.getOrDefault(player.getUUID(),0L);
        boolean spellDescent=until>now;
        if(!spellDescent&&until>0L)STABLE_DESCENT_UNTIL.remove(player.getUUID());
        if(!spellDescent||player.onGround()||player.getAbilities().flying||player.isShiftKeyDown())return;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,12,0,true,false));
        player.fallDistance=0.0F;
    }
'''
        if old_tick not in text:
            raise SystemExit(f"{gear}: armor glide tick block missing")
        text = text.replace(old_tick, new_tick, 1)

        text = text.replace("    private static boolean isGlideBoots(Item i){return i==ModItems.SKYWALKER_BOOTS.get()||i==ModItems.FROSTSTEP_BOOTS.get()||i==ModItems.TEMPEST_BOOTS.get()||i==ModItems.RIFT_BOOTS.get();}\n", "")
        stabilize = "    private static void stabilizeDescent(ServerPlayer p,int tier){Vec3 m=p.getDeltaMovement();double floor=tier>=3?-0.105:tier==2?-0.135:-0.165;if(m.y<floor){double next=m.y+(floor-m.y)*0.22;p.setDeltaMovement(m.x,next,m.z);if(Math.abs(next-m.y)>.015)p.hurtMarked=true;}p.fallDistance=0.0F;}\n"
        if stabilize not in text:
            raise SystemExit(f"{gear}: armor stabilizeDescent method missing")
        text = text.replace(stabilize, "", 1)
        text = text.replace("import net.minecraft.world.phys.Vec3;\n", "")

        anchor = "    public static GearStats stats(Player player){\n"
        if anchor not in text:
            raise SystemExit(f"{gear}: GearStats anchor missing")
        handler = '''    /**
     * Armor landing protection changes only incoming FALL damage. A fully protected soft landing
     * is cancelled before LivingEntity damage processing, preventing hurt flash/sound as well.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!event.getSource().is(DamageTypes.FALL))return;
        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();
        Item chest=player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        int bootTier=bootsTier(boots),robe=robeTier(chest);
        int hat=piece(player.getItemBySlot(EquipmentSlot.HEAD).getItem()).tier;
        if(bootTier<=0&&robe<=0&&hat<=0)return;
        double bootReduction=switch(bootTier){case 1->0.26;case 2->0.46;case 3->0.66;default->0.0;};
        double supportReduction=Math.min(0.18,robe*0.045+hat*0.015);
        double reduction=Math.min(0.82,bootReduction+supportReduction);
        float reduced=(float)Math.max(0.0,event.getAmount()*(1.0-reduction));
        double ignoreThreshold=0.45+bootTier*1.35+robe*0.35;
        if(reduced<=ignoreThreshold){event.setCanceled(true);return;}
        event.setAmount(reduced);
    }

'''
        text = text.replace(anchor, handler + anchor, 1)
        write(gear, text)

    body = read(gear)
    for forbidden in ("isGlideBoots(", "stabilizeDescent(", "getDeltaMovement()"):
        if forbidden in body:
            raise SystemExit(f"{gear}: armor movement legacy remains: {forbidden}")
    for required in ("grantStableDescent", "SLOW_FALLING", "STABLE_DESCENT_UNTIL"):
        if required not in body:
            raise SystemExit(f"{gear}: spell feather-fall support was lost: {required}")


def patch_visual_strength() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java"
    text = read(path)
    if "SATURATION_BOOST" in text:
        return
    old = "    private static int tone(int argb,double brightness,double alphaScale){int a=(int)Math.round(((argb>>>24)&255)*alphaScale);int r=(int)Math.round(((argb>>>16)&255)*brightness);int g=(int)Math.round(((argb>>>8)&255)*brightness);int b=(int)Math.round((argb&255)*brightness);return(clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);}\n"
    new = '''    private static final double SATURATION_BOOST=1.28;
    private static final double ALPHA_BOOST=1.32;
    private static int tone(int argb,double brightness,double alphaScale){
        int baseA=(argb>>>24)&255,baseR=(argb>>>16)&255,baseG=(argb>>>8)&255,baseB=argb&255;
        double average=(baseR+baseG+baseB)/3.0;
        int a=(int)Math.round(baseA*Math.min(1.0,alphaScale*ALPHA_BOOST));
        int r=(int)Math.round((average+(baseR-average)*SATURATION_BOOST)*brightness);
        int g=(int)Math.round((average+(baseG-average)*SATURATION_BOOST)*brightness);
        int b=(int)Math.round((average+(baseB-average)*SATURATION_BOOST)*brightness);
        return(clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }
'''
    if old not in text:
        raise SystemExit(f"{path}: tone method anchor missing")
    text = text.replace(old, new, 1)
    text = text.replace("tone(argb,.42,.58),windowScale*2.10F", "tone(argb,.58,.72),windowScale*2.10F")
    text = text.replace("tone(argb,1.12,.94),windowScale*.92F", "tone(argb,1.02,1.0),windowScale*.92F")
    write(path, text)


def patch_npc_spell_mix() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java"
    text = read(path)
    if "chooseCombatSpell(Mob caster, MageProfile profile)" in text:
        return
    text = text.replace("SpellDefinition visual = visualSpell(profile);",
                        "SpellDefinition visual = chooseCombatSpell(caster, profile);", 1)
    text = text.replace("SpellCatalog.spell(cast.spellId()).orElseGet(() -> visualSpell(profile))",
                        "SpellCatalog.spell(cast.spellId()).orElseGet(() -> chooseCombatSpell(caster, profile))", 1)
    text = text.replace("int required = Math.max(8, 12 + profile.circle() * 2);",
                        "int required = Math.max(8, 8 + visual.circle() * 3);", 1)
    text = text.replace("double power = spellDamage(profile, hostile ? 1.08F : 1.0F);",
                        "double power = spellDamage(profile, hostile ? 1.08F : 1.0F)\n                * (0.76 + visual.circle() * 0.055);", 1)
    start = text.find("    private static SpellDefinition visualSpell(MageProfile profile) {")
    end = text.find("    private static MageProfile profile(Entity entity) {", start)
    if start < 0 or end < 0:
        raise SystemExit(f"{path}: fixed visualSpell block missing")
    replacement = '''    /** High-circle mages favor high magic but deliberately retain mid/low-circle choices. */
    private static SpellDefinition chooseCombatSpell(Mob caster, MageProfile profile) {
        int circle=Math.max(1,Math.min(9,profile.circle()));
        List<SpellDefinition> all=SpellCatalog.spells().stream()
                .filter(spell->spell.circle()<=circle)
                .filter(spell->SpellCatalog.isDamaging(spell.id())).toList();
        if(all.isEmpty())return SpellCatalog.spell("magic_missile").orElseThrow();
        int roll=caster.getRandom().nextInt(100),minCircle,maxCircle;
        if(circle>=6&&roll<55){minCircle=Math.max(1,circle-1);maxCircle=circle;}
        else if(circle>=4&&roll<85){minCircle=Math.max(2,circle-4);maxCircle=Math.max(minCircle,circle-2);}
        else{minCircle=1;maxCircle=Math.max(1,circle/2);}
        List<SpellDefinition> band=all.stream()
                .filter(spell->spell.circle()>=minCircle&&spell.circle()<=maxCircle).toList();
        if(band.isEmpty())band=all;
        List<SpellDefinition> themed=band.stream()
                .filter(spell->preferredSchool(profile.affiliation(),spell.school())).toList();
        List<SpellDefinition> candidates=themed.isEmpty()?band:themed;
        return candidates.get(caster.getRandom().nextInt(candidates.size()));
    }

    private static boolean preferredSchool(MagicTradition affiliation, SpellDefinition.School school) {
        return switch(affiliation){
            case ARCANE -> school==SpellDefinition.School.ARCANE||school==SpellDefinition.School.SPACE||school==SpellDefinition.School.WARD;
            case DIVINE -> school==SpellDefinition.School.LIFE||school==SpellDefinition.School.WARD||school==SpellDefinition.School.ARCANE;
            case OCCULT -> school==SpellDefinition.School.SPACE||school==SpellDefinition.School.ARCANE||school==SpellDefinition.School.FROST;
            case PRIMAL -> school==SpellDefinition.School.FIRE||school==SpellDefinition.School.FROST||school==SpellDefinition.School.WIND;
            default -> true;
        };
    }

'''
    write(path, text[:start] + replacement + text[end:])


def patch_test_profile() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java"
    text = read(path)
    if "enableCreativeTestProfile(ServerPlayer player)" in text:
        return
    anchor = "    public EffectiveStats effectiveStats(ServerPlayer player) {\n"
    if anchor not in text:
        raise SystemExit(f"{path}: effectiveStats anchor missing")
    method = '''    /** Creative test expansion; it only raises/adds progression and never deletes learned state. */
    public int enableCreativeTestProfile(ServerPlayer player) {
        MageState state=state(player);
        state.circle=SpellCatalog.IMPLEMENTED_MAX_CIRCLE;
        state.insight=1_000_000;
        int unlocked=0;
        for(SpellDefinition spell:SpellCatalog.spells()){
            if(state.known.add(spell.id()))unlocked++;
            state.mastery.put(spell.id(),Math.max(100_000,
                    SpellCatalog.isFusionResult(spell.id())?SpellCatalog.masteryRequired(spell.id()):0));
        }
        List<SpellDefinition> top=SpellCatalog.spells().stream()
                .sorted(Comparator.comparingInt(SpellDefinition::circle).reversed()
                        .thenComparing(SpellDefinition::id)).limit(5).toList();
        for(int i=0;i<state.slots.size();i++)state.slots.set(i,i<top.size()?top.get(i).id():"");
        state.cooldowns.clear();
        state.mana=effectiveStats(player).maxMana();
        setDirty();
        return unlocked;
    }

'''
    write(path, text.replace(anchor, method + anchor, 1))


def patch_test_item() -> None:
    item = "src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneTestKitItem.java"
    if not (ROOT / item).exists():
        write(item, '''package kr.moonseungjun.arcanecircle.item;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellKineticsService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import kr.moonseungjun.arcanecircle.world.ArcaneWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** No recipe/loot source: this exists only in the creative tools tab. */
public final class ArcaneTestKitItem extends Item {
    public ArcaneTestKitItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand){
        if(!(player instanceof ServerPlayer serverPlayer)||!(level instanceof ServerLevel serverLevel))return InteractionResult.SUCCESS;
        if(!serverPlayer.hasInfiniteMaterials()){
            serverPlayer.sendSystemMessage(Component.literal("§c[아르카나 시험핵] §f크리에이티브 테스트 전용 아이템입니다."));
            return InteractionResult.FAIL;
        }
        SpellCastingService.clearSession(serverPlayer.getUUID());
        SpellKineticsService.clear(serverPlayer.getUUID());
        MagicPlayerData data=MagicPlayerData.get(serverLevel.getServer());
        int unlocked=data.enableCreativeTestProfile(serverPlayer);
        long marks=ArcaneWorldData.get(serverLevel.getServer()).addMarks(serverPlayer,1_000_000_000L);
        ArcaneNetwork.sync(serverPlayer);
        serverPlayer.sendSystemMessage(Component.literal("§d[아르카나 시험핵] §f9써클 · 전체 주문 · 최대 시험 숙련 · 쿨타임 초기화 적용"));
        serverPlayer.sendSystemMessage(Component.literal("§7추가 해금 "+unlocked+"개 · 아르카나 "+marks+" · 최고써클 주문 5개 자동 장착"));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                           Consumer<Component> tooltip, TooltipFlag flag){
        tooltip.accept(Component.literal("§d[크리에이티브 개발용]"));
        tooltip.accept(Component.literal("§f9써클 · 전체 주문 · 숙련 최대 · 마력 회복"));
        tooltip.accept(Component.literal("§f쿨타임 초기화 · 아르카나 +10억 · 최고써클 5개 장착"));
        tooltip.accept(Component.literal("§8조합법/전리품 없음 · 크리에이티브 탭 전용"));
        super.appendHoverText(stack,context,display,tooltip,flag);
    }
}
''')

    moditems = "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java"
    text = read(moditems)
    if "ArcaneTestKitItem" not in text:
        text = text.replace("import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;\n",
                            "import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;\nimport kr.moonseungjun.arcanecircle.item.ArcaneTestKitItem;\n", 1)
        reg = '''    public static final DeferredItem<BeginnerGrimoireItem> BEGINNER_GRIMOIRE = ITEMS.registerItem(
            "beginner_grimoire", properties -> new BeginnerGrimoireItem(properties.rarity(Rarity.UNCOMMON)));
'''
        if reg not in text:
            raise SystemExit(f"{moditems}: beginner grimoire registration missing")
        text = text.replace(reg, reg + '''    public static final DeferredItem<ArcaneTestKitItem> ARCANE_TEST_KIT = ITEMS.registerItem(
            "arcane_test_kit", properties -> new ArcaneTestKitItem(properties.rarity(Rarity.EPIC)));
''', 1)
        anchor = "            event.accept(BEGINNER_GRIMOIRE.get());\n"
        if anchor not in text:
            raise SystemExit(f"{moditems}: creative tab anchor missing")
        text = text.replace(anchor, anchor + "            event.accept(ARCANE_TEST_KIT.get());\n", 1)
        write(moditems, text)

    write("src/main/resources/assets/arcanecircle/items/arcane_test_kit.json",
          '{"model":{"type":"minecraft:model","model":"arcanecircle:item/arcane_test_kit"}}\n')
    write("src/main/resources/assets/arcanecircle/models/item/arcane_test_kit.json",
          '{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/nether_star"}}\n')
    lang = "src/main/resources/assets/arcanecircle/lang/ko_kr.json"
    text = read(lang)
    if '"item.arcanecircle.arcane_test_kit"' not in text:
        anchor = '  "item.arcanecircle.beginner_grimoire": "초심자 마도서",\n'
        if anchor not in text:
            raise SystemExit(f"{lang}: language anchor missing")
        write(lang, text.replace(anchor, anchor +
                                 '  "item.arcanecircle.arcane_test_kit": "아르카나 시험핵",\n', 1))


def main() -> None:
    patch_versions()
    patch_fall_protection()
    patch_visual_strength()
    patch_npc_spell_mix()
    patch_test_profile()
    patch_test_item()
    print("Arcane Circle alpha.15 runtime-quality migration v2: PASS")


if __name__ == "__main__":
    main()
