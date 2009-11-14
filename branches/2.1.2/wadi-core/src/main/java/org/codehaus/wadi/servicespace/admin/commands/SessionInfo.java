/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.servicespace.admin.commands;

import java.io.Serializable;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SessionInfo implements Serializable {
    private final Peer hostingPeer;
    private final String name;
    private final String contextualiserName;
    private final int contextualiserIndex;
    
    public SessionInfo(Peer hostingPeer, String name, String contextualiserName, int contextualiserIndex) {
        if (null == hostingPeer) {
            throw new IllegalArgumentException("hostingPeer is required");
        } else if (null == name) {
                throw new IllegalArgumentException("name is required");
        } else if (null == contextualiserName) {
            throw new IllegalArgumentException("contextualiserName is required");
        }
        this.hostingPeer = hostingPeer;
        this.name = name;
        this.contextualiserName = contextualiserName;
        this.contextualiserIndex = contextualiserIndex;
    }

    public String getContextualiserName() {
        return contextualiserName;
    }

    public String getName() {
        return name;
    }

    public int getContextualiserIndex() {
        return contextualiserIndex;
    }

    public Peer getHostingPeer() {
        return hostingPeer;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionInfo)) {
            return false;
        }
        SessionInfo other = (SessionInfo) obj;
        return name.equals(other.name);
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    
}
