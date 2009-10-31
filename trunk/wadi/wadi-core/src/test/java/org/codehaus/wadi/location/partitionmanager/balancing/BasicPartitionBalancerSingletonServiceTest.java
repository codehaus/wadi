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
package org.codehaus.wadi.location.partitionmanager.balancing;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.balancing.BasicPartitionBalancerSingletonService;
import org.codehaus.wadi.location.balancing.PartitionBalancer;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.core.Section;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicPartitionBalancerSingletonServiceTest extends RMockTestCase {

    private ServiceSpace serviceSpace;
    private PartitionBalancer balancer;
    private Peer peer;
    private ServiceSpaceName serviceSpaceName;
    private ServiceSpaceListener serviceSpaceListener;

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        balancer = (PartitionBalancer) mock(PartitionBalancer.class);
        peer = (Peer) mock(Peer.class);
        serviceSpaceName = new ServiceSpaceName(URI.create("test"));

        balancer.start();
        serviceSpace.addServiceSpaceListener(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {
            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                serviceSpaceListener = (ServiceSpaceListener) arg0[0];
                return null;
            }
        });
    }
    
    public void testServiceSpaceFailureTriggersPartitionBalancing() throws Exception {
        final CountDownLatch waitForBalancing = new CountDownLatch(1);
        balancer.balancePartitions();
        releaseSemaphore(waitForBalancing);
        
        startVerification();
        
        BasicPartitionBalancerSingletonService service = new BasicPartitionBalancerSingletonService(serviceSpace,
                balancer);
        service.start();

        serviceSpaceListener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, peer, LifecycleState.FAILED),
                Collections.EMPTY_SET);
        
        assertTrue(waitForBalancing.await(2, TimeUnit.SECONDS));
    }
    
    public void testPartitionBalancingTriggersRebalancing() throws Exception {
        Section orderedSection = s.ordered("Failed balancing followed by successful one");
        beginSection(orderedSection);
        balancer.balancePartitions();
        modify().throwException(new MessageExchangeException("test"));
        
        final CountDownLatch waitForBalancing = new CountDownLatch(1);
        balancer.balancePartitions();
        releaseSemaphore(waitForBalancing);
        endSection();

        startVerification();
        
        BasicPartitionBalancerSingletonService service = new BasicPartitionBalancerSingletonService(serviceSpace,
                balancer);
        service.start();
        
        service.queueRebalancing();
        
        assertTrue(waitForBalancing.await(2, TimeUnit.SECONDS));
    }
    
    protected void releaseSemaphore(final CountDownLatch waitForBalancing) {
        modify().perform(new Action() {
            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                waitForBalancing.countDown();
                return null;
            }
        });
    }
    
}
