# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "centos/7"

  config.vm.provision "shell" do |s|
    ssh_pub_key = File.readlines("#{Dir.home}/.ssh/id_rsa.pub").first.strip
    s.inline = <<-SHELL
      mkdir /root/.ssh/
      echo #{ssh_pub_key} >> /root/.ssh/authorized_keys
      chmod 600 /root/.ssh/authorized_keys
    SHELL
  end

  (2..4).each do |i|
    config.vm.define "master#{i}" do |c|
      c.vm.provider "virtualbox" do |v|
        v.cpus = 4
        v.memory = 2000
      end
      c.vm.network "private_network", ip: "192.168.2.#{i}"
      c.vm.hostname = "cassandra#{i}.localhost"
    end
  end

end
