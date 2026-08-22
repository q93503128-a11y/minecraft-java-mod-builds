package kr.moonseungjun.survivalascension.mining;

/*
 * Ore-family equivalence matching is adapted from Veinminer++.
 * Copyright (c) 2026 Kestalkayden. MIT License.
 * See THIRD_PARTY_NOTICES.md and META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt.
 */

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

final class OreVeinMatcher {
    private static TagKey<Block> vanillaTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }

    private static final List<TagKey<Block>> ORE_FAMILIES = List.of(
            vanillaTag("coal_ores"),
            BlockTags.IRON_ORES,
            BlockTags.COPPER_ORES,
            BlockTags.GOLD_ORES,
            vanillaTag("redstone_ores"),
            vanillaTag("emerald_ores"),
            vanillaTag("lapis_ores"),
            vanillaTag("diamond_ores")
    );

    private final Block originBlock;
    private final List<TagKey<Block>> originGroups;

    private OreVeinMatcher(Block originBlock, List<TagKey<Block>> originGroups) {
        this.originBlock = originBlock;
        this.originGroups = originGroups;
    }

    static OreVeinMatcher forOrigin(BlockState originState) {
        List<TagKey<Block>> groups = new ArrayList<>();
        for (TagKey<Block> tag : ORE_FAMILIES) {
            if (originState.is(tag)) groups.add(tag);
        }
        return new OreVeinMatcher(originState.getBlock(), groups);
    }

    boolean matches(BlockState state) {
        if (state.is(originBlock)) return true;
        for (TagKey<Block> tag : originGroups) {
            if (state.is(tag)) return true;
        }
        return false;
    }
}
