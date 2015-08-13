# Gerrit-CI [![BuildStatus](http://img.shields.io/travis/palantir/gerrit-ci.svg?style=flat)](https://travis-ci.org/palantir/gerrit-ci) [![Issues](http://img.shields.io/github/issues/palantir/gerrit-ci.svg?style=flat)](https://github.com/palantir/gerrit-ci/issues)

> Plugin for Gerrit enabling self-service continuous integration workflows with Jenkins.

The intent of Gerrit-CI is to add a self-service CI dashboard to the Gerrit user interface. This
will enable project owners to create customized Jenkins jobs with a few simple clicks. Gerrit-CI was
inspired by [Stashbot](https://github.com/palantir/stashbot) and has similar goals, although there
are differences in the way it operates.

## Admin Guide

In order for Gerrit-ci to create, update, and launch jobs on the Jenkins server, Jenkins and Gerrit must be configured to communicate. This setup is done on the gerrit-ci plugin settings's page as follows:
	
0. On the Gerrit's Installed Plugin page, click on the gerrit-ci settings gear.
0. Fill in the fields as required, and click "Save and update."  

A brief explanation of the fields is given below:

0. Jenkins Url : The URL of the Jenkins server Gerrit should create jobs on.
0. Gerrit User : The Gerrit user that Jenkins will use to clone projects and create comments on changes in Gerrit.
0. Jenkins User : The Jenkins user that Gerrit will use to create, update, and delete jobs.
0. Jenkins Password : The AD password for the Jenkins user above. The user's API token can also be used.
0. Credentials Id : In order to get this value, you must first create a SSH Credential on the Jenkins server. This is  documented here: https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Plugin. 
1. Once you have created this, configure a job to use the credential and then retreive for the <credentialsId> tag in the job's XML configuration.


## User Guide

### Job Types

There are currently two job types, Verify and Publish. Each has a specific purpose, and it is
important to follow the guidelines below when configuring your jobs and writing build scripts.

#### Verify

Verify jobs are intended to build the project's code, run any tests, and report back with
success/failure. They will be triggered whenever a new change is created or a new patch set is
uploaded to an existing change, for branches matching the configured verify branch regular
expression. They will also be triggered if a draft change or patch set is uploaded to these
branches.

Jenkins will comment on the change with a link to the build when it starts running. When it is
finished, Jenkins will comment with the results of the build. Additionally, Jenkins will give a
`+1` value for the `Verified` label if the build was successful and a `-1` value if the build
failed (this can be configured in the settings for the Gerrit-trigger plugin on the Jenkins server).

The name of the created job on the Jenkins server will be `gerrit-ci_<projectName>_verify`, where
`<projectName>` is the name of your project with all forward slashes converted to underscores. For
example, the verify job created for `public/gerrit-ci` will be named
`gerrit-ci_public_gerrit-ci_verify`.

#### Publish

Publish jobs are intended to do everything a verify job does in addition to publishing a resultant
artifact. For branches matching the publish branch regular expression, publish jobs will be
triggered when a ref (e.g. branch or tag) is updated.

The name of the created job on the Jenkins server will be `gerrit-ci_<projectName>_publish`, where
`<projectName>` is the name of your project with all forward slashes converted to underscores. For
example, the publish job created for `public/gerrit-ci` will be named
`gerrit-ci_public_gerrit-ci_publish`.

### Configuration Options

![Gerrit Top Menu](https://cloud.githubusercontent.com/assets/1930963/8972296/90febe3c-360d-11e5-8bcc-8c8ca4ba54cd.png)

To get to the configuration screen for your project, follow these steps:

0. Click `Projects` in the Gerrit top menu
0. Click `List` and find your project
0. Now that your project is selected, click `Gerrit-CI` in the submenu at the top of the screen. 
  
This will bring you to the Gerrit-CI settings page for your project.

##### Job Enabled

When this checkbox is checked, a verify or publish job (depending on which checkbox was checked)
will be created on the Jenkins server for the current project. If the box is unchecked and the
configuration is saved, the job that was previously present on the Jenkins server will be deleted.

##### Branch Regex

The branch regex text box specifies a regular expression that should be used to select branches to
trigger Jenkins builds for. These expressions should match the full ref name (e.g.
`refs/heads/master`, `refs/sandbox/palantir`, or `refs/heads/develop`). For verify jobs, the default
regex is `.*`. This matches every branch that has been created for the project and will run a
verify build when any of the branches are updated with new changes or patch sets. The default regex
for publish jobs is `refs/heads/(develop|master)`. This matches only two branches, `develop` and
`master`. The branches matched by the publish branch regex should be a subset of the branches
matched by the verify branch regex. This way, all changes that will be merged and published will
get verified first.

##### Run Command

The build will run the command placed here after preparing the build node. It will also be the
last command run, so the command should return `0` for a successful build and nonzero for a failed
build. The default command for verify jobs is `./scripts/verify.sh` and the default command for
publish jobs is `./scripts/publish.sh`. This is encouraging the idea that your build commands
should not be entered directly into the text boxes. Instead, the build commands for your project
should be placed into scripts that are version controlled with the rest of the project.

##### Timeout Minutes

This specifies the number of minutes to wait before automatically aborting the build. In order to
abort the build, Jenkins uses the [build-timeout plugin](https://wiki.jenkins-ci.org/display/JENKINS/Build-timeout+Plugin).
Once the timeout is reached, Jenkins behaves as if an invisible hand has clicked the "abort build" button.

##### Junit Enabled

This checkbox flags Jenkins to publish test reports produced by various supported testing tools
in Junit test report format. The location of generated raw XML or other report files
(such as '**/build/cppunit-reports/*.xml') is specified in "Junit test results location". Test results
can thus be recorded and monitored by Jenkins.

## Development

### Requirements

#### Vagrant

The project uses [Vagrant](https://www.vagrantup.com) to standardize the development environment,
so you must download the latest version of the Vagrant installer for your operating system.
Installers can be found here: https://www.vagrantup.com/downloads.html.

#### Gradle

Gerrit-CI uses everybody's favorite build system, Gradle. Installation instructions can be found
at https://gradle.org.

### Initial Setup

First, clone the repo. Then, run `vagrant up` from within the new cloned directory.

This will download and configure a CentOS7 VM for development use. It will also download, install,
and configure Gerrit and Jenkins servers. The configuration of these servers is completely handled
within the Vagrant provisioning process. When `vagrant up` finishes, the servers will be ready for
installing a build of the plugin. Also, Vagrant sets up port forwarding so ports `8000`, `8080`, 
and `29418` on the host machine are forwarded to the VM. This means you can access the running 
servers from the host machine without needing to SSH into the VM first.

### Build & Install

There are several Gradle tasks defined for your use:

##### `build`

Builds the source code into a JAR file located in `./build/libs`.

##### `reload`

Copies the JAR file in `./build/libs` to `~/gerrit/plugins` on the vagrant VM. This is the
directory where plugin JARs need to be stored in order to be loaded by Gerrit. This command also
forces Gerrit to reload the plugins instantly. This process usually would happen at some point, but
it is nice to be able to force it to happen without waiting.

##### `all`

Performs all these tasks in the correct order. This is the Gradle task you will most likely always
use. To run this task, use the following command:

```
$ ./gradlew all
```

## Local Servers

### Gerrit

The new local Gerrit server can be accessed at `http://localhost:8080`. Additionally, the SSH API
can be accessed via port `29418`. Use the following command to access the SSH API:

```
$ ssh -p 29418 localhost gerrit
```

Use the following commands to manage the daemon (from within a `vagrant ssh` session):

```
~/gerrit/bin/gerrit.sh start
~/gerrit/bin/gerrit.sh stop
~/gerrit/bin/gerrit.sh restart
```

If Gerrit won't start due to an error like `No index versions ready; run Reindex`, run the
following command (also from within a `vagrant ssh` session):

```
$ java -jar ~/gerrit.war reindex
```

### Jenkins

The new local Jenkins server can be accessed at `http://localhost:8000`.
