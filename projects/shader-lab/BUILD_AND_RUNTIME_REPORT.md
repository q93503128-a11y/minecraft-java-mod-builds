# Shader Lab Build and Runtime Report

- Source commit: 6d7253ddb06903555067dff03290be763a3a923b
- Mod version: 0.1.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- Wrapper generation: success
- Toolchain verification: success
- Clean build: cancelled
- Shader resource and JAR verification: skipped
- Datagen: NOT APPLICABLE
- GameTest: NOT APPLICABLE — GPU post-processing cannot be validated by server GameTest
- Dedicated server smoke test: NOT APPLICABLE — client-only mod
- Interactive client visual test: NOT RUN — requires a real game window and GPU

A successful build proves compilation and JAR structure only. Final visual compatibility is decided by the in-game test.

## Clean-build log tail
```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.2.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 

> Configure project :
Creating Minecraft artifacts without recompilation.

> Task :clean
```
