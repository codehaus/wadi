/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.jgroups;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Quipu;

/**
 * A WADI Message mapped onto JGroups
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1647 $
 */
public class JGroupsEnvelope implements Envelope, Serializable {

    protected transient JGroupsCluster _cluster;
    protected JGroupsPeer _replyTo;
    protected JGroupsPeer _address;
    protected String _sourceCorrelationId;
    protected String _targetCorrelationId;
    protected Serializable _payload;
    private final Map properties = new HashMap();
    private transient Quipu quipu;

    public String getTargetCorrelationId() {
        return _targetCorrelationId;
    }

    public void setTargetCorrelationId(String correlationId) {
        _targetCorrelationId = correlationId;
    }

    public String getSourceCorrelationId() {
        return _sourceCorrelationId;
    }

    public void setSourceCorrelationId(String correlationId) {
        _sourceCorrelationId = correlationId;
    }

    public Address getReplyTo() {
        return _replyTo;
    }

    public void setReplyTo(Address replyTo) {
        _replyTo = (JGroupsPeer) replyTo;
    }

    public Address getAddress() {
        return _address;
    }

    public void setAddress(Address address) {
        _address = (JGroupsPeer) address;
    }

    public void setPayload(Serializable payload) {
        _payload = payload;
    }

    public Serializable getPayload() {
        return _payload;
    }

    public void setCluster(JGroupsCluster cluster) {
        _cluster = cluster;
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

    public Quipu getQuipu() {
        return quipu;
    }

    public void setQuipu(Quipu quipu) {
        this.quipu = quipu;
        _sourceCorrelationId = quipu.getCorrelationId();
    }

}
