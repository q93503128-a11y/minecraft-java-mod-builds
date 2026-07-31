# Shader Lab Build and Runtime Report

- Source commit: 86845e6bb6e99f97f9f23d7b5da0ec724adbefe0
- Mod version: 0.2.0-alpha.2
- Minecraft: 26.2
- Java: 25
- Gradle: 9.2.1
- NeoForge: 26.2.0.40-beta
- Wrapper generation: success
- Toolchain verification: success
- Clean build: success
- Seven-pass shader resource and JAR verification: success
- Datagen: NOT APPLICABLE
- GameTest: NOT APPLICABLE — GPU post-processing cannot be validated by server GameTest
- Dedicated server smoke test: NOT APPLICABLE — client-only mod
- Interactive client visual test: NOT RUN — requires a real game window and GPU

A successful build proves compilation and JAR structure only. Final visual compatibility is decided by the in-game test.

## JAR verification
```text
Shader Lab JAR verification: PASS
JAR: shaderlab-0.2.0-alpha.2.jar
Bytes: 7907
SHA-256: e325eeaa13d06ec973f83e50b6fcc0924adf0c96a989a161cd958d6fb3c307c0
Required exact entries: 8
Post-effect passes: 7
Pack format range: 88.0 through 107.1
```

## Clean-build log tail
```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 

> Configure project :
Creating Minecraft artifacts without recompilation.

> Task :clean

> Task :createMinecraftArtifacts
Loaded 134 artifacts from /home/runner/work/minecraft-java-mod-builds/minecraft-java-mod-builds/projects/shader-lab/build/tmp/createMinecraftArtifacts/nfrt_artifact_manifest.properties
[1m*** Started working on [4mdownloadManifest[0m[0m
  ↓ https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
 [1m[92m✓[0m Completed [4mdownloadManifest[0m in 0.35s
[1m*** Started working on [4mdownloadJson[0m[0m
 [1m[92m✓[0m Completed [4mdownloadJson[0m in 0.00s
[1m*** Started working on [4mdownloadServer[0m[0m
[1m*** Started working on [4mdownloadClient[0m[0m
 [1m[92m✓[0m Completed [4mdownloadClient[0m in 0.04s
 [1m[92m✓[0m Completed [4mdownloadServer[0m in 0.04s
[1m*** Started working on [4mpreProcessJar[0m[0m
 [1m[92m♻[0m Used cache of [4mpreProcessJar[0m in 0.00s
[1m*** Started working on [4mbinaryPatch[0m[0m
 [1m[92m♻[0m Used cache of [4mbinaryPatch[0m in 0.00s
[1m*** Started working on [4mcopyUnpatchedClasses[0m[0m
 [1m[92m♻[0m Used cache of [4mcopyUnpatchedClasses[0m in 0.00s
[1m*** Started working on [4mapplyDevTransforms[0m[0m
 [1m[92m♻[0m Used cache of [4mapplyDevTransforms[0m in 0.00s
Total runtime: 1.07s


> Task :compileJava FROM-CACHE
> Task :generateModMetadata
> Task :processResources
> Task :classes
> Task :jarJar NO-SOURCE
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :verifyShaderResources
> Task :check
> Task :build
> Task :writeJarChecksum
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1785482480820.json

BUILD SUCCESSFUL in 14s
8 actionable tasks: 7 executed, 1 from cache
```
