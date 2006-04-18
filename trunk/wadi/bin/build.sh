#!/bin/sh

##export OFFLINE='--offline'
export TEST='-Dmaven.test.skip=true'
export VERSION=2.0M2-SNAPSHOT
mvn $OFFLINE $TEST clean:clean install && \
cd wadi-assembly && \
mvn $OFFLINE assembly:directory -Ddescriptor=src/main/assembly/bin.xml && \
cd target/wadi-$VERSION-bin/wadi-$VERSION/bin && \
chmod +x ./*.sh
