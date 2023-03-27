#!/usr/bin/env bash
PLUGIN="MissingInActions"
HOME_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
PLUGIN_JAR=
OLD_PLUGIN=
SANDBOX_NAME="plugins-sandbox-mn"
SANDBOX_IDE=
IDE_LIST=(
# 1.8.193.4
#  191
#  192
#  193
#  201
#  202
#  203
#  211

# 1.8.223.19
# 1.8.212.4
 212
 213

# 1.8.214.6+
  221
  222
  223
  231
  232
  233
)                                   

../update-plugin.sh "${HOME_DIR}" "${PLUGIN}" "${PLUGIN_JAR}" "${OLD_PLUGIN}" "${SANDBOX_NAME}" "${SANDBOX_IDE}" "${IDE_LIST[@]}"
