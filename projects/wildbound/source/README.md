# Wildbound source archive transport

This directory stores the exact cleaned Wildbound CI source baseline as split Base64 text files.

Expected files:

```text
source-min.tar.xz.b64.part00
source-min.tar.xz.b64.part01
...
```

Reconstruction on Linux:

```bash
cat source-min.tar.xz.b64.part* | base64 --decode > source-min.tar.xz
echo "1832e3458319d0ec0f626fc9be92c0532416afe9b0c772ba3668e5a8371c8524  source-min.tar.xz" | sha256sum -c -
tar -xJf source-min.tar.xz
```

Rules:

- Keep part names zero-padded so lexical order is reconstruction order.
- Do not edit, wrap, reformat, or normalize a part file.
- The combined archive must match the recorded SHA-256 before extraction.
- This transport is project-specific. Never concatenate parts from another mod.
- A new Wildbound source baseline must use a new recorded hash and update `../PROJECT.md` and the workflow together.
- Do not place secrets, local credentials, private logs, Minecraft game files, or unlicensed resources in the archive.

The archive contains the source tree, not a finished mod JAR. Completion still requires an actual Java 25 Gradle build and final JAR validation in GitHub Actions.