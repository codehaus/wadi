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
package org.codehaus.wadi.servicespace.basic;

import java.util.Collections;

import org.codehaus.wadi.group.vm.VMEnvelope;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceRegistry;

/**
 * 
 * @version $Revision: $
 */
public class ServiceQueryEndpointTest extends AbstractServiceSpaceTestCase {
    
    private ServiceName serviceName1;
    private ServiceName serviceName2;

    protected void setUp() throws Exception {
        super.setUp();
        serviceName1 = new ServiceName("name1");
        serviceName2 = new ServiceName("name2");
    }
    
    public void testServiceQueryEventForAvailableService() throws Exception {
        beginSection(s.ordered("Get service names - send available event"));
        ServiceRegistry serviceRegistry = (ServiceRegistry) mock(ServiceRegistry.class);
        serviceRegistry.getServiceNames();
        modify().returnValue(Collections.singleton(serviceName1));

        dispatcher.send(address1, new ServiceLifecycleEvent(serviceSpaceName, serviceName1, localPeer,
                LifecycleState.AVAILABLE));
        endSection();
        
        startVerification();
        
        ServiceQueryEndpoint endpoint = new ServiceQueryEndpoint(serviceRegistry, serviceSpace);
        VMEnvelope message = new VMEnvelope();
        ServiceQueryEvent serviceQueryEvent = new ServiceQueryEvent(serviceSpaceName, serviceName1, remote1);
        message.setPayload(serviceQueryEvent);
        assertTrue(endpoint.testDispatchMessage(message));
        endpoint.dispatch(message);
    }

    public void testServiceQueryEventForUnknowService() throws Exception {
        ServiceRegistry serviceRegistry = (ServiceRegistry) mock(ServiceRegistry.class);
        serviceRegistry.getServiceNames();
        modify().returnValue(Collections.singleton(serviceName2));
        
        startVerification();
        
        ServiceQueryEndpoint endpoint = new ServiceQueryEndpoint(serviceRegistry, serviceSpace);
        VMEnvelope message = new VMEnvelope();
        ServiceQueryEvent serviceQueryEvent = new ServiceQueryEvent(serviceSpaceName, serviceName1, remote1);
        message.setPayload(serviceQueryEvent);
        assertTrue(endpoint.testDispatchMessage(message));
        endpoint.dispatch(message);
    }

}
