/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.activecluster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Message;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterMessage implements Message {
    
    private static final String TARGET_COR_ID_KEY = "TARGET_COR_ID_KEY";
	private static final String SOURCE_COR_ID_KEY = "SOURCE_COR_ID_KEY";

	protected ActiveClusterPeer _address;
	protected ActiveClusterPeer _replyTo;
	protected Serializable _payload;
	protected String _sourceCorrelationId;
	protected String _targetCorrelationId;
	private final Map properties;

	public ActiveClusterMessage(ActiveClusterCluster cluster, ObjectMessage message) throws JMSException {
	    PayloadInfo payloadInfo = (PayloadInfo) message.getObject();
	    _payload = payloadInfo.payload;
		_address = payloadInfo.address;
		_replyTo = payloadInfo.replyTo;
		_sourceCorrelationId = message.getStringProperty(SOURCE_COR_ID_KEY);
		_targetCorrelationId = message.getStringProperty(TARGET_COR_ID_KEY);
		properties = payloadInfo.properties;
	}

	public ActiveClusterMessage() {
		properties = new HashMap();
	}

	public ObjectMessage fill(ObjectMessage message) throws JMSException {
		message.setJMSDestination(_address.getACDestination());
		message.setJMSReplyTo(_replyTo.getACDestination());
		message.setStringProperty(SOURCE_COR_ID_KEY, _sourceCorrelationId);
		message.setStringProperty(TARGET_COR_ID_KEY, _targetCorrelationId);
		message.setObject(new PayloadInfo(properties, _payload, _address, _replyTo));
		return message;
	}

	public String getTargetCorrelationId() {
		return _targetCorrelationId;
	}

	public void setTargetCorrelationId(String correlationId) {
		_targetCorrelationId = correlationId;
	}

	public String getSourceCorrelationId() {
		return _sourceCorrelationId;
	}

	public void setSourceCorrelationId(String correlationId) {
		_sourceCorrelationId = correlationId;
	}

	public Address getReplyTo() {
		return _replyTo;
	}

	public void setReplyTo(Address replyTo) {
		_replyTo = (ActiveClusterPeer) replyTo;
	}

	public Address getAddress() {
		return _address;
	}

	public void setAddress(Address address) {
		_address = (ActiveClusterPeer) address;
	}

	public void setPayload(Serializable payload) {
		_payload = payload;
	}

	public Serializable getPayload() {
		return _payload;
	}

	public Map getProperties() {
		return Collections.unmodifiableMap(properties);
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

	private static class PayloadInfo implements Serializable {
		private final Map properties;
		private final Serializable payload;
        private final ActiveClusterPeer address;
        private final ActiveClusterPeer replyTo;
        
        public PayloadInfo(Map properties, Serializable payload, ActiveClusterPeer address, ActiveClusterPeer replyTo) {
            this.properties = properties;
            this.payload = payload;
            this.address = address;
            this.replyTo = replyTo;
        }
		
	}
}
