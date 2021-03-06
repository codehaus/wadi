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
package org.codehaus.wadi.group.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.impl.AbstractCluster;

/**
 * 
 * @version $Revision: 1603 $
 */
public class SerializeMessageTransformer implements EnvelopeTransformer {
    private final VMBroker cluster;
    
    public SerializeMessageTransformer(VMBroker cluster) {
        this.cluster = cluster;
    }

    public Envelope transform(Envelope envelope) {
        Serializable payload = envelope.getPayload();
        if (null == payload) {
            return envelope;
        }
        
        try {
            ByteArrayOutputStream memOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(memOut);
            out.writeObject(payload);
            out.close();

            ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(memIn);
            payload = (Serializable) in.readObject();
            in.close();
        } catch (IOException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        } catch (ClassNotFoundException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
        
        VMEnvelope clone = new VMEnvelope((VMEnvelope) envelope);
        clone.setPayload(payload);
        
        return clone;
    }
}
