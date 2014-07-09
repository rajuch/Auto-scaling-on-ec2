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


