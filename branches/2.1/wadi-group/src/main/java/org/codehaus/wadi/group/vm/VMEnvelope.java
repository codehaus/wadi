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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Quipu;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMEnvelope implements Envelope {
    private Serializable payload;
    private Address replyTo;
    private Address address;
    private String targetCorrelationId;
    private String sourceCorrelationId;
    private Map<String, Object> properties;
    private transient Quipu quipu;

    public VMEnvelope() {
        properties = new HashMap<String, Object>();
    }

    public VMEnvelope(VMEnvelope prototype) {
        this.payload = prototype.payload;
        this.replyTo = prototype.replyTo;
        this.address = prototype.address;
        this.targetCorrelationId = prototype.targetCorrelationId;
        this.sourceCorrelationId = prototype.sourceCorrelationId;
        this.properties = prototype.properties;
    }
    
    public String getTargetCorrelationId() {
        return targetCorrelationId;
    }

    public void setTargetCorrelationId(String correlationId) {
        this.targetCorrelationId = correlationId;
    }

    public String getSourceCorrelationId() {
        return sourceCorrelationId;
    }

    public void setSourceCorrelationId(String correlationId) {
        this.sourceCorrelationId = correlationId;
    }

    public Address getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Address replyTo) {
        this.replyTo = replyTo;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setPayload(Serializable payload) {
        this.payload = payload;
    }

    public Serializable getPayload() {
        return payload;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Quipu getQuipu() {
        return quipu;
    }
    
    public void setQuipu(Quipu quipu) {
        this.quipu = quipu;
        sourceCorrelationId = quipu.getCorrelationId();
    }
    
    public String toString() {
        return "Envelope: payload=[" + payload + "], targetCorrelationId [" + targetCorrelationId + 
            "], sourceCorrelationId [" + sourceCorrelationId + "";
    }

}
