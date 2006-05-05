package org.codehaus.wadi.group;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.vm.VMDispatcher;
import junit.framework.TestCase;
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
public class TestGroup extends TestCase {

    protected static Log _log = LogFactory.getLog(TestGroup.class);

    public TestGroup(String arg0) {
        super(arg0);
    }

    protected interface DispatcherFactory {

        Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception;

    }

    public DispatcherFactory getDispatcherFactory() throws Exception {
        return new DispatcherFactory() {public Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception {return new VMDispatcher(clusterName, peerName, inactiveTime);}};
    }

    protected DispatcherFactory _dispatcherFactory;

    protected void setUp() throws Exception {
        super.setUp();
        _dispatcherFactory=getDispatcherFactory();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    class MyClusterListener implements ClusterListener {

        public int _numPeers=0;
        public int _numUpdates=0;
        public int _numChanges=0;

        public void onPeerAdded(ClusterEvent event) {
            _numPeers++;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onPeerUpdated(ClusterEvent event) {
            _numUpdates++;
        }

        public void onPeerRemoved(ClusterEvent event) {
            _numPeers--;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onPeerFailed(ClusterEvent event) {
            _numPeers--;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onCoordinatorChanged(ClusterEvent event) {
            _numChanges++;
        }

    }

    public void testStuff() throws Exception {
        String clusterName="org.codehaus.wadi.cluster.TEST-"+System.currentTimeMillis();

        DispatcherConfig config=new DummyDispatcherConfig();
        
        Dispatcher dispatcher0=_dispatcherFactory.create(clusterName, "red", 5000);
        dispatcher0.init(config);

        
        MyClusterListener listener0=new MyClusterListener();
        Cluster cluster0=dispatcher0.getCluster();
        cluster0.addClusterListener(listener0);
        
        Dispatcher dispatcher1=_dispatcherFactory.create(clusterName, "green", 5000);
        dispatcher1.init(config);
        Cluster cluster1=dispatcher1.getCluster();
        MyClusterListener listener1=new MyClusterListener();
        cluster1.addClusterListener(listener1);

        cluster0.start();
        assertTrue(cluster0.waitForClusterToComplete(0, 10000));

        cluster1.start();
        assertTrue(cluster0.waitForClusterToComplete(1, 10000));
        assertTrue(cluster1.waitForClusterToComplete(1, 10000));

        assertTrue(listener0._numPeers==1);
        assertTrue(listener1._numPeers==1);

        cluster1.stop();
        assertTrue(cluster0.waitForClusterToComplete(0, 10000));

        cluster0.stop();
    }

}
