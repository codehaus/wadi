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
public class ServiceSpaceInfo implements Serializable {
    private final Peer hostingPeer;
    private final ServiceSpaceName serviceSpaceName;
    
    public ServiceSpaceInfo(Peer hostingPeer, ServiceSpaceName serviceSpaceName) {
        if (null == hostingPeer) {
            throw new IllegalArgumentException("hostingPeer is required");
        } else if (null == serviceSpaceName) {
            throw new IllegalArgumentException("serviceSpaceName is required");
        }
        this.hostingPeer = hostingPeer;
        this.serviceSpaceName = serviceSpaceName;
    }
    
    public ServiceSpaceName getServiceSpaceName() {
        return serviceSpaceName;
    }

    public Peer getHostingPeer() {
        return hostingPeer;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceSpaceInfo)) {
            return false;
        }
        ServiceSpaceInfo other = (ServiceSpaceInfo) obj;
        return hostingPeer.equals(other.hostingPeer) && serviceSpaceName.equals(other.serviceSpaceName);
    }

    public int hashCode() {
        return hostingPeer.hashCode() * serviceSpaceName.hashCode();
    }
 
    public String toString() {
        return "ServiceSpaceInfo name[" + serviceSpaceName + "]; hosted by Peer[" + hostingPeer + "]";
    }
    
}
