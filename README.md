# Test project: XML archives parser

## Functional requiriments

### User-stories
#### Import data
1. Application starts with archives files destination directory
1. Application read the directory and decide what zips are not loaded yet (new)
1. Application for each new zip-file:
   1. Unzip it
   1. Retrieve XMLs
   1. For every XML:
      1. Split in `<item>` nodes
      1. For every `<item>` node:
         1. Store `<item>` node translated in JSON format in Cassandra database
         1. Update Elasticsearch database with new `<item>` JSON-encoded content
#### Data access
1. Retrieve a company information
   1. for a concrete `id` with last `modifiedAt`
   1. for a concrete `id` for all `modifiedAt` sorted by `modifiedAt`

### Formats
#### EGRUL xml format:
```
<batch>
    <item><id>qewr</id><modifiedAt>2016-11-02T12:00:00</modifiedAt><payload>...</payload></item>
    ...
</batch>
```

#### JSON format
Any suitable to store in Elasticsearch with automapping

## Technical requirements

### Cassandra
* 3 nodes deployment

### Elasticsearch
* 1 node deployment

## Test stand
### Installing cluster nodes
**WARNINIG**: It is oneshot installation, if you run this deployment process over existing installation all data will be removed

**Prerequisites**: 

* `virtualbox` installed
* `vagrant` installed
* `ansible` installed (works at least with v2.2.0)


**Notes**: these three nodes will have ips: 192.168.2.2-4, if you want change the ips, modify `Vagrantfile`

Add following lines in `/etc/hosts`
```
192.168.2.2 cassandra2.localhost
192.168.2.3 cassandra3.localhost
192.168.2.4 cassandra4.localhost
192.168.2.5 opscenter5.localhost
```

To launch 3 CentOS server run:
```
vagrant up
```
from project root directory

To install required Cassandra software run:
```
ansible-playbook -i hosts playbooks/cassandra-node.yml
```

To install OpsCenter:
```
ansible-playbook -i hosts playbooks/opscenter-node.yml
```

Go to `opscenter5.localhost:8888` in browser and add an existing cluster, just mention any IP of it, e.g. `192.168.2.2`

