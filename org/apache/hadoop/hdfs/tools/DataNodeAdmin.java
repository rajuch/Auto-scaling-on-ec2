/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongs;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.DataNodeMXBean;
import org.apache.hadoop.hdfs.server.datanode.DatanodeRegistrationMXBean;
import org.apache.hadoop.hdfs.server.datanode.metrics.FSDatasetMBean;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class refreshes the data node data and reports to name node
 */
public class DataNodeAdmin extends Configured implements Tool {

  public static final Log LOG = LogFactory.getLog(DataNodeAdmin.class);

  private DataNodeMXBean dataNodeProxy;
  private DatanodeProtocol namenode;
  private DatanodeRegistrationMXBean regBeanProxy;
  private FSDatasetMBean fsDataProxy;
  private MBeanServerConnection mbsc;

  public static final String DATANODE_NAME = "Hadoop:service=DataNode,name=DataNodeInfo";
  public static final String DATANODE_REG_NAME = "Hadoop:service=DataNode,name=DataNodeRegInfo";
  public static String SERVER_URL = "service:jmx:rmi:///jndi/rmi://:3000/jmxrmi";

  /** Constructor */
  public DataNodeAdmin(Configuration conf) throws IOException, MalformedObjectNameException{
    super(conf);
    createRPCNamenode(getNameNodeAddress(), getConf());
    mbsc = mBeanServerConnection();
    dataNodeProxy = getDataNodeMBeanProxy();
    fsDataProxy = getDatasetMBean();
    regBeanProxy = getDatanodeRegBean();
  }

  /**
   * Get the address of the name node
   * @return address of the name node
   */
  public InetSocketAddress getNameNodeAddress(){
    return NameNode.getServiceAddress(getConf(), true);
  }

  /**
   * Creaates name node proxy
   * @param nameNodeAddr Name node address
   * @param conf Configuration
   * @throws IOException throws IOException
   */
  private void createRPCNamenode(InetSocketAddress nameNodeAddr,
                                 Configuration conf) throws IOException {
    this.namenode = (DatanodeProtocol)RPC.waitForProxy(DatanodeProtocol.class,
                                                       DatanodeProtocol.versionID,
                                                       nameNodeAddr,
                                                       conf);
  }

  /**
   * Creates data node registration object for sending to name node
   * @return data node registration object
   */
  public DatanodeRegistration prepareDataNodeReg() {
    String[] tokens;
    DatanodeRegistration datanodeReg = new DatanodeRegistration();
    datanodeReg.setName(regBeanProxy.getName());
    datanodeReg.setRegistrationID(regBeanProxy.getRegistrationID());
    datanodeReg.setVersion(regBeanProxy.getVersion());
    datanodeReg.setStorageID(regBeanProxy.getStorageID());
    tokens = datanodeReg.getRegistrationID().split("-");
    datanodeReg.setNamespaceID(Integer.parseInt(tokens[1]));
    datanodeReg.setcTime(Long.parseLong(tokens[tokens.length - 1]));
    String ip = "";
    try {
      ip = DNS.getDefaultIP("default");
      datanodeReg.setName(ip + ":" + regBeanProxy.getPort());
    } catch (UnknownHostException ignored) {
       LOG.warn("Could not find ip address of \"default\" inteface.");
    }
    return datanodeReg;
  }

  /**
   * Creates the mbean server connection
   * @throws MalformedObjectNameException
   * @throws IOException
   */
  private MBeanServerConnection mBeanServerConnection() throws MalformedObjectNameException, IOException {
    return JMXConnectorFactory.connect(
            new JMXServiceURL(SERVER_URL), null).getMBeanServerConnection();
  }

  /**
   * Gets the data node registration bean
   * @return  Data node regstration bean
   * @throws MalformedObjectNameException
   */
  private DatanodeRegistrationMXBean getDatanodeRegBean() throws MalformedObjectNameException {
    ObjectName mbeanRegName = new ObjectName(DATANODE_REG_NAME);
    return JMX.newMBeanProxy(mbsc, mbeanRegName, DatanodeRegistrationMXBean.class, true);
  }

  /**
   * Gets the FSDataset mbean
   * @return FSDataset mbean
   * @throws IOException
   */
  private FSDatasetMBean getDatasetMBean() throws IOException{
    Set<ObjectName> names = new TreeSet<ObjectName>(mbsc.queryNames(null, null));
    ObjectName fsDataSetObject = null;
    for (ObjectName name : names) {
      if(name.toString().contains("FSDatasetState"))
        fsDataSetObject = name;
    }
    return JMX.newMBeanProxy(mbsc, fsDataSetObject, FSDatasetMBean.class, true);
  }

  /**
   * Gets the data node mbean proxy
   * @return Data node mbean proxy
   * @throws MalformedObjectNameException
   * @throws IOException
   */
  private DataNodeMXBean getDataNodeMBeanProxy() throws MalformedObjectNameException, IOException{
    return JMX.newMBeanProxy(mbsc, new ObjectName(DATANODE_NAME), DataNodeMXBean.class, true);
  }

  /**
   * Deletes the data node volumes
   */
  private void deleteVolumes(){
    dataNodeProxy.deleteVolumes();
  }

  /**
   * Gets the data node block report
   * @return array of blocks
   */
  private Block[] getDataNodeBlockReport(){
    Map<Block, File> blockMap = fsDataProxy.roughBlockScan();
    Set<Block> blockReport = blockMap.keySet();
    return blockReport.toArray(new Block[0]);
  }

  /**
   * Reports the data node blocks to the name node
   * @param dataNodeReg DataNodeRegistration object
   * @param blocks array of blocks
   * @throws IOException
   */
  private void blockReport(DatanodeRegistration dataNodeReg, Block[] blocks) throws IOException {
    namenode.blockReport(dataNodeReg, BlockListAsLongs.convertToArrayLongs(blocks));
  }

  /**
   * Adds the data node volume mentioned in the hdfs-site.xml under dfs.data.dir property last value
   * @throws MalformedObjectNameException
   * @throws IOException
   */
  public void addDataNodeVolume() throws MalformedObjectNameException, IOException {
    String[] dataDirs = getConf().getStrings(DataNode.DATA_DIR_KEY);
    String dir = dataDirs[dataDirs.length-1];
    System.out.println("added volume:: " + dir);
    dataNodeProxy.addVolume(dir);
  }

  /**
   * Refreshes the data node
   * @return exit code
   */
  public int refreshDataNode(){
    DatanodeRegistration dataReg;
    try{
      Block[] blocks = getDataNodeBlockReport();
      dataReg = prepareDataNodeReg();
      blockReport(dataReg, blocks);
    } catch (IOException e) {
        e.printStackTrace();
    }
    return 0;
  }

  @Override
  public int run(String[] argv) throws Exception {
    if (argv.length < 1)
      return -1;
    int exitCode = -1;
    int i = 0;
    String cmd = argv[i++];
    if ("-deleteVolume".equals(cmd)) {
      deleteVolumes();
      refreshDataNode();
    }
    if("-refresh".equals(cmd))
      refreshDataNode();
    if("-addVolume".equals(cmd)) {
      addDataNodeVolume();
      refreshDataNode();
    }

    return exitCode;
  }

  /** Main Method */
  public static void main(String[] argv) throws Exception {
    int res = ToolRunner.run(new DataNodeAdmin(new Configuration()), argv);
    System.exit(res);
  }
}
