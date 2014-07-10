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

|Existing Procedure(9.3gb data)| Our Procedure(9.3gb data)|
|---|---|
|9 mins| 8-9 sec|


**Time taken for Hive query**,

|4node cluster |Existing Procedure| Our Procedure|
|---|---|---|
|16mins,25sec|13mins,38sec| 9mins,41sec|


### Conclusion:
If we observe the results, our procedure has taken less time because of the data locality when compared to the existing procedure. 


### Future Work
We did these operations, adding/removing node,  before the hive query execution. In next phase will check while executing the hive queries.
    
