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
package org.codehaus.wadi.sandbox.dindex;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

public class BucketFacade extends AbstractBucket /* implements Excludable,*/ {

    protected static final Log _log = LogFactory.getLog(BucketFacade.class);

//    protected transient ReadWriteLock _lock;
//    
//    protected final long _creationTime=System.currentTimeMillis();
//
//    protected int _key;
//    protected Map _map=new ConcurrentHashMap(); // cannot be final as will be Serialised...
//    protected boolean _local; // should own write lock before altering this...
//    
//    public BucketFacade(int key) {
//        //this();
//        _lock=new WriterPreferenceReadWriteLock();
//        _key=key;
//    }
//    
//    protected BucketFacade() {
//        // for deserialisation
//        _lock=new WriterPreferenceReadWriteLock();
//        System.err.println("DESERIALISING..."+_lock);
//    }
//
//    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//        in.defaultReadObject();
//        _lock=new WriterPreferenceReadWriteLock();
//    }
//
//    public Sync getExclusiveLock() {
//        return _lock.writeLock();
//    }
//    
//    public boolean getLocal() {
//        return _local;
//    }
//    
//    // MUST own exclusive lock before calling this...
//    public void setLocal(boolean local) {
//        if (!(_local=local))
//            _map.clear();
//    }
//    
//    public Node get(String sessionName) {
//        if (_local) {
//            boolean acquired=false;
//            try {
//                Utils.safeAcquire(_lock.readLock());
//                acquired=true;
//                return (Node)_map.get(sessionName);
//            } finally {
//                if (acquired)
//                    _lock.readLock().release();
//            }
//        } else {
//            // forward the message to the node which owns this index partition...
//            throw new UnsupportedOperationException();
//        }
//    }
//    
//    public void put(String sessionName, Node node) {
//        assert _local;
//        boolean acquired=false;
//        try {
//            Utils.safeAcquire(_lock.readLock());
//            acquired=true;
//            _map.put(sessionName, node);
//        } finally {
//            if (acquired)
//                _lock.readLock().release();
//        }
//    }
//    
//    public void put(Map map) {
//        assert _local;
//        boolean acquired=false;
//        try {
//            Utils.safeAcquire(_lock.readLock());
//            acquired=true;
//            _map.putAll(map);
//        } finally {
//            if (acquired)
//                _lock.readLock().release();
//        }
//    }
//    
//    // call this with Exclusive lock held...
//    public Map get() {
//        return _map;
//    }
    
    
    protected Bucket _content;
    
    public BucketFacade(int key, Bucket content) {
        super(key);
        _content=content;
    }
    
    public boolean isLocal() {
        return _content.isLocal();
    }

    // TODO - locking...
    public void setContent(Bucket content) {
        _log.info("changing location from: "+_content+" to: "+content);
        _content=content;
    }
    
    public Bucket getContent() {
        return _content;
    }
    
    public void setContent(Destination location) {
        if (_content instanceof RemoteBucket) {
            _log.info("changing location from: local to: "+location);
            ((RemoteBucket)_content).setLocation(location);
        } else {
            _content=new RemoteBucket(_key, location);
        }
    }
}
