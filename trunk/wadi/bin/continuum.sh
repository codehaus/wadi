#!/bin/sh

## an integration script for various ci systems.

downloadSources=false
help=false

while [ -n "$1" ]
do
  case $1 in
      -jdk)
      shift
      echo "selecting jdk $1"
      case $1 in
	  1.4)
	  export JAVA_HOME=/usr/local/java/sun-j2sdk1.4.2_08
	  ;;
	  1.5|5|5.0)
	  export JAVA_HOME=/usr/local/java/sun-jdk1.5.0_07
	  ;;
	  *)
	  echo "parameter not recognised: $1"
	  echo "jdk 1.4, 5 and 5.0 supported"
	  ;;
      esac
      ;;
      -sources)
      echo "enabling source jar download (slow)"
      downloadSources=true
      ;;
      -help)
      help=true
      break
      ;;
      *)
      echo "parameter not recognised: $1"
      help=true
      break
      ;;
  esac
  shift
done

if [ "$help" == true ]
then
    echo
    echo "usage: $0 [-help] [-sources] [-jdk 1.4|1.5|5|5.0]"
    echo
    exit 1
fi

ls -ld .
if [ -w . ]
then
    echo "filesystem is writeable - continuing"
else
    echo "ERROR: filesystem is not writeable - quitting"
    exit
fi

echo "***** Starting build *****"
echo
echo "system is `uname -a`"
echo "time is `date`"
echo "shell is $SHELL"
echo "user is `id`"
type java
java -fullversion 2>&1 | cat
type mvn
mvn -version
echo "cwd is `pwd`"
echo "disk space:"
df -h .
echo

## cleanup
rm -fr ./testresults

PROPS="-e $PROPS"

## execute build, recording status
mvn $PROPS -DdownloadSources=$downloadSources -Dmaven.test.failure.ignore=true clean:clean install eclipse:eclipse site && \
mvn $PROPS -f pom.clover.xml clover:aggregate clover:clover
status=$?

## gather all test results together for BJ
mkdir ./testresults
for i in `find . -name "TEST*.xml"`
do
  mv $i ./testresults/
done

echo
echo "***** BeetleJuice build completed *****"

exit $status
