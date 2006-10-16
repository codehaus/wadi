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

    private final Peer primary;
    private final Peer[] secondaries;
    private final Object replica;

    public ReplicaInfo(Object replica) {
        if (null == replica) {
            throw new IllegalArgumentException("replica is required");
        }
        this.replica = replica;

        primary = null;
        secondaries = null;
    }

    public ReplicaInfo(Peer primary, Peer[] secondaries) {
        if (null == primary) {
            throw new IllegalArgumentException("primary is required");
        } else if (null == secondaries) {
            throw new IllegalArgumentException("secondaries is required");
        }
        this.primary = primary;
        this.secondaries = secondaries;
        
        replica = null;
    }

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
    }

    public ReplicaInfo(ReplicaInfo prototype, ReplicaInfo override) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == override) {
            throw new IllegalArgumentException("override is required");
        }
        
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

    public Peer getPrimary() {
        return primary;
    }
    
    public Peer[] getSecondaries() {
        return secondaries;
    }
    
    public Object getReplica() {
        return replica;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ReplicaInfo: primary [");
        buffer.append(primary);
        buffer.append("]; secondaries:");
        if (null == secondaries) {
            buffer.append("null");
        } else {
            for (int i = 0; i < secondaries.length; i++) {
                buffer.append("[" + i + "] " + secondaries[i]);
            }
        }
        buffer.append("; replica [" + replica + "]");
        return buffer.toString();
    }
    
}