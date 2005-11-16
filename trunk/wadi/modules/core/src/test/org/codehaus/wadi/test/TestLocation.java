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
package org.codehaus.wadi.test;

import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.impl.SimpleEvictable;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import junit.framework.TestCase;

/**
 * Start to test the complex locking in the PiggyBackLocation - difficult - I think we will just go with a Serial solution to start with...
 *
 * This nearly works but we end up releasing too many locks (nthreads%threshold) - think about it...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestLocation extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Constructor for TestLocation.
	 * @param name
	 */
	public TestLocation(String name) {
		super(name);
	}

	class MyMutex extends Mutex {
		protected int _count;

		public void acquire() throws InterruptedException {
            if ( _log.isInfoEnabled() ) {

                _log.info("acquiring: " + _count++);
            }
			super.acquire();
		}

		public void release() {
			super.release();
            if ( _log.isInfoEnabled() ) {

                _log.info("releasing: " + ( --_count ));
            }
		}

	}

	class MyLocation extends SimpleEvictable implements Location {
	    
		public void proxy(HttpServletRequest hreq, HttpServletResponse hres) {
		    // do nothing
		    }
		
		public Destination getDestination() {return null;}
		
	}

	protected Sync _mutex=new MyMutex();
	protected Location _location=new MyLocation();

	class MyThread extends Thread {
		public void run() {
			try {
				_mutex.acquire();
			} catch (InterruptedException ie) {
                if ( _log.isWarnEnabled() ) {

                    _log.warn("interruption", ie);
                }
			}

			try {
			_location.proxy(null, null);
			} catch (Exception e) {
                if ( _log.isWarnEnabled() ) {

                    _log.warn("proxy problem", e);
                }
			} finally {
			  _mutex.release();
			}
		}
	}

	public void testLocation() throws Exception {
		int numThreads=11;
		Thread[] threads=new Thread[numThreads];

		for (int i=0; i<numThreads; i++) {
			(threads[i]=new MyThread()).start();
		}

		for (int i=0; i<numThreads; i++) {
			threads[i].join();
		}
	}
}
