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

import java.io.Serializable;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class ServiceQueryEndpoint implements ServiceEndpoint {

    private final ServiceRegistry registry;
    private final ServiceSpace serviceSpace;

    public ServiceQueryEndpoint(ServiceRegistry registry, ServiceSpace serviceSpace) {
        if (null == registry) {
            throw new IllegalArgumentException("registry is required");
        } else if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.registry = registry;
        this.serviceSpace = serviceSpace;
    }

    public void dispatch(Message om) throws Exception {
        ServiceQueryEvent event = (ServiceQueryEvent) om.getPayload();
        ServiceName serviceName = event.getServiceName();
        if (registry.getServiceNames().contains(serviceName)) {
            Dispatcher dispatcher = serviceSpace.getDispatcher();
            dispatcher.send(event.getQueryingPeer().getAddress(),
                    new ServiceLifecycleEvent(
                            serviceSpace.getServiceSpaceName(), 
                            serviceName,
                            dispatcher.getCluster().getLocalPeer(),
                            LifecycleState.AVAILABLE));
        }
    }

    public void dispose(int nbAttemp, long delayMillis) {
        return;
    }

    public boolean testDispatchMessage(Message om) {
        Serializable payload = om.getPayload();
        return payload instanceof ServiceQueryEvent;
    }
    
}