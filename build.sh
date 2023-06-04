#!/bin/zsh
set -e
gradle6=/usr/local/opt/gradle@6/bin/gradle

$gradle6 build
$gradle6 jacocoTestReport

echo "Coverage report:  build/jacocoHtml/index.html"
