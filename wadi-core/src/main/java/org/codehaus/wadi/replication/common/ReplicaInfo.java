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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision$
 */
public class ReplicaInfo implements Externalizable {
    private static final long serialVersionUID = 1554455972740137174L;

    private Peer primary;
    private Peer[] secondaries;
    private transient Motable payload;
    private int version;

    public ReplicaInfo() {
    }
    
    public ReplicaInfo(Peer primary, Peer[] secondaries, Motable payload) {
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
    
    public Motable getPayload() {
        return payload;
    }

    public void setPayload(Motable payload) {
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

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        primary = (Peer) in.readObject();
        int length = in.readInt();
        secondaries = new Peer[length];
        for (int i = 0; i < length; i++) {
            secondaries[i] = (Peer) in.readObject();
        }
        version = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(primary);
        out.writeInt(secondaries.length);
        for (Peer secondary : secondaries) {
            out.writeObject(secondary);
        }
        out.writeInt(version);
    }

}