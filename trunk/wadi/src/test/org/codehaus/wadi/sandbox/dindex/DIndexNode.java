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
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.MessageDispatcherConfig;
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class DIndexNode implements ClusterListener, MessageDispatcherConfig {
        
        protected final static String _nodeNameKey="nodeName";
        protected final static String _indexPartitionsKey="indexPartitions";
        protected final static String _numIndexPartitionsKey="numIndexPartitions";
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
        protected final Map _indexPartitionTransferRvMap=new ConcurrentHashMap();
        protected final MessageDispatcher _dispatcher=new MessageDispatcher();

        protected final String _nodeName;
        protected final Log _log;
        protected final int _numIndexPartitions;
        
        public DIndexNode(String nodeName, int numIndexPartitions) {
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
            //_distributedState.put(_indexPartitionsKey, new Object[0]);
            _distributedState.put(_numIndexPartitionsKey, new Integer(0));

            
            _dispatcher.init(this);
            _dispatcher.register(this, "onIndexPartitionsTransferRequest", IndexPartitionsTransferRequest.class);
            _dispatcher.register(IndexPartitionsTransferResponse.class, _indexPartitionTransferRvMap, 5000);
            
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
        
        public Cluster getCluster() {
            return _cluster;
        }

        // ClusterListener
        
        public void onNodeUpdate(ClusterEvent event) {
            _log.info("onNodeUpdate: "+getNodeName(event.getNode())+": "+event.getNode().getState());
        }

        protected final Object _planLock=new Object();
        
        public void onNodeAdd(ClusterEvent event) {
            if (_cluster.getLocalNode()==_coordinator) {
                synchronized (_planLock) {
                    Node tgt=event.getNode();
                    _log.info("onNodeAdd: "+getNodeName(tgt));
                    // make and execute plan....
                    // snapshot Cluster state - may be unecessary - TODO
                    Map tmp=_cluster.getNodes();
                    int n=tmp.size();
                    int numNodes=n+1;
                    int partitionsPerNode=_numIndexPartitions/numNodes;
                    int remainder=_numIndexPartitions%numNodes;
                    Node[] nodes=(Node[])tmp.values().toArray(new Node[n]);
                    for (int i=0; i<n; i++) {
                        Node node=nodes[i];
                        Node src=(node==tgt)?_cluster.getLocalNode():node;
                        share(numNodes, i, src, tgt, partitionsPerNode, remainder);
                    }
                    
                }
            }
        }

        protected void share(int numNodes, int index, Node src, Node tgt, int partitionsPerNode, int remainder) {
            int keeps=partitionsPerNode+((index<remainder)?1:0);
            _log.info("sharing: ["+index+"/"+numNodes+"] - "+getNodeName(src)+" keeps: "+keeps);
        }
        
        public void onNodeRemoved(ClusterEvent event) {  // NYI by layer below us...
            synchronized (_planLock) {
                Node node=event.getNode();
                _log.info("onNodeRemoved: "+getNodeName(node));
            }
        }
        
        public void onNodeFailed(ClusterEvent event) {
            synchronized (_planLock) {
                Node node=event.getNode();
                _log.info("onNodeFailed: "+getNodeName(node));
            }
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
                    IndexPartition indexPartition=new IndexPartition(key);
                    _key2IndexPartition.put(key, indexPartition);
                }
                Object[] keys=_key2IndexPartition.keySet().toArray();
                //_distributedState.put(_indexPartitionsKey, keys);
                _distributedState.put(_numIndexPartitionsKey, new Integer(keys.length));
//                try {
//                    _cluster.getLocalNode().setState(_distributedState);
//                } catch (JMSException e) {
//                    _log.error("could not update distributed state");
//                }
            }
        }

        public void onDismissal(ClusterEvent event) {
            _log.info("resigning coordinatorship"); // never happens - coordinatorship is for life..
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
        
        protected static Object _exitSync=new Object();
        
        public static void main(String[] args) throws Exception {
            String nodeName=args[0];
            int numIndexPartitions=Integer.parseInt(args[1]);
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    synchronized (_exitSync) {
                        _exitSync.notifyAll();
                    }
                }
            });
            
            DIndexNode node=new DIndexNode(nodeName, numIndexPartitions);
            node.start();
            synchronized (_exitSync) {
                _exitSync.wait();
            }
            node.stop();            
        }
}
