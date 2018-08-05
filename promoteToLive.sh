#!/usr/bin/env bash

aws lambda update-alias --function-name  "PureLandMusic-Beta" --name live --function-version $1
