/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.location.balancing;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;

import EDU.oswego.cs.dl.util.concurrent.Slot;

/**
 * @version $Revision:1815 $
 */
public class BasicPartitionBalancerSingletonService implements PartitionBalancerSingletonService {
    private static final Log log = LogFactory.getLog(BasicPartitionBalancerSingletonService.class);

    private final ServiceSpace serviceSpace;
	private final PartitionBalancer partitionBalancer;
    private final Slot rebalancingFlag;
    private Thread thread;

    private LeavingServiceSpaceMonitor leavingServiceSpaceMonitor;

    public BasicPartitionBalancerSingletonService(ServiceSpace serviceSpace, PartitionBalancer partitionBalancer) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == partitionBalancer) {
            throw new IllegalArgumentException("partitionBalancer is required");
        }
        this.serviceSpace = serviceSpace;
        this.partitionBalancer = partitionBalancer;

        rebalancingFlag = new Slot();
        leavingServiceSpaceMonitor = new LeavingServiceSpaceMonitor();
    }

    public void start() throws Exception {
        partitionBalancer.start();
        thread = new Thread(this, "WADI Partition Balancer");
        thread.start();
        
        serviceSpace.addServiceSpaceListener(leavingServiceSpaceMonitor);
    }

    public void stop() throws Exception {
        serviceSpace.removeServiceSpaceListener(leavingServiceSpaceMonitor);
        rebalancingFlag.put(Boolean.FALSE);
        thread.join();
        partitionBalancer.stop();
    }

    public void queueRebalancing() { 
        log.info("Queueing partition rebalancing");
        try {
            rebalancingFlag.offer(Boolean.TRUE, 0);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
    }

    public void run() {
        try {
            while (rebalancingFlag.take() == Boolean.TRUE) {
                try {
                    partitionBalancer.balancePartitions();
                } catch (MessageExchangeException e) {
                    log.warn("Rebalancing has failed", e);
                    scheduleRebalancing();
                }
            }
        } catch (InterruptedException e) {
            log.error("Coordinator thread interrupted", e);
        }
    }

    @Override
    public String toString() {
        return "PartitionManager for ServiceSpace [" + serviceSpace.getServiceSpaceName() + "]";
    }
    
    protected void scheduleRebalancing() {
        long retryDelay = 500;
        log.warn("Will retry rebalancing in [" + retryDelay + "] ms");
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        queueRebalancing();
    }
    
    private class LeavingServiceSpaceMonitor implements ServiceSpaceListener {

        public void receive(ServiceSpaceLifecycleEvent event, Set<Peer> newHostingPeers) {
            if (event.getState() == LifecycleState.FAILED) {
                queueRebalancing();
            }
        }
        
    }
    
}
