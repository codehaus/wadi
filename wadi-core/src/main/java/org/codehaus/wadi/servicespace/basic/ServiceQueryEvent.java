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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class ServiceQueryEvent implements Serializable {
    private final ServiceSpaceName serviceSpaceName;
    private final ServiceName serviceName;
    private final Peer queryingNode;
    
    public ServiceQueryEvent(ServiceSpaceName serviceSpaceName, ServiceName serviceName, Peer queryingNode) {
        if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        } else if (null == serviceName) {
            throw new IllegalArgumentException("serviceName is required");
        } else if (null == queryingNode) {
            throw new IllegalArgumentException("queryingNode is required");
        }
        this.serviceSpaceName = serviceSpaceName;
        this.serviceName = serviceName;
        this.queryingNode = queryingNode;
    }
    
    public ServiceSpaceName getServiceSpaceName() {
        return serviceSpaceName;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public Peer getQueryingPeer() {
        return queryingNode;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceQueryEvent)) {
            return false;
        }
        ServiceQueryEvent other = (ServiceQueryEvent) obj;
        return serviceSpaceName.equals(other.serviceSpaceName) && serviceName.equals(other.serviceName)
                && queryingNode.equals(other.queryingNode);
    }

    public String toString() {
        return "Query from [" + queryingNode + "] for service [" + serviceName + "] in space [" + serviceSpaceName + "]";
    }
    
}
