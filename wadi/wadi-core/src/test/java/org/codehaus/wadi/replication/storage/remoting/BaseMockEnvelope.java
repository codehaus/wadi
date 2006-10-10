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
package org.codehaus.wadi.replication.storage.remoting;

import java.io.Serializable;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;

/**
 * 
 * @version $Revision: 1603 $
 */
public class BaseMockEnvelope implements Envelope {

    public String getTargetCorrelationId() {
        throw new UnsupportedOperationException();
    }

    public void setTargetCorrelationId(String correlationId) {
        throw new UnsupportedOperationException();
    }

    public String getSourceCorrelationId() {
        throw new UnsupportedOperationException();
    }

    public void setSourceCorrelationId(String correlationId) {
        throw new UnsupportedOperationException();
    }

    public Address getReplyTo() {
        throw new UnsupportedOperationException();
    }

    public void setReplyTo(Address replyTo) {
        throw new UnsupportedOperationException();
    }

    public Address getAddress() {
        throw new UnsupportedOperationException();
    }

    public void setAddress(Address address) {
        throw new UnsupportedOperationException();
    }

    public void setPayload(Serializable payload) {
        throw new UnsupportedOperationException();
    }

    public Serializable getPayload() {
        throw new UnsupportedOperationException();
    }

    public Map getProperties() {
        throw new UnsupportedOperationException();
    }

    public Object getProperty(String key) {
        throw new UnsupportedOperationException();
    }

    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException();
    }
}