# HMGAPI Icon Packs

Runtime PNG icon replacement for HMG-API-managed launcher tiles on Hyundai/Kia/Genesis Gen5W systems.

**Known-good baseline:** RC7 was tested on a Kia K5 DL322 and displayed custom icons for HMG-API, Tile Manager, Nemesis, and Comfort. This cleanup branch simplifies the implementation and has **not yet been tested on the car**.

## Current icons

- HMG-API — dashboard
- Tile Manager — tiles and checkmark
- Nemesis — settings cog
- Comfort — heated seat

Editable SVG source artwork and 64×64 transparent PNG assets are under `hmgapi-icon-packs/icons/`.

## How it works

The mod preloads PNG files in `zygoteInit` and replaces the icon byte arrays on HMG-API's in-memory launcher entries when `com.android.launcher3` loads. It does **not** patch system APKs or modify factory resources.

This mechanism is proven for HMG-API-managed entries only. Replacing factory Kia/Hyundai icons is a separate experiment.

## Building and testing

GitHub Actions uses the pinned HMG-API SDK and publishes temporary-key-signed test artifacts. See [testing instructions](docs/TESTING.md). A stable signing identity is not configured; temporary CI signatures should not be treated as production releases.

## Development

- `main`: tested RC7 baseline until cleanup is validated
- `backup/rc7-before-cleanup`: immutable recovery point for pre-cleanup history
- `cleanup/png-engine`: streamlined PNG-only engine (requires validation)
- `experiment/rc8-factory-setup`: factory Setup icon investigation
