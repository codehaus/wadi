/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.impl.PiggyBackHttpLocation;

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
			_log.info("acquiring: "+_count++);
			super.acquire();
		}

		public void release() {
			super.release();
			_log.info("releasing: "+(--_count));
		}

	}

	protected Sync _mutex=new MyMutex();
	protected PiggyBackHttpLocation _location=new PiggyBackHttpLocation(3);

	class MyThread extends Thread {
		public void run() {
			try {
				_mutex.acquire();
			} catch (InterruptedException ie) {
				_log.warn("interruption", ie);
			}

			_location.proxy(null, null, getName(), _mutex);
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
