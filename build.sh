#!/bin/zsh
set -e
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
gradle build
gradle jacocoTestReport

echo "Coverage report:  build/jacocoHtml/index.html"
