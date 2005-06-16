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

import org.activecluster.Cluster;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.MessageDispatcherConfig;
import org.codehaus.wadi.impl.MessageDispatcher;

public class DIndexNode implements MessageDispatcherConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    
    //protected final String _clusterUri="peer://org.codehaus.wadi";
    //protected final String _clusterUri="tcp://localhost:61616";
    protected final String _clusterUri="tcp://smilodon:61616";
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
    protected final DefaultClusterFactory _clusterFactory=new DefaultClusterFactory(_connectionFactory);;
    protected final MessageDispatcher _dispatcher=new MessageDispatcher();

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
    protected Cluster _cluster;

    public void start() throws Exception {
        _connectionFactory.start();
        _cluster=_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
        _dispatcher.init(this);
        _dindex=new DIndex(_nodeName, _numBuckets, _clusterFactory, _clusterFactory.getInactiveTime(), _cluster, _dispatcher);
        _dindex.start();
        _log.info("starting Cluster...");
        _cluster.start(); // N.B. must be done AFTER starting DIndex
        _log.info("...Cluster started");
    }
    
    public void stop() throws Exception {
        _dindex.stop();
        _cluster.stop(); // N.B. must be done AFTER stopping DIndex
        _connectionFactory.stop();
    }
    
    public Cluster getCluster() {
        return _cluster;
    }
    
    //-----------------------------------------------------------
    
    protected static Object _exitSync = new Object();

    public static void main(String[] args) throws Exception {
        String nodeName=args[0];
        int numIndexPartitions=Integer.parseInt(args[1]);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.err.println("SHUTDOWN");
                synchronized (_exitSync) {_exitSync.notifyAll();}
                try {
                    synchronized (_exitSync) {_exitSync.wait();}
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });
        
        DIndexNode node=new DIndexNode(nodeName, numIndexPartitions);
        node.start();
        
        synchronized (_exitSync) {_exitSync.wait();}
        
        node.stop();
        
        synchronized (_exitSync) {_exitSync.notifyAll();}
        
    }
}
