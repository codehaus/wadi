#!/bin/sh

mvn -DdownloadSources=true -Dmaven.test.skip=true clean:clean install eclipse:eclipse
