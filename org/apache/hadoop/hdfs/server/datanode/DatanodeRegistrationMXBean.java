package org.apache.hadoop.hdfs.server.datanode;

/**
 * This Interface defines the methods to get the status of the DataNodeRegistration of
 * a data node.
 * It is also used for publishing via JMX (hence we follow the JMX naming
 * convention.)
 *
 */
public interface DatanodeRegistrationMXBean {

  /**
   * Gets the name of the(hostname:port) data node
   * @return  name(hostname:port)
   */
  public String getName();

  /**
   * Gets the data storage id
   * @return storge id
   */
  public String getStorageID();

  /**
   * Gets the port where data node is running
   * @return port
   */
  public int getPort();

  /**
   * Gets the host name
   * @return host name
   */
  public String getHost();

  /**
   * Gets the data node registration id
   * @return data node registration id
   */
  public String getRegistrationID();

  /**
   * Gets the data node version
   * @return version number
   */
  public int getVersion();
}
