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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Lease;
import org.codehaus.wadi.Lease.Handle;
import org.codehaus.wadi.impl.ExtendableLease;
import org.codehaus.wadi.impl.SimpleLease;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class TestLease extends TestCase {

	protected Log _log=LogFactory.getLog(getClass());
	
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
            _sync.acquire();
            _count++;
        }

        public synchronized boolean attempt(long msecs) throws InterruptedException {
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
    
    class Checker implements Runnable {

    	protected CountedSync _lock;

    	public Checker(CountedSync lock) {
    		_lock=lock;
    	}

    	public void run() {
    		try {
    			_lock.acquire();
    			assertTrue(_lock.getCount()==1);
    			_lock.release();
    		} catch(Exception e) {
    			assertTrue(false);
    		};
    	}
    };
    
    public void testSyncLock() throws Exception {
    	final CountedSync lock=new CountedSync();
    	int numThreads=100;
    	Thread[] threads=new Thread[numThreads];
    	for (int i=0; i<numThreads; i++)
    		(threads[i]=new Thread(new Checker(lock))).start();
    	for (int i=0; i<numThreads; i++)
    		threads[i].join();
    	
    }
    
    public void testSimpleLease() throws Exception {
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

    class CountedExtender implements ExtendableLease.Extender {
    	
    	protected int _count;
    	
    	public CountedExtender(int seconds) {
    		_count=seconds;
    	}
    	
    	public boolean extend() {
    		return _count-->1;
    	}
    	
    	public int getCount() {
    		return _count;
    	}
    	
    }
    
    public void testExtendableLease() throws Exception {
    
        CountedSync sync=new CountedSync();
    	ExtendableLease lease=new ExtendableLease("TEST", sync);

        // acquire a Lease - implicit release, with a number of extensions...
        {
            long leasePeriod=1000;
            long count=3;
            CountedExtender extender=new CountedExtender(3);
            assertTrue(sync.getCount()==0);
            assertTrue(extender.getCount()==count);
            long started=System.currentTimeMillis();
            lease.acquire(leasePeriod, extender); // acquire a 1 second lease
            lease.acquire(); // in order for us to acquire it, it must have released itself...
            long elapsed=System.currentTimeMillis()-started;
            assertTrue(elapsed>(leasePeriod*count)); // not the best way to test this...
            lease.release();
            assertTrue(sync.getCount()==0);
            assertTrue(extender.getCount()==0);
        }
    }
    
    // create a large number of Leases and let them time-out...
    public void testScalability() throws Exception {
    	int count=100000;
    	long period=1000;
    	_log.info("Leases: "+count);
    	_log.info("period (millis): "+period);
    	Lease[] _leases=new Lease[count];
    	_log.info("creating Leases...");
    	for (int i=0; i<count; i++)
    		_leases[i]=new SimpleLease("Lease-"+i, new CountedSync());
    	_log.info("acquiring Leases...");
    	long start=System.currentTimeMillis();
    	for (int i=0; i<count; i++)
    		_leases[i].acquire(period);
    	_log.info("reacquiring Leases...");
    	for (int i=0; i<count; i++)
    		_leases[i].acquire();
    	long elapsed=System.currentTimeMillis()-start;
    	_log.info("releasing Leases...");
    	for (int i=0; i<count; i++)
    		_leases[i].release();
    	long overhead=(elapsed-period);
    	_log.info("overhead: "+overhead);
    	_log.info("overhead/Lease: "+overhead/count);
    	_log.info("Leases/second: "+(count*1000)/overhead);
    }
    
}
