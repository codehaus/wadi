#!/bin/sh


## I'm sure it is not meant to be done like this - but I am moving in the right

##rm -fr $HOME/.m2

## I can't collapse these targets onto the same line as modules sites
## are then removed before main site is built...

mvn clean:clean
mvn install
mvn site

## aggregated clover report
mvn -f pom.clover.xml clover:aggregate
mvn -f pom.clover.xml clover:clover
mv target/site/clover/* modules/site/target/site/clover/

cd modules/site/target
tar zcf site.tgz site
ls -l site.tgz
url="file://`pwd`/site/index.html"
echo
echo "check out the url $url"
firefox $url&
scp site.tgz wadi.codehaus.org:
echo "now log into wadi.codehaus.org, untar site.tzg and replace ~wadi/public_html with it..."
