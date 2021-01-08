#!/usr/bin/env bash

set -e

id="amzn1.ask.skill.e256969a-f018-4ed5-967c-e231ecf51a81"
publish=0

function usage(){
    echo "Usage: $0 [--publish]"
    exit 1
}


function parse_agrs(){
    while [ "$1" != "" ]; do
        case $1 in
            --publish|-p)
                publish="1"
                shift
                ;;
             --manifest|-m)
                manifest="1"
                shift
                ;;
            *)
                usage
                ;;
        esac
    done
}

parse_agrs $@


if [ "$publish" -eq "1" ];then
    successCnt=`ask api get-skill-status -s $id|grep SUCCEEDED|wc -l`
    if [ "$successCnt" -ne 6 ]; then
        echo "Skill is not ready for submit:"
        ask api get-skill-status -s $id
        exit 1
    fi
    ask api submit -s $id
else
    gradle build
    for locale in en-US en-GB en-CA en-AU en-IN
    do
        echo "Updating model for $locale..."
        ask api update-model -s $id -f configuration/model.json -l $locale
    done

    ask api update-skill -s $id -f skill.json
fi
