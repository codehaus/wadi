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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.SessionRequestMessage;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class BasicLocalPartition implements LocalPartition {
    private transient final Dispatcher dispatcher;
    private transient final LocalPeer peer;
    private transient final Log log;
    private transient final ReadWriteLock readWriteLock;
    private final int key;
    private final Map<Object, Location> nameToLocation;
    
    protected BasicLocalPartition() {
        dispatcher = null;
        key = -1;
        peer = null;
        log = null;
        nameToLocation = null;
        readWriteLock = null;
    }
    
    public BasicLocalPartition(Dispatcher dispatcher, int key) {
        if (0 > key) {
            throw new IllegalArgumentException("key must be greater than 0");
        } else  if (null == dispatcher) {
            throw new IllegalArgumentException("peer is required");
        }
        this.key = key;
        this.dispatcher = dispatcher;
        
        readWriteLock = new ReentrantReadWriteLock();
        peer = dispatcher.getCluster().getLocalPeer();
        log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        nameToLocation = new HashMap<Object, Location>();
    }

    public BasicLocalPartition(Dispatcher dispatcher, LocalPartition prototype) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        this.dispatcher = dispatcher;
        
        readWriteLock = new ReentrantReadWriteLock();
        key = prototype.getKey();
        peer = dispatcher.getCluster().getLocalPeer();
        log = LogFactory.getLog(getClass().getName() + "#" + key + "@" + peer.getName());
        nameToLocation = prototype.getNameToLocation();
    }

    public int getKey() {
        return key;
    }

    public boolean isLocal() {
        return true;
    }

    public void onMessage(Envelope message, InsertIMToPM request) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            LocalPartitionInsertIMToPMAction action = new LocalPartitionInsertIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            readLock.unlock();
        }
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            LocalPartitionDeleteIMToPMAction action = new LocalPartitionDeleteIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            readLock.unlock();
        }
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            readLock.unlock();
        }
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            LocalPartitionEvacuateIMToPMAction action = new LocalPartitionEvacuateIMToPMAction(dispatcher,
                    nameToLocation,
                    log);
            action.onMessage(message, request);
        } finally {
            readLock.unlock();
        }
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        Address target = dispatcher.getCluster().getLocalPeer().getAddress();
        return dispatcher.exchangeSend(target, request, timeout);
    }

    public void waitForClientCompletion() {
        Lock writeLock = readWriteLock.writeLock();
        try {
            writeLock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WADIRuntimeException(e);
        }
    }
    
    public void put(Collection<Object> names, Peer peer) {
        synchronized (nameToLocation) {
            for (Object name : names) {
                Location oldLocation = nameToLocation.put(name, new Location(name, peer));
                if (null != oldLocation) {
                    nameToLocation.put(name, oldLocation);
                    throw new IllegalStateException("Key [" + name + "] is already bound to [" + oldLocation + "]");
                }
            }
        }
    }
    
    public Map<Object, Location> getNameToLocation() {
        synchronized (nameToLocation) {
            return new HashMap<Object, Location>(nameToLocation);
        }
    }

    public void merge(LocalPartition content) {
        synchronized (nameToLocation) {
            nameToLocation.putAll(content.getNameToLocation());
        }
    }

    public String toString() {
        return "LocalPartition [" + key + "]@[" + peer + "]";
    }

}
