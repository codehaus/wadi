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
public class ReplicaInfo implements Serializable {
    private static final long serialVersionUID = 1554455972740137174L;

    private final NodeInfo primary;
    private final NodeInfo[] secondaries;
    private final Object replica;
    
    public ReplicaInfo(NodeInfo primary, NodeInfo[] secondaries, Object replica) {
        this.primary = primary;
        this.secondaries = secondaries;
        this.replica = replica;
    }

    public ReplicaInfo(ReplicaInfo prototype, Object replica) {
        this.primary = prototype.primary;
        this.secondaries = prototype.secondaries;
        this.replica = replica;
    }

    public ReplicaInfo(ReplicaInfo prototype, NodeInfo primary, NodeInfo[] secondaries) {
        this.primary = primary;
        this.secondaries = secondaries;
        this.replica = prototype.replica;
    }

    public ReplicaInfo(ReplicaInfo prototype, ReplicaInfo override) {
        if (null != override.primary) {
            this.primary = override.primary;
        } else {
            this.primary = prototype.primary;
        }
        if (null != override.secondaries) {
            this.secondaries = override.secondaries;
        } else {
            this.secondaries = prototype.secondaries;
        }
        if (null != override.replica) {
            this.replica = override.replica;
        } else {
            this.replica = prototype.replica;
        }
    }

    public NodeInfo getPrimary() {
        return primary;
    }
    
    public NodeInfo[] getSecondaries() {
        return secondaries;
    }
    
    public Object getReplica() {
        return replica;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ReplicaInfo: primary:");
        buffer.append(primary);
        buffer.append("; secondaries:");
        for (int i = 0; i < secondaries.length; i++) {
            buffer.append("[" + i + "] " + secondaries[i]);
        }
        buffer.append("; replica:" + replica);
        
        return buffer.toString();
    }
}