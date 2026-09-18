# Open-World RPG — R01 Asset Phase C Exact-File Audit

> Date: 2026-09-18  
> Status: **EXACT SOURCE-BYTE / GEOMETRY AUDIT COMPLETE FOR SELECTED KAYKIT BASELINES — VISUAL/MINECRAFT ACCEPTANCE STILL REQUIRED**  
> Parent intake: `R01_ASSET_INTAKE.md`  
> Project contract: `PROJECT.md`  
> Rule: this file records exact upstream bytes/geometry evidence. It does not claim Blockbench/Minecraft/playtest acceptance.

---

# 1. Audit method

This pass did not rely on filenames alone.

For the selected public GitHub sources, the exact files were fetched from pinned upstream commits through the GitHub connector:

- KayKit Adventurers pinned revision: `672074b73ba276876a19e8816ecdc5241817ab47`
- KayKit Restaurant Bits pinned revision: `153c8a7535b48237854cb54ff6890679f8c574d1`

For each selected glTF:

1. exact `.gltf` bytes were fetched;
2. the referenced exact `.bin` bytes were fetched;
3. SHA-256 was computed from the fetched bytes;
4. the POSITION accessor count/min/max and SCALAR index count were read from the glTF;
5. source-unit bounds and approximate triangle count were recorded;
6. referenced shared texture files and license files were fetched and SHA-256 hashed.

Important limitation:

```text
exact upstream source bytes inspected: YES
exact file SHA-256 calculated: YES
GitHub source commit pinned: YES
source-unit geometry bounds inspected: YES
local upstream ZIP archive acquired: NO
Blockbench/Blender visual inspection: NO
Minecraft conversion/render inspection: NO
animation/hand-pivot clipping review: NO
PLAYTESTED: NO
```

A source-file hash below proves which upstream bytes were inspected. It is not a claim that those bytes have already been committed into the public Openworld RPG repository.

---

# 2. KayKit Adventurers — exact starter/equipment baselines

Upstream:

`KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0`

Pinned revision:

`672074b73ba276876a19e8816ecdc5241817ab47`

License file:

```text
LICENSE.txt
SHA-256 ae322141814056dda0deea7540d74c41d87aee1da319977cd1bd84ee5a923629
Git blob 877e44735b5869c10e17a59e3b757905aa390626
```

## 2.1 Model byte/geometry evidence

| Model | R01 use | glTF SHA-256 | BIN SHA-256 | Vertices | Triangles | Source-unit size X×Y×Z |
|---|---|---|---|---:|---:|---|
| `sword_1handed` | Heartland Arming Sword public-safe baseline | `f88345d0c89d52710ddb00555964fe7a6f4024874d7ee902ee4c742c048a1958` | `780ebfd002cd181fd5b1626f5a6ae8dda0ff2df58c3ff1fd6e7584f53a969d8a` | 358 | 300 | 0.5034 × 1.7753 × 0.1306 |
| `dagger` | Wayfarer Daggers baseline | `33c577d4d5d268295f13494c930d20d11bf1a82e862aa7bdab4d7d51bc26fcd0` | `2ef02dbdc53839cd2506f9dea40dcc401cdda830ae6a887f499333cce0d11be3` | 207 | 172 | 0.2593 × 1.2058 × 0.1517 |
| `shield_round` | Watch Buckler baseline | `ac502d8b193da7b1224139e3b825f856ec9fa15cd5aa6ca10295cc927402c275` | `42da365328fc8804f44cd3db6becc8bc98729f11862b63e51b42820559d6e1e0` | 322 | 284 | 0.8826 × 0.8826 × 0.3319 |
| `staff` | Initiate Staff public-safe baseline | `eb2e277510325a242974ee842453882de2d55b09ec94a43148aff1dd4237c67a` | `acf921e547edd8e05e24e0b2edec74fec7c987ccad1ee73549a0726a8059ebce` | 498 | 440 | 0.5762 × 2.1547 × 0.2923 |
| `wand` | Initiate Wand public-safe baseline | `b726392709e3e7fb4b5157f950500d4a98281503f7cac7d6afdf164578169338` | `db1c466f40f5ae29468e56dff16af6bfd448518f543a5a4ae2638557084473d9` | 158 | 150 | 0.1606 × 0.9661 × 0.1606 |
| `spellbook_open` | Apprentice Focus open-state baseline | `5a7891f8fe12a0c84dfbb64454b0b6b51328894011021e5c664bc8701aaf40ad` | `888e45aff64aba1bea6ef3abfad27024b2697f7adbab319c089bc70c077c4a5b` | 418 | 292 | 0.8282 × 0.5688 × 0.2219 |
| `spellbook_closed` | Apprentice Focus closed-state baseline | `25d118c6f1937476eeb480a0e2f072a87f416e15464f63185325b1e3584124d2` | `4cf311c021d81fd418b8eed01dec8f8af743d8f74dd81722f7c5f42f3ba89633` | 399 | 292 | 0.2930 × 0.5750 × 0.4271 |
| `quiver` | Hunter attachment baseline | `9d209d5e66037e713393f648c7ae8815687c23d63779e40b17d7917d7e45e015` | `47091e8d290ab093560f92b680c0cd83934ea2190e817d51dd4ddb0c7825934e` | 386 | 255 | 0.3054 × 0.9294 × 0.1945 |
| `arrow` | projectile scale baseline | `316d885d2377b8d389edab91df4fc2797264beff317adfd66bc88357d68f6b8c` | `4b12b8c3d0d6a13d50e2f56a07be898d9663052b61ab878659f28875464381f5` | 73 | 52 | 0.1173 × 0.7485 × 0.1016 |

These source-unit sizes are conversion evidence only. They do not dictate Minecraft block scale.

## 2.2 Shared texture evidence

| Texture | Size | SHA-256 |
|---|---:|---|
| `knight_texture.png` | 1024×1024 | `5d250ccc5da020e6126bfa3839f83bd9a465a951ed223e4d13c08b1925e154d4` |
| `rogue_texture.png` | 1024×1024 | `a4032e877c3b91939f5cdbb630349c1998fdbc3211bbd587c111125500fe4cc5` |
| `mage_texture.png` | 1024×1024 | `ea49f094b960402635fe51db9f1864960c97271b17f2e3554e5aca1b2bbba144` |

## 2.3 Admission effect

This closes the following uncertainty for these specific files:

- the path exists at a pinned public CC0 revision;
- the glTF references a real binary mesh rather than a guessed filename;
- exact source bytes and source geometry bounds are now known;
- the files are sufficiently lightweight to remain technically plausible as Minecraft-conversion baselines.

This does **not** yet decide:

- whether the Heartland sword should use this fallback rather than the preferred Quaternius/other grounded candidate;
- final held scale/pivot;
- final item icon crop;
- armor/hand clipping;
- Better Combat attack compatibility;
- final Riverwood Bow, Quarry Maul, River Pike or Mythic model choice.

---

# 3. KayKit Restaurant Bits — exact meal/prop candidate evidence

Upstream:

`KayKit-Game-Assets/KayKit-Restaurant-Bits-1.0`

Pinned revision:

`153c8a7535b48237854cb54ff6890679f8c574d1`

License file:

```text
LICENSE.txt
SHA-256 4da4ad3a3face68a71c11c8adc8e93017fdeb8fd67fcb0ca8286bd9c3dbbf674
Git blob dea6eaef2b7c6df89543e6c911184acdee4b4354
```

Shared texture:

```text
restaurantbits_texture.png
1024 × 1024
SHA-256 1bfff8f880c5e5e1b793bc46fa1f91149b7f70766174a817b3d5962495c5e40f
```

## 3.1 Exact model byte/geometry evidence

| Model | Intended review role | glTF SHA-256 | BIN SHA-256 | Vertices | Triangles | Source-unit size X×Y×Z |
|---|---|---|---|---:|---:|---|
| `food_dinner` | Herbed Louxia Roast current candidate | `d5882d178346c41305152ee9199d7fbee3fa8e34704292e906d13372f374a242` | `cdab092747bd4cdafaf8966e0d510f227e7becd695d71d312a3055fdfa098e47` | 1174 | 1196 | 1.0375 × 0.9078 × 0.9500 |
| `food_stew` | Glow Broth primary bowl candidate | `753f6b30842589c744f959f5ddb90bf07fba4aba30c5d72dc0ba300549b5bff4` | `74cd8cfe414a46bc5308e93e37bb810694f752e9924ec2abcbb165874cb396eb` | 612 | 560 | 0.9500 × 0.3697 × 0.9500 |
| `stew_bowl` | Glow Broth alternate serving-state candidate | `72f77353d1ac4fcac8e21b5480fe1666544b8399590ce5a337b31e9fbc2be338` | `164b0c1a17eb985f8fd0270a6ba12279a93648ec8298a0cb486e079c50c2ceae` | 402 | 304 | 0.7854 × 0.2500 × 0.7854 |
| `stew_pot` | cooking-station prop / pot state | `a8cb68c28917a23bc06f1f1c0773bec00966e7029272e355518bd004ce9d77dc` | `f5201cea5ac83687c2df57192e6166b7cda020b5c30edd28be85d20b78c27875` | 402 | 304 | 0.7370 × 0.2500 × 0.7370 |
| `food_ingredient_steak` | Louxia-meat editable visual source candidate | `651a04027439cb47c814db3b943cc28f10f7093e74695214ba86441d212873f3` | `6196cd9e50e6c2db8fdf0472f0cf031245e898cc4fe98424cfad8b207cedb717` | 354 | 352 | 1.0521 × 0.2832 × 0.9025 |
| `food_ingredient_steak_pieces` | prepared-meat component candidate | `0bf450b3fab9b6eccaebac0b9c182b123a13f3d95039716429d1c9c17df661a5` | `6c8a3aae1abc8f36fac0a673de6f63a220eff2ed34f4e2e22692212abc9ab5b6` | 240 | 264 | 0.6470 × 0.4348 × 0.6228 |
| `food_ingredient_ham_cooked` | cooked-meat silhouette comparison | `ee6657913d75a268a26a14d81651f5db1ad8b599fd42c058106a411f37536834` | `31f9e1d3ec63c65fca72a315acc3a7972cb033f390e31352c8c1861e2c72d9c0` | 220 | 272 | 1.3911 × 0.8300 × 0.8300 |

## 3.2 Admission effect

The R01 meal mapping is now stronger than a filename-only shortlist:

- `food_dinner`, `food_stew`, `stew_bowl` and supporting ingredients are exact, hashed public CC0 source files;
- their mesh complexity is small enough for ordinary meal/prop usage;
- the candidate source family is internally coherent through one shared atlas.

Still open:

- visual recipe-fit: `food_dinner` must actually read as Herbed Louxia Roast after allowed adaptation;
- `food_stew` vs `stew_bowl` final Glow Broth state;
- Trail Skewers remains the Kenney `skewerVegetables` editable-base path until its official source bytes are acquired/reviewed;
- Minecraft held/world scale and inventory-icon render;
- eat animation + hand/prop synchronization.

---

# 4. R01 production consequence

After this pass:

```text
KayKit Adventurers exact selected source bytes: VERIFIED
KayKit Restaurant Bits selected source bytes: VERIFIED
exact file-level SHA-256 for audited rows: VERIFIED
source-unit geometry/count sanity: VERIFIED

R01 exact visual acceptance: NOT COMPLETE
R01 all asset filenames: NOT COMPLETE
R01 fish binary review: NOT COMPLETE
R01 apparel archive review: NOT COMPLETE
R01 VFX/audio final bindings: NOT COMPLETE
R01 asset ready: NO
```

Do not repeat filename discovery for the rows audited here. Their next step is visual conversion/retarget/Minecraft acceptance, not another web search.
