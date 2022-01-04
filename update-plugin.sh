#!/usr/bin/env bash
PLUGIN="MissingInActions"
HOME_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
PLUGIN_JAR=
OLD_PLUGIN=
SANDBOX_NAME=
IDE_VERSION=213
SANDBOX_IDE=

cd "${HOME_DIR}" || exit

echo updating "/Volumes/Pegasus/Data" for latest "${PLUGIN}"
#cp out/artifacts/"${PLUGIN}.jar" "/Volumes/Pegasus/Data"
cp "${PLUGIN}.zip" "/Volumes/Pegasus/Data"

../update-plugin.sh "${HOME_DIR}" "${PLUGIN}" "${PLUGIN_JAR}" "${OLD_PLUGIN}" "${IDE_VERSION}" "${SANDBOX_NAME}" "${SANDBOX_IDE}"
