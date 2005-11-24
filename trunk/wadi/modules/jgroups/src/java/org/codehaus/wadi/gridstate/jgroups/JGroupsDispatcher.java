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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.activecluster.ClusterEvent;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.impl.AbstractDispatcher;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;

/**
 * A Dispatcher for JGroups
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDispatcher extends AbstractDispatcher implements MessageListener, MembershipListener {

	protected final Destination _clusterDestination;
	protected final Map _clusterState;

	protected Channel _channel;
	protected MessageDispatcher _dispatcher;
	protected Destination _localDestination;
	protected Map _localState;
    protected Vector _members=new Vector();
    protected JGroupsCluster _cluster;

	public JGroupsDispatcher(String nodeName, String clusterName, long inactiveTime) {
		super(nodeName, clusterName, inactiveTime);
		_clusterState=new HashMap();
		_clusterDestination=new JGroupsDestination(null); // null Address means 'all nodes'
		register(this, "onMessage", JGroupsStateUpdate.class);
	}

    //-----------------------------------------------------------------------------------------------
	// MessageListener API

	public void receive(Message msg) {
		onMessage((JGroupsObjectMessage)msg.getObject());
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
		_channel=new JChannel();
		_dispatcher=new MessageDispatcher(_channel, this, this, null);
		_channel.connect(_clusterName);
		_localDestination=new JGroupsDestination(_channel.getLocalAddress());
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
		return _members.size();
	}

	public ObjectMessage createObjectMessage() {
		return new JGroupsObjectMessage();
	}

	public void send(Destination to, ObjectMessage message) throws Exception {
		_channel.send(((JGroupsDestination)to).getAddress(), _channel.getLocalAddress(), (JGroupsObjectMessage)message);
	}

	protected Map getState(Address address) {
		if (_channel.getLocalAddress()==address)
			return _localState;
		else
			return (Map)_clusterState.get(address);
	}

	public String getNodeName(Destination destination) {
		Map state=getState(((JGroupsDestination)destination).getAddress());
		if (state==null)
			return "<unknown>";
		else
			return (String)state.get("nodeName"); // TODO - use a static String
	}

	public Destination getLocalDestination() {
		return _localDestination;
	}

	public Destination getClusterDestination() {
		return _clusterDestination;
	}

	public Map getDistributedState() {
		return _localState;
	}

	public synchronized void setDistributedState(Map state) throws Exception {
		// this seems to be the only test that ActiveCluster does, so there is no point in us doing any more...
		if (_localState!=state) {
			_localState=state;
			ObjectMessage message=new JGroupsObjectMessage();
			message.setJMSReplyTo(_localDestination);
			message.setObject(new JGroupsStateUpdate(_localState));
			_channel.send(null, _channel.getLocalAddress(), (JGroupsObjectMessage)message); // broadcast
		}
	}

	public void onMessage(ObjectMessage message, JGroupsStateUpdate update) throws Exception {
		Address from=((JGroupsDestination)message.getJMSReplyTo()).getAddress();
        if (_log.isTraceEnabled()) _log.trace("STATE UPDATE: " + update + " from: " + from);
		synchronized (_clusterState) {
			_clusterState.put(from, update.getState());
		}
		// FIXME - Memory Leak here, until we start removing dead nodes from the table - need membership listener
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

    // MembershipListener API

    protected final Object _viewLock=new Object();

    public void viewAccepted(View newView) {
    	if (_log.isTraceEnabled()) _log.trace("handling JGroups viewAccepted("+newView+")...");

    	// this is meant to happen if a network split is healed and two
    	// clusters try to reconcile their separate states into one -
    	// I have a plan...
    	if(newView instanceof MergeView)
            if (_log.isWarnEnabled()) _log.warn("NYI - merging: view is " + newView);

    	synchronized (_viewLock) {
    		Vector oldMembers=_members;
    		Vector newMembers=newView.getMembers();

    		// notify nodes removed
    		for (int i=0; i<oldMembers.size(); i++) {
    			Address address=(Address)oldMembers.get(i);
    			if (!newMembers.contains(address))
    				if (_listener!=null)
    					_listener.onNodeAdd(new ClusterEvent(null, ensureNode(address) ,ClusterEvent.FAILED_NODE));
    		}
    		// notify nodes added
    		for (int i=0; i<newMembers.size(); i++) {
    			Address address=(Address)newMembers.get(i);
    			if (!oldMembers.contains(address))
    				if (_listener!=null)
    					_listener.onNodeAdd(new ClusterEvent(null, ensureNode(address) ,ClusterEvent.ADD_NODE));
    		}
		}

    	_members=newView.getMembers(); // N.B. This View includes ourself
        if (_log.isInfoEnabled()) _log.info("JGroups View: " + _members);

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

	//------------------------------------------------------------------------------------
	// aargh ! - we are using an AC i/f here - short-term - saves rewriting everything...

	protected ClusterListener _listener;

	public void setClusterListener(ClusterListener listener) {
		_listener=listener;
	}

	protected final Map _nodes=new HashMap();

	protected Node ensureNode(Address address) {
		Node node;
		synchronized (_nodes) {
			if ((node=(Node)_nodes.get(address))==null)
				_nodes.put(address, (node=new JGroupsNode(new JGroupsDestination(address))));
		}
		return node;
	}

	class JGroupsNode implements Node {

		protected final JGroupsDestination _destination;

		protected boolean _isCoordinator=false;

		JGroupsNode(JGroupsDestination destination) {
			_destination=destination;
		}

		public Destination getDestination() {
			return _destination;
		}

		public Map getState() {
			return JGroupsDispatcher.this.getState(_destination.getAddress());
		}

		public String getName() {
			return JGroupsDispatcher.this.getNodeName(_destination);
		}

		public boolean isCoordinator() {
			return false;
		}

		public Object getZone() {
			throw new UnsupportedOperationException("not used");
		}

	}

}
