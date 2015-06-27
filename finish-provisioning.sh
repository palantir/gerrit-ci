#!/bin/bash

# Set up id_admin ssh key
cd ~
eval `ssh-agent -s`
ssh-add ~/.ssh/id_admin

# Create jenkins Gerrit account
cat ~/.ssh/id_jenkins.pub | ssh -p 29418 admin@localhost gerrit create-account jenkins --email jenkins@test.com --full-name Jenkins --ssh-key -

# Create jenkins-ro Gerrit group
ssh -p 29418 admin@localhost gerrit create-group jenkins-ro --member jenkins --description "'Users with permissions needed for Gerrit-CI'"

JENKINS_RO_UUID=`ssh -p 29418 admin@localhost gerrit ls-groups -v | grep jenkins-ro | cut -f 2`
echo $JENKINS_RO_UUID

# Install Verified Label
git config --global user.name "Administrator"
git config --global user.email admin@test.com
mkdir tmp
cd tmp
git init
git remote add origin ssh://admin@localhost:29418/All-Projects
git fetch origin refs/meta/config:refs/remotes/origin/meta/config
git checkout meta/config
echo "$JENKINS_RO_UUID	jenkins-ro" >> groups
git config -f project.config --add label.Verified.function MaxWithBlock
git config -f project.config --add label.Verified.value '-1 Fails'
git config -f project.config --add label.Verified.value '0 No score'
git config -f project.config --add label.Verified.value '+1 Verified'
git config -f project.config --add capability.streamEvents "group jenkins-ro"
git config -f project.config --add access.refs/heads/*.label-Verified "-1..+1 group Administrators"
git config -f project.config --add access.refs/heads/*.label-Verified "-1..+1 group Project Owners"
git commit -a -m "Install Verified label"
git push origin meta/config:meta/config
cd ..
rm -rf ./tmp

