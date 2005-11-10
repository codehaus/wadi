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

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.impl.AbstractDispatcher;
import org.jgroups.Address;
import org.jgroups.Channel;
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

	protected Channel _channel;
	protected MessageDispatcher _dispatcher;
	protected Destination _localDestination;
	protected Map _localState;
	protected Map _clusterState;
	
	public JGroupsDispatcher() {
		super();
		_clusterState=new HashMap();
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
		_channel=((JGroupsDispatcherConfig)_config).getChannel();
		_dispatcher=new MessageDispatcher(_channel, this, null, null);
		_channel.connect("WADI"); // TODO - parameterise name
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
		_log.trace("STATE UPDATE: "+update+" from: "+from);
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
	
}
