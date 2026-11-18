# Testing on DL322

## Known-good version

RC7 displayed all four HMG-API-managed custom icons on the Kia K5 DL322. The cleaned PNG-only implementation is not yet verified on the head unit.

1. Open the repository's [GitHub Actions](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/actions) page.
2. Download the test artifact for the desired build; check the source branch and version before installing.
3. Install through the normal HMG-API mod workflow while parked, with a recovery method available.
4. Verify HMG-API, Tile Manager, Nemesis, and Comfort launcher icons.
5. Check logs for `Loaded icon`, `Replaced icon for`, and `PNG icon overrides applied`.
6. Disable or uninstall the mod to restore HMG-API's original icon data.

## Signing

CI packages use disposable signing keys. A future release must use a persistent author signing key (`MOD_KEY`) so updates retain the same identity. No persistent-key release workflow is configured.

## Factory icons

The RC8 branch is an experimental investigation. Factory icon replacement has not been demonstrated, and no system APKs should be modified.
