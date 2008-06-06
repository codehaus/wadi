package org.codehaus.wadi.core;

import junit.framework.TestCase;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;

public class JDK5ConcurrentMotableMapTest extends TestCase {

    private JDK5ConcurrentMotableMap map;
    private volatile boolean expectNull;
    private volatile boolean failure;
    
    protected void setUp() throws Exception {
        map = new JDK5ConcurrentMotableMap();
    }
    
    public void testRemoveBetweenAcquireExclusiveAndMultipleAcquire() throws Exception {
        final String id = "name";
        map.put(id, new SimpleMotable());
        
        Thread[] readers = new Thread[10];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        Motable motable = map.acquire(id);
                        if (expectNull) {
                            if (null != motable) {
                                failure = true;
                            }
                        } else {
                            map.release(motable);
                        }
                    }
                }
            });
            readers[i].start();
        }
        
        Thread.sleep(200);
        
        Motable motable = map.acquireExclusive(id, 500);
        map.remove(id);
        expectNull = true;
        map.releaseExclusive(motable);
        
        for (int i = 0; i < readers.length; i++) {
            readers[i].interrupt();
            readers[i].join();
        }
        
        assertFalse(failure);
    }
    
    public void testMotableIsBusyThrowException() throws Exception {
        String id = "name";
        map.put(id, new SimpleMotable());

        map.acquireExclusive(id, 1);
        try {
            map.acquireExclusive(id, 1);
        } catch (MotableBusyException e) {
        }
    }
    
}
