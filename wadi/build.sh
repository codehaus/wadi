#!/bin/sh -x

## until I can work out maven, this will have to do :-(
version=2.0-SNAPSHOT

maven clean-repo
mkdir -p ~/.maven/repository/wadi/jars
rm -f lib/wadi-*-$version.jar

for i in core jetty5 jetty6 tomcat50 tomcat55
do
    pushd modules/$i
    maven -Dmaven.test.skip=true clean jar
    cp target/wadi-$i-$version.jar ~/.maven/repository/wadi/jars/
    cp target/wadi-$i-$version.jar ../../lib
    popd
done

pushd modules/webapp
maven -Dmaven.test.skip=true clean war:war
popd

rm -fr WEB-INF/classes
