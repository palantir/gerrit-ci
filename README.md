# Gerrit-CI [![Issues](http://img.shields.io/github/issues/palantir/gerrit-ci.svg?style=flat)](https://github.com/palantir/gerrit-ci/issues)

> Plugin for Gerrit enabling self-service continuous integration workflows with Jenkins.

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
