# HMGAPI Icon Packs

Runtime icon replacement for HMG-API-managed launcher tiles on Hyundai/Kia/Genesis Gen5W systems.

**RC7** was tested on a Kia K5 DL322 and successfully displayed four custom PNG icons. **RC8** is an unverified experiment to replace the factory Setup icon using only existing HMG-API hooks.

## Included icons

- HMG-API — dashboard
- Tile Manager — tiles and checkmark
- Nemesis — settings cog
- Comfort — heated seat

SVG artwork and 64×64 transparent PNG assets are in `hmgapi-icon-packs/icons/`.

## Branch policy

- **main:** current experimental build; GitHub Actions builds this branch
- **backup/rc7-before-cleanup:** frozen, known-good RC7 source and original history
- **cleanup/png-engine:** clean PNG-only baseline for reference

## Downloads

Use [Releases](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/releases) for the **direct installable ZIP**. Builds are experimental and use temporary CI signing keys, not a permanent author signature. GitHub Actions also retains its internal SDK test artifact.

## How it works

The mod preloads PNGs at zygote startup and replaces HMG-API's in-memory launcher icon bytes. RC8 additionally observes factory launcher entries and attempts to replace the Setup drawable via existing mapped hooks. No factory APKs are modified.

See [testing instructions](docs/TESTING.md).
