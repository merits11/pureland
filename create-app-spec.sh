#!/bin/bash
set -e
echo "Update AppSpec..."
app_spec="build/appspec.yaml"
current_version=$(aws lambda get-alias --function-name PureLandMusic-Beta --name live |jq -r '.FunctionVersion')
(( target_version = current_version + 1 ))
echo "TargetVersion is calculated as $target_version"
cat > "$app_spec" <<- EOM
version: 0.0
Resources:
  - myLambdaFunction:
      Type: AWS::Lambda::Function
      Properties:
        Name: " PureLandMusic-Beta"
        Alias: "live"
        CurrentVersion: "$current_version"
        TargetVersion: "$target_version"

EOM
