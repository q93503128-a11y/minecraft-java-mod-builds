# Shader Lab Build and Runtime Report

- Source commit: 481348ad17a373876e86a7b16bc1e4a67d9d3080
- Mod version: 0.1.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- Wrapper generation: success
- Toolchain verification: success
- Clean build: success
- Shader resource and JAR verification: success
- Datagen: NOT APPLICABLE
- GameTest: NOT APPLICABLE — GPU post-processing cannot be validated by server GameTest
- Dedicated server smoke test: NOT APPLICABLE — client-only mod
- Interactive client visual test: NOT RUN — requires a real game window and GPU

A successful build proves compilation and JAR structure only. Final visual compatibility is decided by the in-game test.

## JAR verification
```text
Shader Lab JAR verification: PASS
JAR: shaderlab-0.1.0-alpha.1.jar
Bytes: 6052
SHA-256: 057271b4eaf4852307004914c4723171392fa13ed57cdb5478e9bd0eac65e43d
Required exact entries: 6
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
 [1m[92m✓[0m Completed [4mdownloadManifest[0m in 0.49s
[1m*** Started working on [4mdownloadJson[0m[0m
 [1m[92m✓[0m Completed [4mdownloadJson[0m in 0.00s
[1m*** Started working on [4mdownloadServer[0m[0m
[1m*** Started working on [4mdownloadClient[0m[0m
 [1m[92m✓[0m Completed [4mdownloadServer[0m in 0.04s
 [1m[92m✓[0m Completed [4mdownloadClient[0m in 0.04s
[1m*** Started working on [4mpreProcessJar[0m[0m
 [1m[92m♻[0m Used cache of [4mpreProcessJar[0m in 0.00s
[1m*** Started working on [4mbinaryPatch[0m[0m
 [1m[92m♻[0m Used cache of [4mbinaryPatch[0m in 0.00s
[1m*** Started working on [4mcopyUnpatchedClasses[0m[0m
 [1m[92m♻[0m Used cache of [4mcopyUnpatchedClasses[0m in 0.00s
[1m*** Started working on [4mapplyDevTransforms[0m[0m
 [1m[92m♻[0m Used cache of [4mapplyDevTransforms[0m in 0.00s
Total runtime: 1.24s


> Task :compileJava
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1785479820902.json

BUILD SUCCESSFUL in 16s
8 actionable tasks: 8 executed
```
