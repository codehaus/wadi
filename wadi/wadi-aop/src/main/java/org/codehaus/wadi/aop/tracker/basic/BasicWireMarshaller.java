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
package org.codehaus.wadi.aop.tracker.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.aop.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.util.Streamer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicWireMarshaller implements WireMarshaller {
    private final Streamer streamer;
    private final ClassIndexerRegistry registry;
    private final InstanceAndTrackerReplacer replacer;
    
    public BasicWireMarshaller(Streamer streamer, ClassIndexerRegistry registry, InstanceAndTrackerReplacer replacer) {
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        } else if (null == registry) {
            throw new IllegalArgumentException("registry is required");
        } else if (null == replacer) {
            throw new IllegalArgumentException("replacer is required");
        }
        this.streamer = streamer;
        this.registry = registry;
        this.replacer = replacer;
    }

    public byte[] marshall(ValueUpdaterInfo[] valueUpdaterInfos) {
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();

        try {
            ObjectOutput out = streamer.getOutputStream(memOut);
            
            out.writeInt(valueUpdaterInfos.length);
            
            for (ValueUpdaterInfo valueUpdaterInfo : valueUpdaterInfos) {
                valueUpdaterInfo.writeExternal(out);
            }

            out.close();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        
        return memOut.toByteArray();
    }

    public ValueUpdaterInfo[] unmarshall(byte[] serialized) {
        ByteArrayInputStream memIn = new ByteArrayInputStream(serialized);
        
        ValueUpdaterInfo[] valueUpdaterInfos;
        try {
            ObjectInput in = streamer.getInputStream(memIn);
            int nbValueUpdaterInfo = in.readInt();
            
            valueUpdaterInfos = new ValueUpdaterInfo[nbValueUpdaterInfo];
            for (int i = 0; i < valueUpdaterInfos.length; i++) {
                ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, registry);
                valueUpdaterInfo.readExternal(in);
                valueUpdaterInfos[i] = valueUpdaterInfo;
            }
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        
        return valueUpdaterInfos;
    }

}
