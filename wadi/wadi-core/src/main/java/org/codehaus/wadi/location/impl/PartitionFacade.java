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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionFacade extends AbstractPartition {

    protected final ReadWriteLock _lock = new WriterPreferenceReadWriteLock();
    protected final LinkedQueue _queue = new LinkedQueue();
    protected final PartitionConfig _config;
    protected final Log _log;
    protected final String _keyString;
    protected Partition _content;

    public PartitionFacade(int key, Partition content, boolean queueing, PartitionConfig config) {
        super(key);
        // saves creating a new String every time we print the key...
        _keyString = "" + key; 
        _config = config;
        _log = LogFactory.getLog(getClass().getName() + "#" + _key + "@" + _config.getLocalPeerName());
        if (content instanceof UnknownPartition) {
            Utils.acquireWithoutTimeout("Partition [exclusive]", _keyString, _lock.writeLock());
        }
        _content = content;
        if (_log.isTraceEnabled()) {
            _log.trace("initialising location to: " + _content);
        }
    }

    // 'Partition' API
    public boolean isLocal() { // locking ?
        return _content.isLocal();
    }
    
    public boolean isUnknown() {
        return _content instanceof UnknownPartition;
    }

    // incoming...
    public void onMessage(Message message, InsertIMToPM request) {
        Sync sync = _lock.readLock(); // SHARED
        try {
            Utils.acquireWithoutTimeout("Partition [shared]", _keyString, sync);
            _content.onMessage(message, request);
        } finally {
            Utils.release("Partition [shared]", _keyString, sync);
        }
    }

    public void onMessage(Message message, DeleteIMToPM request) {
        Sync sync = _lock.readLock(); // SHARED
        try {
            Utils.acquireWithoutTimeout("Partition [shared]", _keyString, sync);
            _content.onMessage(message, request);
        } finally {
            Utils.release("Partition [shared]", _keyString, sync);
        }
    }

    public void onMessage(Message message, EvacuateIMToPM request) {
        Sync sync = _lock.readLock(); // SHARED
        try {
            Utils.acquireWithoutTimeout("Partition [shared]", _keyString, sync);
            _content.onMessage(message, request);
        } finally {
            Utils.release("Partition [shared]", _keyString, sync);
        }
    }

    public void onMessage(Message message, MoveIMToPM request) {
        Sync sync = _lock.readLock(); // SHARED
        try {
            Utils.acquireWithoutTimeout("Partition [shared]", _keyString, sync);
            _content.onMessage(message, request);
        } finally {
            Utils.release("Partition [shared]", _keyString, sync);
        }
    }

    // outgoing...
    public Message exchange(SessionRequestMessage request, long timeout) throws Exception {
        Sync sync = _lock.readLock(); // SHARED
        try {
            Utils.acquireWithoutTimeout("Partition [shared]", _keyString, sync);
            return _content.exchange(request, timeout);
        } finally {
            Utils.release("Partition [shared]", _keyString, sync);
        }
    }

    // 'PartitionFacade' API
    public Partition getContent() {
        Sync sync = _lock.writeLock(); // EXCLUSIVE
        try {
            Utils.acquireWithoutTimeout("Partition [exclusive]", _keyString, sync);
            return _content;
        } finally {
            Utils.release("Partition [exclusive]", _keyString, sync);
        }
    }

    public void setContent(Partition content) {
        Sync sync = _lock.writeLock(); // EXCLUSIVE
        try {
            if (!(_content instanceof UnknownPartition)) {
                Utils.acquireWithoutTimeout("Partition [exclusive]", _keyString, sync);
            }
            _content = content;
        } finally {
            if (!(_content instanceof UnknownPartition)) {
                Utils.release("Partition [exclusive]", _keyString, sync);
            }
        }
    }

    public void setContentRemote(Address location) {
        Sync sync = _lock.writeLock(); // EXCLUSIVE
        try {
            if (!(_content instanceof UnknownPartition)) {
                Utils.acquireWithoutTimeout("Partition [exclusive]", _keyString, sync);
            }
            if (_content instanceof RemotePartition) {
                ((RemotePartition) _content).setAddress(location);
            } else {
                if (_log.isTraceEnabled()) {
                    _log.trace("[" + _key + "] changing location from: " + _content + " to: "
                            + _config.getPeerName(location));
                }
                _content = new RemotePartition(_key, _config, location);
            }
        } finally {
            if (!(_content instanceof UnknownPartition)) {
                Utils.release("Partition [exclusive]", _keyString, sync);
            }
        }
    }

    /**
     * Acquire an exclusive lock around the Partition which we encapsulate and
     * return it.
     * 
     * @return The encapsulated Partition
     * @throws InterruptedException
     */
    public Partition acquire() {
        Utils.acquireWithoutTimeout("Partition [exclusive]", _keyString, _lock.writeLock()); // EXCLUSIVE
        return _content;
    }

    /**
     * Release the exclusive lock around the Partition which we encapsulate.
     */
    public void release() {
        Utils.release("Partition [exclusive]", _keyString, _lock.writeLock());
    }

    /**
     * Set the address of Partition which we encasulate to that given and then
     * release the exclusive lock held around it.
     * 
     * @param address
     *            The new address of the Partition owner
     */
    public void release(Address address) {
        if (_log.isTraceEnabled()) {
            _log.trace("[" + _key + "] changing location from: " + _content + " to: " + _config.getPeerName(address));
        }
        _content = new RemotePartition(_key, _config, address);
        release();
    }
    
    public String toString() {
        return "PartitionFacade for [" + _content + "]";
    }
    
}
