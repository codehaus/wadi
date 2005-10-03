#!/bin/sh -x

flags="-o"

## until I can work out maven, this will have to do :-(
version=DUMMY
version=2.0-SNAPSHOT

maven $flags clean-repo
mkdir -p ~/.maven/repository/wadi/jars
rm -f lib/wadi-*-$version.jar

for i in core jetty5 jetty6 tomcat50 tomcat55
do
    pushd modules/$i
    maven $flags clean
    maven $flags
    cp target/wadi-$i-$version.jar ~/.maven/repository/wadi/jars/
    cp target/wadi-$i-$version.jar ../../lib
    popd
done

pushd modules/webapp
maven $flags clean
maven $flags
mkdir -p ~/.maven/repository/wadi/wars
cp target/wadi-webapp-$version.war ~/.maven/repository/wadi/wars/
popd

pushd modules/assembly
maven $flags dist:prepare-bin-filesystem
popd

rm -fr WEB-INF/classes
