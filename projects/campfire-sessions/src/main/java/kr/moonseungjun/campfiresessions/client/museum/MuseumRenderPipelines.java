package kr.moonseungjun.campfiresessions.client.museum;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Render pipelines for immutable Smithsonian-derived museum geometry.
 */
public final class MuseumRenderPipelines {
    /**
     * CFMS v1 stores position(3f), normal(3f), UV0(2f): 32 bytes.
     *
     * <p>The first accepted draw path uses Minecraft's position/texture shader, so
     * the normal bytes remain in the GPU buffer but are skipped by this vertex
     * binding. They stay available for the dedicated normal/occlusion material pass
     * without reconverting or re-uploading geometry.</p>
     */
    public static final VertexFormat STATIC_MESH_BASE_COLOR_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", 24, GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .build();

    public static final RenderPipeline STATIC_MESH_BASE_COLOR = RenderPipeline.builder()
            .withLocation(id("pipeline/museum_static_mesh_base_color"))
            .withVertexShader("core/position_tex")
            .withFragmentShader("core/position_tex")
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, STATIC_MESH_BASE_COLOR_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();

    private static final Map<Identifier, RenderType> BASE_COLOR_TYPES = new HashMap<>();

    private MuseumRenderPipelines() {}

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(STATIC_MESH_BASE_COLOR);
    }

    public static RenderType baseColor(Identifier texture) {
        return BASE_COLOR_TYPES.computeIfAbsent(texture, MuseumRenderPipelines::createBaseColorType);
    }

    private static RenderType createBaseColorType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(STATIC_MESH_BASE_COLOR)
                .withTexture("Sampler0", texture)
                .createRenderSetup();
        return RenderType.create(
                CampfireSessions.MOD_ID + ":museum_static_mesh_base_color/" + texture.toString().replace(':', '/'),
                setup
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, path);
    }
}
