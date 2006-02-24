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
import org.codehaus.wadi.dindex.messages.RelocationRequest;
import org.codehaus.wadi.dindex.messages.RelocationResponse;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.DeletePMToIM;
import org.codehaus.wadi.dindex.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.dindex.newmessages.EvacuatePMToIM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertPMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.dindex.newmessages.MovePMToSM;
import org.codehaus.wadi.dindex.newmessages.MoveSMToPM;
import org.codehaus.wadi.dindex.newmessages.MovePMToIM;
import org.codehaus.wadi.gridstate.Dispatcher;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LocalPartition extends AbstractPartition implements Serializable {

	protected transient Log _log=LogFactory.getLog(getClass());

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

  // a Locus provides two things :
  // - a sync point for the session destination which is not the destination itself
  // - a container for the session destination, reducing access to id:destination table
  static class Locus implements Serializable {
    
    protected Destination _destination;
    
    public Locus(Destination destination) {
      _destination=destination;
    }
    
    public Destination getDestination() {
      return _destination;
    }
    
    public void setDestination(Destination destination) {
      _destination=destination;
    }
    
  }
  
  public void onMessage(ObjectMessage message, InsertIMToPM request) {
    Destination newDestination=null;
    try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
    boolean success=false;
    String key=request.getKey();
    
    // optimised for expected case - id not already in use...
    Locus newLocus=new Locus(newDestination);
    synchronized (_map) {
      Locus oldLocus=(Locus)_map.put(key, newLocus); // remember location of new session
      if (oldLocus==null) {
        // id was not already in use - expected outcome
        success=true;
      } else {
        // id was already in use - unexpected outcome - put it back and forget new location
        _map.put(key, oldLocus);
      }
    }
    
    // log outside sync block...
    if (success) {
      if (_log.isDebugEnabled()) _log.debug("insert: "+key+" {"+_config.getNodeName(newDestination)+"}");
    } else {
      if (_log.isWarnEnabled()) _log.warn("insert: "+key+" {"+_config.getNodeName(newDestination)+"} failed - key already in use");
    }
    
    DIndexResponse response=new InsertPMToIM(success);
    _config.getDispatcher().reply(message, response);
  }

  public void onMessage(ObjectMessage message, DeleteIMToPM request) {
    String key=request.getKey();
    Locus locus=null;
    boolean success=false;
    
    synchronized (_map) {
      locus=(Locus)_map.remove(key);
    }
    
    if (locus!=null) {
      Destination oldDestination=locus.getDestination();
      if (_log.isDebugEnabled()) _log.debug("delete: "+key+" {"+_config.getNodeName(oldDestination)+"}");
      success=true;
    } else {
      if (_log.isWarnEnabled()) _log.warn("delete: "+key+" failed - key not present");
    }
    
    DIndexResponse response=new DeletePMToIM(success);
    _config.getDispatcher().reply(message, response);
  }

  // called on Partition Master
  public void onMessage(ObjectMessage message, MoveIMToPM request) {
    
    // TODO - whilst we are in here, we should have a SHARED lock on this Partition, so it cannot be moved
    // The Partitions lock should be held in the Facade, so that it can swap Partitions in and out whilst holding an exclusive lock
    // Partition may only be migrated when exclusive lock has been taken, this may only happen when all shared locks are released - this implies that no PM session locks will be in place...
    
    String key=request.getKey();
    Dispatcher _dispatcher=_config.getDispatcher();
    try {
      
      Locus locus=null;
      synchronized (_map) { // although we are not changing the structure of the map - others may be doing so...
        locus=(Locus)_map.get(key);
      }
      
      if (locus==null) {
        // session does not exist - tell IM
        _dispatcher.reply(message,new MovePMToIM());
        return;
      } else {
        synchronized (locus) { // ensures that no-one else tries to relocate session whilst we are doing so...
          
          Destination im=message.getJMSReplyTo();
          Destination sm=locus.getDestination();
          
          if (sm.equals(im)) {
            // session does exist - but is already located at the IM
            // whilst we were waiting for the partition lock, another thread must have migrated the session to the IM...
            // How can this happen - the first Thread should have been holding the InvocationLock...
            _log.warn("session already at required location: "+key+" {"+_config.getNodeName(im)+"} - should not happen");
            // FIXME - need to reply to IM with something
            // I think we need a further two messages here :
            // MovePMToIM - holds lock in Partition whilst informing IM that it already has session
            // MoveIMToPM2 - IM acquires local state-lock and then acks to PM so that it can release distributed lock in partition
          } else {
            // session does exist - we need to ask SM to move it to IM
            Destination pm=_dispatcher.getLocalDestination();
            String imCorrelationId=_dispatcher.getOutgoingCorrelationId(message);
            MovePMToSM request2=new MovePMToSM(key, im, pm, imCorrelationId);
            ObjectMessage tmp=_dispatcher.exchangeSend(pm, sm, request2, _config.getInactiveTime());
            
            if (tmp==null) {
              _log.error("move: "+key+" {"+_config.getNodeName(sm)+"->"+_config.getNodeName(im)+"}");
              // FIXME - error condition - what should we do ?
              // we should check whether SM is still alive and send it another message... - CONSIDER
            } else {
              MoveSMToPM response=(MoveSMToPM)tmp.getObject();
              if (response.getSuccess()) {
                // alter location
                locus.setDestination(im);
                if (_log.isDebugEnabled()) _log.debug("move: "+key+" {"+_config.getNodeName(sm)+"->"+_config.getNodeName(im)+"}");
              } else {
                if (_log.isWarnEnabled()) _log.warn("move: "+key+" {"+_config.getNodeName(sm)+"->"+_config.getNodeName(im)+"} - failed - no response from "+_config.getNodeName(sm));
              } 
            }
          }
        }
      }
    } catch (Exception e) {
      _log.error("UNEXPECTED PROBLEM RELOCATING STATE: "+key);
    }
  }

  public void onMessage(ObjectMessage message, EvacuateIMToPM request) {
		Destination newDestination=null;
		try{newDestination=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
    String key=request.getKey();
    boolean success=false;
    
    Locus locus=null;
		synchronized (_map) {
			locus=(Locus)_map.get(key);
		}
    
    Destination oldDestination=null;
    if (locus==null) {
      if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getNodeName(newDestination)+"} failed - key not in use");
    } else {
      synchronized (locus) {
        oldDestination=locus.getDestination();
        if (oldDestination.equals(newDestination)) {
          if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getNodeName(newDestination)+"} failed - evacuee is already there !");
        } else {
          locus.setDestination(newDestination);
          success=true;
        }
      }
    }

    if (_log.isDebugEnabled()) _log.debug("evacuate {"+request.getKey()+" : "+_config.getNodeName(oldDestination)+" -> "+_config.getNodeName(newDestination)+"}");
		DIndexResponse response=new EvacuatePMToIM(success);
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

	public ObjectMessage exchange(DIndexRequest request, long timeout) throws Exception {
		if (_log.isTraceEnabled()) _log.trace("local dispatch - needs optimisation");
		Dispatcher dispatcher=_config.getDispatcher();
		Destination from=dispatcher.getLocalDestination();
		Destination to=from;
		return dispatcher.exchangeSend(from, to, request, timeout);
	}

}
