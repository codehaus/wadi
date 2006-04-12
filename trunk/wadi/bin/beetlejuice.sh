#!/bin/sh

## an integration script for the BeetleJuice ci system.

export JAVA_HOME=/usr/local/java-1.4
export M2_HOME=/usr/local/maven-2.0
export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH

echo "***** Starting BeetleJuice build *****"
echo
echo "system is `uname -a`"
echo "shell is $SHELL"
echo "user is `id`"
type java
java -fullversion
type mvn
mvn -version
echo "cwd is `pwd`"
echo
mvn  -Duser.home=. clean:clean && \
mvn  -Duser.home=. package
echo
echo "***** BeetleJuice build completed *****"

