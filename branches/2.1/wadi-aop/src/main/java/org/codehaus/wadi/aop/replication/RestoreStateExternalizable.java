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

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.InFlyInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.WireMarshaller;
import org.codehaus.wadi.aop.util.ClusteredStateHelper;


/**
 * 
 * @version $Revision: 1538 $
 */
public class RestoreStateExternalizable implements Externalizable {
    private final InstanceRegistry instanceRegistry;
    private final WireMarshaller marshaller;
    private ClusteredStateSessionMemento memento;

    public RestoreStateExternalizable(InstanceRegistry instanceRegistry, WireMarshaller marshaller) {
        if (null == instanceRegistry) {
            throw new IllegalArgumentException("instanceRegistry is required");
        } else if (null == marshaller) {
            throw new IllegalArgumentException("marshaller is required");
        }
        this.instanceRegistry = instanceRegistry;
        this.marshaller = marshaller;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int length = in.readInt();
        byte[] serInstTracker = new byte[length];
        in.readFully(serInstTracker);
        
        InFlyInstanceRegistry inFlyInstanceRegistry = new InFlyInstanceRegistry(instanceRegistry);
        ClusteredStateHelper.deserialize(inFlyInstanceRegistry, marshaller, serInstTracker);
        inFlyInstanceRegistry.merge();
        
        String instanceId = in.readUTF();
        memento = (ClusteredStateSessionMemento) instanceRegistry.getInstance(instanceId);
    }

    public ClusteredStateSessionMemento getMemento() {
        return memento;
    }

}
