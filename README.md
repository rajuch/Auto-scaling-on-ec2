###﻿Problem Statement:
﻿
Hive on hadoop,

  when user adds the node to the cluster, name node trasfers the data to the new node to balance the cluster and to take the new node into the action (for the execution of tasks). 
   
  when user decommisions the node from the cluster, name node transfers the data from the decommision node to the other nodes for maintaining the replication factor.
  
  These operations takes lot of time when dealing with the huge data (in tera bytes) and these operations have impact on the hive query execution time too..

### Solution:

**Adding the node:**

  Before adding the new node, move the data from the existing nodes to the new node and add it to the cluster, it balances the cluster and if any new task comes then new node can take it right away as it has the data (data locality).
 
**Decommisioning the node:**

  Before decommisioning, move the data from the decommisioning node to the other nodes in the cluster, it maintains the replication factor.

Note: Transferring the data is the manual operation and these operations should be done before the execution of hive query

### Procedure:

Maintain the data in multiple volumes. In hdfs-site.xml add the entries of the data volumes as shown below.

```
<property>
   <name>dfs.data.dir</name> 
   <value>/u1/hadoop/data,/u2/hadoop/data</value> 
</property>

```

**Adding the node:**

1. Remove the volume from the existing nodes and add these volumes to the new node.
	* Add the removing volume entries to the below property in hdfs-site.xml 
	
	```
	<property>
     	<name>dfs.remove.dir</name>
     	<value>/u1/hadoop/data</value>
	</property>
	```
   * Execute the below command to update the volumeMap of datanode and to report the remaining  blocks to the namenode
   
	  `bin/hadoop datanodeadmin -deleteVolume`
   * Unmount the volume and mount it on new node and change the StorageID in the VERSION file (change the ip address of the storage id to the new node)
   * Add the volume entry to below property in hdfs-site.xml in new node
    ```
	<property>
        <name>dfs.data.dir</name>
        <volume>/u1/hadoop/data</volume>
	</property>
    ```
2. Add the new node to the cluster (see Appendix)

**Removing the node:**

1. Remove the volumes from the decommiosion node 

   `bin/hadoop datanodeadmin -deletevolume`
   
2. Start decommisioning of the node (see Appendix)
3. Mount the volumes on the existing datanodes one by one.

	For every volume,

           * Mount the volume on data node and change the StorageID in the VERSION file (change the ip address of the storage id to the new node)
           * Add the entry in hdfs-site.xml at the end of the existing volume entries (because data node picks the last entry as the new
           entry)
           * And run the below command
           
             `bin/hadoop datanodeadmin -addVolume`

### Statistics:
Hadoop Version: **1.4.0**

Hive version: **0.10.0**

Cluster: **4 node cluster**

Data: **22 GB**

**Adding node:**

New procedure

Time taken to unmount the volume(6.0GB) and mount on new node(node5) : **11.48 mins** (scp transfer)

Time to update the datanode about the deleted volume and report to namenode:  **3.042 sec**

Time to bring up the new node:  **5sec**

Time to update the Namenode about the new node data:  **300msec**

So in a few minutes user can able to add the new node with the data.

Existing procedure:

Time to bring up the new node:  **5sec**

Time to update the Namenode about the new node data:  **300msec**

**Decommisoning of node:**

With 6 gb data (existing procedure) :  **60 mins**

With the new procedure (6gb data), 

scp transfer (11.48mins) + namenode updation(300msec): **less than 12 mins**

**Time taken for Hive query**,

 on 4 node cluster: **16mins, 25sec**
 
after adding new node without copying the data (i.e. new node has no data): **13mins, 38sec**  (5 node cluster)

after adding new node after copying the data from the other nodes to new node: **9mins, 41sec** (5 node cluster)


### Conclusion:
If we observe the results, new procedure has taken less time because of the data locality when compared to the existing procedure. We can add the nodes to the cluster when we execute the big hive queries and we can remove the nodes when we dont require, in small amount of time.


### Future Work
We did these operations, adding/removing node,  before the hive query execution. We would like to do these operation while executing the query to minimize the time for executing the big queries on big data.
    



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

