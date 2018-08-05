#!/bin/zsh
set -e
gradle build
aws cloudformation package --template-file template.yaml --s3-bucket wage_facts_code_bucket --output-template-file packaged-template.yaml
