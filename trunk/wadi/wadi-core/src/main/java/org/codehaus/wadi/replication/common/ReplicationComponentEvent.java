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
package org.codehaus.wadi.replication.common;

import java.io.Serializable;

/**
 * 
 * @version $Revision$
 */
public class ReplicationComponentEvent implements Serializable {
    private static final long serialVersionUID = -1229388548745295604L;
    
    private final ComponentEventType type;
    private final NodeInfo hostingNode;

    public ReplicationComponentEvent(ComponentEventType type, NodeInfo hostingNode) {
        this.type = type;
        this.hostingNode = hostingNode;
    }

    public NodeInfo getHostingNode() {
        return hostingNode;
    }

    public ComponentEventType getType() {
        return type;
    }

    public String toString() {
        return type + "; " + hostingNode;
    }
}