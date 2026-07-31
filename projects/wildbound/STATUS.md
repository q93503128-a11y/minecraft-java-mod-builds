# Wildbound build status

Last updated: `2026-07-31`

## Current state

- Project contract: REGISTERED
- Shared-repository isolation rules: DOCUMENTED
- Java 25 GitHub Actions workflow: ADDED
- Original source archive inspected locally: YES
- Cleaned source baseline prepared: YES
- Source transport parts committed to this repository: PENDING
- GitHub-hosted dependency resolution: NOT RUN
- GitHub-hosted `clean build`: NOT RUN
- `BUILD SUCCESSFUL`: NOT CONFIRMED
- Validated `wildbound-1.8.1.jar`: NOT AVAILABLE
- In-game client test: NOT RUN

## Next required action

Commit the exact split source transport files documented in `source/README.md`, verify their combined SHA-256, and then run `.github/workflows/build-wildbound.yml`.

Any worker receiving only this repository URL must not claim Wildbound is built merely because `PROJECT.md` and the workflow exist. The real completion point is a successful Java 25 Actions run with a structurally validated JAR artifact.

## Do not do

- Do not replace Wildbound with a smaller reimplementation.
- Do not borrow or overwrite files from Countryside Days, Village Guardians, or another project.
- Do not commit a renamed source ZIP as a JAR.
- Do not mark the build complete without the real artifact.
