/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.jgroups;

import java.util.Collection;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.Node;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
import org.codehaus.wadi.jgroups.messages.StateUpdate;
import org.jgroups.Address;
import org.jgroups.ChannelException;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A Dispatcher for JGroups
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDispatcher extends AbstractDispatcher {

  protected final boolean _excludeSelf=true;

  protected JGroupsCluster _cluster;
  protected Address _localAddress;
  protected MessageDispatcher _dispatcher;

  public JGroupsDispatcher(String nodeName, String clusterName, long inactiveTime) throws ChannelException {
    super(clusterName, nodeName, inactiveTime);
    _cluster=new JGroupsCluster(clusterName);
    register(_cluster, "onMessage", StateUpdate.class);
  }

  //-----------------------------------------------------------------------------------------------
  // WADI Dispatcher API

  public void init(DispatcherConfig config) throws Exception {
    super.init(config);
    _dispatcher=new MessageDispatcher(_cluster.getChannel(), _cluster, _cluster, null);
    _cluster.init(this);
  }

  public void start() throws Exception {
    _cluster.start();
    _localAddress=_cluster.getLocalAddress();
    _dispatcher.start();
  }

  public void stop() throws Exception {
    _dispatcher.stop();
    _cluster.stop();
  }

  public int getNumNodes() {
    return _cluster.getNumNodes();
  }

  public ObjectMessage createObjectMessage() {
    return new JGroupsObjectMessage();
  }

  public void send(Destination to, ObjectMessage message) throws Exception {
    _cluster.send(to, message);
  }

  protected Node getNode(Address address) {
    return _cluster.getDestination(address).getNode();
  }

  public String getNodeName(Destination destination) {
	JGroupsDestination d=(JGroupsDestination)destination;
	if (d.getNode()==null && d!=_cluster.getDestination()) {
		// the Destination may have come in over the wire and not have been initialised...
		_log.warn("UNINITIALISED DESTINATION - from over wire");
		d.init(getNode(d.getAddress()));
	}
    return d.getName();
  }

  public Destination getLocalDestination() {
    return _cluster.getLocalDestination();
  }

  public Destination getClusterDestination() {
    return _cluster.getDestination();
  }

  public Cluster getCluster() {
    return _cluster;
  }

  public Map getDistributedState() {
    return _cluster.getLocalNode().getState();
  }

  public synchronized void setDistributedState(Map state) throws Exception {
    // this seems to be the only test that ActiveCluster does, so there is no point in us doing any more...
    LocalNode localNode=_cluster.getLocalNode();
    //if (localNode.getState()!=state) {
      localNode.setState(state);
   // }
  }

  public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
    return ((JGroupsObjectMessage)message).getIncomingCorrelationId();
  }

  public void setIncomingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
    ((JGroupsObjectMessage)message).setIncomingCorrelationId(correlationId);
  }

  public String getOutgoingCorrelationId(ObjectMessage message) throws Exception {
    return ((JGroupsObjectMessage)message).getOutgoingCorrelationId();
  }

  public void setOutgoingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
    ((JGroupsObjectMessage)message).setOutgoingCorrelationId(correlationId);
  }

  public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
    throw new UnsupportedOperationException("NYI");
  }

  public void setClusterListener(ClusterListener listener) {
    _cluster.setClusterListener(listener);
  }

  public Destination getDestination(String name) {
      throw new UnsupportedOperationException();
  }

  public String getLocalNodeName() {
      throw new UnsupportedOperationException();
  }

  protected void hook() {
	  JGroupsCluster._cluster.set(_cluster);
	  super.hook();
  }
  
}
