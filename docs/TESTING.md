# First DL322 test

Current RC4 attempts runtime icon replacement for four mod tiles and logs drawable hook callbacks. It does not modify factory APKs.

1. Open the repository's Actions tab and inspect the latest **Build and test HMGAPI Icon Packs** run.
2. Download the **HMGAPI-Icon-Packs-<version>** artifact. This CI package is signed with a **throwaway test key**, not a persistent author key. It is for an initial isolated test only; a future release signed with another key will not update over it.
3. Install using HMG-API's normal mod installation route, while parked, with a known-good recovery procedure.
4. Restart the head unit if HMG-API asks. Check the mod logs for `DIAG BEFORE`, `DIAG AFTER`, and `REPLACED` entries. Capture any error messages.
5. Turn the mod off or remove it to return to the previous state.

Next milestone: establish whether HMG-API's synthetic drawable resolver permits downstream overrides.

## Signing for stable releases

HMG-API mod packages use **author signing**, not Android APK signing. The SDK includes `hmgapi.engine.Signing` for key generation and `mkmodpkg.sh` for signed packages. Keep a permanent private key outside GitHub and provide it to a protected GitHub Actions release environment as `MOD_KEY` (base64 of the PKCS#8 private key). Commit only its public half. Never use CI's throwaway key for public releases. Once an author key signs a mod, updates must use the same key.

A persistent-key release workflow is not configured yet.
