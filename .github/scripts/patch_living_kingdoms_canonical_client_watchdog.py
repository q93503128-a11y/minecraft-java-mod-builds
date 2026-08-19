from pathlib import Path

path = Path('.github/workflows/build-living-kingdoms.yml')
text = path.read_text(encoding='utf-8')
start_marker = '      - name: Graphical UI and client startup audit\n'
end_marker = '      - name: Package internal alpha.12 checkpoint\n'
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('canonical client audit step boundaries not found')

replacement = '''      - name: Graphical UI and client startup audit\n        working-directory: ${{ env.PROJECT_DIR }}\n        shell: bash\n        env:\n          LIBGL_ALWAYS_SOFTWARE: '1'\n        run: |\n          set -euo pipefail\n          sudo apt-get update -qq\n          sudo apt-get install -y --no-install-recommends xvfb libgl1-mesa-dri >/dev/null\n          log=../../logs/client-smoke.log\n          : > "$log"\n\n          # Run the graphical client in its own process group. The CI diagnostics finish after\n          # ~216 client ticks, but Gradle/Xvfb descendants can outlive the Java process on hosted\n          # runners. Poll the authoritative PASS markers and terminate the whole process group as\n          # soon as the graphical proof is complete, with a hard-kill fallback.\n          setsid xvfb-run -a -s '-screen 0 1280x720x24' \\\n            gradle --no-daemon runClient --stacktrace --console=plain \\\n            > "$log" 2>&1 &\n          client_group=$!\n          outcome=timeout\n          for attempt in $(seq 1 240); do\n            if grep -Fq 'LK_CLIENT_DIAGNOSTIC_PASS screen=origin_selection fixed_erden_origin=true' "$log" \\\n              && grep -Fq 'LK_CLIENT_LOADING_DIAGNOSTIC_PASS screen=realm_loading non_pausing=true' "$log" \\\n              && grep -Fq 'LK_CLIENT_CODEX_DIAGNOSTIC_PASS screens=overview,equipment,map,skills' "$log"; then\n              outcome=pass\n              break\n            fi\n            if grep -Eqi 'Caught exception from Living Kingdoms|Error during client initialization|Exception in client tick loop|Rendering screen|Missing textures in model livingkingdoms|IllegalStateException' "$log"; then\n              outcome=fatal\n              break\n            fi\n            if ! kill -0 "$client_group" 2>/dev/null; then\n              outcome=client_exit\n              break\n            fi\n            sleep 1\n          done\n\n          kill -TERM -- "-$client_group" 2>/dev/null || true\n          sleep 3\n          kill -KILL -- "-$client_group" 2>/dev/null || true\n          wait "$client_group" 2>/dev/null || true\n\n          cat "$log"\n          echo "CLIENT_AUDIT_OUTCOME=$outcome"\n          test "$outcome" = pass\n          grep -F 'LK_CLIENT_DIAGNOSTIC_PASS screen=origin_selection fixed_erden_origin=true' "$log"\n          grep -F 'LK_CLIENT_LOADING_DIAGNOSTIC_PASS screen=realm_loading non_pausing=true' "$log"\n          grep -F 'LK_CLIENT_CODEX_DIAGNOSTIC_PASS screens=overview,equipment,map,skills' "$log"\n          if grep -Eqi 'Caught exception from Living Kingdoms|Error during client initialization|Exception in client tick loop|Rendering screen|Missing textures in model livingkingdoms' "$log"; then\n            exit 1\n          fi\n\n'''

if 'CLIENT_AUDIT_OUTCOME=$outcome' in text:
    print('Living Kingdoms canonical client watchdog already installed')
else:
    text = text[:start] + replacement + text[end:]
    path.write_text(text, encoding='utf-8')
    print('Living Kingdoms canonical graphical client audit now uses a bounded process-group watchdog')
