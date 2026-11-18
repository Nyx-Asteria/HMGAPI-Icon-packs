# First DL322 test

The first build is **observation-only**. It never replaces launcher icons or modifies factory files.

1. Open the repository's Actions tab and inspect the **Build and test HMGAPI Icon Packs** run.
2. Download the **mod-test-packages** artifact. This CI package is signed with a **throwaway test key**, not a persistent author key. It is for an initial isolated test only; a future release signed with another key will not update over it.
3. Install using HMG-API's normal mod installation route, while parked, with a known-good recovery procedure.
4. Restart the head unit if HMG-API asks. In the HMG-API mod logs, find `entry package=` and note the icon resource IDs for `hmgapi.app`, `dev.lemoyne.comfort`, `dev.lemoyne.tiles`, and `com.nemesis.manager`.
5. Turn the mod off or remove it to return to the previous state.

Next milestone: add optional icon replacement after confirming the resource mapping and how HMG-API's existing synthetic drawable IDs are resolved.

## Signing for stable releases

HMG-API mod packages use **author signing**, not Android APK signing. The SDK includes `hmgapi.engine.Signing` for key generation and `mkmodpkg.sh` for signed packages. Keep a permanent private key outside GitHub and provide it to a protected GitHub Actions release environment as `MOD_KEY` (base64 of the PKCS#8 private key). Commit only its public half. Never use CI's throwaway key for public releases. Once an author key signs a mod, updates must use the same key.

A persistent-key release workflow will be added after the first build passes and the author key is generated securely.
