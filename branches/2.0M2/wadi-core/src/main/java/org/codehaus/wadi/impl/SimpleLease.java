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
    
    protected static final Log _lockLog = LogFactory.getLog("org.codehaus.wadi.LOCKS");
    protected static final ClockDaemon _daemon;
    
    static {
        _daemon=new ClockDaemon();
        ThreadFactory factory=new ThreadFactory() {
            public Thread newThread(Runnable command) {
                Thread thread=new Thread(command, "WADI Lease Management");
                thread.setDaemon(true);
                return thread;
            }
        };
        _daemon.setThreadFactory(factory);
    }

    public static class SimpleHandle implements Handle {

        protected final Object _taskId;
        
        protected SimpleHandle(Object taskId) {
            _taskId=taskId;
        }

        public String toString() {
            return "SimpleHandle";
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
            if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - implicit release: "+SimpleLease.this+"."+_handle);
            // only release iff handle is still extant - and within sync block
            synchronized (_handles) {
                if (_handles.remove(_handle))
                    _sync.release();
            }
        }
    }
    
    protected final String _label;
    protected final Sync _sync;
    protected final Set _handles=new TreeSet();

    // 'Sync' API
    
    public SimpleLease(String label, Sync sync) {
        _label=label;
        _sync=sync;
    }
    
    public String toString() {
        return "SimpleLease [" + _label + "]";
    }
    
    // 'Sync' API
    
    public void acquire() throws InterruptedException {
        _sync.acquire();
        if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - acquisition: "+this);
    }

    public boolean attempt(long msecs) throws InterruptedException {
        boolean success=_sync.attempt(msecs);
        if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - acquisition: "+this);
        return success;
    }

    public void release() {
        _sync.release();
        if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - explicit release: "+this);
    }

    // 'Lease' API

    protected Handle setAlarm(long leasePeriod) {
        Releaser releaser=new Releaser();
        Handle handle=new SimpleHandle(_daemon.executeAfterDelay(leasePeriod, releaser));
        if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - acquisition: "+this+"."+handle);
        synchronized (_handles) {_handles.add(handle);}
        releaser.init(handle);
        return handle;
    }
    
    public Handle acquire(long leasePeriod) throws InterruptedException {
        _sync.acquire();
        return setAlarm(leasePeriod);
    }

    public Handle attempt(long timeframe, long leasePeriod) throws InterruptedException {
        if (_sync.attempt(timeframe)) {
        	return setAlarm(leasePeriod);
        }
        else
            return null;
    }

    public boolean release(Handle handle) {
        if (handle==null) throw new IllegalArgumentException("bad Handle: "+handle);
        boolean extant;
        synchronized (_handles) {extant=_handles.remove(handle);}
        if (extant) {
            ClockDaemon.cancel(((SimpleHandle)handle).getTaskId());
            _sync.release();
            if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - explicit release: "+this+"."+handle);
        } else {
            if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - explicit release missed: "+this+"."+handle);
        }
        return extant;
    }

}
