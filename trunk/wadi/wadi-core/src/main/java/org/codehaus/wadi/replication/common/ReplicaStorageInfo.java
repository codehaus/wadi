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
package org.codehaus.wadi.replication.common;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 
 * @version $Revision: 2340 $
 */
public class ReplicaStorageInfo implements Externalizable {

    private ReplicaInfo replicaInfo;
    private byte[] serializedPayload;

    public ReplicaStorageInfo() {
    }
    
    public ReplicaStorageInfo(ReplicaInfo replicaInfo, byte[] serializedPayload) {
        if (null == replicaInfo) {
            throw new IllegalArgumentException("replicaInfo is required");
        } else if (null == serializedPayload) {
            throw new IllegalArgumentException("serializedPayload is required");
        }
        this.replicaInfo = replicaInfo;
        this.serializedPayload = serializedPayload;
    }

    public ReplicaInfo getReplicaInfo() {
        return replicaInfo;
    }

    public byte[] getSerializedPayload() {
        return serializedPayload;
    }

    public int getVersion() {
        return replicaInfo.getVersion();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        replicaInfo = (ReplicaInfo) in.readObject();
        int length = in.readInt();
        serializedPayload = new byte[length];
        in.readFully(serializedPayload);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(replicaInfo);
        out.writeInt(serializedPayload.length);
        out.write(serializedPayload);
    }

}