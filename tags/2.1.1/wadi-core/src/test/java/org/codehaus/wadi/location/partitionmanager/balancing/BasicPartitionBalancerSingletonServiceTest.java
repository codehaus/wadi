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

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        balancer = (PartitionBalancer) mock(PartitionBalancer.class);
        peer = (Peer) mock(Peer.class);
        serviceSpaceName = new ServiceSpaceName(URI.create("test"));
    }
    
    public void testServiceSpaceFailureQueueRebalancing() throws Exception {
        serviceSpace.addServiceSpaceListener(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {

            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                ServiceSpaceListener listener = (ServiceSpaceListener) arg0[0];
                listener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, peer, LifecycleState.FAILED),
                        Collections.EMPTY_SET);
                return null;
            }
            
        });

        balancer.start();
        balancer.balancePartitions();
        
        startVerification();
        
        BasicPartitionBalancerSingletonService service = new BasicPartitionBalancerSingletonService(serviceSpace,
                balancer);
        service.start();
        
        Thread.sleep(500);
    }
    
}
