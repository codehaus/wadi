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
package org.codehaus.wadi.location.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.DIndexRequest;
import org.codehaus.wadi.location.DIndexResponse;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.newmessages.DeleteIMToPM;
import org.codehaus.wadi.location.newmessages.DeletePMToIM;
import org.codehaus.wadi.location.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.location.newmessages.EvacuatePMToIM;
import org.codehaus.wadi.location.newmessages.InsertIMToPM;
import org.codehaus.wadi.location.newmessages.InsertPMToIM;
import org.codehaus.wadi.location.newmessages.MoveIMToPM;
import org.codehaus.wadi.location.newmessages.MovePMToIM;
import org.codehaus.wadi.location.newmessages.MovePMToSM;
import org.codehaus.wadi.location.newmessages.MoveSMToPM;

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
    _log=LogFactory.getLog(getClass().getName()+"#"+_key+"@"+_config.getLocalPeerName());
  }

  public boolean isLocal() {
    return true;
  }

  public String toString() {
    return "<LocalPartition:"+_key+"@"+(_config==null?"<unknown>":_config.getLocalPeerName())+">";
  }

   public void put(String name, Address address) {
     synchronized (_map) {
       // TODO - check key was not already in use...
       _map.put(name, new Locus(address));
     }
   }

  // a Locus provides two things :
  // - a sync point for the session destination which is not the destination itself
  // - a container for the session destination, reducing access to id:destination table
  static class Locus implements Serializable {

    protected Address _address;

    public Locus(Address address) {
      _address=address;
    }

    public Address getAddress() {
      return _address;
    }

    public void setAddress(Address address) {
      _address=address;
    }

  }

  public void onMessage(Message message, InsertIMToPM request) {
    Address newAddress=message.getReplyTo();
    boolean success=false;
    String key=request.getKey();

    // optimised for expected case - id not already in use...
    Locus newLocus=new Locus(newAddress);
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
      if (_log.isDebugEnabled()) _log.debug("insert: "+key+" {"+_config.getPeerName(newAddress)+"}");
    } else {
      if (_log.isWarnEnabled()) _log.warn("insert: "+key+" {"+_config.getPeerName(newAddress)+"} failed - key already in use");
    }

    DIndexResponse response=new InsertPMToIM(success);
    try {
        _config.getDispatcher().reply(message, response);
    } catch (MessageExchangeException e) {
        _log.warn("See exception", e);
    }
  }

  public void onMessage(Message message, DeleteIMToPM request) {
    String key=request.getKey();
    Locus locus=null;
    boolean success=false;

    synchronized (_map) {
      locus=(Locus)_map.remove(key);
    }

    if (locus!=null) {
      Address oldAddress=locus.getAddress();
      if (_log.isDebugEnabled()) _log.debug("delete: "+key+" {"+_config.getPeerName(oldAddress)+"}");
      success=true;
    } else {
      if (_log.isWarnEnabled()) _log.warn("delete: "+key+" failed - key not present");
    }

    DIndexResponse response=new DeletePMToIM(success);
    try {
        _config.getDispatcher().reply(message, response);
    } catch (MessageExchangeException e) {
        _log.warn("See exception", e);
    }
  }

  // called on Partition Master
  public void onMessage(Message message, MoveIMToPM request) {

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

          Address im=message.getReplyTo();
          Address sm=locus.getAddress();

          if (sm.equals(im)) {
            // session does exist - but is already located at the IM
            // whilst we were waiting for the partition lock, another thread must have migrated the session to the IM...
            // How can this happen - the first Thread should have been holding the InvocationLock...
            _log.warn("session already at required location: "+key+" {"+_config.getPeerName(im)+"} - should not happen");
            // FIXME - need to reply to IM with something
            // I think we need a further two messages here :
            // MovePMToIM - holds lock in Partition whilst informing IM that it already has session
            // MoveIMToPM2 - IM acquires local state-lock and then acks to PM so that it can release distributed lock in partition
          } else {
            // session does exist - we need to ask SM to move it to IM
            Address pm=_dispatcher.getLocalAddress();
            String imCorrelationId=message.getOutgoingCorrelationId();
            MovePMToSM request2=new MovePMToSM(key, im, pm, imCorrelationId);
            Message tmp=_dispatcher.exchangeSend(sm, request2, _config.getInactiveTime());

            if (tmp==null) {
              _log.error("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"}");
              // FIXME - error condition - what should we do ?
              // we should check whether SM is still alive and send it another message... - CONSIDER
            } else {
              MoveSMToPM response=(MoveSMToPM)tmp.getPayload();
              if (response.getSuccess()) {
                // alter location
                locus.setAddress(im);
                if (_log.isDebugEnabled()) _log.debug("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"}");
              } else {
                if (_log.isWarnEnabled()) _log.warn("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"} - failed - no response from "+_config.getPeerName(sm));
              }
            }
          }
        }
      }
    } catch (Exception e) {
      _log.error("UNEXPECTED PROBLEM RELOCATING STATE: "+key);
    }
  }

  public void onMessage(Message message, EvacuateIMToPM request) {
    Address newAddress=message.getReplyTo();
    String key=request.getKey();
    boolean success=false;

    Locus locus=null;
    synchronized (_map) {
      locus=(Locus)_map.get(key);
    }

    Address oldAddress=null;
    if (locus==null) {
      if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getPeerName(newAddress)+"} failed - key not in use");
    } else {
      synchronized (locus) {
        oldAddress=locus.getAddress();
        if (oldAddress.equals(newAddress)) {
          if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getPeerName(newAddress)+"} failed - evacuee is already there !");
        } else {
          locus.setAddress(newAddress);
	  if (_log.isDebugEnabled()) _log.debug("evacuate {"+request.getKey()+" : "+_config.getPeerName(oldAddress)+" -> "+_config.getPeerName(newAddress)+"}");
          success=true;
        }
      }
    }

    DIndexResponse response=new EvacuatePMToIM(success);
    try {
        _config.getDispatcher().reply(message, response);
    } catch (MessageExchangeException e) {
        _log.warn("See exception", e);
    }
  }

  public Message exchange(DIndexRequest request, long timeout) throws Exception {
    if (_log.isTraceEnabled()) _log.trace("local dispatch - needs optimisation");
    Dispatcher dispatcher=_config.getDispatcher();
    Address target=dispatcher.getLocalAddress();
    return dispatcher.exchangeSend(target, request, timeout);
  }

}
