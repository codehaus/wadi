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

import java.util.Map;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.ClusterFactory;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

public class TestDIndex extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestDIndex(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public static class MyNode implements ClusterListener {
        
        protected final static String _nodeNameKey="nodeName";
        
        //protected final String _clusterUri="peer://org.codehaus.wadi";
        protected final String _clusterUri="tcp://localhost:61616";
        protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
        protected final ClusterFactory _clusterFactory=new DefaultClusterFactory(_connectionFactory);
        protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
        protected final Map _distributedState=new ConcurrentHashMap();

        protected final String _nodeName;
        protected final Log _log;
        
        public MyNode(String nodeName) {
            _nodeName=nodeName;
            _log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
        }
        
        protected Cluster _cluster;
        protected Node _coordinator;
        
        public void start() throws Exception {
            _log.info("starting...");
            _connectionFactory.start();
            _cluster=_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
            _cluster.addClusterListener(this);
            _distributedState.put(_nodeNameKey, _nodeName);
            //_distributedState.put("http", _httpAddress);
            _cluster.getLocalNode().setState(_distributedState);
            _coordinator=_cluster.getLocalNode();
            _log.info("assuming Coordinatorship until I hear otherwise");
            _cluster.start();
            _log.info("...started");
        }
        
        public void stop() throws Exception {
            _log.info("stopping...");
            _cluster.stop();
            _connectionFactory.stop();
            _log.info("...stopped");
        }
        
        protected String getContextPath() {
            return "/";
        }
        
        protected Cluster getCluster() {
            return _cluster;
        }

        // ClusterListener
        
        public void onNodeAdd(ClusterEvent event) {
            _log.info("onNodeAdd: "+getNodeName(event.getNode()));
        }

        public void onNodeUpdate(ClusterEvent event) {
            _log.info("onNodeUpdate: "+getNodeName(event.getNode()));
        }

        public void onNodeRemoved(ClusterEvent event) {
            _log.info("onNodeRemoved: "+getNodeName(event.getNode()));
        }

        public void onNodeFailed(ClusterEvent event) {
            _log.info("onNodeFailed: "+getNodeName(event.getNode()));
        }

        public void onCoordinatorChanged(ClusterEvent event) {
            _log.info("onCoordinatorChanged: "+getNodeName(event.getNode()));
            Node newCoordinator=event.getNode();
            if (newCoordinator!=_coordinator) {
                if (_coordinator==_cluster.getLocalNode())
                    _log.info("resigning coordinatorship"); // never happens - coordinatorship is for life..
                _coordinator=newCoordinator;
                if (_coordinator==_cluster.getLocalNode())
                    _log.info("accepting coordinatorship");
            }
        }
        
        public static String getNodeName(Node node) {
            return (String)node.getState().get(_nodeNameKey);
        }
        
        public boolean isCoordinator() {
            return _cluster.getLocalNode().isCoordinator();
        }
    }
    
    public void testDindex() throws Exception {
        assertTrue(true);
        
        MyNode red=new MyNode("red");
        MyNode green=new MyNode("green");
        MyNode blue=new MyNode("blue");
        
        _log.info("0 nodes running");
        red.start();
        red.getCluster().waitForClusterToComplete(1, 6000);
        if (red.isCoordinator())
            _log.info("red is Coordinator");
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
        
        
        _log.info("3 nodes running");
        red.stop();
        blue.getCluster().waitForClusterToComplete(3, 6000);
        green.getCluster().waitForClusterToComplete(3, 6000);
        _log.info("2 nodes running");
        green.stop();
        blue.getCluster().waitForClusterToComplete(3, 6000);
        _log.info("1 nodes running");
        blue.stop();
        _log.info("0 nodes running");
    }

}
