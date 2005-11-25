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

import org.activecluster.Node;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.ExtendedCluster;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.impl.SimplePartitionMapper;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;

public class DIndexNode implements DispatcherConfig, PartitionManagerConfig {

    protected final Log _log=LogFactory.getLog(getClass());

    //protected final String _clusterUri="peer://org.codehaus.wadi";
    //protected final String _clusterUri="tcp://localhost:61616";
    //protected final String _clusterUri="tcp://smilodon:61616";
    protected final String _clusterUri="vm://localhost";
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final ActiveClusterDispatcher _dispatcher;
    protected final Map _distributedState=new ConcurrentHashMap();
    protected final String _nodeName;
    protected final PartitionMapper _mapper;
    protected final int _numPartitions;

    public String getContextPath() {
        return "/";
    }

    public DIndexNode(String nodeName, int numPartitions, PartitionMapper mapper) {
        _nodeName=nodeName;
        _dispatcher=new ActiveClusterDispatcher(_nodeName, _clusterName, _clusterUri, 5000L);
        _numPartitions=numPartitions;
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
        _mapper=mapper;
    }

    protected DIndex _dindex;

    public void start() throws Exception {
        _dispatcher.init(this);
        _dindex=new DIndex(_nodeName, _numPartitions, _dispatcher.getInactiveTime(), _dispatcher, _distributedState, _mapper);
        _dindex.init(this);
	_log.info("starting Cluster...");
        _dispatcher.setDistributedState(_distributedState);
        _dispatcher.start();
	_log.info("...Cluster started");
        _dindex.start();
    }

    public void stop() throws Exception {
        _dindex.stop();
    }

    public ExtendedCluster getCluster() {
        return (ExtendedCluster)_dispatcher.getCluster();
    }

    public DIndex getDIndex() {
        return _dindex;
    }

    public Destination getDestination() {
        return _dispatcher.getLocalDestination();
    }

    // DIndexConfig

    public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
      _log.warn("findRelevantSessionNames() - NYI");
    }

    //-----------------------------------------------------------

  protected static Latch _latch0=new Latch();
  protected static Latch _latch1=new Latch();

    protected static Object _exitSync = new Object();

    public static void main(String[] args) throws Exception {
        String nodeName=args[0];
        int numPartitions=Integer.parseInt(args[1]);

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

            DIndexNode node=new DIndexNode(nodeName, numPartitions, new SimplePartitionMapper(numPartitions));
            node.start();

            _latch0.acquire();

            node.stop();
        } finally {
            _latch1.release();
        }
    }

	public Node getCoordinatorNode() {
		throw new UnsupportedOperationException("NYI");
	}

	public long getInactiveTime() {
		throw new UnsupportedOperationException("NYI");
	}
}
