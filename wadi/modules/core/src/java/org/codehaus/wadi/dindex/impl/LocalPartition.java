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
package org.codehaus.wadi.dindex.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.PartitionConfig;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.DIndexResponse;
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexDeletionResponse;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionResponse;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationResponse;
import org.codehaus.wadi.dindex.messages.RelocationRequest;
import org.codehaus.wadi.dindex.messages.RelocationResponse;

public class LocalPartition extends AbstractPartition implements Serializable {
	
	protected static final Log _log = LogFactory.getLog(LocalPartition.class);
	
	protected Map _map=new HashMap();
	protected transient PartitionConfig _config;
	
	public LocalPartition(int key) {
		super(key);
	}
	
	protected LocalPartition() {
		super();
		// for deserialisation...
	}
	
	public void init(PartitionConfig config) {
		_config=config;
	}
	
	public boolean isLocal() {
		return true;
	}
	
	public String toString() {
		return "<local:"+_key+">";
	}
	
	public void put(String name, Destination destination) {
		synchronized (_map) {
			// TODO - check key was not already in use...
			_map.put(name, destination);	
		}
	}
	
	public void onMessage(ObjectMessage message, DIndexInsertionRequest request) {
		Destination newDestination=null;
		try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
		boolean success=false;
		String key=request.getName();
		synchronized (_map) {
			if (!_map.containsKey(key))
				_map.put(key, newDestination); // remember location of actual session...
		}
		if (success) {
			if (_log.isDebugEnabled()) _log.debug("insertion {"+request.getName()+" : "+_config.getNodeName(newDestination) + "}");
		} else {
			if (_log.isWarnEnabled()) _log.warn("insertion {"+request.getName()+" : "+_config.getNodeName(newDestination) + "} failed - key alread in use");
		}
		DIndexResponse response=new DIndexInsertionResponse(success);
		_config.getDispatcher().reply(message, response);
	}
	
	public void onMessage(ObjectMessage message, DIndexDeletionRequest request) {
		Destination oldDestination;
		String key=request.getName();
		synchronized (_map) {
			oldDestination=(Destination)_map.remove(key);
		}
		if (oldDestination==null) throw new IllegalStateException("session "+key+" is not known in this partition");
		if (_log.isDebugEnabled()) _log.debug("deletion {"+key+" : "+_config.getNodeName(oldDestination)+"}");
		DIndexResponse response=new DIndexDeletionResponse();
		_config.getDispatcher().reply(message, response);
	}
	
	public void onMessage(ObjectMessage message, DIndexRelocationRequest request) {
		Destination newDestination=null;
		try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
		Destination oldDestination=null;
		synchronized (_map) {
			oldDestination=(Destination)_map.put(request.getName(), newDestination);
		}
		if (_log.isDebugEnabled()) _log.debug("relocation {"+request.getName()+" : "+_config.getNodeName(oldDestination)+" -> "+_config.getNodeName(newDestination)+"}");
		DIndexResponse response=new DIndexRelocationResponse();
		_config.getDispatcher().reply(message, response);
	}
	
	public void onMessage(ObjectMessage message, DIndexForwardRequest request) {
		// we have got to someone who actually knows where we want to go.
		// strip off wrapper and deliver actual request to its final destination...
		String name=request.getName();
		Destination destination=null;
		synchronized (_map) {
			destination=(Destination)_map.get(name);
		}
		if (destination==null) { // session could not be located...
			DIndexRequest r=request.getRequest();
			if (r instanceof RelocationRequest) {
				assert message!=null;
				assert name!=null;
				assert _config!=null;
				_config.getDispatcher().reply(message, new RelocationResponse(name));
			} else {
				if (_log.isWarnEnabled()) _log.warn("unexpected nested request structure - ignoring: " + r);
			}
		} else { // session succesfully located...
			assert destination!=null;
			assert request!=null;
			assert _config!=null;
			if (_log.isTraceEnabled()) _log.trace("directing: " + request + " -> " + _config.getNodeName(destination));
			if (!_config.getDispatcher().forward(message, destination, request.getRequest()))
				_log.warn("could not forward message");
		}
	}
	
}
