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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.dindex.impl.PartitionFacade;
import org.codehaus.wadi.impl.DistributableSession;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;

import junit.framework.TestCase;

public class TestDIndex extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestDIndex(String name) {
        super(name);
    }

    protected final int _numPartitions=3;
    protected final FixedWidthSessionIdFactory _factory=new FixedWidthSessionIdFactory(5, "0123456789".toCharArray(), _numPartitions);


    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    class Foo implements Runnable {

        protected final PartitionFacade _facade;

        public Foo(PartitionFacade facade) {
            _facade=facade;
        }

        public void run() {

        }

    }
    
    protected long heartbeatTimeout=5*1000;
    protected long responseTimeout=30*60*1000;
 

    public void testDindex() throws Exception {
        assertTrue(true);

        DIndexNode red=new DIndexNode("red", _numPartitions, _factory, heartbeatTimeout);
        DIndexNode green=new DIndexNode("green", _numPartitions, _factory, heartbeatTimeout);
        DIndexNode blue=new DIndexNode("blue", _numPartitions, _factory, heartbeatTimeout);


        red.start();
        green.start();
        blue.start();

        while(red.getCluster().getNodes().size()<2 ||
        	  green.getCluster().getNodes().size()<2 ||
        	  blue.getCluster().getNodes().size()<2 ||
        	  green.getDIndex().getPartitionManager().getPartitionKeys().size()==0 ||
        	  blue.getDIndex().getPartitionManager().getPartitionKeys().size()==0)
        	Thread.sleep(1000);

        _log.info("partitions distributed");

        String name=_factory.create(1); // blue
        _log.info("inserting: "+name);
        Motable motable=new DistributableSession(new DummyDistributableSessionConfig());
        motable.init(0, 0, 100000, name);
        red.insert(name, motable, 30*1000L);
        _log.info("inserted: "+name);
        _log.info("fetching: "+name);
        DIndex g=green.getDIndex();
        g.relocate2(name, "green", 1, false, responseTimeout);
        // Motable motable2=(Motable)
        green.get(name);
        _log.info("fetched: "+name);
        //green.getDIndex().remove(name);
        //blue.getDIndex().put(name, name);

        Thread.sleep(10*1000);
        blue.stop();
        green.stop();
        red.stop();
        Thread.sleep(6000);
        _log.info("0 nodes running");
    }

}
