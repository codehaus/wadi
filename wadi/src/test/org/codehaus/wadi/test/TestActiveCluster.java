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
package org.codehaus.wadi.test;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import junit.framework.TestCase;

import org.activecluster.Cluster;
import org.activecluster.ClusterFactory;
import org.activecluster.impl.ActiveMQClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.RestartableClusterFactory;


public class TestActiveCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

//    public void testRestart() throws Exception {
//        testRestart("vm://localhost");
//        testRestart("peer://WADI-TEST");
//        testRestart("multicast://224.1.2.3:5123");
//        testRestart("tcp://localhost:61616");
////      testRestart("jgroups:default");
//    }
//    
//    public void testRestart(String protocol) throws Exception {
//        ActiveMQConnectionFactory connectionFactory=new ActiveMQConnectionFactory(protocol);
//        connectionFactory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter())); // peer protocol seems to ignore this...
//        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
//        //ClusterFactory clusterFactory=new RestartableClusterFactory(new CustomClusterFactory(connectionFactory));
//        ClusterFactory clusterFactory=new ActiveMQClusterFactory(connectionFactory);
//        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
//        Cluster cluster=clusterFactory.createCluster(clusterName);
//
//        cluster.start();
//        cluster.stop();
//        cluster.start();
//        cluster.stop();
//    }
    
    protected int _numMessages=0;
    
    public class MyMessageListener implements MessageListener {
        
        public void onMessage(Message message) {
            try {
                if (message instanceof BytesMessage) {
                    BytesMessage bm=(BytesMessage)message;
                    _log.info("got the message: "+bm);
                    assertTrue(bm.getBooleanProperty("foo"));
                    _numMessages++;
                }
            } catch (JMSException e) {
                _log.error("something went wrong", e);
            }
        }
        
    }
    
    public void testProperties() throws Exception {
        ActiveMQConnectionFactory connectionFactory=new ActiveMQConnectionFactory("tcp://localhost:61616");
        ActiveMQClusterFactory clusterFactory=new ActiveMQClusterFactory(connectionFactory);
        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
        Cluster cluster0=clusterFactory.createCluster(clusterName);
        Cluster cluster1=clusterFactory.createCluster(clusterName);
        
        Destination destination1=cluster1.getDestination();
        MessageConsumer consumer1=cluster1.createConsumer(destination1, null, false);
        MessageListener listener1=new MyMessageListener();
        consumer1.setMessageListener(listener1);
        
        cluster0.start();
        cluster1.start();
        
        BytesMessage message=cluster0.createBytesMessage();
        message.writeObject("hello");
        message.setBooleanProperty("foo", true);
        cluster0.send(cluster0.getDestination(), message);
        
        Thread.sleep(1000);
        assertTrue(_numMessages==1);
    }
    
//    public void testClusterCompletion() throws Exception {
//        testClusterCompletion("peer://WADI-TEST");
//        testClusterCompletion("multicast://224.1.2.3:5123");
//        testClusterCompletion("tcp://localhost:61616");
////      testClusterCompletion("jgroups:default");
//        }
//     
//    public void testClusterCompletion(String protocol) throws Exception {
//
//        ActiveMQConnectionFactory connectionFactory=new ActiveMQConnectionFactory(protocol);
//        ActiveMQClusterFactory clusterFactory=new ActiveMQClusterFactory(connectionFactory);
//        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
//        Cluster cluster0=clusterFactory.createCluster(clusterName);
//        Cluster cluster1=clusterFactory.createCluster(clusterName);
//
//        cluster0.start();
//        cluster1.start();
//        
//        cluster1.waitForClusterToComplete(1, 6000);
//
//        Thread.sleep(6000);
//        
//        _log.info("stopping");
//        cluster1.stop();
//        cluster0.stop();
//    }

}
