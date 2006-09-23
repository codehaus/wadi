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
import org.codehaus.wadi.group.Message;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMMessage implements Message {
    private Serializable payload;
    private Address replyTo;
    private Address address;
    private String incomingCorrelationId;
    private String outgoingCorrelationId;
    private Map properties;

    public VMMessage() {
        properties = new HashMap();
    }

    public VMMessage(VMMessage prototype) {
        this.payload = prototype.payload;
        this.replyTo = prototype.replyTo;
        this.address = prototype.address;
        this.incomingCorrelationId = prototype.incomingCorrelationId;
        this.outgoingCorrelationId = prototype.outgoingCorrelationId;
        this.properties = prototype.properties;
    }
    
    public String getTargetCorrelationId() {
        return incomingCorrelationId;
    }

    public void setTargetCorrelationId(String correlationId) {
        this.incomingCorrelationId = correlationId;
    }

    public String getSourceCorrelationId() {
        return outgoingCorrelationId;
    }

    public void setSourceCorrelationId(String correlationId) {
        this.outgoingCorrelationId = correlationId;
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

    public String toString() {
        return "VMMessage: payload=" + payload;
    }

    public Map getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
}
