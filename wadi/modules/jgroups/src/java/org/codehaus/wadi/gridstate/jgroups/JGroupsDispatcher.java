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
package org.codehaus.wadi.gridstate.jgroups;

import java.util.Collection;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.ClusterListener;
import org.activecluster.LocalNode;
import org.activecluster.Node;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.impl.AbstractDispatcher;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A Dispatcher for JGroups
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDispatcher extends AbstractDispatcher implements MessageListener {
  
  protected final boolean _excludeSelf=true;
  
  protected JGroupsCluster _cluster;
  protected Address _localAddress;
  protected MessageDispatcher _dispatcher;
  
  public JGroupsDispatcher(String nodeName, String clusterName, long inactiveTime) {
    super(nodeName, clusterName, inactiveTime);
    _cluster=new JGroupsCluster();
    register(_cluster, "onMessage", JGroupsStateUpdate.class);
  }
  
  //-----------------------------------------------------------------------------------------------
  // MessageListener API
  
  public void receive(Message msg) {
    Address src=msg.getSrc();
    Address dest=msg.getDest();
    if (_excludeSelf && dest.isMulticastAddress() && src==_cluster.getLocalAddress()) {
      _log.debug("ignoring message from self: "+msg);
    } else {
      JGroupsObjectMessage jom=(JGroupsObjectMessage)msg.getObject();
      jom.setCluster(_cluster);
      try {
        _log.info("JOM arriving: "+jom.getObject());
        _log.info("FROM: "+src);
        jom.setJMSReplyTo(_cluster.getDestination(src));
        _log.info("TO: "+dest);
        jom.setJMSDestination(_cluster.getDestination(dest));
      } catch (JMSException e) {
        _log.warn("unexpected JGroups problem", e);
      }
      onMessage(jom);
    }
  }
  
  public byte[] getState() {
    return null;
    // not used
  }
  
  public void setState(byte[] state) {
    // not used
  }
  
  public void init(DispatcherConfig config) throws Exception {
    super.init(config);
    JChannel channel=new JChannel();
    _dispatcher=new MessageDispatcher(channel, this, _cluster, null);
    //channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
    channel.connect(_clusterName);
    _cluster.init(channel);
    _localAddress=_cluster.getLocalAddress();
  }
  
  //-----------------------------------------------------------------------------------------------
  // Dispatcher API
  
  public void start() throws Exception {
    _dispatcher.start();
  }
  
  public void stop() throws Exception {
    _dispatcher.stop();
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
    Address address=((JGroupsDestination)destination).getAddress();
    Node node=getNode(address);
    
    if (node==null) {
      return "<cluster>";
    }
          
    Map state=node.getState();
    if (state==null)
      return "<unknown>";
    else
      return (String)state.get("nodeName"); // TODO - use a static String
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
    if (localNode.getState()!=state) {
      localNode.setState(state);
    }
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
  
}
