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

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import junit.framework.TestCase;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterEvent;
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
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

//    protected int _numMessages=0;
//
//    public class MyMessageListener implements MessageListener {
//
//        public void onMessage(Message message) {
//            try {
//                if (message instanceof BytesMessage) {
//                    BytesMessage bm=(BytesMessage)message;
//                    _log.info("got the message: "+bm);
//                    assertTrue(bm.getBooleanProperty("foo"));
//                    _numMessages++;
//                }
//            } catch (JMSException e) {
//                _log.error("something went wrong", e);
//            }
//        }
//
//    }
//
//    public void testProperties() throws Exception {
//        ActiveMQConnectionFactory connectionFactory=new ActiveMQConnectionFactory("tcp://localhost:61616");
//        ActiveMQClusterFactory clusterFactory=new ActiveMQClusterFactory(connectionFactory);
//        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
//        Cluster cluster0=clusterFactory.createCluster(clusterName);
//        Cluster cluster1=clusterFactory.createCluster(clusterName);
//
//        Destination destination1=cluster1.getDestination();
//        MessageConsumer consumer1=cluster1.createConsumer(destination1, null, false);
//        MessageListener listener1=new MyMessageListener();
//        consumer1.setMessageListener(listener1);
//
//        cluster0.start();
//        cluster1.start();
//
//        BytesMessage message=cluster0.createBytesMessage();
//        message.writeObject("hello");
//        message.setBooleanProperty("foo", true);
//        cluster0.send(cluster0.getDestination(), message);
//
//        Thread.sleep(1000);
//        assertTrue(_numMessages==1);
//    }

    public void testClusterCompletion() throws Exception {
        testClusterCompletion("peer://WADI-TEST");
//        testClusterCompletion("multicast://224.1.2.3:5123");
//        testClusterCompletion("tcp://localhost:61616");
//      testClusterCompletion("jgroups:default");
        }

    protected void configureCluster(Cluster cluster, String nodeName) {
        Map state=new HashMap();
        state.put("id", nodeName);
        try {
            cluster.getLocalNode().setState(state);
        } catch (JMSException e){
	  _log.error("could not initialise node state", e);
        }

        cluster.addClusterListener(new ClusterListener() {

            public void onNodeAdd(ClusterEvent event) {
                // TODO Auto-generated method stub

            }

            public void onNodeUpdate(ClusterEvent event) {
                // TODO Auto-generated method stub

            }

            public void onNodeRemoved(ClusterEvent event) {
                // TODO Auto-generated method stub

            }

            public void onNodeFailed(ClusterEvent event) {
                // TODO Auto-generated method stub

            }

            public void onCoordinatorChanged(ClusterEvent event) {
                // TODO Auto-generated method stub

            }

        });
    }

    public void testClusterCompletion(String protocol) throws Exception {

        ActiveMQConnectionFactory connectionFactory=Utils.getConnectionFactory();
        DefaultClusterFactory clusterFactory=new DefaultClusterFactory(connectionFactory);
        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
        Cluster cluster0=clusterFactory.createCluster(clusterName);
        configureCluster(cluster0, "cluster0");
        Cluster cluster1=clusterFactory.createCluster(clusterName);
        configureCluster(cluster0, "cluster1");

        cluster0.start();
        cluster1.start();

        cluster1.waitForClusterToComplete(1, 6000);

        Thread.sleep(20*1000);

	_log.info("stopping");
        cluster1.stop();
        Thread.sleep(20*1000);
        cluster0.stop();
        Thread.sleep(20*1000);
	_log.info("stopped");
    }

}
