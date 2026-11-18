# DL322 testing

## Fallback

RC7 is the last head-unit-verified build. Its original source and history are preserved at `backup/rc7-before-cleanup`.

## Current test (RC8)

1. Download the installable `hmgapi-icon-packs-1.0.0-rc.8.zip` directly from [Releases](https://github.com/Nyx-Asteria/HMGAPI-Icon-packs/releases) after the workflow succeeds.
2. Install via HMG-API while parked, with a known recovery method.
3. Verify HMG-API, Tile Manager, Nemesis, and Comfort still show custom icons.
4. Look for a purple/magenta Setup icon with a white gear and green checkmark.
5. Collect logs containing `RC8 Setup candidate`, `RC8 Setup drawable replaced`, and `RC8 menu loaded`.
6. If the factory icon does not change, keep the logs: the probe may reveal which icon IDs were observed.

RC8 uses only existing HMG-API points; it does not modify system APKs.

## Signing

CI uses a temporary signing key for every build. These packages are not stable releases and may not update in place across different runs. A permanent author key and release signing workflow are not yet configured.
