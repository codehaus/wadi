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
package org.codehaus.wadi.sandbox.gridstate.jgroups;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.JGroupsDispatcherConfig;
import org.codehaus.wadi.impl.AbstractDispatcher;
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

	public JGroupsDispatcher() {
		super();
	}

    //-----------------------------------------------------------------------------------------------
	// MessageListener API

	public void receive(Message msg) {
		onMessage((JGroupsObjectMessage)msg.getObject());
	}

	public byte[] getState() {
		return null;
		// NYI
	}

	public void setState(byte[] state) {
		// NYI
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

	public String getNodeName(Destination destination) {
		return "<unknown>";
		// NYI
	}

	public Destination getLocalDestination() {
		return _localDestination;
	}

	public void setDistributedState(Map state) throws Exception {
		_log.warn("setDistributedState - NYI"); // NYI
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
