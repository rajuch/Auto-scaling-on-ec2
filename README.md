###﻿Problem Statement:
﻿
While running hive on hadoop, 

   when load on cluster is high, adding the computing node decreases the execution time of the queries but not that much because new node works on the other nodes data as it has no data,
   
   when load on the cluster is low, removing the node from the cluster also takes lot of time. 
   
### Solution:

To reduce the time to scale the Hadoop cluster, we came up with the following solution

   Prior to adding a new node, move the data from the existing nodes to the new node. This balances the cluster and if a new task comes, the newly added node can take it up as it already has the data (data locality).
   
   When decommissioning a node, move the data available on that node to the other nodes in the cluster. This helps in maintaining the replication factor.
  
Following this approach reduces the impact on time taken for queries when adding/removing the nodes.

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

|Existing Procedure| New Procedure|
|---|---|
|5-6 sec| 12mins|

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

|Existing Procedure(6gb data)| New Procedure(6gb data)|
|---|---|
|60 mins| < 12mins|


With 6 gb data (existing procedure) :  **60 mins**

With the new procedure (6gb data), 

scp transfer (11.48mins) + namenode updation(300msec): **less than 12 mins**

**Time taken for Hive query**,

|4node cluster |Existing Procedure| New Procedure|
|---|---|---|
|16mins,25sec|13mins,38sec| 9mins,41sec|

 on 4 node cluster: **16mins, 25sec**
 
after adding new node without copying the data (i.e. new node has no data): **13mins, 38sec**  (5 node cluster)

after adding new node after copying the data from the other nodes to new node: **9mins, 41sec** (5 node cluster)

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

