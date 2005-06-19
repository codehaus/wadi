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

import org.activemq.util.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.dindex.impl.BucketFacade;
import org.codehaus.wadi.dindex.impl.DummyBucket;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;

import junit.framework.TestCase;

public class TestDIndex extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestDIndex(String name) {
        super(name);
    }

    protected int _numIndexPartitions;
    
    protected void setUp() throws Exception {
        super.setUp();
        _numIndexPartitions=24;
    }

    protected void tearDown() throws Exception {
        _numIndexPartitions=0;
        super.tearDown();
    }
    
    class Foo implements Runnable {
        
        protected final BucketFacade _facade;
        
        public Foo(BucketFacade facade) {
            _facade=facade;
        }
        
        public void run() {
            
        }
        
    }
    
    public void testQueueing() {
        
        boolean isQueueing=true;
        long timeStamp=System.currentTimeMillis();
        BucketFacade facade=new BucketFacade(0, timeStamp, new DummyBucket(0), isQueueing);
        
        int numThreads=10;
        Thread thread[]=new Thread[numThreads];
        for (int i=0; i<numThreads; i++) {
            (thread[i]=new Thread(new Foo(facade))).start();
        }
        
        // do stuff with BucketFacade
        
        try {
            for (int i=0; i<numThreads; i++) {
                thread[i].join();
            } 
        } catch (InterruptedException e) {
            _log.warn("interrupted", e);
        }
    }
    
    public void testDindex() throws Exception {
        assertTrue(true);
        
        DIndexNode red=new DIndexNode("red", _numIndexPartitions);
        DIndexNode green=new DIndexNode("green", _numIndexPartitions);
        DIndexNode blue=new DIndexNode("blue", _numIndexPartitions);
        DIndexNode yellow=new DIndexNode("yellow", _numIndexPartitions);
        DIndexNode pink=new DIndexNode("pink", _numIndexPartitions);
        
        _log.info("0 nodes running");
        red.start();
        red.getCluster().waitForClusterToComplete(1, 6000);
        _log.info("1 node running");
        green.start();
        red.getCluster().waitForClusterToComplete(2, 6000);
        green.getCluster().waitForClusterToComplete(2, 6000);
        _log.info("2 nodes running");
        blue.start();
        red.getCluster().waitForClusterToComplete(3, 6000);
        green.getCluster().waitForClusterToComplete(3, 6000);
        blue.getCluster().waitForClusterToComplete(3, 6000);
        _log.info("3 nodes running");
        yellow.start();
        red.getCluster().waitForClusterToComplete(4, 6000);
        green.getCluster().waitForClusterToComplete(4, 6000);
        blue.getCluster().waitForClusterToComplete(4, 6000);
        yellow.getCluster().waitForClusterToComplete(4, 6000);
        _log.info("4 nodes running");
        pink.start();
        red.getCluster().waitForClusterToComplete(5, 6000);
        green.getCluster().waitForClusterToComplete(5, 6000);
        blue.getCluster().waitForClusterToComplete(5, 6000);
        yellow.getCluster().waitForClusterToComplete(5, 6000);
        pink.getCluster().waitForClusterToComplete(5, 6000);
        _log.info("5 nodes running");
        
//        SessionIdFactory factory=new TomcatSessionIdFactory();
//        
//        for (int i=0; i<10; i++) {
//            String name=factory.create();
//            red.getDIndex().put(name, name, green.getDestination());
//            green.getDIndex().remove(name, red.getDestination());
//            //blue.getDIndex().put(name, name);
//            //yellow.getDIndex().remove(name);
//        }
        
        _log.info("5 nodes running");
        pink.stop();
        yellow.getCluster().waitForClusterToComplete(4, 6000);
        blue.getCluster().waitForClusterToComplete(4, 6000);
        green.getCluster().waitForClusterToComplete(4, 6000);
        red.getCluster().waitForClusterToComplete(4, 6000);
        _log.info("4 nodes running");
        yellow.stop();
        blue.getCluster().waitForClusterToComplete(3, 6000);
        green.getCluster().waitForClusterToComplete(3, 6000);
        red.getCluster().waitForClusterToComplete(3, 6000);
        _log.info("3 nodes running");
        blue.stop();
        green.getCluster().waitForClusterToComplete(2, 6000);
        red.getCluster().waitForClusterToComplete(2, 6000);
        _log.info("2 nodes running");
        green.stop();
        red.getCluster().waitForClusterToComplete(1, 6000);
        _log.info("1 nodes running");
        red.stop();
        _log.info("0 nodes running");
    }

}
