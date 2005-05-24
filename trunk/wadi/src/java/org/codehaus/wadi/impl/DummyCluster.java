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
package org.codehaus.wadi.impl;

import java.io.Serializable;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.activecluster.ClusterListener;
import org.activecluster.LocalNode;
import org.codehaus.wadi.ExtendedCluster;

public class DummyCluster implements ExtendedCluster {

    public DummyCluster() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Destination createQueue(String name) throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public Topic getDestination() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addClusterListener(ClusterListener listener) {
        // TODO Auto-generated method stub

    }

    public void removeClusterListener(ClusterListener listener) {
        // TODO Auto-generated method stub

    }

    public LocalNode getLocalNode() {
        // TODO Auto-generated method stub
        return null;
    }

    public void send(Destination destination, Message message)
            throws JMSException {
        // TODO Auto-generated method stub

    }

    public MessageConsumer createConsumer(Destination destination)
            throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public MessageConsumer createConsumer(Destination destination,
            String selector) throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public MessageConsumer createConsumer(Destination destination,
            String selector, boolean noLocal) throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public Message createMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public BytesMessage createBytesMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public MapMessage createMapMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectMessage createObjectMessage(Serializable object)
            throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public StreamMessage createStreamMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public TextMessage createTextMessage() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean waitForClusterToComplete(int expectedCount, long timeout)
            throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    public void start() throws JMSException {
        // TODO Auto-generated method stub

    }

    public void stop() throws JMSException {
        // TODO Auto-generated method stub

    }

}
