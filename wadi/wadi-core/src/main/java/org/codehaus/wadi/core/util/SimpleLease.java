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
package org.codehaus.wadi.core.util;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.WADIRuntimeException;

/**
 * SimpleLease - first shot at a scalable, best-effort Lease impl. It is written around Sync and ClockDaemon from the
 * Concurrency library...
 * 
 * @author jules
 * @version $Revision$
 */
public class SimpleLease implements Lease {

    protected static final Log _lockLog = LogFactory.getLog("org.codehaus.wadi.LOCKS");

    protected static final ScheduledThreadPoolExecutor _daemon;

    static {
        _daemon = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            public Thread newThread(Runnable command) {
                Thread thread = new Thread(command, "WADI Lease Management");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public static class SimpleHandle implements Handle {

        protected final transient Runnable runnable;

        protected SimpleHandle(Runnable runnable) {
            this.runnable = runnable;
        }

        public String toString() {
            return "SimpleHandle";
        }

        public int compareTo(Object that) {
            // for the moment...
            return System.identityHashCode(this) - System.identityHashCode(that);
        }

    }

    public class Releaser implements Runnable {

        protected Handle _handle;

        void init(Handle handle) {
            _handle = handle;
        }

        public void run() {
            if (_lockLog.isTraceEnabled())
                _lockLog.trace(_label + " - implicit release: " + SimpleLease.this + "." + _handle);
            // only release iff handle is still extant - and within sync block
            synchronized (_handles) {
                if (_handles.remove(_handle))
                    _sync.unlock();
            }
        }
    }

    protected final String _label;
    protected final Lock _sync;
    protected final Set<Handle> _handles = new TreeSet<Handle>();

    public SimpleLease(String label, Lock sync) {
        _label = label;
        _sync = sync;
    }

    public String toString() {
        return "SimpleLease [" + _label + "]";
    }

    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            throw new WADIRuntimeException(e);
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        _sync.lockInterruptibly();
        if (_lockLog.isTraceEnabled())
            _lockLog.trace(_label + " - acquisition: " + this);
    }

    public boolean tryLock() {
        try {
            return tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new WADIRuntimeException(e);
        }
    }
    
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        boolean success = _sync.tryLock(time, TimeUnit.MILLISECONDS);
        if (_lockLog.isTraceEnabled())
            _lockLog.trace(_label + " - acquisition: " + this);
        return success;
    }
    
    public void unlock() {
        _sync.unlock();
        if (_lockLog.isTraceEnabled())
            _lockLog.trace(_label + " - explicit release: " + this);
    }

    // 'Lease' API

    protected Handle setAlarm(long leasePeriod) {
        Releaser releaser = new Releaser();
        _daemon.schedule(releaser, leasePeriod, TimeUnit.MILLISECONDS);
        Handle handle = new SimpleHandle(releaser);
        if (_lockLog.isTraceEnabled())
            _lockLog.trace(_label + " - acquisition: " + this + "." + handle);
        synchronized (_handles) {
            _handles.add(handle);
        }
        releaser.init(handle);
        return handle;
    }

    public Handle acquire(long leasePeriod) throws InterruptedException {
        _sync.lockInterruptibly();
        return setAlarm(leasePeriod);
    }

    public Handle attempt(long timeframe, long leasePeriod) throws InterruptedException {
        if (_sync.tryLock(timeframe, TimeUnit.MILLISECONDS)) {
            return setAlarm(leasePeriod);
        } else
            return null;
    }

    public boolean release(Handle handle) {
        if (handle == null)
            throw new IllegalArgumentException("bad Handle: " + handle);
        boolean extant;
        synchronized (_handles) {
            extant = _handles.remove(handle);
        }
        if (extant) {
            _daemon.remove(((SimpleHandle) handle).runnable);
            _sync.unlock();
            if (_lockLog.isTraceEnabled())
                _lockLog.trace(_label + " - explicit release: " + this + "." + handle);
        } else {
            if (_lockLog.isTraceEnabled())
                _lockLog.trace(_label + " - explicit release missed: " + this + "." + handle);
        }
        return extant;
    }

    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

}
