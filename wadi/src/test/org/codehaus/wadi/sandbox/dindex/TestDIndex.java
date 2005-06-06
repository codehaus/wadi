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

import javax.jms.JMSException;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
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

    protected int _numIndexPartitions;
    
    protected void setUp() throws Exception {
        super.setUp();
        _numIndexPartitions=1024;
    }

    protected void tearDown() throws Exception {
        _numIndexPartitions=0;
        super.tearDown();
    }
    
    public static class MyNode implements ClusterListener {
        
        protected final static String _nodeNameKey="nodeName";
        protected final static String _indexPartitionsKey="indexPartitions";
        protected final static String _birthTimeKey="birthTime";
        
        //protected final String _clusterUri="peer://org.codehaus.wadi";
        protected final String _clusterUri="tcp://localhost:61616";
        protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
        protected final DefaultClusterFactory _clusterFactory=new DefaultClusterFactory(_connectionFactory);
        protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
        protected final Map _distributedState=new ConcurrentHashMap();
        protected final Object _coordinatorSync=new Object();
        protected final Object _coordinatorLock=new Object();
        protected final Map _key2IndexPartitionNode=new ConcurrentHashMap(); // contains full set of keys
        protected final Map _key2IndexPartition=new ConcurrentHashMap(); // contains subset of keys

        protected final String _nodeName;
        protected final Log _log;
        protected final int _numIndexPartitions;
        
        public MyNode(String nodeName, int numIndexPartitions) {
            _nodeName=nodeName;
            _log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
            _numIndexPartitions=numIndexPartitions;
        }
        
        protected Cluster _cluster;
        protected Node _coordinator;
        
        public void start() throws Exception {
            _log.info("starting...");
            _connectionFactory.start();
            _cluster=_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
            _cluster.addClusterListener(this);
            _distributedState.put(_nodeNameKey, _nodeName);
            _distributedState.put(_birthTimeKey, new Long(System.currentTimeMillis()));
            _cluster.getLocalNode().setState(_distributedState);
            _cluster.start();
            _log.info("...started");
            
            synchronized (_coordinatorSync) {
                _coordinatorSync.wait(_clusterFactory.getInactiveTime());
            }
            
            synchronized (_coordinatorLock) {
                // if no-one else is going to be coordinator - then it must be us !
                if (_coordinator==null)
                    onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
            }
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
        
        public void onNodeUpdate(ClusterEvent event) {
            _log.info("onNodeUpdate: "+getNodeName(event.getNode())+": "+event.getNode().getState());
        }

        public void onNodeAdd(ClusterEvent event) {
            _log.info("onNodeAdd: "+getNodeName(event.getNode()));
            if (isCoordinator())
                onRedistributeIndexPartitions(event);
        }

        public void onNodeRemoved(ClusterEvent event) {  // NYI by layer below us...
            _log.info("onNodeRemoved: "+getNodeName(event.getNode()));
            if (isCoordinator())
                onRedistributeIndexPartitions(event);
        }

        public void onNodeFailed(ClusterEvent event) {
            _log.info("onNodeFailed: "+getNodeName(event.getNode()));
            if (isCoordinator())
                onRedistributeIndexPartitions(event);
        }

        public void onCoordinatorChanged(ClusterEvent event) {
            synchronized (_coordinatorLock) {
                _log.info("onCoordinatorChanged: "+getNodeName(event.getNode()));
                Node newCoordinator=event.getNode();
                if (newCoordinator!=_coordinator) {
                    if (_coordinator==_cluster.getLocalNode())
                        onDismissal(event);
                    _coordinator=newCoordinator;
                    if (_coordinator==_cluster.getLocalNode())
                        onElection(event);
                }
                synchronized (_coordinatorSync) {
                    _coordinatorSync.notifyAll();
                }
            }
        }
        
        // MyNode
        
        public void onElection(ClusterEvent event) {
            _log.info("accepting coordinatorship");
            if (_cluster.getNodes().size()==0) {
                // initialise Index Partitions
                _log.info("allocating "+_numIndexPartitions+" index partitions");
                for (int i=0; i<_numIndexPartitions; i++) {
                    Integer key=new Integer(i);
                    Map indexPartition=new ConcurrentHashMap();
                    _key2IndexPartition.put(key, indexPartition);
                }
                _distributedState.put(_indexPartitionsKey, _key2IndexPartition.keySet().toArray());
//                try {
//                    _cluster.getLocalNode().setState(_distributedState);
//                } catch (JMSException e) {
//                    _log.error("could not update distributed state");
//                }
            } else {
                onRedistributeIndexPartitions(event);
            }
        }

        public void onDismissal(ClusterEvent event) {
            _log.info("resigning coordinatorship"); // never happens - coordinatorship is for life..
        }


        public void onRedistributeIndexPartitions(ClusterEvent event) {
            _log.info("redistributing Index Partitions");
            int indexPartitionsPerNode=_numIndexPartitions/(_cluster.getNodes().size()+1);
            int remainder=_numIndexPartitions%indexPartitionsPerNode;
            _log.info("each Node should carry "+indexPartitionsPerNode+(remainder>0?("-"+(indexPartitionsPerNode+1)):"")+" index partitions");
            // go through Nodes, looking at their state and [re]assigning Partitions to them...
        }
        
        public static String getNodeName(Node node) {
            return (String)node.getState().get(_nodeNameKey);
        }
        
        public boolean isCoordinator() {
            synchronized (_coordinatorLock) {
                return _cluster.getLocalNode()==_coordinator;
            }
        }
        
        public Node getCoordinator() {
            synchronized (_coordinatorLock) {
                return _coordinator;
            }
        }
    }
    
    public void testDindex() throws Exception {
        assertTrue(true);
        
        MyNode red=new MyNode("red", _numIndexPartitions);
        MyNode green=new MyNode("green", _numIndexPartitions);
        MyNode blue=new MyNode("blue", _numIndexPartitions);
        
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
