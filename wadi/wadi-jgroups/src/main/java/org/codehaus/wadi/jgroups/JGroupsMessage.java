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
import org.codehaus.wadi.group.Message;
import org.jgroups.Address;

/**
 * A WADI Message mapped onto JGroups
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1647 $
 */
public class JGroupsMessage implements Message, Serializable {

    protected static String _prefix="<"+Utils.basename(JGroupsMessage.class)+": ";
    protected static String _suffix=">";

    protected transient JGroupsCluster _cluster;
    protected transient Address _replyTo;
    protected transient Address _address;

    protected String _sourceCorrelationId;
    protected String _targetCorrelationId;
    protected Serializable _payload;

    // 'java.lang.Object' API

    public String toString() {
        return _prefix+_payload+_suffix;
    }

    // 'org.codehaus.wadi.group.Message' API

    public String getTargetCorrelationId() {
        return _targetCorrelationId;
    }

    public void setTargetCorrelationId(String correlationId) {
        _targetCorrelationId=correlationId;
    }

    public String getSourceCorrelationId() {
        return _sourceCorrelationId;
    }

    public void setSourceCorrelationId(String correlationId) {
        _sourceCorrelationId=correlationId;
    }

    public org.codehaus.wadi.group.Address getReplyTo() {
        return (JGroupsPeer)_cluster.getPeer(_replyTo);
    }

    public void setReplyTo(org.codehaus.wadi.group.Address replyTo) {
        _replyTo=((JGroupsPeer)replyTo).getJGAddress();
    }

    public org.codehaus.wadi.group.Address getAddress() {
        return (JGroupsPeer)_cluster.getPeer(_address);
    }

    public void setAddress(org.codehaus.wadi.group.Address address) {
        _address=((JGroupsPeer)address).getJGAddress();
    }

    public void setPayload(Serializable payload) {
        _payload=payload;
    }

    public Serializable getPayload() {
        return _payload;
    }

    // 'org.wadi.codehaus.jgroups.JGroupsMessage' API

    public void setCluster(JGroupsCluster cluster) {
        _cluster=cluster;
    }

}
