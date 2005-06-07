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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

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
import EDU.oswego.cs.dl.util.concurrent.Sync;

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
        protected volatile boolean _running;
        protected Thread _planThread;
        
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
            
            _running=true;
            (_planThread=new Thread(new PlanExecutor())).start();

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
            _running=false;
            synchronized (_planSync) {_planSync.notifyAll();} // wake PlanExecutor
            _planThread.join();
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

        public void onNodeAdd(ClusterEvent event) {
            Node node=event.getNode();
            _log.info("onNodeAdd: "+getNodeName(node));
            layout(node);
        }

        public void onNodeRemoved(ClusterEvent event) {  // NYI by layer below us...
            Node node=event.getNode();
            _log.info("onNodeRemoved: "+getNodeName(node));
            layout(node);
        }

        public void onNodeFailed(ClusterEvent event) {
            Node node=event.getNode();
            _log.info("onNodeFailed: "+getNodeName(node));
            layout(node);
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

        
        public class Plan {
            
            public Node _node;
            public int _minIndexPartitionsPerNode;
            public int _numIndexPartitionsRemainder;
            public int _maxIndexPartitionsPerNode;
        }

        protected Object _planSync=new Object();
        protected Plan _plan;
        
        protected void layout(Node node) {
            Plan plan=new Plan();
            plan._node=node;
            plan._minIndexPartitionsPerNode=_numIndexPartitions/(_cluster.getNodes().size()+1);
            plan._numIndexPartitionsRemainder=_numIndexPartitions%plan._minIndexPartitionsPerNode;
            plan._maxIndexPartitionsPerNode=plan._numIndexPartitionsRemainder>0?(plan._minIndexPartitionsPerNode+1):plan._minIndexPartitionsPerNode;
            _log.info("MIN:"+plan._minIndexPartitionsPerNode+", MAX:"+plan._maxIndexPartitionsPerNode);
            synchronized (_planSync) {
                _plan=plan;
                _planSync.notifyAll(); // we have a new plan
            }
            
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
        
        public class PlanExecutor implements Runnable {
            
            public void run() {
                try {
                    while (_running) {
                        _log.info("plan - sleeping");
                        try {
                            synchronized (_planSync) {_planSync.wait();} // wait for a new plan...
                        } catch (InterruptedException e) {
                            _log.info("woken by interruption", e);
                            Thread.interrupted();
                        }
                        _log.info("plan - waking");
                        _log.info("plan started");
                        Plan plan;
                        synchronized (_planSync) {plan=_plan;}
                        
                        //Object[] keys=(Object[])_distributedState.get(_indexPartitionsKey);
                        int numKeys=((Integer)_distributedState.get(_numIndexPartitionsKey)).intValue();
                        int offset=numKeys-plan._minIndexPartitionsPerNode;
                        
                        if (offset>0)
                            giveIndexPartitions(offset, plan._node);
                        if (offset<0)
                            _log.info("hoping to receive "+(-offset)+" IndexPartitions from Cluster");
                        if (offset==0)
                            _log.info("empty plan");
                        
                        _log.info("plan completed");
                        
                        // keep running plan 'til failure or completion... - TODO
                    }
                } catch (InterruptedException e) {
                    _log.info("interrupted - quitting");
                    Thread.interrupted();
                }
            }
        }
        
        public boolean giveIndexPartitions(int numIndexPartitions, Node recipient) throws InterruptedException {
            _log.info("giving "+numIndexPartitions+" IndexPartitions to "+getNodeName(recipient));
            List acquired=new ArrayList(numIndexPartitions);
            try {
                for (Iterator i=_key2IndexPartition.values().iterator(); acquired.size()<numIndexPartitions && i.hasNext(); ) {
                    IndexPartition partition=(IndexPartition)i.next();
                    Sync lock=partition.getExclusiveLock();
                    long timeout=500; // how should we work this out ?
                    if (lock.attempt(timeout))
                        acquired.add(partition);
                }
                _log.info("sending "+acquired.size()+" IndexPartions to "+getNodeName(recipient));
                String correlationId=""+System.currentTimeMillis();
                MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
                settingsInOut.from=_cluster.getLocalNode().getDestination();
                settingsInOut.to=recipient.getDestination();
                settingsInOut.correlationId=correlationId;
                IndexPartitionsTransferRequest request=new IndexPartitionsTransferRequest((IndexPartition[])acquired.toArray(new IndexPartition[acquired.size()]));
                IndexPartitionsTransferResponse response=(IndexPartitionsTransferResponse)_dispatcher.exchangeMessages(request, _indexPartitionTransferRvMap, settingsInOut, 5000); // TODO FIXME
                if (response.getSuccess()) {
                    _log.info("transfer successful");
                    for (int i=0; i<acquired.size(); i++)
                        _key2IndexPartition.remove(((IndexPartition)acquired.get(i)).getKey());
                    _log.info("old partitions removed");
                    Object[] keys=_key2IndexPartition.keySet().toArray();
                    //_distributedState.put(_indexPartitionsKey, keys);
                    _distributedState.put(_numIndexPartitionsKey, new Integer(keys.length));
                    _log.info("local state updated");
                    _cluster.getLocalNode().setState(_distributedState);
                    _log.info("distributed state updated");
                } else {
                    _log.warn("transfer unsuccessful");
                }
            } catch (Throwable t) {
                _log.warn("unexpected problem", t);
            } finally {
                for (int i=0; i<acquired.size(); i++)
                    ((IndexPartition)acquired.get(i)).getExclusiveLock().release();
                _log.info("locks released");
            }
            
            return true;
        }

        
        public void onIndexPartitionsTransferRequest(ObjectMessage om, IndexPartitionsTransferRequest request) {
            IndexPartition[] indexPartitions=request.getIndexPartitions();
            _log.info("received "+indexPartitions.length);
            boolean success=false;
            // read incoming data into our own local model
            for (int i=0; i<indexPartitions.length; i++) {
                IndexPartition partition=indexPartitions[i];
                // we should lock these until acked - then unlock them - TODO...
                _key2IndexPartition.put(partition.getKey(), partition);
            }
            success=true;
            boolean acked=false;
            // acknowledge safe receipt to donor
            try {
                _dispatcher.replyToMessage(om, new IndexPartitionsTransferResponse(success));
                acked=true;
                try {
                    // notify rest of Cluster of change of partition ownership...
                    Object[] keys=_key2IndexPartition.keySet().toArray();
                    //_distributedState.put(_indexPartitionsKey, keys);
                    _distributedState.put(_numIndexPartitionsKey, new Integer(keys.length));
                    _cluster.getLocalNode().setState(_distributedState);
                } catch (JMSException e) {
                    _log.error("could not update distributed state", e);
                }
            } catch (JMSException e) {
                _log.warn("problem acknowledging reciept of IndexPartitions - donor may have died", e);
            }
            if (acked) {
                // unlock Partitions here... - TODO
            } else {
                // chuck them... - TODO
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
