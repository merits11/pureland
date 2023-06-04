#!/bin/zsh
set -e
if [ $# -ne 1 ]; then
    echo "Usage: $0 <stage>"
    exit 1
fi
JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home
gradle6=/usr/local/opt/gradle@6/bin/gradle

stage="$1"
$gradle6 clean
$gradle6 build
#echo "Creating dynamo DB stack ..."
#aws cloudformation deploy --template-file dynamo.yaml --stack-name "PureLandDynamo${stage}" --capabilities CAPABILITY_IAM --parameter-overrides TableNameSuffix=${stage}
echo "Updating Lambda stack ..."
aws cloudformation package --template-file template.yaml --s3-bucket purelandcodebucket --output-template-file packaged-template.yaml
aws cloudformation deploy --template-file packaged-template.yaml --stack-name "PureLand${stage}" --capabilities CAPABILITY_IAM --parameter-overrides FunctionNameSuffix=${stage}
echo "Publishing function PureLandMusic-${stage}"
aws lambda publish-version --function-name  "PureLandMusic-${stage}" --description "Published at $(date)"
#aws lambda publish-version --function-name  "PureLandMusic-Alpha" --description "Published at $(date)"


