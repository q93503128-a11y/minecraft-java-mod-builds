# Village Guardians source payload

The source snapshot is stored as Base64 text parts because this shared build repository keeps source inputs reviewable while avoiding committed runtime JARs.

## Reconstruction

Run from the repository root:

```bash
mkdir -p work/village-guardians
cat projects/village-guardians/source/source.tar.xz.b64.part* \
  | base64 --decode \
  > work/village-guardians/source.tar.xz

echo "4fc0c1f550daae0f2e618e7119ef1a02c698b34bf0c2bb57ade6ef6481aaeb6f  work/village-guardians/source.tar.xz" \
  | sha256sum -c -

xz -t work/village-guardians/source.tar.xz
tar -xJf work/village-guardians/source.tar.xz -C work/village-guardians
```

The parts must be concatenated in lexical order from `part00` through `part13`. Do not insert line breaks, edit a part manually, or omit a part. The dedicated GitHub Actions workflow performs the same hash and XZ integrity checks before Gradle is allowed to run.

## Important

- This payload is source input, not an installable mod.
- A `.jar` is valid only after the dedicated workflow completes the real Java 25 / NeoForge build and structural validation.
- Do not commit generated JARs, Gradle caches, Java runtimes, or NeoForge dependency caches here.
