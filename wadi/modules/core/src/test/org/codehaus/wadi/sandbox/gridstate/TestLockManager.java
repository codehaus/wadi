package org.codehaus.wadi.sandbox.gridstate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
		_log.info("starting: "+lm);
		String key="abc";
		for (int i=0; i<_numThreads; i++)
			(_threads[i]=new Thread(new TestThread(key, lm), "TestThread-"+i)).start();
		for (int i=0; i<_numThreads; i++) {
			_threads[i].join();
			_threads[i]=null;
		}
		_log.info("finished: "+lm);
	}

}
