#!/bin/zsh
set -e
gradle build
gradle jacocoTestReport

echo "Coverage report:  build/jacocoHtml/index.html"
