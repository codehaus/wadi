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
package org.codehaus.wadi.location.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.codehaus.wadi.location.CoordinatorConfig;
import org.codehaus.wadi.partition.BasicPartitionBalancer;
import org.codehaus.wadi.partition.PartitionBalancer;

import EDU.oswego.cs.dl.util.concurrent.Slot;

//it's important that the Plan is constructed from snapshotted resources (i.e. the ground doesn't
//shift under its feet), and that it is made and executed as quickly as possible - as a node could
//leave the Cluster in the meantime...

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class Coordinator implements Runnable {
	protected static final Log _log = LogFactory.getLog(Coordinator.class);

    private final Slot _flag = new Slot();
    private final CoordinatorConfig _config;
    private final Cluster _cluster;
    private final Dispatcher _dispatcher;
    private final int _numPartitions;
    private final PartitionBalancer partitionBalancer;

    protected Thread _thread;

    public Coordinator(CoordinatorConfig config) {
        _config = config;
        _cluster = _config.getCluster();
        _dispatcher = _config.getDispatcher();
        _numPartitions = _config.getNumPartitions();
        partitionBalancer = newPartitionBalancer();
    }

    public synchronized void start() throws Exception {
        partitionBalancer.start();
        startCoordinatorThread();
    }

    public synchronized void stop() throws Exception {
        _flag.put(Boolean.FALSE);
        _thread.join();
        partitionBalancer.stop();
    }

    public synchronized void queueRebalancing() {
        _log.info("Queueing partition rebalancing");
        try {
            _flag.offer(Boolean.TRUE, 0);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
    }

    public void run() {
        AbstractCluster._cluster.set(_cluster);
        try {
            while (_flag.take() == Boolean.TRUE) {
                rebalanceClusterState();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            _log.warn("Coordinator thread interrupted - Restarting Coordinator thread");
            startCoordinatorThread();
        }
    }

    protected PartitionBalancer newPartitionBalancer() {
        return new BasicPartitionBalancer(_dispatcher, _numPartitions);
    }

    public void rebalanceClusterState() {
        try {
            partitionBalancer.balancePartitions();
        } catch (MessageExchangeException e) {
            scheduleRebalancing();
        }
    }

    protected void scheduleRebalancing() {
        long retryDelay = 500;
        _log.warn("Will retry rebalancing in " + retryDelay + " millis...");
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
        }
        queueRebalancing();
    }

    protected void startCoordinatorThread() {
        _thread = new Thread(this, "WADI Coordinator");
        _thread.start();
    }
}
