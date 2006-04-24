#!/bin/bash

iters=$1
test=$2

let i=0

mvn --offline clean resources:resources resources:testResources compiler:compile compiler:testCompile

while [ $i -lt $iters ]
do
  let i+=1
  echo "ITERATION: $i/$iters"
  mvn -Dtest=$test surefire:test || exit $?
done

