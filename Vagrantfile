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

    # Configure and run Gerrit
    git config -f gerrit/etc/gerrit.config auth.type DEVELOPMENT_BECOME_ANY_ACCOUNT
    git config -f gerrit/etc/gerrit.config plugins.allowRemoteAdmin true
    gerrit/bin/gerrit.sh restart

    # Install and run Jenkins
    curl -L -O http://mirrors.jenkins-ci.org/war/latest/jenkins.war
    nohup java -jar jenkins.war --httpPort=8000 > jenkins.log 2>&1 &
    echo $! > jenkins.pid
  SHELL
end
