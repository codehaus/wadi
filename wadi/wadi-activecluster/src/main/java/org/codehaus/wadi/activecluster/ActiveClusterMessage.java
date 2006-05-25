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
package org.codehaus.wadi.activecluster;

import java.io.Serializable;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.impl.AbstractCluster;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterMessage implements Message {
    private static final String INCOMING_COR_ID_KEY = "INCOMING_COR_ID_KEY";
    private static final String OUTGOING_COR_ID_KEY = "OUTGOING_COR_ID_KEY";

    static ObjectMessage unwrap(Message message) {
        if (false == message instanceof ActiveClusterMessage) {
            throw new IllegalArgumentException("Expected " + 
                            ActiveClusterMessage.class.getName() +
                            ". Was:" + message.getClass().getName());
        }
        
        return ((ActiveClusterMessage) message).adaptee;
    }
    
    private final ObjectMessage adaptee;

    private Address _address;
    private Address _replyTo;
    
    public ActiveClusterMessage(ObjectMessage adaptee) {
        this.adaptee = adaptee;
    }

    public ActiveClusterMessage(javax.jms.Message message) {
        if (false == message instanceof ObjectMessage) {
            throw new IllegalArgumentException("Expected " +
                            ObjectMessage.class.getName() + 
                            ". Was " + message.getClass().getName());
        }
        
        this.adaptee = (ObjectMessage) message;
    }

    public String getTargetCorrelationId() {
        try {
            return adaptee.getStringProperty(INCOMING_COR_ID_KEY);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public void setTargetCorrelationId(String correlationId) {
        try {
            adaptee.setStringProperty(INCOMING_COR_ID_KEY, correlationId);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }        
    }

    public String getSourceCorrelationId() {
        try {
            return adaptee.getStringProperty(OUTGOING_COR_ID_KEY);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public void setSourceCorrelationId(String correlationId) {
        try {
            adaptee.setStringProperty(OUTGOING_COR_ID_KEY, correlationId);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public Address getReplyTo() {
        if (_replyTo!=null)
            return _replyTo;
        
        try {
            Destination replyTo = adaptee.getJMSReplyTo();
            if (null == replyTo) {
                return null;
            }
            
            return (ActiveClusterPeer)AbstractCluster.get(replyTo);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public void setReplyTo(Address replyTo) {
        _replyTo=replyTo;
        try {
            adaptee.setJMSReplyTo(((ActiveClusterPeer)replyTo).getACDestination());
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public Address getAddress() {
        if (_address!=null)
            return _address;
        
        try {
            return (ActiveClusterPeer)AbstractCluster.get(adaptee.getJMSDestination());
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public void setAddress(Address address) {
        _address=address;
        try {
            adaptee.setJMSDestination(((ActiveClusterPeer)address).getACDestination());
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public void setPayload(Serializable payload) {
        try {
            adaptee.setObject(payload);
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }

    public Serializable getPayload() {
        try {
            return adaptee.getObject();
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
    }
}
