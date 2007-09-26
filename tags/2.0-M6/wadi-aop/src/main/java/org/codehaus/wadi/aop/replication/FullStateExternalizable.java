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
package org.codehaus.wadi.aop.replication;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.util.ClusteredStateHelper;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FullStateExternalizable implements Externalizable {
    protected final InstanceIdFactory instanceIdFactory;
    protected final ClusteredStateSessionMemento memento;
    
    public FullStateExternalizable(InstanceIdFactory instanceIdFactory, ClusteredStateSessionMemento memento) {
        if (null == instanceIdFactory) {
            throw new IllegalArgumentException("instanceIdFactory is required");
        } else if (null == memento) {
            throw new IllegalArgumentException("memento is required");
        }
        this.instanceIdFactory = instanceIdFactory;
        this.memento = memento;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        byte[] serInstTracker = serialize();
        out.writeInt(serInstTracker.length);
        out.write(serInstTracker);
        ClusteredStateMarker stateMarker = (ClusteredStateMarker) memento;
        String instanceId = stateMarker.$wadiGetTracker().getInstanceId();
        out.writeUTF(instanceId);
    }

    protected byte[] serialize() {
        return ClusteredStateHelper.serializeFully(instanceIdFactory, memento);
    }
    
}