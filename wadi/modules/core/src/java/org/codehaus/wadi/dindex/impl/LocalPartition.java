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
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationResponse;
import org.codehaus.wadi.dindex.messages.RelocationRequest;
import org.codehaus.wadi.dindex.messages.RelocationResponse;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.DeletePMToIM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertPMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.dindex.newmessages.MovePMToSM;
import org.codehaus.wadi.dindex.newmessages.MoveSMToPM;
import org.codehaus.wadi.dindex.newmessages.MovePMToIM;
import org.codehaus.wadi.gridstate.Dispatcher;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class LocalPartition extends AbstractPartition implements Serializable {

	protected transient Log _log=LogFactory.getLog(getClass());
	protected transient Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

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
		_log=LogFactory.getLog(getClass().getName()+"#"+_key+"@"+_config.getLocalNodeName());
		_lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");
	}

	public boolean isLocal() {
		return true;
	}

	public String toString() {
		return "<LocalPartition:"+_key+"@"+(_config==null?"<unknown>":_config.getLocalNodeName())+">";
	}

	public void put(String name, Destination destination) {
		synchronized (_map) {
			// TODO - check key was not already in use...
			_map.put(name, destination);
		}
	}

	// we probably don't need the partition lock for this - but lets play safe to start with
	public void onMessage(ObjectMessage message, InsertIMToPM request) {
		Destination newDestination=null;
		try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
		boolean success=false;
		String key=request.getKey();
		Sync sync=null;
		try {
		  if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquiring: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync=_config.getPMSyncs().acquire(key); // TODO - PMSyncs are actually WLocks on a given sessions location (partition entry) - itegrate
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquired: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);

			synchronized (_map) {
				if (!_map.containsKey(key)) {
					_map.put(key, newDestination); // remember location of actual session...
					success=true;
				}
			}
			if (success) {
				if (_log.isDebugEnabled()) _log.debug("insert: "+key+" {"+_config.getNodeName(newDestination)+"}");
			} else {
				if (_log.isWarnEnabled()) _log.warn("insert: {"+key+" {"+_config.getNodeName(newDestination)+"} failed - key already in use");
			}

			DIndexResponse response=new InsertPMToIM(success);
			_config.getDispatcher().reply(message, response);
		} finally {
		  if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - releasing: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync.release();
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - released: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
		}
	}

	// we probably do not need to take the Partition lock whilst we do this, but, for the moment, lets do everything logically, then optimise later...
	public void onMessage(ObjectMessage message, DeleteIMToPM request) {
		Destination oldDestination;
		String key=request.getKey();
		Sync sync=null;
		try {
		  if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquiring: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync=_config.getPMSyncs().acquire(key); // TODO - PMSyncs are actually WLocks on a given sessions location (partition entry) - itegrate
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquired: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);

			synchronized (_map) {
				oldDestination=(Destination)_map.remove(key);
			}
			if (oldDestination==null) throw new IllegalStateException("session "+key+" is not known in this partition");
			if (_log.isDebugEnabled()) _log.debug("delete: "+key+" {"+_config.getNodeName(oldDestination)+"}");
			DIndexResponse response=new DeletePMToIM();
			_config.getDispatcher().reply(message, response);
		} finally {
		  if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - releasing: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync.release();
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - released: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
		}
	}

	public void onMessage(ObjectMessage message, DIndexRelocationRequest request) {
		Destination newDestination=null;
		try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
		Destination oldDestination=null;
		synchronized (_map) {
			oldDestination=(Destination)_map.put(request.getKey(), newDestination);
		}
		if (_log.isDebugEnabled()) _log.debug("relocation {"+request.getKey()+" : "+_config.getNodeName(oldDestination)+" -> "+_config.getNodeName(newDestination)+"}");
		DIndexResponse response=new DIndexRelocationResponse();
		_config.getDispatcher().reply(message, response);
	}

	public void onMessage(ObjectMessage message, DIndexForwardRequest request) {
		// we have got to someone who actually knows where we want to go.
		// strip off wrapper and deliver actual request to its final destination...
		String name=request.getKey();
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

	// called on Partition Master
	public void onMessage(ObjectMessage message1, MoveIMToPM request) {

		// TODO - whilst we are in here, we should have a SHARED lock on this Partition, so it cannot be moved
		// We should take an exclusive PM lock on the session ID, so no-one else can r/w its location whilst we are doing so.
		// The Partitions lock should be held in the Facade, so that it can swap Partitions in and out whilst holding an exclusive lock
		// Partition may only be migrated when exclusive lock has been taken, this may only happen when all shared locks are released - this implies that no PM session locks will be in place...

		String key=request.getKey();
		Dispatcher _dispatcher=_config.getDispatcher();
		// what if we are NOT the PM anymore ?
		// get write lock on location
		//String nodeName=_config.getLocalNodeName();
		Sync sync=null;
		//String agent=null;
		try {
			Destination im=message1.getJMSReplyTo();
			//agent=_config.getNodeName(im);

			// PMSyncs should prevent _map entry from being messed with whilst we are messing with it - lock should be exclusive
			// should synchronise map access - or is it ConcurrentHashMap ?
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquiring: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync=_config.getPMSyncs().acquire(key); // TODO - PMSyncs are actually WLocks on a given sessions location (partition entry) - itegrate
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - acquired: "+key+ " ["+Thread.currentThread().getName()+"]"+" : "+sync);

			// exchange messages with StateMaster
			Destination destination=(Destination)_map.get(key);

			if (destination==null) {
				// session does not exist - tell IM
				_dispatcher.reply(message1,new MovePMToIM());
				return;
			}

			if (destination.equals(im)) {
				// whilst we were waiting for the partition lock, another thread migrated the session into our InvocationMaster...
				// How can this happen - the first Thread should have been holding the InvocationLock...
				_log.warn("IM REQUESTING RELOCATION IS ALREADY SM");
			}


			// session does exist - ask the SM to move it to the IM

			// exchangeSendLoop GetPMToSM to SM
			Destination pm=_dispatcher.getLocalDestination();
			Destination sm=destination;
			String poCorrelationId=null;
			try {
				poCorrelationId=_dispatcher.getOutgoingCorrelationId(message1);
				//_log.info("Process Owner Correlation ID: "+poCorrelationId);
			} catch (Exception e) {
				_log.error("unexpected problem", e);
			}

			MovePMToSM request2=new MovePMToSM(key, im, pm, poCorrelationId);
			ObjectMessage message2=_dispatcher.exchangeSend(pm, sm, request2, _config.getInactiveTime());
			if (message2==null)
				_log.error("NO RESPONSE WITHIN TIMEFRAME - PANIC!");

			MoveSMToPM response=null; // the reply from the SM confirming successful move...
			try {
				response=(MoveSMToPM)message2.getObject();
			} catch (JMSException e) {
				_log.error("unexpected problem", e); // should be sorted in loop
			}

			if (response.getSuccess()) {
				// alter location
				Destination oldOwner=(Destination)_map.put(key, im); // The IM is now the SM
				_log.debug("move: "+key+" {"+_config.getNodeName(oldOwner)+"->"+_config.getNodeName(im)+"}");
			} else {
				_log.warn("state relocation failed: "+key);
			}
		} catch (JMSException e) {
			_log.error("could not read src address from incoming message");
		}
		finally {
		  if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - releasing: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
			sync.release();
			if (_lockLog.isTraceEnabled()) _lockLog.trace("Partition - released: "+key+" ["+Thread.currentThread().getName()+"]"+" : "+sync);
		}
	}



	public ObjectMessage exchange(DIndexRequest request, long timeout) throws Exception {
		if (_log.isTraceEnabled()) _log.trace("local dispatch - needs optimisation");
		Dispatcher dispatcher=_config.getDispatcher();
		Destination from=dispatcher.getLocalDestination();
		Destination to=from;
		return dispatcher.exchangeSend(from, to, request, timeout);
	}

}
