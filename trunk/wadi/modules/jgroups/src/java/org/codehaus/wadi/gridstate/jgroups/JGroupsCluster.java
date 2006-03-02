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
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.View;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster, MembershipListener {

  protected final Log _log = LogFactory.getLog(getClass());
  protected final List _clusterListeners=new ArrayList();
  protected final Map _addressToDestination=new HashMap();
  protected final Map _destinationToNode=new HashMap(); // we don't need this, but i/f does - yeugh !

  protected ElectionStrategy _electionStrategy;
  protected ClusterListener _listener;
  protected JGroupsLocalNode _localNode;
  protected Address _localAddress;
  protected JGroupsDestination _localDestination;
  protected JGroupsTopic _clusterTopic;
  protected Channel _channel;

	public JGroupsCluster() {
		super();
	}

  // 'Cluster' api
  
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
	  try {
	    _channel.send(((JGroupsDestination)destination).getAddress(), _localAddress, (JGroupsObjectMessage)message);
	  } catch (Exception e) {
	    JMSException jmse=new JMSException("unexpected JGroups problem");
	    jmse.setLinkedException(e);
	    throw jmse;
	  }
	}

	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MessageConsumer createConsumer(Destination destination, String selector) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public Message createMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public BytesMessage createBytesMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MapMessage createMapMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public ObjectMessage createObjectMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public StreamMessage createStreamMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public TextMessage createTextMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public TextMessage createTextMessage(String text) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public boolean waitForClusterToComplete(int expectedCount, long timeout) throws InterruptedException {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}

	public void start() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}

	public void stop() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}

  // MembershipListener API
  
  public void viewAccepted(View newView) {
    if (_log.isTraceEnabled()) _log.trace("handling JGroups viewAccepted("+newView+")...");
    
    // this is meant to happen if a network split is healed and two
    // clusters try to reconcile their separate states into one -
    // I have a plan...
    if(newView instanceof MergeView)
      if (_log.isWarnEnabled()) _log.warn("NYI - merging: view is " + newView);
    
    synchronized (_addressToDestination) {
      Vector newMembers=newView.getMembers();
      
      // manage leavers
      for (Iterator i=_addressToDestination.entrySet().iterator(); i.hasNext(); ){
        Map.Entry entry=(Map.Entry)i.next();
        Address address=(Address)entry.getKey();
        JGroupsDestination destination=(JGroupsDestination)entry.getValue();
        if (!newMembers.contains(address)) {
          
          // notify listener
          if (_listener!=null) {
            _listener.onNodeFailed(new ClusterEvent(this, destination.getNode() ,ClusterEvent.FAILED_NODE));
          }
          // remove node
          _addressToDestination.remove(address);
          synchronized (_destinationToNode) {
            _destinationToNode.remove(destination);
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
      for (int i=0; i<newMembers.size(); i++) {
        Address address=(Address)newMembers.get(i);
        if (!_addressToDestination.containsKey(address)) {
          // insert node
          JGroupsDestination destination=new JGroupsDestination(address);
          JGroupsRemoteNode node=new JGroupsRemoteNode(this, destination, null);
          destination.init(node);
          _addressToDestination.put(address, destination);
//          synchronized (_destinationToNode) {
//          _destinationToNode.put(destination, node);
//          }
//          // notify listener
//          if (_listener!=null)
//            _listener.onNodeAdd(new ClusterEvent(this, node ,ClusterEvent.ADD_NODE));
        }
      }
    }
    
    if (_log.isInfoEnabled()) _log.info("JGroups View: " + newView.getMembers());
    
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
  
  // 'JGroupsCluster' api
  
  public void init(Channel channel) throws Exception {
    _channel=channel;
    _clusterTopic=new JGroupsTopic("CLUSTER", null); // null address means broadcast to all members
    _clusterTopic.init(null); // no corresponding Node
    _localAddress=_channel.getLocalAddress();
    _log.info("LOCAL ADDRESS: "+_localAddress);
    _localDestination=new JGroupsDestination(_localAddress);
    _localNode=new JGroupsLocalNode(this, _localDestination, null);
    _localDestination.init(_localNode);
    _addressToDestination.put(_localAddress, _localDestination);
    // _destinationToNode.put(_localDestination, _localNode); // getNodes() should NOT contain LocalNode...

    // publish our distributed state
    _localNode.setState(_localNode.getState());
    
//    // have a look around...
//    View view=_channel.getView();
//    viewAccepted(view); // build our rep
//    Vector members=view.getMembers();
//    for (int i=0; i<members.size(); i++) {
//      Address address=(Address)members[i];
//      // simulate a join...
//      JGroupsObjectMessage message=new JGroupsObjectMessage();
//      message.setJMSReplyTo(getDestination(address));
//      onMessage()
//    }
    
    // hhmm... - I guess when we send out update message, anyone who does not know us should send one back ?
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
  
  public int getNumNodes() {
    synchronized (_destinationToNode) {
      return _destinationToNode.size();
    }
  }
  
  public void setClusterListener(ClusterListener listener) {
    _listener=listener;
  }
  
  public void onMessage(ObjectMessage message, JGroupsStateUpdate update) throws Exception {
    JGroupsDestination destination=(JGroupsDestination)message.getJMSReplyTo();
    if (_log.isTraceEnabled()) _log.trace("STATE UPDATE: " + update + " from: " + /*getNodeName(*/destination/*)*/);
    Node node=destination.getNode();
    Map state=update.getState();
    if (node instanceof JGroupsRemoteNode)
      ((JGroupsRemoteNode)node).setState(state);
    else
      _log.warn("state update from non-remote node: "+node);

    // from viewAccepted()
    synchronized (_destinationToNode) {
      if (_destinationToNode.put(destination, node)==null) {
        // first time we have seen this node - send it our own state
        //.....
      }
    }
    // notify listener
    if (_listener!=null)
      _listener.onNodeAdd(new ClusterEvent(this, node, ClusterEvent.ADD_NODE));
    
    // elect coordinator
    if (_electionStrategy!=null)
      _electionStrategy.doElection(this);
  }
  
}
