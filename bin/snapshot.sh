#!/bin/sh

## build snapshot artefacts and assemblies
## deploy artefacts to remote snapshot repository
## copy assemblies to remote dist directory

snapshots=https://dav.codehaus.org/snapshots.repository
#snapshots=codehaus.org:/home/projects/wadi/repository-snapshot
#snapshots=codehaus.org:/home/projects/wadi/repository-snapshot/test
distributions=https://dav.codehaus.org/snapshots.dist/wadi/
#distributions=/home/projects/wadi/dist/distributions/test
suffixes="tar.gz tar.bz2 zip"

## TODO: we should make a fresh checkout before attempting this...
## TODO: what about src distros ?
## TODO: do we need checksums ?

mvn clean:clean install && \
cd wadi-assembly && \
mvn assembly:assembly -Ddescriptor=src/main/assembly/bin.xml && \
cd .. && \
mvn deploy && \
version=`cat ./wadi-assembly/target/VERSION.txt` && \
tmpfile=/tmp/wadi-snapshot-$$.xml && \
scp $snapshots/org/codehaus/wadi/wadi/$version/maven-metadata.xml $tmpfile && \
timestamp=`grep timestamp $tmpfile | sed -e 's/.*>\(.*\)<.*/\1/g'` && \
buildnumber=`grep buildNumber $tmpfile | sed -e 's/.*>\(.*\)<.*/\1/g'` && \
uid="$timestamp-$buildnumber" && \
rm -f $tmpfile && \
cd wadi-assembly/target/ && \
for i in $suffixes; do scp wadi-$version-bin.$i codehaus.org:$distributions/wadi-$version-$uid-bin.$i || exit $?; done && \
ssh codehaus.org "cd $distributions; for i in $suffixes; do rm -f wadi-$version-bin.\$i && ln -s wadi-$version-$uid-bin.\$i wadi-$version-bin.\$i || exit \$?; done; ls -l"
