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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicServiceHolder implements Lifecycle {
    private static final Log log = LogFactory.getLog(BasicServiceHolder.class);
    
    protected final ServiceSpace serviceSpace;
    protected final Lifecycle service;
    private final ServiceName serviceName;
    private volatile boolean started;
 
    public BasicServiceHolder(ServiceSpace serviceSpace, ServiceName serviceName, Lifecycle service) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == service) {
            throw new IllegalArgumentException("service is required");
        } else if (null == service) {
            throw new IllegalArgumentException("service is required");
        }
        this.serviceSpace = serviceSpace;
        this.serviceName = serviceName;
        this.service = service;
    }
    
    public void start() throws Exception {
        sendLifecycleEventToCluster(LifecycleState.STARTING);
        try {
            service.start();
            started = true;
            sendLifecycleEventToCluster(LifecycleState.STARTED);
        } catch (Exception e) {
            sendLifecycleEventToCluster(LifecycleState.FAILED);
            throw e;
        }
    }
    
    public void stop() throws Exception {
        started = false;
        sendLifecycleEventToCluster(LifecycleState.STOPPING);
        try {
            service.stop();
            sendLifecycleEventToCluster(LifecycleState.STOPPED);
        } catch (Exception e) {
            sendLifecycleEventToCluster(LifecycleState.FAILED);
            throw e;
        }
    }

    public boolean isStarted() {
        return started;
    }
    
    public Lifecycle getService() {
        return service;
    }

    protected void sendLifecycleEventToCluster(LifecycleState state) {
        Dispatcher dispatcher = serviceSpace.getDispatcher();
        LocalPeer localPeer = dispatcher.getCluster().getLocalPeer();
        ServiceLifecycleEvent event = new ServiceLifecycleEvent(serviceSpace.getServiceSpaceName(), 
            serviceName, 
            localPeer,
            state);
        try {
            dispatcher.send(dispatcher.getCluster().getAddress(), event);
        } catch (MessageExchangeException e) {
            log.warn("Cannot send lifecycle event [" + event + "] to cluster", e);
        }
    }
    
    public String toString() {
        return "Holder for service [" + service + "] named [" + serviceName + "] in space [" + serviceSpace + "]"; 
    }
    
}
