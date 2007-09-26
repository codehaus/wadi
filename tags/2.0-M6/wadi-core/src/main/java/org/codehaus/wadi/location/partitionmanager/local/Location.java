/**
 * Copyright 2007 The Apache Software Foundation
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

import org.codehaus.wadi.core.util.Lease;
import org.codehaus.wadi.core.util.SimpleLease;
import org.codehaus.wadi.group.Peer;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * a Location provides two things :
 * - a sync point for the session Peer which is not the Peer itself
 * - a container for the session Peer, reducing access to id:peer table
 * 
 * @version $Revision: 1538 $
 */
public class Location implements Serializable {
    protected Object _key;
    protected Peer peer;
    protected transient Lease _sharedLease;
    protected transient Sync _exclusiveLock;

    public Location(Object key, Peer peer) {
        _key = key;
        this.peer = peer;
        
        initLeaseAndSync(key);
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

    public synchronized void setPeer(Peer peer) {
        this.peer = peer;
    }

    public synchronized Peer getSMPeer() {
        return peer;
    }

    public Object getKey() {
        return _key;
    }
    
    public String toString() {
        return "Location key [" + _key + "]@[" + peer + "]";
    }

}