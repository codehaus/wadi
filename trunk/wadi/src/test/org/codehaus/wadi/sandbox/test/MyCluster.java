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
package org.codehaus.wadi.sandbox.test;

import java.util.Timer;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.codehaus.activecluster.LocalNode;
import org.codehaus.activecluster.impl.DefaultCluster;

public class MyCluster extends DefaultCluster {
    public MyCluster(final LocalNode localNode, Topic dataTopic, Topic destination, Connection connection, Session session, MessageProducer producer, Timer timer, long inactiveTime) throws JMSException {
    	super(localNode, dataTopic, destination, connection, session, producer, timer, inactiveTime);
    }

    public Destination createQueue(String name) throws JMSException {
    	return getSession().createQueue(name);
    }
}