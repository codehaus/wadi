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
    private transient Object payload;
    private int version;

    public ReplicaInfo(Peer primary, Peer[] secondaries, Object payload) {
        if (null == primary) {
            throw new IllegalArgumentException("primary is required");
        } else if (null == secondaries) {
            throw new IllegalArgumentException("secondaries is required");
        } else if (null == payload) {
            throw new IllegalArgumentException("payload is required");
        }
        this.primary = primary;
        this.secondaries = secondaries;
        this.payload = payload;
        
        version = 0;
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
        
        payload = prototype.payload;
        version = prototype.version + 1;
    }

    public Peer getPrimary() {
        return primary;
    }
    
    public Peer[] getSecondaries() {
        return secondaries;
    }
    
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getVersion() {
        return version;
    }
    
    public void updateSecondaries(Peer[] secondaries) {
        this.secondaries = secondaries;
        version++;
    }
    
    public void increaseVersion() {
        version++;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ReplicaInfo: primary [");
        buffer.append(primary);
        buffer.append("]; secondaries:");
        for (int i = 0; i < secondaries.length; i++) {
            buffer.append("[" + i + "] " + secondaries[i]);
        }
        buffer.append("; version [" + version + "]");
        return buffer.toString();
    }

}