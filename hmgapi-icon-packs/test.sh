#!/bin/sh
set -eu
test -f "${HMGAPI_MOD_BUILD:?}/hooks.jar"
python3 -m json.tool hmgapi-icon-packs/mod.json >/dev/null
python3 -m json.tool hmgapi-icon-packs/settings.json >/dev/null
echo "POC manifest and compiled hooks present"
