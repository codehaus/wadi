/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.activecluster.ClusterEvent;
import org.codehaus.activecluster.ClusterListener;

/**
 * Test ActiveCluster, ActiveMQ, with an eye to putting WADI on top of
 * them.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestCluster
  extends TestCase
{
  protected Log         _log=LogFactory.getLog(TestCluster.class);

  public TestCluster(String name)
  {
    super(name);
  }

  protected void
    setUp()
    throws Exception
  {
  }

  protected void
    tearDown()
    throws InterruptedException
  {
  }

  //----------------------------------------

  class MyClusterListener
    implements ClusterListener
  {
    public void
      onNodeAdd(ClusterEvent ce)
    {
      _log.info("node added");
    }

    public void
      onNodeRemove(ClusterEvent ce)
    {
      _log.info("node removed");
    }

    public void
      onNodeUpdate(ClusterEvent ce)
    {
      _log.info("node updated");
    }
  }

  public void
    testCluster()
    throws Exception
  {
    TopicConnectionFactory tcf=new ActiveMQConnectionFactory();
    TopicConnection tc=tcf.createTopicConnection();
    TopicSession ts=tc.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
    Topic topic=ts.createTopic("wadi-global");

    ClusterFactory dcf=new DefaultClusterFactory(tc);
    Cluster cluster=dcf.createCluster(topic);
    cluster.addClusterListener(new MyClusterListener());

    assertTrue(true);
  }
}
