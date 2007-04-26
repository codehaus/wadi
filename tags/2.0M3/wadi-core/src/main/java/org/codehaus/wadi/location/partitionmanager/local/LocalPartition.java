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
package org.codehaus.wadi.location.partitionmanager.local;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.util.Lease;
import org.codehaus.wadi.core.util.SimpleLease;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.location.partitionmanager.Partition;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.SessionRequestMessage;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class LocalPartition implements Partition, Serializable {
    private transient final Dispatcher dispatcher;
    private transient final LocalPeer peer;
    private transient final Log log;
    private transient final WaitableInt nbClients;
    private final int key;
    private final Map nameToLocation;
    private final long relocationTimeout;
    
    protected LocalPartition() {
        dispatcher = null;
        key = -1;
        peer = null;
        log = null;
        nameToLocation = null;
        relocationTimeout = -1;
        nbClients = null;
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
        
        nbClients = new WaitableInt(0);
        peer = dispatcher.getCluster().getLocalPeer();
        log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        nameToLocation = new HashMap();
    }

    public LocalPartition(Dispatcher dispatcher, LocalPartition prototype) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        this.dispatcher = dispatcher;
        
        nbClients = new WaitableInt(0);
        key = prototype.key;
        relocationTimeout = prototype.relocationTimeout;
        peer = dispatcher.getCluster().getLocalPeer();
        log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        nameToLocation = prototype.nameToLocation;
    }

    public int getKey() {
        return key;
    }

    public boolean isLocal() {
        return true;
    }

    public void onMessage(Envelope message, InsertIMToPM request) {
        nbClients.increment();
        try {
            LocalPartitionInsertIMToPMAction action = new LocalPartitionInsertIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            nbClients.decrement();
        }
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        nbClients.increment();
        try {
            LocalPartitionDeleteIMToPMAction action = new LocalPartitionDeleteIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            nbClients.decrement();
        }
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        nbClients.increment();
        try {
            LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher,
                    nameToLocation,
                    log,
                    relocationTimeout);
            action.onMessage(message, request);
        } finally {
            nbClients.decrement();
        }
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        nbClients.increment();
        try {
            LocalPartitionEvacuateIMToPMAction action = new LocalPartitionEvacuateIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            nbClients.decrement();
        }
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        Address target = dispatcher.getCluster().getLocalPeer().getAddress();
        return dispatcher.exchangeSend(target, request, timeout);
    }

    public void waitForClientCompletion() {
        try {
            nbClients.whenEqual(0, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WADIRuntimeException(e);
        }
    }
    
    public void put(Collection names, Peer peer) {
        synchronized (nameToLocation) {
            for (Iterator iter = names.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                Location oldLocation = (Location) nameToLocation.put(name, new Location(name, peer));
                if (null != oldLocation) {
                    nameToLocation.put(name, oldLocation);
                    throw new IllegalStateException("Key [" + name + "] is already bound to [" + oldLocation + "]");
                }
            }
        }
    }
    
    public Map getNameToLocation() {
        synchronized (nameToLocation) {
            return new HashMap(nameToLocation);
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
