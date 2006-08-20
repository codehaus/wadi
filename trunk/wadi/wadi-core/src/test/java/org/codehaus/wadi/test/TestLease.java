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
package org.codehaus.wadi.test;

import org.codehaus.wadi.Lease;
import org.codehaus.wadi.Lease.Handle;
import org.codehaus.wadi.impl.SimpleLease;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import junit.framework.TestCase;

public class TestLease extends TestCase {
    
    public TestLease(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public static class CountedSync implements Sync {

        protected int _count=0;
        protected Sync _sync=new Mutex();
        
        public void acquire() throws InterruptedException {
            _count++;
            _sync.acquire();
        }

        public boolean attempt(long msecs) throws InterruptedException {
            if (_sync.attempt(msecs)) {
                _count++;
                return true;
            } else {
                return false;
            }
        }

        public void release() {
            _count--;
            _sync.release();
        }
        
        public int getCount() {
            return _count;
        }
        
    }
    
    public void testLease() throws Exception {
        CountedSync sync=new CountedSync();
        Lease lease=new SimpleLease("TEST", sync);

        // acquire a Sync
        {
            assertTrue(sync.getCount()==0);
            lease.acquire();
            assertTrue(sync.getCount()==1);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // attempt a Sync
        {
            assertTrue(sync.getCount()==0);
            lease.attempt(0);
            assertTrue(sync.getCount()==1);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // acquire a Lease - implicit release
        {
            long leasePeriod=1000;
            assertTrue(sync.getCount()==0);
            long started=System.currentTimeMillis();
            lease.acquire(leasePeriod); // acquire a 1 second lease
            lease.acquire(); // in order for us to acquire it, it must have released itself...
            long elapsed=System.currentTimeMillis()-started;
            assertTrue(elapsed>leasePeriod);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // attempt a Lease - implicit release
        {
            long leasePeriod=1000;
            assertTrue(sync.getCount()==0);
            long started=System.currentTimeMillis();
            lease.attempt(0, leasePeriod); // attempt a 1 second lease
            lease.acquire(); // in order for us to acquire it, it must have released itself...
            long elapsed=System.currentTimeMillis()-started;
            // Implementation note: add 100 to take into account non exact timing accuracy. 
            assertTrue(elapsed + 100 >leasePeriod);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // acquire a Lease - explicit release
        {
            assertTrue(sync.getCount()==0);
            long started=System.currentTimeMillis();
            Handle handle=lease.acquire(10000); // acquire a 10 second lease
            assertTrue(lease.release(handle));
            lease.acquire(); // in order for us to acquire it, it must have been released somehow...
            long elapsed=System.currentTimeMillis()-started;
            assertTrue(elapsed<5000);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // acquire a Lease - implicit and missed explicit release
        {
            assertTrue(sync.getCount()==0);
            Handle handle=lease.acquire(1); // acquire a 1 millisecond lease
            lease.acquire(); // in order for us to acquire it, it must have been released somehow...
            lease.release();
            assertTrue(sync.getCount()==0);
            assertTrue(!lease.release(handle));
            assertTrue(sync.getCount()==0);
        }
        // fail to acquire a Lease...
        {
            assertTrue(sync.getCount()==0);
            lease.acquire();
            assertTrue(sync.getCount()==1);
            assertTrue(lease.attempt(0, 1)==null); // wanted a 1 millisecond lease
            assertTrue(sync.getCount()==1);
            lease.release();
            assertTrue(sync.getCount()==0);
        }
        // bad handle...
        {
            try {
                lease.release(null);
                assertTrue(false); // should not get to here...
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }
    
}
