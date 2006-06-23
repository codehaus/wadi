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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Lease;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.impl.SimpleLease;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.SessionResponseMessage;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.InsertPMToIM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToPM;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class LocalPartition extends AbstractPartition implements Serializable {

    protected transient Log _log=LogFactory.getLog(getClass());

    protected Map _map=new HashMap();
    protected transient PartitionConfig _config;

    public LocalPartition(int key) {
        super(key);
    }

    public void init(PartitionConfig config) {
        _config=config;
        _log=LogFactory.getLog(getClass().getName()+"#"+_key+"@"+_config.getLocalPeerName());
    }

    // 'java.lang.Object' API
    
    public String toString() {
        return "<LocalPartition:"+_key+"@"+(_config==null?"<unknown>":_config.getLocalPeerName())+">";
    }

    // 'Partition' API
    
    public boolean isLocal() {
        return true;
    }

    public void onMessage(Message message, InsertIMToPM request) {
        Address newAddress=message.getReplyTo();
        boolean success=false;
        Object key=request.getKey();

        // optimised for expected case - id not already in use...
        Location newLocation=new Location(key, newAddress);
        synchronized (_map) {
            Location oldLocation=(Location)_map.put(key, newLocation); // remember location of new session
            if (oldLocation==null) {
                // id was not already in use - expected outcome
                success=true;
            } else {
                // id was already in use - unexpected outcome - put it back and forget new location
                _map.put(key, oldLocation);
            }
        }

        // log outside sync block...
        if (success) {
            if (_log.isDebugEnabled()) _log.debug("insert: "+key+" {"+_config.getPeerName(newAddress)+"}");
        } else {
            if (_log.isWarnEnabled()) _log.warn("insert: "+key+" {"+_config.getPeerName(newAddress)+"} failed - key already in use");
        }

        SessionResponseMessage response=new InsertPMToIM(success);
        try {
            _config.getDispatcher().reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public void onMessage(Message message, DeleteIMToPM request) {
        Object key=request.getKey();
        Location location=null;
        boolean success=false;

        synchronized (_map) {
            location=(Location)_map.remove(key);
        }

        if (location!=null) {
            Address oldAddress=location.getAddress();
            if (_log.isDebugEnabled()) _log.debug("delete: "+key+" {"+_config.getPeerName(oldAddress)+"}");
            success=true;
        } else {
            if (_log.isWarnEnabled()) _log.warn("delete: "+key+" failed - key not present");
        }

        SessionResponseMessage response=new DeletePMToIM(success);
        try {
            _config.getDispatcher().reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public void onMessage(Message message, MoveIMToPM request) {

        // TODO - whilst we are in here, we should have a SHARED lock on this Partition, so it cannot be moved
        // The Partitions lock should be held in the Facade, so that it can swap Partitions in and out whilst holding an exclusive lock
        // Partition may only be migrated when exclusive lock has been taken, this may only happen when all shared locks are released - this implies that no PM session locks will be in place...

        Object key=request.getKey();
        Dispatcher dispatcher=_config.getDispatcher();
        try {

            Location location=null;
            synchronized (_map) { // although we are not changing the structure of the map - others may be doing so...
                location=(Location)_map.get(key);
            }

            if (location==null) {
                // session does not exist - tell IM
                dispatcher.reply(message,new MovePMToIM());
                return;
            } else {

                // we need to make a decision here - based on the info available to us...
                // are we going to relocate the Session to the Invocation or the Invocation to the Session ?
                // call out to a pluggable strategy...

                // we need to know whether the IM's LBPolicy supports 'resticking' - otherwise relocating invocation is not such a smart thing to do...

                // if the InvocationMaster is shuttingDown, we know we should relocate the Invocation - lets go with that for now...
                // if the StateMaster is shuttingDown, we know we should relocate the session - but how would we know ?

                Address im=message.getReplyTo();
                Address pm=dispatcher.getCluster().getLocalPeer().getAddress();



                String sourceCorrelationId=message.getSourceCorrelationId();
                boolean relocateSession=!request.getShuttingDown();
                // tmp test...
                //relocateSession=(Math.random()>0.5); // 50/50
                relocateSession=true;

                //_log.info("**** RELOCATING: "+(relocateSession?"SESSION":"INVOCATION")+" *****");

                if (relocateSession)
                    relocateSession(location, key, im, pm, sourceCorrelationId);
                else
                    relocateInvocation(location, key, im, pm, sourceCorrelationId);
            }
        } catch (Exception e) {
            _log.error("UNEXPECTED PROBLEM RELOCATING STATE: "+key);
        }
    }

    protected void relocateSession(Location location, Object key, Address im, Address pm, String imCorrelationId) throws MessageExchangeException {
        // session does exist - we need to ask SM to move it to IM
        Sync lock=location.getExclusiveLock();
        try {
            lock.acquire(); // ensures that no-one else tries to relocate session whilst we are doing so...
            Address sm=location.getAddress(); // wait til we have a lock on Location before retrieving the SM
            if (sm.equals(im)) {
                // session does exist - but is already located at the IM
                // whilst we were waiting for the partition lock, another thread must have migrated the session to the IM...
                // How can this happen - the first Thread should have been holding the InvocationLock...
                _log.warn("session already at required location: "+key+" {"+_config.getPeerName(im)+"} - should not happen");
                // FIXME - need to reply to IM with something
                // I think we need a further two messages here :
                // MovePMToIM - holds lock in Partition whilst informing IM that it already has session
                // MoveIMToPM2 - IM acquires local state-lock and then acks to PM so that it can release distributed lock in partition

                // sounds like we just keep going but pass a null session directly to IM - save going round the houses via SM - whch is IM
                // but can this actually happen ? - it has not yet !
                // I guess a session could be evacuated to a peer that is trying to get hold of it...
                // this test should be made above

                // should only need a single response - if IM fails to receive it, it can just ask again - no data is being transferred
            }
            
            Dispatcher dispatcher=_config.getDispatcher();

            MovePMToSM request=new MovePMToSM(key, im, pm, imCorrelationId);
            Message tmp=dispatcher.exchangeSend(sm, request, _config.getInactiveTime());

            if (tmp==null) {
                _log.error("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"}");
                // FIXME - error condition - what should we do ?
                // we should check whether SM is still alive and send it another message... - CONSIDER
            } else {
                MoveSMToPM response=(MoveSMToPM)tmp.getPayload();
                if (response.getSuccess()) {
                    // alter location
                    location.setAddress(im);
                    if (_log.isDebugEnabled()) _log.debug("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"}");
                } else {
                    if (_log.isWarnEnabled()) _log.warn("move: "+key+" {"+_config.getPeerName(sm)+"->"+_config.getPeerName(im)+"} - failed - no response from "+_config.getPeerName(sm));
                }
            }
        } catch (InterruptedException e) {
            _log.error("unexpected interruption waiting to perform Session relocation: "+key, e);
        } finally {
            lock.release();
        }
    }

    protected void relocateInvocation(Location location, Object key, Address im, Address pm, String imCorrelationId) throws MessageExchangeException {
        long leasePeriod=5000;  // TODO - parameterise
        try {
            Lease.Handle handle=location.getSharedLease().acquire(leasePeriod);
            handle=null; // need to work on making these Serializable - TODO
            Address sm=location.getAddress(); // wait til we have a lock on Location before retrieving the SM
            
            if (sm.equals(im)) {
                // do something similar to above - but remember - we only have a lease...
                _log.warn("session already at required location: "+key+" {"+_config.getPeerName(im)+"} - should not happen");
            }

            Dispatcher dispatcher=_config.getDispatcher();
            // send a message back to the IM, informing it that it has a lease and should relocate its invocation to the SM...
            MovePMToIMInvocation response=new MovePMToIMInvocation(handle, leasePeriod, sm);
            dispatcher.reply(pm, im, imCorrelationId, response);
        } catch (InterruptedException e) {
            _log.error("unexpected interruption waiting to perform Invocation relocation: "+key, e);
        }
        // think about how a further message from the IM could release the sharedLease...
    }

    public void onMessage(Message message, EvacuateIMToPM request) {
        Address newAddress=message.getReplyTo();
        Object key=request.getKey();
        boolean success=false;

        Location location=null;
        synchronized (_map) {
            location=(Location)_map.get(key);
        }

        Address oldAddress=null;
        if (location==null) {
            if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getPeerName(newAddress)+"} failed - key not in use");
        } else {
            Sync lock=location.getExclusiveLock();
            try {
                lock.acquire();
                oldAddress=location.getAddress();
                if (oldAddress.equals(newAddress)) {
                    if (_log.isWarnEnabled()) _log.warn("evacuate: "+key+" {"+_config.getPeerName(newAddress)+"} failed - evacuee is already there !");
                } else {
                    location.setAddress(newAddress);
                    if (_log.isDebugEnabled()) _log.debug("evacuate {"+request.getKey()+" : "+_config.getPeerName(oldAddress)+" -> "+_config.getPeerName(newAddress)+"}");
                    success=true;
                }
            } catch (InterruptedException e) {
                _log.error("unexpected interruption waiting to perform relocation: "+key, e);
            } finally {
                lock.release();
            }
        }

        SessionResponseMessage response=new EvacuatePMToIM(success);
        try {
            _config.getDispatcher().reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public Message exchange(SessionRequestMessage request, long timeout) throws Exception {
        if (_log.isTraceEnabled()) _log.trace("local dispatch - needs optimisation");
        Dispatcher dispatcher=_config.getDispatcher();
        Address target=dispatcher.getCluster().getLocalPeer().getAddress();
        return dispatcher.exchangeSend(target, request, timeout);
    }

    // 'LocalPartition' API
    
    // used by PartitionManager...
    
    public void put(String name, Address address) {
        synchronized (_map) {
            // TODO - check key was not already in use...
            _map.put(name, new Location(name, address));
        }
    }

    // a Location provides two things :
    // - a sync point for the session Address which is not the Address itself
    // - a container for the session Address, reducing access to id:address table
    static class Location implements Serializable {

        protected Object _key; // needed for logging :-(
        protected Address _address;
        protected transient Lease _sharedLease;
        protected transient Sync _exclusiveLock;

        public Location(Object key, Address address) {
            _key=key;
            _address=address;
            WriterPreferenceReadWriteLock rwLock=new WriterPreferenceReadWriteLock();
            _sharedLease=new SimpleLease(key.toString(), rwLock.readLock());
            _exclusiveLock=rwLock.writeLock();
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeObject(_key);
            stream.writeObject(_address);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            _key=stream.readObject();
            _address=(Address)stream.readObject();
            WriterPreferenceReadWriteLock rwLock=new WriterPreferenceReadWriteLock();
            _sharedLease=new SimpleLease(_key.toString(), rwLock.readLock());
            _exclusiveLock=rwLock.writeLock();
        }

        public Address getAddress() {
            return _address;
        }

        public void setAddress(Address address) {
            _address=address;
        }

        public Lease getSharedLease() {
            return _sharedLease;
        }

        public Sync getExclusiveLock() {
            return _exclusiveLock;
        }

    }

}
