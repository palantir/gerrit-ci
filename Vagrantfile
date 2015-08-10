Vagrant.configure(2) do |config|
  config.vm.box = "chef/centos-7.0"

  config.vm.network "forwarded_port", guest: 8000, host: 8000
  config.vm.network "forwarded_port", guest: 8080, host: 8080
  config.vm.network "forwarded_port", guest: 29418, host: 29418

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

   config.vm.provider "virtualbox" do |vb|
     # Customize the amount of memory on the VM:
     vb.cpus = 2
     vb.memory = "1024"
   end

  config.vm.provision "shell", privileged: false, inline: <<-SHELL
    set -e
    cd ~

    # Install necessary packages
    sudo yum -y install git
    sudo yum -y install vim

    # Install Java
    sudo yum -y install java-1.7.0-openjdk

    # Install Gerrit
    curl -L -O http://gerrit-releases.storage.googleapis.com/gerrit-2.11.1.war
    mkdir gerrit
    java -jar gerrit-2.11.1.war init -d gerrit --batch

    # Generate SSH keys
    ssh-keygen -f /home/vagrant/.ssh/id_jenkins -N "" -C "jenkins@localhost"
    ssh-keygen -f /home/vagrant/.ssh/id_admin -N "" -C "admin@localhost"
    cp ~/.ssh/id_admin /vagrant

    # Configure and run Gerrit
    git config -f gerrit/etc/gerrit.config auth.type DEVELOPMENT_BECOME_ANY_ACCOUNT
    git config -f gerrit/etc/gerrit.config plugins.allowRemoteAdmin true
    gerrit/bin/gerrit.sh restart

    # Configure Gerrit admin user

    # Get GerritAccount cookie
    GERRIT_USER_ID=$(curl -X POST "http://localhost:8080/login/%23%2F?action=create_account" \
        -D - -o /dev/null -s \
        | grep 'Set-Cookie:' \
        | sed 's/Set-Cookie: GerritAccount=//g' \
        | cut -b 1-34)

    # Get xGerritAuth token
    AUTH_KEY=$(curl "http://localhost:8080/" -s -H "Cookie: GerritAccount=$GERRIT_USER_ID" \
        | grep -o -E 'xGerritAuth=\"[^\"]+\"' \
        | sed 's/"//g' \
        | sed 's/xGerritAuth=//g')

    # Change username to "admin"
    curl -X POST "http://localhost:8080/gerrit_ui/rpc/AccountSecurity" -s \
        -H "Accept: application/json" \
        -H "Content-Type: application/json" \
        -d '{\"jsonrpc\": \"2.0\", \"method\": \"changeUserName\", \"params\": [\"admin\"], \"xsrfKey\": \"'"$AUTH_KEY"'\"}' \
        -H "Cookie: GerritAccount=$GERRIT_USER_ID"

    # Change full name to "Administrator"
    curl -X POST "http://localhost:8080/gerrit_ui/rpc/AccountSecurity" -s \
        -H "Accept: application/json" \
        -H "Content-Type: application/json" \
        -d '{\"jsonrpc\": \"2.0\", \"method\": \"updateContact\", \"params\": [\"Administrator\", null, null], \"xsrfKey\": \"'"$AUTH_KEY"'\"}' \
        -H "Cookie: GerritAccount=$GERRIT_USER_ID"

    # Change email to "admin@test.com"
    curl -X PUT "http://localhost:8080/accounts/self/emails/admin%40test.com" -s \
        -H "Cookie: GerritAccount=$GERRIT_USER_ID" \
        -H "X-Gerrit-Auth: $AUTH_KEY"

    # Upload id_admin.pub SSH key
    curl -X POST "http://localhost:8080/accounts/self/sshkeys" -s \
        -H "Cookie: GerritAccount=$GERRIT_USER_ID" \
        -H "X-Gerrit-Auth: $AUTH_KEY" \
        -d "$(cat ~/.ssh/id_admin.pub)"

    # Set up id_admin ssh key
    cd ~
    eval `ssh-agent -s`
    ssh-add ~/.ssh/id_admin

    # Create jenkins Gerrit account
    cat ~/.ssh/id_jenkins.pub | ssh -p 29418 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
        admin@localhost gerrit create-account jenkins --email jenkins@test.com --full-name Jenkins --ssh-key -

    # Create jenkins-ro Gerrit group
    ssh -p 29418 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
        admin@localhost gerrit create-group jenkins-ro \
        --member jenkins --description "'Users with permissions needed for Gerrit-CI'"

    JENKINS_RO_UUID=`ssh -p 29418 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
        admin@localhost gerrit ls-groups -v | grep jenkins-ro | cut -f 2`
    echo $JENKINS_RO_UUID

    # Make an executable file with preset ssh options for git fetch and push
    echo 'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $*' > ~/ssh
    chmod +x ~/ssh

    # Install Verified Label
    git config --global user.name "Administrator"
    git config --global user.email admin@test.com
    mkdir tmp
    cd tmp
    git init
    git remote add origin ssh://admin@localhost:29418/All-Projects
    GIT_SSH='/home/vagrant/ssh' git fetch origin refs/meta/config:refs/remotes/origin/meta/config
    git checkout meta/config
    echo "$JENKINS_RO_UUID	jenkins-ro" >> groups
    git config -f project.config --add label.Verified.function MaxWithBlock
    git config -f project.config --add label.Verified.value '-1 Fails'
    git config -f project.config --add label.Verified.value '0 No score'
    git config -f project.config --add label.Verified.value '+1 Verified'
    git config -f project.config --add capability.streamEvents "group jenkins-ro"
    git config -f project.config --add access.refs/heads/*.label-Verified "-1..+1 group Administrators"
    git config -f project.config --add access.refs/heads/*.label-Verified "-1..+1 group Project Owners"
    git config -f project.config --add access.refs/heads/*.label-Verified "-1..+1 group jenkins-ro"
    git commit -a -m "Install Verified label"
    GIT_SSH='/home/vagrant/ssh' git push origin refs/heads/meta/config:refs/meta/config
    cd ..
    rm -rf ./tmp

    # Download Jenkins
    curl -L -O http://mirrors.jenkins-ci.org/war/latest/jenkins.war

    mkdir -p .jenkins/plugins

    # Install build-timeout plugin and dependencies
    wget http://updates.jenkins-ci.org/latest/build-timeout.hpi -O .jenkins/plugins/build-timeout.hpi
    wget http://updates.jenkins-ci.org/latest/token-macro.hpi -O .jenkins/plugins/token-macro.hpi

    # Install gerrit-trigger plugin
    wget http://updates.jenkins-ci.org/latest/gerrit-trigger.hpi -O .jenkins/plugins/gerrit-trigger.hpi

    # Install git plugin and dependencies
    wget http://updates.jenkins-ci.org/latest/git.hpi -O .jenkins/plugins/git.hpi
    wget http://updates.jenkins-ci.org/latest/git-client.hpi -O .jenkins/plugins/git-client.hpi
    wget http://updates.jenkins-ci.org/latest/scm-api.hpi -O .jenkins/plugins/scm-api.hpi

    # Install junit stuff
    wget http://updates.jenkins-ci.org/latest/junit.hpi -O .jenkins/plugins/junit.hpi
    wget http://updates.jenkins-ci.org/latest/xunit.hpi -O .jenkins/plugins/xunit.hpi

    # Start Jenkins
    nohup java -jar jenkins.war --httpPort=8000 > jenkins.log 2>&1 &
    echo $! > jenkins.pid

    # Do nothing while Jenkins is starting up
    while [ -n "$(curl http://localhost:8000 2>&1 | grep -E '(Connection refused|Please wait while Jenkins is getting ready to work)')" ]; do
        echo "Waiting for Jenkins..."
        sleep 2
    done

    # Wait for Jenkins to finish downloading plugin metadata
    while [ -z "$(tail jenkins.log | grep 'Finished Download metadata.')" ]; do
        echo "Waiting for plugin metadata download..."
        sleep 2
    done

    # Download jenkins-cli
    wget http://localhost:8000/jnlpJars/jenkins-cli.jar -O jenkins-cli.jar

    # Update plugins that are out of date
    for f in $(java -jar jenkins-cli.jar -s http://localhost:8000 list-plugins | grep -e ')$' | awk '{ print $1 }'); do
        java -jar jenkins-cli.jar -s http://localhost:8000 install-plugin "$f"
    done

    # Configure gerrit-trigger plugin and ssh credentials
    cp /vagrant/resources/gerrit-trigger.xml ~/.jenkins
    cp /vagrant/resources/credentials.xml ~/.jenkins
    cp /vagrant/resources/jenkins.model.JenkinsLocationConfiguration.xml ~/.jenkins

    java -jar jenkins-cli.jar -s http://localhost:8000 restart

    echo "Gerrit started at http://localhost:8080"
    echo "Jenkins started at http://localhost:8000"
  SHELL
end
