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
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activecluster.impl.ReplicatedLocalNode;
import org.codehaus.activecluster.impl.StateService;
import org.codehaus.activecluster.impl.StateServiceStub;

public class MyClusterFactory extends DefaultClusterFactory {
	public MyClusterFactory(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}

    protected Cluster createCluster(Connection connection, Session session, Topic groupDestination) throws JMSException {
        Topic dataTopic=session.createTopic(getDataTopicPrefix()+groupDestination.getTopicName());
        MessageProducer producer=createProducer(session, null);
        producer.setDeliveryMode(getDeliveryMode());
        MessageProducer keepAliveProducer=session.createProducer(dataTopic);
        keepAliveProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        StateService serviceStub=new StateServiceStub(session, keepAliveProducer);
        Destination localInbox=null;
        if (isUseQueueForInbox()) {
            localInbox=session.createTemporaryQueue();
        } else {
            localInbox=session.createTemporaryTopic();
        }
        ReplicatedLocalNode localNode=new ReplicatedLocalNode(localInbox, serviceStub);
        Timer timer=new Timer();
        MyCluster answer=new MyCluster(localNode, dataTopic, groupDestination, connection, session, producer, timer, getInactiveTime());
        return answer;
    }
}