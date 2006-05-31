/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.impl;

import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Lease;
import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

/**
 * SimpleLease - first shot at a scalable, best-effort Lease impl. It is written around Sync and ClockDaemon
 * from the Concurrency library...
 *
 * @author jules
 * @version $Revision$
 */
public class SimpleLease implements Lease {
    
    protected static final Log _log = LogFactory.getLog(SimpleLease.class);
    protected static final ClockDaemon _daemon;
    
    static {
        _daemon=new ClockDaemon();
//        ThreadFactory factory=new ThreadFactory() {
//            public Thread newThread(Runnable command) {
//                Thread thread=new Thread("WADI Lease Management");
//                thread.setDaemon(true);
//                return thread;
//            }
//        };
//        _daemon.setThreadFactory(factory);
    }

    public static class SimpleHandle implements Handle {

        protected final Object _taskId;
        
        protected SimpleHandle(Object taskId) {
            _taskId=taskId;
        }

        // 'Object' API
        
        public String toString() {
            return "<"+Utils.basename(getClass())+":"+_taskId+">";
        }
        
        // 'Comparable' API
        
        public int compareTo(Object that) {
            // for the moment...
            return System.identityHashCode(this)-System.identityHashCode(that);
        }
        
        // 'SimpleHandle' API
        
        protected Object getTaskId() {
            return _taskId;
        }
        
    }
    
    public class Releaser implements Runnable {

        protected Handle _handle;

        void init(Handle handle) {
            _handle=handle;
        }

        public void run() {
            if (_log.isTraceEnabled()) _log.trace("implicit release: "+_handle+"/"+SimpleLease.this);
            // only release iff handle is still extant - and within sync block
            synchronized (_handles) {
                if (_handles.remove(_handle))
                    _sync.release();
            }
        }
    }
    
    protected final Sync _sync;
    protected final Set _handles=new TreeSet();

    // 'Sync' API
    
    public SimpleLease(Sync sync) {
        _sync=sync;
    }
    
    public void acquire() throws InterruptedException {
        _sync.acquire();
    }

    public boolean attempt(long msecs) throws InterruptedException {
        return _sync.attempt(msecs);
    }

    public void release() {
        _sync.release();
    }

    // 'Lease' API
    
    public Handle acquire(long leasePeriod) throws InterruptedException {
        _sync.acquire();
        Releaser releaser=new Releaser();
        Handle handle;
        synchronized (_handles) {
            handle=new SimpleHandle(_daemon.executeAfterDelay(leasePeriod, releaser));
            _handles.add(handle);
            releaser.init(handle);
        }
        if (_log.isTraceEnabled()) _log.trace("acquisition: "+handle+"/"+this);
        return handle;
    }

    public Handle attempt(long timeframe, long leasePeriod) throws InterruptedException {
        if (_sync.attempt(timeframe)) {
            Releaser releaser=new Releaser();
            Handle handle=new SimpleHandle(_daemon.executeAfterDelay(leasePeriod, releaser));
            if (_log.isTraceEnabled()) _log.trace("acquisition: "+handle+"/"+this);
            synchronized (_handles) {_handles.add(handle);}
            releaser.init(handle);
            return handle;
        }
        else
            return null;
    }

    public boolean release(Handle handle) {
        if (_log.isTraceEnabled()) _log.trace("explicit release: "+handle+"/"+this);
        boolean extant;
        synchronized (_handles) {extant=_handles.remove(handle);}
        if (extant) {
            ClockDaemon.cancel(((SimpleHandle)handle).getTaskId());
            _sync.release();
        }
        return extant;
    }

}
