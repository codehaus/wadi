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

import java.util.Timer;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.activecluster.LocalNode;
import org.activecluster.impl.DefaultCluster;
import org.codehaus.wadi.ExtendedCluster;

public class CustomCluster extends DefaultCluster implements ExtendedCluster {
    
    Connection _connection;
    
    public CustomCluster(final LocalNode localNode, Topic dataTopic, Topic destination, Connection connection, Session session, MessageProducer producer, Timer timer, long inactiveTime) throws JMSException {
    	super(localNode, dataTopic, destination, connection, session, producer, timer, inactiveTime);
        _connection=connection; // remember it here, we cannot fetch it from super because it is private :-(
    }

    public Destination createQueue(String name) throws JMSException {
    	return getSession().createQueue(name);
    }
    
    public Connection getConnection() {return _connection;}
}