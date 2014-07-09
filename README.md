###﻿Problem Statement:
﻿
Scaling a Hadoop cluster with Hive has the following issues

1. Adding a computing node(Scaling up) when load on the cluster is high decreases the execution time of the queries but its there is still a huge time lag as the new node works on data from other nodes.
2. The process of removing a node from the cluster(Scaling down) when load on the cluster is low, is also time consuming.

### Solution:

To reduce the time to scale the Hadoop cluster, we came up with the following solution

   Prior to adding a new node, move the data from the existing nodes to the new node. This balances the cluster and if a new task comes, the newly added node can take it up as it already has the data (data locality).
   
   When decommissioning a node, move the data available on that node to the other nodes in the cluster. This helps in maintaining the replication factor.
  
Following this approach reduces the impact on time taken for queries when adding/removing the nodes.

Note: Transferring the data is the manual operation and these operations should be done before the execution of hive query

### Statistics:
Hadoop Version: **1.4.0**

Hive version: **0.10.0**

Cluster: **4 node cluster**

Data: **22 GB**

**Adding node:**

|Existing Procedure| Our Procedure|
|---|---|
|5-6 sec| 8-9 sec|

**Decommisoning of node:**

|Existing Procedure(6gb data)| Our Procedure(6gb data)|
|---|---|
|60 mins| 6-7 sec|


**Time taken for Hive query**,

|4node cluster |Existing Procedure| Our Procedure|
|---|---|---|
|16mins,25sec|13mins,38sec| 9mins,41sec|


### Conclusion:
If we observe the results, new procedure has taken less time because of the data locality when compared to the existing procedure. 


### Future Work
We did these operations, adding/removing node,  before the hive query execution. In next phase will check while executing the hive queries.
    



### Appendix:
**Data Locality**

 For higher performance, MapReduce tries to assign workloads to these servers where the data to be processed is stored. This is known as data locality.
 
**Scaling the cluster**

Adding a node to the hadoop cluster:

1. Set proper DNS mapping in “/etc/hosts” for assigned IP address in network. 
2. In the new node, add namenode(master) IP address to “/etc/hosts” 
3. Install SSH and copy the SSH keys to the node from master nodes in the cluster 
	you just have to add the master’s public SSH key (which should be in $HOME/.ssh/id\_rsa.pub) to the authorized\_keys file of slave 
   command: ssh-copy-id -i $HOME/.ssh/id_rsa.pub hduser@slave 
4. Copy Hadoop install files to the adding nodes and make proper configuration by synchronizing “core-site.xml”, “mapred-site.xml”, “hdfs-site.xml” from other nodes in cluster only 
5. Add the IP address of new node to “/etc/hosts” in the namenode(master) and other slave nodes 
6. Append the IP address of new node to “conf/slaves” in the master 
7. Start Hadoop thread in new node manually,

```
  bin/hadoop-daemon.sh start datanode 
  bin/hadoop-daemon.sh start tasktracker
  ```

8. Refresh the nodes on namenode(master)

  `bin/hadoop dfsadmin -refreshNodes`
  
Removing a node from the hadoop cluster:

1. Add list of datanode(s) that needs to decommission to exclude file (assume this file is located under hadoop/conf directory) on namenode.
2. Modify the namenode hdfs-site.xml config file to set the property for dfs.hosts.exclude (see below). ignore this step if you have already have this property set. 

```
	<property> 
   		<name>dfs.hosts.exclude</name> 
   		<value>/usr/lib/hadoop/conf/exclude</value> 
   		<description> List of nodes to decommission </description> 
	</property> 
```

3. Modify the namenode “mapred-site.xml” config file to set the property for mapred.hosts.exclude (see below), ignore this step if you have already have this property set.

```
	<property> 
   		<name>mapred.hosts.exclude</name> 
   		<value>/usr/lib/hadoop/conf/exclude</value> 
   		<description> List of nodes to decommission </description> 
	</property>
```

4. Now update the namenode(master only) using following command.

   `hadoop dfsadmin –refreshNodes `

