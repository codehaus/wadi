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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.RankedRWLock;

import junit.framework.TestCase;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestRWLock extends TestCase {

	protected Log _log=LogFactory.getLog(getClass());
	
	class Overlapper implements Runnable {
		
		RankedRWLock _rrwlock;
		int _numIters;
		
		public Overlapper(RankedRWLock rrwlock, int numIters) {
			_rrwlock=rrwlock;
			_numIters=numIters;
		}
		
		public void run() {
			for (int i=0; i<_numIters; i++) {
				try {
					RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
					_rrwlock.readLock().acquire();
					Thread.yield();
					RankedRWLock.setPriority(RankedRWLock.INVALIDATION_PRIORITY);
					_rrwlock.overlap();
					Thread.yield();
				} catch (InterruptedException e) {
					_log.warn("unexpected interruption during lock acquisition", e);
				} finally {
					RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
					_rrwlock.writeLock().release();
				}
			}
		}
	}
	
	public void testOverlap() throws Exception {
		int numThreads=1;
		int numIters=100;
	
		RankedRWLock rrrwlock=new RankedRWLock();
		Overlapper overlapper=new Overlapper(rrrwlock, numIters);
		Thread[] threads=new Thread[numThreads];
		for (int i=0; i<numThreads; i++)
			(threads[i]=new Thread(overlapper)).start();
		for (int i=0; i<numThreads; i++)
			threads[i].join();
	}
}
