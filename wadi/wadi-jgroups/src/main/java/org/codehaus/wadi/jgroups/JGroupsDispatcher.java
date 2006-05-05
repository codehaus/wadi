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
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
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
    
    // TODO - where is this method.
//    register(_cluster, "onMessage", StateUpdate.class);
  }

  //-----------------------------------------------------------------------------------------------
  // WADI Dispatcher API

  public void init(DispatcherConfig config) throws Exception {
    super.init(config);
    _dispatcher=new MessageDispatcher(_cluster.getChannel(), _cluster, _cluster, null);
    _cluster.init(this);
  }

  public void start() throws MessageExchangeException {
    try {
        _cluster.start();
    } catch (ClusterException e) {
        throw new MessageExchangeException(e);
    }
    _localAddress=_cluster.getLocalAddress();
    _dispatcher.start();
  }

  public void stop() throws MessageExchangeException {
    _dispatcher.stop();
    try {
        _cluster.stop();
    } catch (ClusterException e) {
        throw new MessageExchangeException(e);
    }
  }

  public int getNumNodes() {
    return _cluster.getNumNodes();
  }

  public Message createMessage() {
    return new JGroupsMessage();
  }

  public void send(org.codehaus.wadi.group.Address to, Message message) throws MessageExchangeException {
    _cluster.send(to, message);
  }

  protected Peer getNode(Address address) {
    return _cluster.getAddress(address).getNode();
  }

  public String getPeerName(org.codehaus.wadi.group.Address address) {
	JGroupsAddress d=(JGroupsAddress)address;
	assert(d!=null);
	assert(_cluster!=null);
	if (d.getNode()==null && d!=_cluster.getAddress()) {
		// the Destination may have come in over the wire and not have been initialised...
		_log.warn("UNINITIALISED DESTINATION - from over wire");
		d.init(getNode(d.getAddress()));
	}
    return d.getName();
  }

  public org.codehaus.wadi.group.Address getLocalAddress() {
    return _cluster.getLocalDestination();
  }

  public org.codehaus.wadi.group.Address getClusterAddress() {
    return _cluster.getAddress();
  }

  public Cluster getCluster() {
    return _cluster;
  }

  public Map getDistributedState() {
    return _cluster.getLocalPeer().getState();
  }

  public synchronized void setDistributedState(Map state) throws MessageExchangeException {
    // this seems to be the only test that ActiveCluster does, so there is no point in us doing any more...
    LocalPeer localNode=_cluster.getLocalPeer();
    //if (localNode.getState()!=state) {
      localNode.setState(state);
   // }
  }

  public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
    throw new UnsupportedOperationException("NYI");
  }

  public org.codehaus.wadi.group.Address getAddress(String name) {
      throw new UnsupportedOperationException();
  }

  public String getLocalPeerName() {
      throw new UnsupportedOperationException();
  }

  protected void hook() {
	  JGroupsCluster._cluster.set(_cluster);
	  super.hook();
  }

    public LocalPeer getLocalPeer() {
        return _cluster.getLocalPeer();
    }
}
