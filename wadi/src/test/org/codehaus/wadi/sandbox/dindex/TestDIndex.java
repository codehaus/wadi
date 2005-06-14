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
package org.codehaus.wadi.sandbox.dindex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class TestDIndex extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestDIndex(String name) {
        super(name);
    }

    protected int _numIndexPartitions;
    
    protected void setUp() throws Exception {
        super.setUp();
        _numIndexPartitions=1024;
    }

    protected void tearDown() throws Exception {
        _numIndexPartitions=0;
        super.tearDown();
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
