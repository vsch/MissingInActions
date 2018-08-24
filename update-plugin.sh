#!/usr/bin/env bash
HOME_DIR="/Users/vlad/src/projects/MissingInActions"
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
    for PRODUCT in ${@:2}
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
    for PRODUCT in ${@:2}
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

UpdProduct "IdeaIC2018-1-EAP" "IdeaIC2018-2-EAP" "IdeaIC2017.3" "IdeaIC2018.1" "IdeaIC2018.2" "IntelliJIdea2018.1" "IntelliJIdea2018.2" "PhpStorm2018.1" "PhpStorm2018.2" "WebStorm2018.1" "WebStorm2018.2" "CLion2018.1" "CLion2018.2"

