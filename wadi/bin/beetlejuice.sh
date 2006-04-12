#!/bin/sh

## an integration script for the BeetleJuice ci system.

mvn clean:clean && \
mvn install && \
echo "BUILD SUCCESSFUL"

