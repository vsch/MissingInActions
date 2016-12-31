#!/usr/bin/env bash
HOME_DIR="/Users/vlad/src/MissingInActions"
PLUGIN="MissingInActions"

cd ${HOME_DIR}

# installed versions on selected products to latest
function UpdProduct() {
    for PRODUCT in "$@"
    do
        if [ -d /Users/vlad/Library/"Application Support"/${PRODUCT} ]; then
            echo updating ${PRODUCT} for latest ${PLUGIN}
            rm -fr /Users/vlad/Library/"Application Support"/${PRODUCT}/${PLUGIN}
            unzip -bq ${PLUGIN}.zip -d /Users/vlad/Library/"Application Support"/${PRODUCT}
        else
            echo product ${PRODUCT} does not exist in /Users/vlad/Library/"Application Support"/
        fi
    done
}

function UpdJar() {
    ZIP=$1
    for PRODUCT in ${2:+"$@"}
    do
        if [ -d /Users/vlad/Library/"Application Support"/${PRODUCT} ]; then
            echo updating ${PRODUCT} jar for ${ZIP}
            rm -fr /Users/vlad/Library/"Application Support"/${PRODUCT}/${ZIP}
            mkdir -p /Users/vlad/Library/"Application Support"/${PRODUCT}/${ZIP}/lib
            cp ${ZIP}.jar /Users/vlad/Library/"Application Support"/${PRODUCT}/${ZIP}/lib
        else
            echo product ${PRODUCT} does not exist in /Users/vlad/Library/"Application Support"/
        fi
    done
}

function UpdZip() {
    ZIP=$1
    for PRODUCT in ${2:+"$@"}
    do
        if [ -d /Users/vlad/Library/"Application Support"/${PRODUCT} ]; then
            echo updating ${PRODUCT} zip for ${ZIP}
            rm -fr /Users/vlad/Library/"Application Support"/${PRODUCT}/${ZIP}
            unzip -bq ${ZIP}.zip -d /Users/vlad/Library/"Application Support"/${PRODUCT}
        else
            echo product ${PRODUCT} does not exist in /Users/vlad/Library/"Application Support"/
        fi
    done
}

#UpdProduct "IdeaIC16" "IntelliJIdea16" "IdeaIC15" "IntelliJIdea15" "WebIde100" "WebIde110"
#UpdJar CodeGlance "IdeaIC16" "IntelliJIdea16" "IdeaIC15" "IntelliJIdea15" "RubyMine80" "PyCharm40" "PyCharm50" "Webide100" "Webide110"
UpdProduct "IdeaIC2016-3" "IdeaIC2017-1" "IdeaIC2016.3" "IdeaIC2017.1" "IntelliJIdea2016.3" "IntelliJIdea2016.3.eap" "IntelliJIdea2017.1" "PhpStorm2016.3" "PhpStorm2016.3.eap"

