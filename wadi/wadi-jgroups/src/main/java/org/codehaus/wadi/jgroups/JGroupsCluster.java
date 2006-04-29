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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.jgroups.messages.StateRequest;
import org.codehaus.wadi.jgroups.messages.StateResponse;
import org.codehaus.wadi.jgroups.messages.StateUpdate;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.MessageListener;
import org.jgroups.View;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;

// TODO - fix outstanding issues
// TODO - regular state updates

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster, MembershipListener, MessageListener, JGroupsClusterMessageListener {
	public static final ThreadLocal _cluster=new ThreadLocal();

  protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
  protected final Log _log=LogFactory.getLog(getClass());
  protected final String _clusterName;
  protected final Channel _channel;
  protected final List _clusterListeners=new ArrayList();
  protected final Map _destinationToNode=new HashMap(); // we don't need this, but i/f does - yeugh !
  protected final boolean _excludeSelf=true;
  protected final Map _clusterState=new HashMap(); // a Map (Cluster) of Maps (Nodes) associating a Key (Address) with a Value (State)
  protected final JGroupsLocalNode _localPeer;
  protected final Latch _latch=new Latch();
  protected final Latch _initLatch=new Latch();
  public final Map _addressToDestination=new ConcurrentHashMap();

  protected ElectionStrategy _electionStrategy;
  protected ClusterListener _listener;
  protected JGroupsDispatcher _dispatcher;
  protected Address _localAddress;
  protected JGroupsAddress _localDestination;
  protected JGroupsTopic _clusterTopic;

  public JGroupsCluster(String clusterName) throws ChannelException {
    super();
    _clusterName=clusterName;
    //_channel=new JChannel("state_transfer.xml"); // uses an xml stack config file from JGroups distro
    _channel=new JChannel("default.xml"); // uses an xml stack config file from JGroups distro
    _localPeer=new JGroupsLocalNode(this, _clusterState);
    _cluster.set(this); // set ThreadLocal
  }

  // ActiveCluster 'Cluster' API
  public Map getPeers() {
    return _destinationToNode;
  }

  public void addClusterListener(ClusterListener listener) {
    synchronized (_clusterListeners) {
      _clusterListeners.add(listener);
    }
  }

  public void removeClusterListener(ClusterListener listener) {
    synchronized (_clusterListeners) {
      _clusterListeners.remove(listener);
    }
  }

  public LocalPeer getLocalPeer() {
    return _localPeer;
  }

  public void setElectionStrategy(ElectionStrategy strategy) {
    _electionStrategy=strategy;
  }

  public void send(org.codehaus.wadi.group.Address destination, Message message) throws MessageExchangeException {
    JGroupsMessage msg=(JGroupsMessage)message;
    JGroupsAddress dest=(JGroupsAddress)destination;
    try {
      msg.setCluster(this);
      msg.setAddress(destination);
      if (_messageLog.isTraceEnabled()) _messageLog.trace("outgoing: "+msg.getPayload()+" {"+_localDestination.getName()+"->"+dest.getName()+"} - "+msg.getIncomingCorrelationId()+"/"+msg.getOutgoingCorrelationId());
      msg.setCluster(null);
      _channel.send(dest.getAddress(), _localAddress, msg);
    } catch (Exception e) {
      _log.warn("unexpected JGroups problem", e);
      throw new MessageExchangeException(e);
    }
  }

  public boolean waitForClusterToComplete(int expectedCount, long timeout) throws InterruptedException {
    throw new UnsupportedOperationException("NYI");
  }

  public void start() throws ClusterException {
    try {
      //_channel.setOpt(Channel.LOCAL, Boolean.FALSE); // exclude ourselves from our own broadcasts... - BUT also from Unicasts :-(
      _channel.connect(_clusterName);
      _log.info("connected to channel");
      _clusterTopic=new JGroupsTopic("CLUSTER", null); // null address means broadcast to all members
      _localAddress=_channel.getLocalAddress();
      _clusterState.put(_localAddress, new HashMap()); // initialise our distributed state
      _localDestination=new JGroupsAddress(_localAddress);
      _localDestination.init(_localPeer);
      _localPeer.setDestination(_localDestination);
      _addressToDestination.put(_localAddress, _localDestination);
      _latch.release(); // allow new view to be accepted
    } catch (Exception e) {
      _log.warn("unexpected JGroups problem", e);
      throw new ClusterException(e);
    }

    // we don't really want anyone else running until we have initialised properly... - how ?
    _initLatch.release();
  }

  public void stop() throws ClusterException {
	  _channel.disconnect();
	  _channel.close();
	  _log.info("disconnected from channel");
	  // TODO
	  // hmmm... sometimes we get messages hitting the Dispatcher even after we have closed the channel - we need to wait for them somehow...
	  //_clusterTopic=null;
	  //_localDestination=null;
	  //_localAddress=null;
	  _clusterState.clear();
  }

  // JGroups MembershipListener API

  public void viewAccepted(View newView) {
	  // must not run until we are initialised...
	  try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
    // we don't want to overtake the thread that is initialising this object...
    try {
      _latch.acquire();
    } catch (InterruptedException e) {
      // hmmm...
    }

    if (_log.isTraceEnabled()) _log.trace("handling JGroups viewAccepted("+newView+")...");

    // this is meant to happen if a network split is healed and two
    // clusters try to reconcile their separate states into one -
    // I have a plan...
    if(newView instanceof MergeView)
      if (_log.isWarnEnabled()) _log.warn("NYI - merging: view is " + newView);

    synchronized (_destinationToNode) {
      Vector newMembers=newView.getMembers();

      // manage leavers
      for (Iterator i=_destinationToNode.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry=(Map.Entry)i.next();
        JGroupsAddress destination=(JGroupsAddress)entry.getKey();
        Peer node=(Peer)entry.getValue();
        if (!newMembers.contains(destination.getAddress())) {

          i.remove();
  		  runCoordinatorElection();

  		  // notify listener
          if (_listener!=null && destination!=_localDestination) {
            _listener.onPeerFailed(new ClusterEvent(this, node ,ClusterEvent.PEER_FAILED));
          }
          // remove node
          _addressToDestination.remove(destination.getAddress());

          // garbage collect this nodes share of cluster state...
          synchronized (_clusterState) {
            _clusterState.remove(destination.getAddress());
          }
        }
      }

      // notify joiners
      new Thread(new JoinerThread(newMembers)).start();

    }

    if (_log.isInfoEnabled()) {
    	List members=new ArrayList();
    	for (Iterator i=newView.getMembers().iterator(); i.hasNext(); )
    		members.add(_addressToDestination.get(i.next()));
    	_log.info("JGroups View: " + members);
    }

  }

  class JoinerThread implements Runnable {
	  
	  Vector newMembers;
	  
	  JoinerThread(Vector newMembers) {
		  this.newMembers=newMembers;
	  }
	  
	  public void run() {
		  // now we need a quick round trip to fetch the distributed data for this node - yeugh !
		  for (int i=0; i<newMembers.size(); i++) {
			  Address address=(Address)newMembers.get(i);
			  JGroupsAddress destination=JGroupsAddress.get(JGroupsCluster.this, address);
			  if (destination!=_localDestination) {
				  if (_clusterState.get(address)==null) {
					  
					  //fetch state
					  try {
						  JGroupsMessage tmp=(JGroupsMessage)_dispatcher.exchangeSend(_localDestination, destination, new StateRequest(), 5000);
						  Map state=((StateResponse)tmp.getPayload()).getState();
						  synchronized (_clusterState) {
							  _clusterState.put(address, state);
						  }
					  } catch (Exception e) {
						  _log.warn("problem retrieving remote state for fresh joiner...", e);
						  return;
					  }
					  
					  // insert node
					  synchronized (_destinationToNode) {
						  _destinationToNode.put(destination, destination.getNode());
					  }
					  
					  // notify listener
					  if (_listener!=null)
						  _listener.onPeerAdded(new ClusterEvent(JGroupsCluster.this, destination.getNode(), ClusterEvent.PEER_ADDED));
					  
					  // elect coordinator
					  runCoordinatorElection();
				  }
			  }
		  }
	  }
  }
  
  protected void runCoordinatorElection() {
	  if (_electionStrategy!=null) {
		  Peer coordinator=_localPeer;
		  if (_destinationToNode.size()>0) {
              coordinator=_electionStrategy.doElection(JGroupsCluster.this);
		  }
		  _listener.onCoordinatorChanged(new ClusterEvent(JGroupsCluster.this, coordinator, ClusterEvent.COORDINATOR_ELECTED));
	  }
  }

  public void suspect(Address suspected_mbr) {
	  try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
    if (_log.isTraceEnabled()) _log.trace("handling suspect("+suspected_mbr+")...");
    if (_log.isWarnEnabled()) _log.warn("cluster suspects member may have been lost: " + suspected_mbr);
    _log.trace("...suspect() handled");
  }

  // Block sending and receiving of messages until viewAccepted() is called
  public void block() {
    _log.trace("handling block()...");
    // NYI
    _log.trace("... block() handled");

  }

  //-----------------------------------------------------------------------------------------------
  // JGroups MessageListener API

  public void receive(org.jgroups.Message msg) {
  	  // TODO: this may be a little contentious - think of a better way...
	  try {_initLatch.acquire();} catch (Exception e) {_log.error("problem syncing Cluster initialisation", e);}
	  synchronized (_channel) {
      Address src=msg.getSrc();
      Address dest=msg.getDest();
      if (_excludeSelf && (dest==null || dest.isMulticastAddress()) && src==_localAddress) {
        _log.debug("ignoring message from self: "+msg);
      } else {
    	  _cluster.set(this); // setup a ThreadLocal to be read during deserialisation...
        Object o=msg.getObject();

        JGroupsMessage message=(JGroupsMessage)o;
        message.setCluster(this);
        org.codehaus.wadi.group.Address replyTo=getAddress(src);
        message.setReplyTo(replyTo);
        message.setAddress(getAddress(dest));
        _dispatcher.onMessage(message);
      }
    }
  }

  public byte[] getState() {
	  throw new UnsupportedOperationException("we do not use JGroups' state exchange protocol");
  }

  public void setState(byte[] state) {
	  throw new UnsupportedOperationException("we do not use JGroups' state exchange protocol");
  }

  // WADI 'JGroupsCluster' API

  public void init(JGroupsDispatcher dispatcher) throws Exception {
    _dispatcher=dispatcher;
    
    ServiceEndpointBuilder endpointBuilder = new ServiceEndpointBuilder();
    endpointBuilder.addSEI(_dispatcher, JGroupsClusterMessageListener.class, this);
    endpointBuilder.addCallback(_dispatcher, StateResponse.class);
  }

  public Channel getChannel() {
    return _channel;
  }

  public int getNumNodes() {
    synchronized (_destinationToNode) {
      return _destinationToNode.size()+1; // TODO - resolve - getNumNodes() returns N, but getNodes() returns N-1
    }
  }

  public Address getLocalAddress() {
    return _localAddress;
  }

  public JGroupsAddress getLocalDestination() {
    return _localDestination;
  }

  /**
   * Translate a JGroups Address into an ActiveCluster Destination.
   * @param address
   * @return
   */
  public JGroupsAddress getAddress(Address address) {
	  JGroupsAddress destination;
	  if (address==null || address.isMulticastAddress())
		  destination=_clusterTopic;
	  else
		  destination=JGroupsAddress.get(this, address);
	  
	  return destination;
  }

  // Dispatcher API

  public void setClusterListener(ClusterListener listener) {
    _listener=listener;
  }

  public void onMessage(Message message, StateUpdate update) throws Exception {
    JGroupsAddress destination=(JGroupsAddress)message.getReplyTo();
    Peer node=destination.getNode();
    Map state=update.getState();
    if (node instanceof JGroupsRemoteNode) {
      ((JGroupsRemoteNode)node).setState(state);
      _listener.onPeerUpdated(new ClusterEvent(this, node, ClusterEvent.PEER_UPDATED));
    } else {
      _log.warn("state update from non-remote node: "+node);
    }
  }

  public void onMessage(Message message, StateRequest request) throws Exception {
    Map state=_localPeer.getState();
    _dispatcher.reply(message, new StateResponse(state));
  }

  public Map getClusterState() {
	  return _clusterState;
  }
  
	
	protected static final String _prefix="<"+JGroupsCluster.class.getPackage().getName()+": ";
	protected static final String _suffix=">";
	
	public String toString() {
		return _prefix+_localPeer.getName()+"@"+_clusterName+_suffix;
	}

    public org.codehaus.wadi.group.Address getAddress() {
        return _clusterTopic;
    }
}
