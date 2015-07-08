#!/bin/bash

# Install the new JAR file
cat build/libs/gerrit-ci.jar | ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
    -p 29418 -i ./id_admin admin@localhost gerrit plugin install - --name gerrit-ci.jar
