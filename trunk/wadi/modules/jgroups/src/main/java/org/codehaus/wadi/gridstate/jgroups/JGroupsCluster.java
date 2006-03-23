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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.ClusterListener;
import org.activecluster.LocalNode;
import org.activecluster.Node;
import org.activecluster.election.ElectionStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.gridstate.jgroups.messages.StateRequest;
import org.codehaus.wadi.gridstate.jgroups.messages.StateResponse;
import org.codehaus.wadi.gridstate.jgroups.messages.StateUpdate;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.Utils;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.MessageListener;
import org.jgroups.View;
import EDU.oswego.cs.dl.util.concurrent.Latch;

// TODO - fix outstanding issues
// TODO - regular state updates

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster, MembershipListener, MessageListener {
  protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
  protected final Log _log=LogFactory.getLog(getClass());
  protected final String _clusterName;
  protected final Channel _channel;
  protected final List _clusterListeners=new ArrayList();
  protected final Map _addressToDestination=new HashMap();
  protected final Map _destinationToNode=new HashMap(); // we don't need this, but i/f does - yeugh !
  protected final boolean _excludeSelf=true;
  protected final Map _clusterState=new HashMap(); // a Map (Cluster) of Maps (Nodes) associating a Key (Address) with a Value (State)
  protected final JGroupsLocalNode _localNode;
  protected final Latch _latch=new Latch();
  protected final Latch _latch2=new Latch();
  
  protected ElectionStrategy _electionStrategy;
  protected ClusterListener _listener;
  protected JGroupsDispatcher _dispatcher;
  protected Address _localAddress;
  protected JGroupsDestination _localDestination;
  protected JGroupsTopic _clusterTopic;
  
  public JGroupsCluster(String clusterName) throws ChannelException {
    super();
    _clusterName=clusterName;
    _channel=new JChannel("state_transfer.xml"); // uses an xml stack config file from JGroups distro
    //_channel=new JChannel("default_stack.xml"); // uses an xml stack config file from JGroups distro
    _localNode=new JGroupsLocalNode(this, _clusterState);
  }
  
  // ActiveCluster 'Cluster' API
  
  public Topic getDestination() {
    return _clusterTopic;
  }
  
  public Map getNodes() {
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
  
  public LocalNode getLocalNode() {
    return _localNode;
  }
  
  public void setElectionStrategy(ElectionStrategy strategy) {
    _electionStrategy=strategy;
  }
  
  public void send(Destination destination, Message message) throws JMSException {
    JGroupsObjectMessage msg=(JGroupsObjectMessage)message;
    JGroupsDestination dest=(JGroupsDestination)destination;
    try {
      msg.setCluster(this);
      msg.setJMSDestination(destination);
      if (_messageLog.isTraceEnabled()) _messageLog.trace("outgoing: "+msg.getObject()+" {"+_localDestination.getName()+"->"+dest.getName()+"} - "+msg.getIncomingCorrelationId()+"/"+msg.getOutgoingCorrelationId());
      msg.setCluster(null);
      _channel.send(dest.getAddress(), _localAddress, msg);
    } catch (Exception e) {
      _log.warn("unexpected JGroups problem", e);
      JMSException jmse=new JMSException("unexpected JGroups problem");
      jmse.setLinkedException(e);
      throw jmse;
    }
  }
  
  public MessageConsumer createConsumer(Destination destination) throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public MessageConsumer createConsumer(Destination destination, String selector) throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public Message createMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public BytesMessage createBytesMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public MapMessage createMapMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public ObjectMessage createObjectMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public StreamMessage createStreamMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public TextMessage createTextMessage() throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public TextMessage createTextMessage(String text) throws JMSException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public boolean waitForClusterToComplete(int expectedCount, long timeout) throws InterruptedException {
    throw new UnsupportedOperationException("NYI");
  }
  
  public void start() throws JMSException {
    try {
      //_channel.setOpt(Channel.LOCAL, Boolean.FALSE); // exclude ourselves from our own broadcasts... - BUT also from Unicasts :-(
      _channel.connect(_clusterName);
      _log.info("connected to channel");
      _clusterTopic=new JGroupsTopic("CLUSTER", null); // null address means broadcast to all members
      _localAddress=_channel.getLocalAddress();
      _clusterState.put(_localAddress, new HashMap()); // initialise our distributed state
      _localDestination=new JGroupsDestination(_localAddress);
      _localDestination.init(_localNode);
      _localNode.setDestination(_localDestination);
      _addressToDestination.put(_localAddress, _localDestination);
      _latch.release(); // allow new view to be accepted
    } catch (Exception e) {
      _log.warn("unexpected JGroups problem", e);
      JMSException jmse=new JMSException("unexpected JGroups problem");
      jmse.setLinkedException(e);
      throw jmse;
    }
    
    try{_latch2.acquire();}catch(Exception e){};
  }
  
  public void stop() throws JMSException {
  }
  
  // JGroups MembershipListener API
  
  public void viewAccepted(View newView) {
    
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
    
    synchronized (_addressToDestination) {
      Vector newMembers=newView.getMembers();
      
      // manage leavers
      for (Iterator i=_addressToDestination.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry=(Map.Entry)i.next();
        Address address=(Address)entry.getKey();
        JGroupsDestination destination=(JGroupsDestination)entry.getValue();
        if (!newMembers.contains(address)) {
          
          // notify listener
          if (_listener!=null && destination!=_localDestination) {
            _listener.onNodeFailed(new ClusterEvent(this, destination.getNode() ,ClusterEvent.FAILED_NODE));
          }
          // remove node
          _addressToDestination.remove(address);
          synchronized (_destinationToNode) {
            _destinationToNode.remove(destination);
          }
          
          // garbage collect this nodes share of cluster state...
          synchronized (_clusterState) {
            _clusterState.remove(address);
          }
          
          try {
            // elect coordinator
            if (_electionStrategy!=null)
              _electionStrategy.doElection(this);
          } catch (JMSException e) {
            _log.warn("problem performing coordinator election", e);
          }
        }
      }
      
      // notify joiners
      new Thread(new JoinerThread(newMembers)).start();

    }
    
    if (_log.isInfoEnabled()) _log.info("JGroups View: " + newView.getMembers());
    
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
        if (!_addressToDestination.containsKey(address)) {
          // insert node
          JGroupsDestination destination=new JGroupsDestination(address);
          JGroupsRemoteNode node=new JGroupsRemoteNode(JGroupsCluster.this, destination, _clusterState);
          destination.init(node);
          _addressToDestination.put(address, destination);
          
          synchronized (_destinationToNode) {
            _destinationToNode.put(destination, node);
          }
          
          // notify listener...
          if (destination!=_localDestination) {
            try {
              JGroupsObjectMessage tmp=(JGroupsObjectMessage)_dispatcher.exchangeSend(_localDestination, destination, new StateRequest(), 5000);
              Map state=((StateResponse)tmp.getObject()).getState();
              _clusterState.put(address, state);
            } catch (Exception e) {
              _log.warn("problem retrieving remote state for fresh joiner...", e);
              return;
            }
            
            // notify listener
            if (_listener!=null)
              _listener.onNodeAdd(new ClusterEvent(JGroupsCluster.this, node, ClusterEvent.ADD_NODE));
            
            // elect coordinator
            if (_electionStrategy!=null && _destinationToNode.size()>0) {
              try {
                Node coordinator=_electionStrategy.doElection(JGroupsCluster.this);
                _listener.onCoordinatorChanged(new ClusterEvent(JGroupsCluster.this, coordinator, ClusterEvent.ELECTED_COORDINATOR));
              } catch (JMSException e) {
                _log.error("unexpected problem whilst running coordinator election", e);
              }
            }
          }
        }
      }
      _latch2.release();
    }
  }
  
  public void suspect(Address suspected_mbr) {
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
    synchronized (_channel) {
      Address src=msg.getSrc();
      Address dest=msg.getDest();
      if (_excludeSelf && (dest==null || dest.isMulticastAddress()) && src==_localAddress) {
        _log.debug("ignoring message from self: "+msg);
      } else {
        Object o=msg.getObject();
        
        JGroupsObjectMessage message=(JGroupsObjectMessage)o;
        message.setCluster(this);
        Destination replyTo=getDestination(src);
        if (replyTo==null) {
          _log.warn("yikes"); // TODO - we need to map Address to a Destination NOW !
          _addressToDestination.put(src, replyTo=new JGroupsDestination(src));
        }
        try {
          message.setJMSReplyTo(replyTo);
          message.setJMSDestination(getDestination(dest));
        } catch (JMSException e) {
          _log.warn("unexpected JGroups problem", e);
        }
        _dispatcher.onMessage(message);
      }
    }
  }
  
  Streamer _streamer=new SimpleStreamer();
  
  public byte[] getState() {
    _log.info("GET STATE CALLED - sending: "+_clusterState);
    try {
      byte[] tmp=Utils.objectToByteArray(_clusterState, _streamer);
      return tmp; 
    } catch (Exception e) {
      _log.error("problem preparing cluster state for transfer", e);
      return new byte[0];
    }
  }
  
  public void setState(byte[] state) {
    if (state==null) {
      _log.info("SET STATE CALLED - received: null - we must be the first node");
      return;
    }
    
    try {
      Map nodes=(Map)Utils.byteArrayToObject(state, _streamer);
      _log.info("SET STATE CALLED - receiving: "+nodes);
      _clusterState.putAll(nodes); // copies in new values for node state
    } catch (Exception e) {
      _log.error("problem accepting cluster state transfer", e);
    }
  }
  
  // WADI 'JGroupsCluster' API
  
  public void init(JGroupsDispatcher dispatcher) throws Exception {
    _dispatcher=dispatcher;
    _dispatcher.register(this, "onMessage", StateRequest.class);
    _dispatcher.register(StateResponse.class, 5000);
  }
  
  public Channel getChannel() {
    return _channel;
  }
  
  public int getNumNodes() {
    synchronized (_destinationToNode) {
      return _destinationToNode.size();
    }
  }
  
  public Address getLocalAddress() {
    return _localAddress;
  }
  
  public JGroupsDestination getLocalDestination() {
    return _localDestination;
  }
  
  /**
   * Translate a JGroups Address into an ActiveCluster Destination.
   * @param address
   * @return
   */
  public JGroupsDestination getDestination(Address address) {
    JGroupsDestination destination;
    if (address==null || address.isMulticastAddress())
      destination=_clusterTopic;
    else {
      synchronized (_addressToDestination) {
        Object tmp=_addressToDestination.get(address);
        destination=(JGroupsDestination)tmp;
      }
    }
    
    if (destination==null) {
      _log.warn("unknown Address: "+address);
    }
    
    return destination;
  }
  
  // Dispatcher API
  
  public void setClusterListener(ClusterListener listener) {
    _listener=listener;
  }
  
  public void onMessage(ObjectMessage message, StateUpdate update) throws Exception {
    JGroupsDestination destination=(JGroupsDestination)message.getJMSReplyTo();
    Node node=destination.getNode();
    Map state=update.getState();
    if (node instanceof JGroupsRemoteNode)
      ((JGroupsRemoteNode)node).setState(state);
    else
      _log.warn("state update from non-remote node: "+node);
  }
  
  public void onMessage(ObjectMessage message, StateRequest request) throws Exception {
    Map state=_localNode.getState();
    _dispatcher.reply(message, new StateResponse(state));
  }
  
}
