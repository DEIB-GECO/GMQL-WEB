#!/bin/sh

PROJECT_NAME=`grep ^name build.sbt  | sed s"/name.*:=.*\"\(.*\)\"/\1/"`
VERSION=`grep ^version build.sbt  | sed s"/version.*:=.*\"\(.*\)\"/\1/"`
FILE=target/universal/$PROJECT_NAME-$VERSION.zip
echo "VARAIBLES:"
echo $PROJECT_NAME
echo $VERSION
echo $FILE
git config --global user.email "travis@travis-ci.org"
git config --global user.name "Travis CI"
git remote add origin https://${GH_TOKEN}@github.com/DEIB-GECO/GMQL-WEB.git
git push --delete origin $VERSION || true
echo $TRAVIS_TAG
if [ -z "$TRAVIS_TAG" ]; then TRAVIS_TAG=$VERSION; fi
echo $TRAVIS_TAG
ls target/universal/