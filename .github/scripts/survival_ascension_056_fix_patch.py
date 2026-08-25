from pathlib import Path

fix_path = Path(__file__)
patch_path = fix_path.with_name("survival_ascension_056_patch.py")
text = patch_path.read_text(encoding="utf-8")

old = "from textwrap import dedent\n"
new = '''from textwrap import dedent as _textwrap_dedent

def dedent(text: str) -> str:
    # Java source anchors intentionally start with their real class indentation.
    # Documentation/test payloads start at column zero and still use normal dedent.
    return text if text[:1].isspace() else _textwrap_dedent(text)
'''
if text.count(old) != 1:
    raise SystemExit(f"expected one dedent import, got {text.count(old)}")
text = text.replace(old, new, 1)

old_player = '''        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer ? sourcePlayer : null;
        if (player == null && rangedShot && event.getEntity().level() instanceof ServerLevel hitLevel) {
            player = AscensionAffixes.rangedProjectileOwner(direct, hitLevel);
        }
        if (player == null || event.getEntity() == player || event.getAmount() <= 0.0F) return;
'''
new_player = '''        ServerPlayer directSourcePlayer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer ? sourcePlayer : null;
        ServerPlayer player = directSourcePlayer != null ? directSourcePlayer
                : rangedShot && event.getEntity().level() instanceof ServerLevel hitLevel
                ? AscensionAffixes.rangedProjectileOwner(direct, hitLevel) : null;
        if (player == null || event.getEntity() == player || event.getAmount() <= 0.0F) return;
'''
if text.count(old_player) != 1:
    raise SystemExit(f"expected one mutable incoming-damage player block, got {text.count(old_player)}")
text = text.replace(old_player, new_player, 1)

old_audit = '"player = AscensionAffixes.rangedProjectileOwner(direct, hitLevel)",'
new_audit = '"AscensionAffixes.rangedProjectileOwner(direct, hitLevel)",'
if text.count(old_audit) != 1:
    raise SystemExit(f"expected one incoming-damage audit token, got {text.count(old_audit)}")
text = text.replace(old_audit, new_audit, 1)

cleanup = "Path(__file__).unlink()\nprint(\"Survival Ascension 0.56 ranged attribution patch applied; staging hook removed\")"
cleanup_new = "(ROOT / '.alpha56-trigger').unlink(missing_ok=True)\nPath(__file__).unlink()\nprint(\"Survival Ascension 0.56 ranged attribution patch applied; staging hook/trigger removed\")"
if text.count(cleanup) != 1:
    raise SystemExit(f"expected one patch cleanup block, got {text.count(cleanup)}")
text = text.replace(cleanup, cleanup_new, 1)

compile(text, str(patch_path), "exec")
patch_path.write_text(text, encoding="utf-8")
fix_path.unlink()
print("0.56 staging anchors/effectively-final attribution/trigger cleanup repaired; fixer self-removed")
