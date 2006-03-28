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
package org.codehaus.wadi.gridstate.activecluster;

import java.util.Timer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.DestinationMarshaller;
import org.apache.activecluster.impl.DefaultCluster;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activecluster.impl.ReplicatedLocalNode;
import org.apache.activecluster.impl.StateService;
import org.apache.activecluster.impl.StateServiceStub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class CustomClusterFactory extends DefaultClusterFactory {

  protected static final Log log=LogFactory.getLog(CustomClusterFactory.class);

  public CustomClusterFactory(ConnectionFactory connectionFactory) {
    super(connectionFactory);
    System.setProperty("activemq.broker.disable-clean-shutdown", "true");
  }

  protected Cluster createCluster(Connection connection,Session session,String name,Destination groupDestination,DestinationMarshaller marshaller) throws JMSException{
	  String dataDestination = getDataTopicPrefix()+ marshaller.getDestinationName(groupDestination);
	  log.info("Creating cluster group producer on topic: "+groupDestination);
	  MessageProducer producer=createProducer(session,null);
	  producer.setDeliveryMode(getDeliveryMode());
	  log.info("Creating cluster data producer on data destination: "+dataDestination);
	  Topic dataTopic=session.createTopic(dataDestination);
	  MessageProducer keepAliveProducer=session.createProducer(dataTopic);
	  keepAliveProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	  StateService serviceStub=new StateServiceStub(session,keepAliveProducer,marshaller);
	  Destination localInboxDestination=session.createTopic(dataDestination+"."+name);
	  ReplicatedLocalNode localNode=new ReplicatedLocalNode(name,localInboxDestination,serviceStub);
	  Timer timer=new Timer();
	  DefaultCluster answer=new CustomCluster(localNode,dataTopic,groupDestination,marshaller,connection,session,producer,timer,getInactiveTime());
	  return answer;
  }
  
}
