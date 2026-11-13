# Class management design review

These ten bilingual images are offline layout reviews for the five class management screens whose server systems are not present yet. They are **not in-game screens** and are not registered or loaded by the mod.

The layouts use the existing class card art and icons, `menu_v1` surface, title plate, button skin, frame corners, rails, and ornaments. The outer frame PNGs are read as-is; this review did not modify them. Empty values use an em dash and the review badge stays visible so no placeholder can be mistaken for live game state. The swarm layout starts with no saved tactics and has no built-in preset.

Files are named `<class>_<ko|en>.jpg`; `class_management_review_contact_sheet.jpg` is a quick overview. Regenerate them from the project root with:

```powershell
java tools/RenderClassManagementDesignPreviews.java src/main/resources/assets/anomalousclasses/textures/gui ui/reviews/2026-11-13/class-management-design-review
```

This Java2D composition is useful for reviewing information hierarchy and asset reuse. It does not reproduce Minecraft's `GuiGraphics`, font renderer, GUI scaling, or GPU sampling and cannot substitute for a client screenshot.
