#!/bin/sh

## an integration script for continuum.
## if the build takes too long (maybe a test hangs), it will kill itself

echo "***** Continuum build started *****"
echo

help=false
site=false
target=compile
JAVA_HOME=/usr/local/java/j2sdk1.4.2_13

while [ -n "$1" ]
do
  case $1 in
      -jdk)
      shift
      echo "selecting jdk $1"
      case $1 in
	  1.4)
	  export JAVA_HOME=/usr/local/java/j2sdk1.4.2_13
	  ;;
	  1.5|5|5.0)
	  export JAVA_HOME=/usr/local/java/jdk1.5.0_09
	  ;;
	  *)
	  echo "parameter not recognised: $1"
	  echo "jdk 1.4, 5 and 5.0 supported"
	  ;;
      esac
      ;;
      -target)
      shift
      echo "selecting target $1"
      case $1 in
	  compile|test|site|eclipse)
	  target=$1
	  ;;
	  *)
	  echo "target not recognised: $1"
	  echo "compile|test|site supported"
	  ;;
      esac
      site=true
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
    echo "usage: $0 [-help] [-sources] [-jdk 1.4|1.5|5|5.0] [-target compile|test|site]"
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

## cleanup maven repo
rm -fr ~/.m2/repository/org/codehaus/wadi

PROPS="$PROPS -Djava.awt.headless=true"
PROPS="-e $PROPS"

set -m ## enable job control

## kick off our build (damocles) in the background
case $target in

    compile)
    timeout=600 ## 10 mins
    mvn $PROPS clean:clean && \
    mvn $PROPS compiler:compile & damocles=$!
    ;;
    test)
    timeout=1800 ## 1/2 hr
    mvn $PROPS clean:clean && \
    mvn $PROPS install & damocles=$!
    ;;
    site)
    timeout=6400 ## 2 hrs
    PROPS="$PROPS -Dmaven.test.failure.ignore=true"
    mvn $PROPS clean:clean && \
    mvn $PROPS install clover:instrument clover:aggregate site:site site:deploy & damocles=$!
    ;;
    eclipse)
    timeout=3600 ## 1 hr
    PROPS="$PROPS -DdownloadSources=true -Dmaven.test.failure.ignore=true"
    mvn $PROPS clean:clean && \
    mvn $PROPS install eclipse:clean eclipse:eclipse & damocles=$!
    ;;

esac

## kick off a 'sword' - this will kill us if we take too long...
sh -c "sleep $timeout && date && echo 'build has overrun - killing it' && kill -9 -$damocles" & sword=$!

## bring our task (damocles) back into the foreground
fg %1
status=$?

## remember the 'sword's pid, so when we finish our own task, we can kill it...
kill -9 $sword

echo
echo "***** Continuum build completed *****"
echo
echo "status: $status"

exit $status
