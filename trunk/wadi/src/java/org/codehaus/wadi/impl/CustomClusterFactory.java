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
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.activecluster.Cluster;
import org.activecluster.impl.DefaultCluster;
import org.activecluster.impl.DefaultClusterFactory;
import org.activecluster.impl.ReplicatedLocalNode;
import org.activecluster.impl.StateService;
import org.activecluster.impl.StateServiceStub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CustomClusterFactory extends DefaultClusterFactory {

  protected static final Log log=LogFactory.getLog(CustomClusterFactory.class);

  public CustomClusterFactory(ConnectionFactory connectionFactory) {
    super(connectionFactory);
    System.setProperty("activemq.broker.disable-clean-shutdown", "true");
  }

  protected Cluster createCluster(Connection connection, Session session, Topic groupDestination) throws JMSException {
    Topic dataTopic = session.createTopic(getDataTopicPrefix() + groupDestination.getTopicName());

    log.info("Creating cluster group producer on topic: " + groupDestination);

    MessageProducer producer = createProducer(session, null);
    producer.setDeliveryMode(getDeliveryMode());

    log.info("Creating cluster data producer on topic: " + dataTopic);

    MessageProducer keepAliveProducer = session.createProducer(dataTopic);
    keepAliveProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    StateService serviceStub = new StateServiceStub(session, keepAliveProducer);

    Destination localInbox = null;
    if (isUseQueueForInbox()) {
      localInbox = session.createTemporaryQueue();
    }
    else {
      localInbox = session.createTemporaryTopic();
    }
    ReplicatedLocalNode localNode = new ReplicatedLocalNode(localInbox, serviceStub);
    Timer timer = new Timer();
    DefaultCluster answer = new CustomCluster(localNode, dataTopic, groupDestination, connection, session, producer, timer, getInactiveTime());

//     connection.setExceptionListener(new ExceptionListener() {

//         // could we check the exception's cause and step up the level if it is
//         // not harmless ?... - TODO
//         public void onException(JMSException e) {
//             log.trace("JMS Exception:", e);
//         }

//     });

    return answer;
  }
}
