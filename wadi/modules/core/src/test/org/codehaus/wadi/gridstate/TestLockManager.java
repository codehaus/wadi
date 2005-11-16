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
package org.codehaus.wadi.gridstate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.LockManager;
import org.codehaus.wadi.gridstate.impl.SmartLockManager;
import org.codehaus.wadi.gridstate.impl.StupidLockManager;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import junit.framework.TestCase;

public class TestLockManager extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());

	protected final int _numThreads=1000;
	protected final int _numIterations=100;
	protected final Thread[] _threads=new Thread[_numThreads];

	public static void main(String[] args) {
	}

	public TestLockManager(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	class TestThread implements Runnable {

		protected final Object _key;
		protected final LockManager _manager;

		TestThread(String key, LockManager manager) {
			_key=key;
			_manager=manager;
		}

		public void run() {
			for (int i=0; i<_numIterations; i++) {
				Sync sync=_manager.acquire(_key);
				sync.release();
			}
		}
	}

	public void testLockManagers() throws Exception {
		run(new SmartLockManager(""));
		run(new StupidLockManager(""));
	}

	protected void run(LockManager lm) throws Exception {
        if ( _log.isInfoEnabled() ) {

            _log.info("starting: " + lm);
        }
		String key="abc";
		for (int i=0; i<_numThreads; i++)
			(_threads[i]=new Thread(new TestThread(key, lm), "TestThread-"+i)).start();
		for (int i=0; i<_numThreads; i++) {
			_threads[i].join();
			_threads[i]=null;
		}
        if ( _log.isInfoEnabled() ) {

            _log.info("finished: " + lm);
        }
	}

}