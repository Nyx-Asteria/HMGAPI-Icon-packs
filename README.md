# HMGAPI Icon Packs

Experimental launcher icon replacement for Hyundai/Kia/Genesis Gen5W using [HMG-API](https://github.com/Open-HMG/HMG-API).

**Current version:** 1.0.0-rc.4 (prerelease; diagnostic testing on DL322).

## Included icon designs

- HMG-API — dashboard
- Tile Manager — three boxes and a checkmark
- Nemesis Shortcut — settings cog
- Comfort — heated seat

Icons are rendered as gradient drawables at runtime. The factory APKs are not modified.

## Builds

Open [GitHub Actions](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/actions). Successful builds have a final **Download test package** job, which publishes an artifact named `HMGAPI-Icon-Packs-<version>` and a link in the run summary. Test artifacts are signed with **temporary keys**; they are not persistent release signatures.

See [DL322 testing instructions](docs/TESTING.md). The drawable override is experimental and may not execute because of HMG-API core hook ordering.
