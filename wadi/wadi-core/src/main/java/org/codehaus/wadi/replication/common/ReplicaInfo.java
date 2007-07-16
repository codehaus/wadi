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

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision$
 */
public class ReplicaInfo implements Serializable {
    private static final long serialVersionUID = 1554455972740137174L;

    private Peer primary;
    private Peer[] secondaries;
    private Object replica;
    private int version;

    public ReplicaInfo(Peer primary, Peer[] secondaries, Object replica) {
        if (null == primary) {
            throw new IllegalArgumentException("primary is required");
        } else if (null == secondaries) {
            throw new IllegalArgumentException("secondaries is required");
        } else if (null == replica) {
            throw new IllegalArgumentException("replica is required");
        }
        this.primary = primary;
        this.secondaries = secondaries;
        this.replica = replica;
        
        version = 0;
    }

    public ReplicaInfo(ReplicaInfo prototype, Peer[] secondaries) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == secondaries) {
            throw new IllegalArgumentException("secondaries is required");
        }
        this.secondaries = secondaries;
        primary = prototype.primary;
        replica = prototype.replica;
        version = prototype.version + 1;
    }

    public ReplicaInfo(ReplicaInfo prototype, Object replica) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == replica) {
            throw new IllegalArgumentException("replica is required");
        }
        this.replica = replica;
        
        primary = prototype.primary;
        secondaries = prototype.secondaries;
        version = prototype.version + 1;
    }

    public ReplicaInfo(ReplicaInfo prototype, Peer primary, Peer[] secondaries) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == primary) {
            throw new IllegalArgumentException("primary is required");
        } else if (null == secondaries) {
            throw new IllegalArgumentException("secondaries is required");
        }
        this.primary = primary;
        this.secondaries = secondaries;
        
        replica = prototype.replica;
        version = prototype.version + 1;
    }

    public void mergeWith(ReplicaInfo replicaInfo) {
        primary = replicaInfo.primary;
        secondaries = replicaInfo.secondaries;
        version = replicaInfo.version;
        replica = mergeReplica(replicaInfo.replica);
    }

    protected Object mergeReplica(Object otherReplica) {
        return otherReplica;
    }

    public Peer getPrimary() {
        return primary;
    }
    
    public Peer[] getSecondaries() {
        return secondaries;
    }
    
    public Object getReplica() {
        return replica;
    }

    public int getVersion() {
        return version;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ReplicaInfo: primary [");
        buffer.append(primary);
        buffer.append("]; secondaries:");
        for (int i = 0; i < secondaries.length; i++) {
            buffer.append("[" + i + "] " + secondaries[i]);
        }
        buffer.append("; replica [" + replica + "]; version [" + version + "]");
        return buffer.toString();
    }

}