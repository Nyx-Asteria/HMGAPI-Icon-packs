# HMGAPI Icon Packs

## RC9 — integrated Kia / Hyundai theme preview

RC9 adds a **Hyundai icon pack** toggle in HMG-API mod settings. Off = Kia, on = Hyundai. The setting applies on rebuild/restart; it is not a live theme switch.

- Replaces six HMG-API-managed mod icons: HMG-API, Tile Manager, NEMESIS, Comfort, Button Mapper, and Brake Indicator.
- Uses existing `launcher.menu_entry.new`, `launcher.menu_list.load`, and `framework.resources.get_drawable` points to identify and replace recognized factory icons.
- Distinguishes Setup vs User's Manual and Map vs Navigation Menu by activity name.
- No factory APK modifications or new HMG-API points.
- Preserves the four tested RC7 Kia PNG assets; new plugin icons and factory artwork are generated vector previews at runtime.
- Hyundai uses blue/cyan/green gradients on **transparent backgrounds**; Kia uses cyan/violet/magenta. Setup uses the cog rather than the RC8 test checkmark.

**Important:** This is an *experimental integrated build*, not a pixel-perfect export of the approved concept sheets. Factory coverage depends on matching entry identities and the drawable resource owner. Unmatched entries retain their stock icons. The Hyundai style is an original interpretation, not extracted OEM artwork.

The last head-unit-confirmed factory replacement is RC8; the frozen RC7 fallback is at `backup/rc7-before-cleanup`.

[Download test builds from Releases](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/releases). CI uses temporary signatures.
