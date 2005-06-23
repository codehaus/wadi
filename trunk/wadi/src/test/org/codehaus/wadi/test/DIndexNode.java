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

import java.util.Collection;
import java.util.Map;

import javax.jms.Destination;

import org.activecluster.Cluster;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.dindex.DIndexConfig;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.Dispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;

public class DIndexNode implements DispatcherConfig, DIndexConfig {

    protected final Log _log=LogFactory.getLog(getClass());

    //protected final String _clusterUri="peer://org.codehaus.wadi";
    //protected final String _clusterUri="tcp://localhost:61616";
    protected final String _clusterUri="tcp://smilodon:61616";
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
    protected final CustomClusterFactory _clusterFactory=new CustomClusterFactory(_connectionFactory);
    protected final Dispatcher _dispatcher=new Dispatcher();
    protected final Map _distributedState=new ConcurrentHashMap();
    protected final String _nodeName;
    protected final int _numBuckets;

    protected String getContextPath() {
        return "/";
    }

    public DIndexNode(String nodeName, int numBuckets) {
        _nodeName=nodeName;
        _numBuckets=numBuckets;
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
    }

    protected DIndex _dindex;
    protected ExtendedCluster _cluster;

    public void start() throws Exception {
        _connectionFactory.start();
        _cluster=(ExtendedCluster)_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
        _dispatcher.init(this);
        _dindex=new DIndex(_nodeName, _numBuckets, _clusterFactory.getInactiveTime(), _cluster, _dispatcher, _distributedState);
        _dindex.init(this);
        _log.info("starting Cluster...");
        _cluster.getLocalNode().setState(_distributedState);
        _cluster.start();
        _log.info("...Cluster started");
        _dindex.start();
    }

    public void stop() throws Exception {
        _dindex.stop();
        _cluster.stop();
        _connectionFactory.stop();
    }

    public ExtendedCluster getCluster() {
        return _cluster;
    }

    public DIndex getDIndex() {
        return _dindex;
    }
    
    public Destination getDestination() {
        return _cluster.getLocalNode().getDestination();
    }
    
    // DIndexConfig
    
    public void findRelevantSessionNames(int numBuckets, Collection[] resultSet) {
        _log.warn("findRelevantSessionNames() - NYI");
    }
    
    //-----------------------------------------------------------

  protected static Latch _latch0=new Latch();
  protected static Latch _latch1=new Latch();

    protected static Object _exitSync = new Object();

    public static void main(String[] args) throws Exception {
        String nodeName=args[0];
        int numIndexPartitions=Integer.parseInt(args[1]);
        
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.err.println("SHUTDOWN");
                    _latch0.release();
                    try {
                        _latch1.acquire();
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                }
            });
            
            DIndexNode node=new DIndexNode(nodeName, numIndexPartitions);
            node.start();
            
            _latch0.acquire();
            
            node.stop();
        } finally {
            _latch1.release();
        }
    }
}
