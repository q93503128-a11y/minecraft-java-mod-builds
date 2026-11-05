# Yin-Yang Taiji build and runtime report

- Verified source revision: `000bf58cf32457303396ffb611a010e967b70d20` (GitHub Actions Build Test run 42)
- Project version: `test` 0.4.0; mod ID `moon_test`
- Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Java 25
- Build tools: Gradle 9.6.0, Fabric Loom 1.17.20
- Required external animation library: Player Animation Library 1.2.8+26.3 (not bundled)
- Clean build: PASS
- Executable JAR structure and required class/resource checks: PASS
- Gacha code/resource leakage check: PASS
- JAR SHA-256: `86122f17433594727523ea35bccf7643f11ad569bb2d8efc2f146279f822821f`
- Datagen and GameTest: NOT PROVIDED / NOT RUN
- Client and dedicated-server smoke tests: NOT RUN
- 1st-person, 3rd-person, orientation, frame cost, and audio timing visual review: NOT RUN
- Multiplayer synchronization test: NOT RUN
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

The JAR was built and its archive structure verified in GitHub Actions. The current environment did not have a Fabric 26.3 client profile with PAL installed, so no live gameplay or visual inspection is claimed. The skill's projectile collision and impact are presentation-only client effects; it does not damage entities or modify blocks.
