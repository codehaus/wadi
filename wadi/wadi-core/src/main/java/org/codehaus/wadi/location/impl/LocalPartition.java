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
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.impl.SimpleLease;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.SessionResponseMessage;
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
public class LocalPartition implements Partition, Serializable {
    private transient final Dispatcher dispatcher;
    private transient final LocalPeer peer;
    private transient final Log _log;
    private final int key;
    private final Map _map;
    private final long relocationTimeout;
    
    protected LocalPartition() {
        dispatcher = null;
        key = -1;
        peer = null;
        _log = null;
        _map = null;
        relocationTimeout = -1;
    }
    
    public LocalPartition(Dispatcher dispatcher, int key, long relocationTimeout) {
        if (0 > key) {
            throw new IllegalArgumentException("key must be greater than 0");
        } else  if (null == dispatcher) {
            throw new IllegalArgumentException("peer is required");
        } else if (1 > relocationTimeout) {
            throw new IllegalArgumentException("relocationTimeout must be positive");
        }
        this.key = key;
        this.dispatcher = dispatcher;
        this.relocationTimeout = relocationTimeout;
        
        peer = dispatcher.getCluster().getLocalPeer();
        _log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        _map = new HashMap();
    }

    public LocalPartition(Dispatcher dispatcher, LocalPartition prototype) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        this.dispatcher = dispatcher;
        
        key = prototype.key;
        relocationTimeout = prototype.relocationTimeout;
        peer = dispatcher.getCluster().getLocalPeer();
        _log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        _map = prototype._map;
    }

    public int getKey() {
        return key;
    }

    public boolean isLocal() {
        return true;
    }

    public void onMessage(Envelope message, InsertIMToPM request) {
        Object key = request.getKey();
        Peer newPeer = request.getPeer();

        boolean success = false;
        Location newLocation = new Location(key, newPeer);
        synchronized (_map) {
            // remember location of new session
            Location oldLocation = (Location) _map.put(key, newLocation);
            if (oldLocation == null) {
                // id was not already in use - expected outcome
                success = true;
            } else {
                // id was already in use - unexpected outcome - put it back and forget new location
                _map.put(key, oldLocation);
            }
        }

        if (success) {
            if (_log.isDebugEnabled()) {
                _log.debug("inserted [" + key + "]@[" + newPeer + "]");
            }
        } else {
            _log.warn("insert [" + key + "]@[" + newPeer + "] failed; key already in use");
        }

        SessionResponseMessage response = new InsertPMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        Object key = request.getKey();
        
        Location location;
        synchronized (_map) {
            location = (Location) _map.remove(key);
        }

        boolean success = false;
        if (location != null) {
            if (_log.isDebugEnabled()) {
                _log.debug("deleted [" + key + "] located at [" + location.getSMPeer() + "]");
            }
            success = true;
        } else {
            _log.warn("delete [" + key + "] failed; key not present");
        }

        SessionResponseMessage response = new DeletePMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        Object key = request.getKey();
        try {
            Location location;
            synchronized (_map) {
                location = (Location) _map.get(key);
            }

            if (location == null) {
                // session does not exist - tell IM
                dispatcher.reply(message, new MovePMToIM());
                return;
            }
            
            // we need to make a decision here - based on the info available to us...
            // are we going to relocate the Session to the Invocation or the Invocation to the Session ?
            // call out to a pluggable strategy...
            
            // we need to know whether the IM's LBPolicy supports 'resticking' - otherwise relocating invocation is 
            // not such a smart thing to do...
            
            // if the InvocationMaster is shuttingDown, we know we should relocate the Invocation - lets go with that 
            // for now...
            // if the StateMaster is shuttingDown, we know we should relocate the session - but how would we know ?
            
            Peer imPeer = request.getIMPeer();
            Peer pmPeer = dispatcher.getCluster().getLocalPeer();
            
            String sourceCorrelationId = message.getSourceCorrelationId();
            boolean relocateSession = request.isRelocateSession();
            if (relocateSession) {
                relocateSession(location, imPeer, pmPeer, sourceCorrelationId);
            } else {
                relocateInvocation(location, imPeer, pmPeer, sourceCorrelationId);
            }
        } catch (Exception e) {
            _log.error("UNEXPECTED PROBLEM RELOCATING STATE: " + key);
        }
    }

    protected void relocateSession(Location location, Peer imPeer, Peer pmPeer, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        
        // session does exist - we need to ask SM to move it to IM
        Sync lock = location.getExclusiveLock();
        try {
            // ensures that no-one else tries to relocate session whilst we are doing so...
            // wait til we have a lock on Location before retrieving the SM
            lock.acquire(); 
            Peer smPeer = location.getSMPeer();
            if (smPeer == imPeer) {
                // session does exist - but is already located at the IM
                // whilst we were waiting for the partition lock, another thread
                // must have migrated the session to the IM...
                // How can this happen - the first Thread should have been
                // holding the InvocationLock...
                _log.warn("session [" + key + "] already at [" + imPeer + "]; should not happen");
                // FIXME - need to reply to IM with something
                // I think we need a further two messages here :
                // MovePMToIM - holds lock in Partition whilst informing IM that
                // it already has session
                // MoveIMToPM2 - IM acquires local state-lock and then acks to
                // PM so that it can release distributed lock in partition

                // sounds like we just keep going but pass a null session
                // directly to IM - save going round the houses via SM - whch is
                // IM
                // but can this actually happen ? - it has not yet !
                // I guess a session could be evacuated to a peer that is trying
                // to get hold of it...
                // this test should be made above

                // should only need a single response - if IM fails to receive
                // it, it can just ask again - no data is being transferred
            }

            MovePMToSM request = new MovePMToSM(key, imPeer, pmPeer, imCorrelationId);
            Envelope tmp;
            try {
                tmp = dispatcher.exchangeSend(smPeer.getAddress(), request, relocationTimeout);
            } catch (MessageExchangeException e) {
                _log.error("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed", e);
                // FIXME - error condition - what should we do ?
                // we should check whether SM is still alive and send it another
                // message... - CONSIDER
                return;
            }

            MoveSMToPM response = (MoveSMToPM) tmp.getPayload();
            if (response.getSuccess()) {
                // alter location
                location.setPeer(imPeer);
                if (_log.isDebugEnabled()) {
                    _log.debug("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "]");
                }
            } else {
                _log.warn("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed");
            }
        } catch (InterruptedException e) {
            _log.error("unexpected interruption waiting to perform Session relocation: " + key, e);
            Thread.currentThread().interrupt();
        } finally {
            lock.release();
        }
    }

    protected void relocateInvocation(Location location, Peer imPeer, Peer pmPeer, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        
        // TODO - parameterise
        long leasePeriod = 5000;
        try {
            Lease.Handle handle = location.getSharedLease().acquire(leasePeriod);
            // need to work on making these Serializable - TODO
//            handle = null;
            
            // wait til we have a lock on Location before retrieving the SM
            Peer smPeer = location.getSMPeer(); 
            if (smPeer == imPeer) {
                // do something similar to above - but remember - we only have a lease...
                _log.warn("session [" + key + "] already at [" + imPeer + "]; should not happen");
            }

            // send a message back to the IM, informing it that it has a lease
            // and should relocate its invocation to the SM...
            MovePMToIMInvocation response = new MovePMToIMInvocation(handle, leasePeriod, smPeer);
            dispatcher.reply(pmPeer.getAddress(), imPeer.getAddress(), imCorrelationId, response);
        } catch (InterruptedException e) {
            _log.error("unexpected interruption waiting to perform Invocation relocation: " + key, e);
        }
        // think about how a further message from the IM could release the
        // sharedLease...
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        Peer newPeer = request.getPeer();
        Object key = request.getKey();

        Location location;
        synchronized (_map) {
            location = (Location) _map.get(key);
        }

        boolean success = false;
        Peer oldPeer = null;
        if (location == null) {
            _log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; key not in use");
        } else {
            Sync lock = location.getExclusiveLock();
            try {
                lock.acquire();
                oldPeer = location.getSMPeer();
                if (oldPeer == newPeer) {
                    _log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; evacuee is already there");
                } else {
                    location.setPeer(newPeer);
                    if (_log.isDebugEnabled()) {
                        _log.debug("evacuate [" + request.getKey() + "] [" + oldPeer + "]->[" + newPeer + "]");
                    }
                    success = true;
                }
            } catch (InterruptedException e) {
                _log.error("unexpected interruption waiting to perform relocation: " + key, e);
            } finally {
                lock.release();
            }
        }

        SessionResponseMessage response = new EvacuatePMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            _log.warn("See exception", e);
        }
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        Address target = dispatcher.getCluster().getLocalPeer().getAddress();
        return dispatcher.exchangeSend(target, request, timeout);
    }

    public void put(String name, Peer peer) {
        synchronized (_map) {
            Location oldLocation = (Location) _map.put(name, new Location(name, peer));
            if (null != oldLocation) {
                _map.put(name, oldLocation);
                throw new IllegalStateException("Key [" + name + "] is already bound to [" + oldLocation + "]");
            }
        }
    }

    /**
     * a Location provides two things :
     * - a sync point for the session Peer which is not the Peer itself
     * - a container for the session Peer, reducing access to id:peer table
     */
    static class Location implements Serializable {
        protected Object _key;
        protected Peer peer;
        protected transient Lease _sharedLease;
        protected transient Sync _exclusiveLock;

        public Location(Object key, Peer peer) {
            _key = key;
            this.peer = peer;
            
            initLeaseAndSync(key);
        }

        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        private void initLeaseAndSync(Object key) {
            WriterPreferenceReadWriteLock rwLock = new WriterPreferenceReadWriteLock();
            _sharedLease = new SimpleLease(key.toString(), rwLock.readLock());
            _exclusiveLock = rwLock.writeLock();
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.writeObject(_key);
            stream.writeObject(peer);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            _key = stream.readObject();
            peer = (Peer) stream.readObject();
            
            initLeaseAndSync(_key);
        }

        public Lease getSharedLease() {
            return _sharedLease;
        }

        public Sync getExclusiveLock() {
            return _exclusiveLock;
        }

        public Peer getSMPeer() {
            return peer;
        }

        public Object getKey() {
            return _key;
        }
        
        public String toString() {
            return "Location key [" + _key + "]@[" + peer + "]";
        }

    }

    public String toString() {
        return "LocalPartition [" + key + "]@[" + peer + "]";
    }
    
    // 'Peer' API

    // strictly speaking, I'm not happy about exposing a Peer's Address. This should be a temporary
    // measure until the Dispatcher's public API is updated in terms of Peers rather than Addresses.
	public Address getAddress() {
		return peer.getAddress();
	}

	public String getName() {
		return peer.getName();
	}

	public PeerInfo getPeerInfo() {
		return peer.getPeerInfo();
	}

}
