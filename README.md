# HMGAPI Icon Packs

## RC10 — selectable PNG icon packs

The mod provides two themes for the six HMG-API launcher tiles: **Kia** and **Hyundai**. All twelve plugin icons are separate PNG files stored under `hmgapi-icon-packs/icons/kia/` and `hmgapi-icon-packs/icons/hyundai/`. They are loaded during zygote initialization and applied to the HMG-API launcher entries in memory.

In HMG-API mod settings, turn **Hyundai icon pack** on for Hyundai, or off for Kia (default). Rebuild/restart for changes to take effect. **Enable icon replacements** disables all replacements when off.

Plugin tiles: HMG-API, NEMESIS, Tile Manager, Comfort, Button Mapper, and Brake Indicator.

Factory icons are handled separately through existing launcher hooks and runtime Canvas drawing; factory APKs are never modified. The factory replacements are experimental and depend on the firmware's entry identities and drawable resources. Unmatched icons remain unchanged.

The `backup/rc7-before-cleanup` branch preserves the known-working four-icon fallback. CI test packages use temporary signatures.

[Releases](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/releases)
