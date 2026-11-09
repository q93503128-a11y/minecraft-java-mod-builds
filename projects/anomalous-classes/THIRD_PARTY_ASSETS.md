# Asset sources and licenses

| Asset | Author/source | License/use | Modified | Used in |
|---|---|---|---|---|
| Six class card illustrations, v3 | Original project assets created for this mod. Five retain the reviewed v2 scenes; Parasite Morpher uses a new host takeover scene. No external image, model, or font file is included. | Project-created/generated artwork; no third-party asset source | Unified to 512×288; the parasite image is center-cropped without stretching | `src/main/resources/assets/anomalousclasses/textures/gui/class_cards_v3/` |
| Six class emblems, v2 | Original voxel-pixel emblems created for this mod. Background checkerboard was removed and real alpha was applied. | Project-created/generated artwork; no third-party asset source | Transparent 96×96 canvas, alpha-cropped, nearest-neighbor resampling | `src/main/resources/assets/anomalousclasses/textures/gui/class_icons_v2/` |
| Six class skill-icon atlases, v1 | Original per-class 4×4 icon sheets generated for this project with ChatGPT ImageGen; each cell maps to a canonical skill-tree node in node order. | Project-created/generated artwork; no external art source or third-party license dependency | Normalized to 1024×1024 transparent PNG atlases with FFmpeg | `src/main/resources/assets/anomalousclasses/textures/gui/skill_icons_v1/` |
| Shared profile and skill-tree UI textures, v1 | Original frame, blank title plaque, plum surface, button states, node states, connection paths, and rank-progress textures generated for this project with ChatGPT ImageGen. | Project-created/generated artwork; no external asset or font | Normalized to the documented PNG atlas sizes with FFmpeg | `src/main/resources/assets/anomalousclasses/textures/gui/menu_v1/` |
| Previous card art and emblems | Preserved project originals from the earlier visual pass. | Kept for project history; excluded from the runtime JAR | Archived unchanged | `artwork/archive/` |
| Selection frame and controls | Project-created `GuiGraphics` drawing code | Uses Minecraft UI rendering APIs only | Drawn at runtime | `ClassSelectionScreen` |

The original image files supplied/generated for the new artwork are not redistributed as separate source assets; the normalized PNG files above are the project runtime assets. No paid asset, third-party font, or external UI library is used.
