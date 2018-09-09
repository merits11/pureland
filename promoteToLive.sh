#!/usr/bin/env bash
cp configuration/purelandhelp.html  /Users/junmao/Dropbox/Alexa/purelandhosting/
aws lambda update-alias --function-name  "PureLandMusic-Beta" --name live --function-version $1
