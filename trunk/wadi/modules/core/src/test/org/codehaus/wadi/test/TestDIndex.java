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
import org.codehaus.wadi.dindex.impl.PartitionFacade;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;
import org.codehaus.wadi.impl.SimplePartitionMapper;

import junit.framework.TestCase;

public class TestDIndex extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestDIndex(String name) {
        super(name);
    }

    protected int _numPartitions;

    protected void setUp() throws Exception {
        super.setUp();
        _numPartitions=3;
    }

    protected void tearDown() throws Exception {
        _numPartitions=0;
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

    public void testDindex() throws Exception {
        assertTrue(true);

        DIndexNode red=new DIndexNode("red", _numPartitions, new SimplePartitionMapper(_numPartitions));
        DIndexNode green=new DIndexNode("green", _numPartitions, new SimplePartitionMapper(_numPartitions));
        DIndexNode blue=new DIndexNode("blue", _numPartitions, new SimplePartitionMapper(_numPartitions));


        _log.info("0 nodes running");

        red.start();
        green.start();
        blue.start();
        red.getCluster().waitForClusterToComplete(3, 6000);
        green.getCluster().waitForClusterToComplete(3, 6000);
        blue.getCluster().waitForClusterToComplete(3, 6000);
        _log.info("3 nodes running");

        FixedWidthSessionIdFactory factory=new FixedWidthSessionIdFactory(5, "0123456789".toCharArray(), _numPartitions);

        String name=factory.create(_numPartitions-1);
        red.getDIndex().insert(name);
        green.getDIndex().relocate(name, "green", 1, false, 2000L);
        //green.getDIndex().remove(name);
        //blue.getDIndex().put(name, name);

        blue.stop();
        green.stop();
        red.stop();
        Thread.sleep(6000);
        _log.info("0 nodes running");
    }

}
