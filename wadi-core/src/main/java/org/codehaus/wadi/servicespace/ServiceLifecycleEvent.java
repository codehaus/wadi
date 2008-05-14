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
package org.codehaus.wadi.servicespace;

import java.io.Serializable;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: $
 */
public class ServiceLifecycleEvent implements Serializable {
    private final ServiceSpaceName serviceSpaceName;
    private final ServiceName serviceName;
    private final Peer hostingPeer;
    private final LifecycleState state;
    
    public ServiceLifecycleEvent(ServiceSpaceName serviceSpaceName, ServiceName serviceName, Peer hostingPeer,
            LifecycleState state) {
        if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        } else if (null == serviceName) {
            throw new IllegalArgumentException("serviceName is required");
        } else if (null == hostingPeer) {
            throw new IllegalArgumentException("hostingPeer is required");
        } else if (null == state) {
            throw new IllegalArgumentException("state is required");
        }
        this.serviceSpaceName = serviceSpaceName;
        this.serviceName = serviceName;
        this.hostingPeer = hostingPeer;
        this.state = state;
    }
    
    public ServiceSpaceName getServiceSpaceName() {
        return serviceSpaceName;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public LifecycleState getState() {
        return state;
    }

    public Peer getHostingPeer() {
        return hostingPeer;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceLifecycleEvent)) {
            return false;
        }
        ServiceLifecycleEvent other = (ServiceLifecycleEvent) obj;
        return serviceSpaceName.equals(other.serviceSpaceName) && serviceName.equals(other.serviceName)
                && state == other.state;
    }
    
    public String toString() {
        return "Service [" + serviceName + "] in space [" + serviceSpaceName + "] hosted by [" + hostingPeer + "] is ["
                + state + "]";
    }
    
}
